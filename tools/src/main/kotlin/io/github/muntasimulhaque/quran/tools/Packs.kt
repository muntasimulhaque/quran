package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.core.Search
import java.io.File
import java.sql.Connection

/**
 * Splits the built content database into packs, and describes them in a
 * catalog the app can read.
 *
 * A pack is one unit of content a reader can have, or not have: the core
 * text and layout, a translation, a tafsir, a word list, a script. The app
 * ships the core pack, the two translations, and the two reciter timing
 * packs; the recitation audio itself is downloaded per surah on the reader's
 * word, except Al-Fatiha, whose seven ayahs the app carries as assets in
 * the default voice (owner decision, 15.8; the default voice alone, 22.2).
 *
 * The pack files are derived from the audited database, so there is exactly
 * one place where content is assembled and one set of gates that guard it.
 */
class Packs(private val root: File) {

    private val content = File(root, "content")
    private val database = File(content, "quran.db")
    private val directory = File(content, "packs")
    private val catalog = File(content, "catalog.json")

    /** The licenses the pack's own datasets carry, as the manifest records them. */
    private fun licenseOf(manifest: Manifest, id: String): String =
        PackSources.licenses(manifest, id).joinToString(SEPARATOR).ifEmpty { NO_LICENSE }

    /** The same licenses in Bangla, for the catalog's second line. */
    private fun licenseOfBn(manifest: Manifest, id: String): String =
        PackSources.licenses(manifest, id, bangla = true).joinToString(SEPARATOR).ifEmpty { NO_LICENSE }

    /**
     * The catalog's `defaults` block, with every id checked against the packs
     * this run writes. A default that names no pack is a typo that would
     * leave a language reading the wrong content or none, so it stops the
     * build here rather than reaching a reader (owner decision, 20.2).
     */
    private fun defaultsJson(ids: Set<String>): JsonObject = buildJsonObject {
        for (row in PackSources.languageDefaults) {
            for (id in listOf(row.translation, row.tafsir, row.words)) {
                check(id in ids) { "packs: the ${row.tag} defaults name $id, which is not a pack" }
            }
            put(
                row.tag,
                buildJsonObject {
                    put("translation", row.translation)
                    put("tafsir", row.tafsir)
                    put("words", row.words)
                },
            )
        }
    }

    fun run(): Int {
        if (!database.exists()) {
            println("packs: content/quran.db is missing; run tools build first")
            return 2
        }
        val manifest = loadManifest(root)
        directory.mkdirs()
        val entries = mutableListOf<JsonObject>()
        openSqlite(database).use { source ->
            for (pack in PackSources.packs) {
                val file = File(directory, "${pack.id}.db")
                if (file.exists()) file.delete()
                buildPack(source, file, pack)
                entries += catalogEntry(
                    pack,
                    file,
                    licenseOf(manifest, pack.id),
                    licenseOfBn(manifest, pack.id),
                )
                println(
                    "packs: ${pack.id} ${file.length() / 1024} KB sha256 ${sha256(file).substring(0, 12)}" +
                        if (pack.shipped) " (shipped)" else "",
                )
            }
        }
        wordAudioEntry(manifest)?.let { entry ->
            entries += entry
            println("packs: ${PackSources.wordAudio.id} ${entry["bytes"]} bytes, tag $WORD_AUDIO_TAG")
        }
        val json = buildJsonObject {
            put("version", 1)
            put("baseUrl", "https://github.com/muntasimulhaque/quran/releases/download")
            put("defaults", defaultsJson(PackSources.all.map { it.id }.toSet()))
            put("packs", JsonArray(entries))
        }
        catalog.writeText(
            Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), json),
        )
        println("packs: ${entries.size} pack(s) written, catalog at content/catalog.json")
        return 0
    }

    /**
     * Uploads every pack to its own content-addressed Release. The tag is
     * derived from the pack's hash, so a pack version is never overwritten
     * and an old catalog entry keeps resolving.
     */
    fun publish(): Int {
        if (!catalog.exists()) {
            println("packs: content/catalog.json is missing; run tools packs build first")
            return 2
        }
        val root = Json.parseToJsonElement(catalog.readText()).jsonObject
        val entries = root["packs"]?.jsonArray ?: JsonArray(emptyList())
        var failures = 0
        for (entry in entries) {
            val pack = entry.jsonObject
            val id = pack["id"]?.jsonPrimitive?.content ?: continue
            if (pack["type"]?.jsonPrimitive?.content == "word-audio") continue
            val tag = pack["tag"]?.jsonPrimitive?.content ?: continue
            val file = File(directory, "$id.db")
            if (!file.exists()) {
                println("packs: $id is missing at ${file.path}")
                failures++
                continue
            }
            val exists = runCatching {
                ProcessBuilder("gh", "release", "view", tag, "--repo", REPO)
                    .redirectErrorStream(true).start().waitFor() == 0
            }.getOrDefault(false)
            if (!exists) {
                println("packs: creating release $tag")
                runGh(listOf("gh", "release", "create", tag, "--repo", REPO, "--title", tag, "--notes", "Content pack $id."))
            }
            // The asset name carries the hash, so a re-upload can never
            // collide with a different build of the same pack.
            val folder = File(directory, id).apply { mkdirs() }
            val asset = File(folder, "$id.db")
            file.copyTo(asset, overwrite = true)
            println("packs: uploading ${asset.name} to $tag")
            val code = runGh(listOf("gh", "release", "upload", tag, "--repo", REPO, asset.path, "--clobber"))
            if (code != 0) failures++
        }
        if (failures != 0) {
            println("packs: $failures pack(s) failed")
            return 1
        }
        println("packs: published")
        return 0
    }

    /**
     * The Saheeh pack is the reader's shape of the publisher's translation.
     *
     * QuranEnc exports the notes numbered across the whole book, and its
     * first note belongs to the surah title, which an ayah-keyed file cannot
     * carry. The pack numbers each surah's notes from one and starts the
     * next surah again at one, the rule the Bangla pack follows and the one
     * the reader's page was asked for (owner decision, 14.6). The
     * same pass corrects the export's one printed slip at 3:32, and drops
     * 7:143's marker whose body the export lost. [SaheehText] holds both
     * rules and their reasons.
     *
     * The database keeps the publisher's shape; this pack is the only place
     * the reader's numbering is written, so a full rebuild of `quran.db`
     * still reproduces its own hash.
     */
    private fun buildSaheehTranslation(target: Connection, packId: String) {
        target.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE translation (ayah_number INTEGER NOT NULL, text TEXT NOT NULL, " +
                    "footnotes TEXT NOT NULL, text_search TEXT NOT NULL)",
            )
        }
        target.prepareStatement(
            "INSERT INTO translation(ayah_number, text, footnotes, text_search) VALUES(?,?,?,?)",
        ).use { insert ->
            target.prepareStatement(
                "SELECT t.ayah_number, t.text, t.footnotes, a.surah " +
                    "FROM src.translation t JOIN src.ayah a ON a.number = t.ayah_number " +
                    "WHERE t.pack = ? ORDER BY t.ayah_number",
            ).use { select ->
                select.setString(1, packId)
                select.executeQuery().use { rs ->
                    var corrected = 0
                    var dropped = 0
                    var notes = 0
                    var surah = 0
                    var surahNotes = 0
                    while (rs.next()) {
                        val number = rs.getInt(1)
                        val source = rs.getString(2) ?: ""
                        val storedNotes = notesFromJson(rs.getString(3) ?: "[]")
                        val ayahSurah = rs.getInt(4)
                        if (ayahSurah != surah) {
                            surah = ayahSurah
                            surahNotes = 0
                        }
                        val correctedText = saheehCorrected(number, source)
                        if (correctedText != source) corrected += 1
                        val local = localizeSaheeh(correctedText, storedNotes, surahNotes)
                        dropped += local.dropped.size
                        notes += local.notes
                        if (local.unused.isNotEmpty()) {
                            error("Saheeh pack: ayah $number has note(s) ${local.unused} with no marker")
                        }
                        insert.setInt(1, number)
                        insert.setString(2, local.text)
                        insert.setString(3, footnotesJson(local.footnotes))
                        // The search index holds the words, never the note
                        // numbers: a reader who types a number is not asking
                        // for the ayahs whose notes carry it.
                        insert.setString(4, Search.normalizeForIndex(RichText.plain(local.text)))
                        insert.addBatch()
                        surahNotes += local.notes
                    }
                    insert.executeBatch()
                    if (corrected != saheehCorrections.size) {
                        error(
                            "Saheeh pack: expected ${saheehCorrections.size} correction(s), " +
                                "applied $corrected",
                        )
                    }
                    if (dropped != 1) {
                        error("Saheeh pack: expected 1 marker without a note, found $dropped")
                    }
                    println(
                        "packs: Saheeh: $notes notes within their surahs, " +
                            "$corrected correction(s), $dropped dead marker(s) dropped",
                    )
                }
            }
        }
    }

    /**
     * The Bangla pack is the reader's shape too. The database numbers each
     * ayah's notes from one, because the QUL row marks them with stars; the
     * pack lets the number climb through the surah and starts the next surah
     * at one, the same rule as Saheeh (owner decision, 14.6). The
     * note bodies and the sentence's markers move together, and the search
     * index keeps its words.
     */
    private fun buildBayanTranslation(target: Connection, packId: String) {
        target.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE translation (ayah_number INTEGER NOT NULL, text TEXT NOT NULL, " +
                    "footnotes TEXT NOT NULL, text_search TEXT NOT NULL)",
            )
        }
        target.prepareStatement(
            "INSERT INTO translation(ayah_number, text, footnotes, text_search) VALUES(?,?,?,?)",
        ).use { insert ->
            target.prepareStatement(
                "SELECT t.ayah_number, t.text, t.footnotes, t.text_search, a.surah " +
                    "FROM src.translation t JOIN src.ayah a ON a.number = t.ayah_number " +
                    "WHERE t.pack = ? ORDER BY t.ayah_number",
            ).use { select ->
                select.setString(1, packId)
                select.executeQuery().use { rs ->
                    var surah = 0
                    var surahNotes = 0
                    var notes = 0
                    while (rs.next()) {
                        val number = rs.getInt(1)
                        val source = rs.getString(2) ?: ""
                        val storedNotes = notesFromJson(rs.getString(3) ?: "[]")
                        val search = rs.getString(4) ?: ""
                        val ayahSurah = rs.getInt(5)
                        if (ayahSurah != surah) {
                            surah = ayahSurah
                            surahNotes = 0
                        }
                        val expected = (1..storedNotes.size).toList()
                        val markers = Regex("\\[(\\d+)]").findAll(source)
                            .map { it.groupValues[1].toInt() }
                            .toList()
                        if (storedNotes.keys.toList() != expected || markers != expected) {
                            error(
                                "Bangla pack: ayah $number draws $markers for " +
                                    "${storedNotes.size} note(s)",
                            )
                        }
                        val renumbered = LinkedHashMap<Int, String>(storedNotes.size)
                        for ((old, text) in storedNotes) renumbered[surahNotes + old] = text
                        val footnotes = renumbered.entries.joinToString(" ") { (n, text) ->
                            "[$n]- $text"
                        }
                        insert.setInt(1, number)
                        insert.setString(2, bayanSurahMarkers(source, surahNotes))
                        insert.setString(3, footnotesJson(footnotes))
                        insert.setString(4, search)
                        insert.addBatch()
                        surahNotes += storedNotes.size
                        notes += storedNotes.size
                    }
                    insert.executeBatch()
                    println("packs: Bangla: $notes notes within their surahs")
                }
            }
        }
    }

    private fun buildPack(source: Connection, file: File, pack: PackSources.Pack) {
        openSqlite(file).use { target ->
            target.createStatement().use { statement ->
                statement.execute("PRAGMA journal_mode=OFF")
                statement.execute("PRAGMA synchronous=OFF")
                target.prepareStatement("ATTACH DATABASE ? AS src").use { attach ->
                    attach.setString(1, sourcePath())
                    attach.execute()
                }
            }
            target.autoCommit = false
            target.createStatement().use { statement ->
                when (pack.type) {
                    "script" -> {
                        statement.execute("CREATE TABLE meta AS SELECT * FROM src.meta")
                        // The recitation map, one row per voice's audio
                        // folder, so the app can name any voice's files
                        // without attaching that voice's timing pack. SQLite
                        // allows ten attached databases, and a library that
                        // held the core, both translations, both tafsirs,
                        // both word lists, the root, and both voices would
                        // cross the ceiling, so the map is what keeps the
                        // unchosen voice's files nameable without its pack
                        // (owner decision, 20.0).
                        statement.execute("CREATE TABLE recitation AS SELECT * FROM src.recitation")
                        statement.execute("CREATE TABLE surah AS SELECT * FROM src.surah")
                        statement.execute("CREATE TABLE ayah AS SELECT * FROM src.ayah")
                        statement.execute(
                            "CREATE TABLE word AS SELECT id, ayah_number, surah, ayah, position, marker, " +
                                "text, text_search, text_skeleton FROM src.word",
                        )
                        // The core pack is the one place the running ayah number had
                        // no index of its own: every other pack indexes its
                        // `ayah_number`, and the index this replaces led on
                        // the surah and the ayah inside it, which no query in
                        // the app reads. Opening a surah was a whole pass
                        // over the eighty thousand rows of this table, 11.2
                        // ms however few ayahs were asked for, and a search's
                        // read of the matched words back was another 10.2
                        // ms. With this index both land under a tenth of a
                        // millisecond. See `Build` for the same index on the
                        // library the tools read.
                        statement.execute("CREATE INDEX word_ayah ON word(ayah_number, position)")
                        statement.execute("CREATE INDEX ayah_surah ON ayah(surah, ayah)")
                        // A table made by SELECT loses its primary key, so the
                        // running number needs an index of its own: an ayah
                        // card, a playback read, and the FTS join all look a
                        // row up by it.
                        statement.execute("CREATE INDEX ayah_number ON ayah(number)")
                        // The introductions are core content: every reader
                        // meets them, whether or not a word list is on the
                        // device. They used to travel only with words-en, so
                        // a fresh install and a Bangla word list had no door
                        // to them at all.
                        statement.execute("CREATE TABLE surah_info AS SELECT * FROM src.surah_info")
                        createFts(statement, "ayah_fts", "text_search", "ayah")
                        // The recall index: the print's long vowels and hamza
                        // seat removed, so a modern spelling meets a word the
                        // print writes another way. The app asks it only when
                        // the exact index names no ayah (owner decision, 19.7).
                        createFts(statement, "ayah_skeleton_fts", "text_skeleton", "ayah")
                    }
                    "translation" -> {
                        when (pack.id) {
                            "translation-saheeh-en" -> buildSaheehTranslation(target, pack.id)
                            "translation-al-bayan-bn" -> buildBayanTranslation(target, pack.id)
                            else -> statement.execute(
                                "CREATE TABLE translation AS SELECT ayah_number, text, footnotes, text_search " +
                                    "FROM src.translation WHERE pack = '${pack.id}'",
                            )
                        }
                        statement.execute("CREATE INDEX translation_ayah ON translation(ayah_number)")
                        createFts(statement, "translation_fts", "text_search", "translation")
                    }
                    "tafsir" -> {
                        statement.execute(
                            "CREATE TABLE tafsir_passage AS SELECT source_id, surah, from_ayah, to_ayah, text, " +
                                "text_search FROM src.tafsir_passage WHERE pack = '${pack.id}'",
                        )
                        statement.execute(
                            "CREATE TABLE tafsir_ayah AS SELECT ayah_number, passage_id FROM src.tafsir_ayah " +
                                "WHERE pack = '${pack.id}'",
                        )
                        statement.execute("CREATE INDEX tafsir_ayah_ayah ON tafsir_ayah(ayah_number)")
                        statement.execute("CREATE INDEX tafsir_ayah_passage ON tafsir_ayah(passage_id)")
                        // A matched passage's text is read back by its own id,
                        // and the table is tens of megabytes of prose: without
                        // this index the read is a whole scan of the pack,
                        // which is what made a warm tafsir search slow even
                        // after the search itself became an index read (owner
                        // report, 6.5).
                        statement.execute("CREATE INDEX tafsir_passage_source ON tafsir_passage(source_id)")
                        createFts(statement, "tafsir_passage_fts", "text_search", "tafsir_passage")
                    }
                    "words" -> {
                        statement.execute(
                            "CREATE TABLE word_meaning AS SELECT word_id, ayah_number, position, meaning, " +
                                "meaning_search FROM src.word_meaning WHERE language = '${pack.language}'",
                        )
                        statement.execute("CREATE INDEX word_meaning_ayah ON word_meaning(ayah_number, position)")
                        createFts(statement, "word_meaning_fts", "meaning_search", "word_meaning")
                        // The word's grammar and lemma travel with the list
                        // that opens the sheet, so the reader who turns word
                        // meanings on gets the study lines in the same
                        // download (owner decision, 18.0).
                        statement.execute(
                            "CREATE TABLE grammar_lemma AS SELECT id, text, words_count, forms_count " +
                                "FROM src.grammar_lemma",
                        )
                        statement.execute(
                            "CREATE TABLE word_grammar AS SELECT ayah_number, position, lemma_id, pos, " +
                                "inflection, form, passive, pattern FROM src.word_grammar",
                        )
                        // The places page reads one dictionary form's words
                        // across the Book; the index makes that read one
                        // lookup. A pack built before it answers the same
                        // rows through a scan (owner decision, 18.9).
                        statement.execute(
                            "CREATE INDEX word_grammar_lemma ON word_grammar(lemma_id)",
                        )
                        // The surah introductions travel with the English list,
                        // so a second language does not carry them twice.
                        if (pack.language == "en") {
                            statement.execute("CREATE TABLE surah_info AS SELECT * FROM src.surah_info")
                        }
                    }
                    "recitation" -> {
                        val reciter = pack.id.removePrefix("reciter-")
                        statement.execute(
                            "CREATE TABLE recitation AS SELECT id, name, credit FROM src.recitation " +
                                "WHERE id = '$reciter'",
                        )
                        statement.execute(
                            "CREATE TABLE recitation_ayah AS SELECT ayah_number, audio_path, segments " +
                                "FROM src.recitation_ayah WHERE recitation = '$reciter'",
                        )
                        statement.execute("CREATE INDEX recitation_ayah_ayah ON recitation_ayah(ayah_number)")
                    }
                    "word-root" -> {
                        // The count is the pack's own rows, not the source's
                        // field: the number the sheet names and the places
                        // the door opens are one number. The source's field
                        // disagreed for fourteen roots, and the rows are what
                        // the reader sees (owner decision, 18.9).
                        statement.execute(
                            "CREATE TABLE word_root AS SELECT r.ayah_number, r.position, r.arabic, " +
                                "r.latin, c.words_count FROM src.word_root r " +
                                "JOIN (SELECT arabic, COUNT(*) AS words_count FROM src.word_root " +
                                "GROUP BY arabic) c ON c.arabic = r.arabic",
                        )
                        statement.execute("CREATE INDEX word_root_ayah ON word_root(ayah_number, position)")
                        // The places page looks a root up by its letters.
                        statement.execute("CREATE INDEX word_root_arabic ON word_root(arabic)")
                    }
                }
            }
            target.commit()
            target.autoCommit = true
        }
    }
    /**
     * The full text index over one pack's searchable column, so a search
     * reads an index instead of scanning every row of the pack. The tafsir
     * alone is tens of megabytes of prose, and scanning it on every settled
     * keystroke was the one search a reader could feel
     * (owner decision, 6.5). 
     *
     * The table is external content: the tokens are indexed, the text stays in
     * the content table, and a query reads the passages it matched rather
     * than a second copy of the pack. The index is built here, from the pack's
     * own audited column, so the content build stays the only place text is
     * assembled.
     */
    private fun createFts(
        statement: java.sql.Statement,
        table: String,
        column: String,
        content: String,
    ) {
        statement.execute(
            "CREATE VIRTUAL TABLE $table USING fts4($column, content='$content')",
        )
        statement.execute(
            "INSERT INTO $table(docid, $column) SELECT rowid, $column FROM $content",
        )
    }

    private fun catalogEntry(
        pack: PackSources.Pack,
        file: File,
        license: String,
        licenseBn: String,
    ): JsonObject {
        val hash = sha256(file)
        val tag = "pack-${pack.id}-${hash.substring(0, 8)}"
        return buildJsonObject {
            put("id", pack.id)
            put("type", pack.type)
            put("name", pack.name)
            if (pack.nameBn.isNotBlank()) put("nameBn", pack.nameBn)
            put("language", pack.language)
            put("credit", pack.credit)
            if (pack.creditBn.isNotBlank()) put("creditBn", pack.creditBn)
            put("license", license)
            if (licenseBn.isNotBlank()) put("licenseBn", licenseBn)
            put("version", pack.version)
            put("shipped", pack.shipped)
            put("ayahs", pack.ayahs)
            put("bytes", file.length())
            put("sha256", hash)
            put("tag", tag)
            put("url", "https://github.com/muntasimulhaque/quran/releases/download/$tag/${pack.id}.db")
        }
    }

    private fun wordAudioEntry(manifest: Manifest): JsonObject? {
        val file = File(content, "word-audio/manifest.json")
        if (!file.isFile) return null
        val header = runCatching { Json.parseToJsonElement(file.readText()).jsonObject }.getOrNull()
            ?: return null
        val total = header["totalBytes"]?.jsonPrimitive?.longOrNull ?: 0L
        val hash = sha256(file)
        return buildJsonObject {
            put("id", PackSources.wordAudio.id)
            put("type", PackSources.wordAudio.type)
            put("name", PackSources.wordAudio.name)
            put("language", PackSources.wordAudio.language)
            put("credit", PackSources.wordAudio.credit)
            put("creditBn", PackSources.wordAudio.creditBn)
            put("license", licenseOf(manifest, PackSources.wordAudio.id))
            put("licenseBn", licenseOfBn(manifest, PackSources.wordAudio.id))
            put("version", PackSources.wordAudio.version)
            put("shipped", PackSources.wordAudio.shipped)
            put("ayahs", PackSources.wordAudio.ayahs)
            put("bytes", total)
            put("sha256", hash)
            put("tag", WORD_AUDIO_TAG)
        }
    }

    private fun sourcePath(): String = database.absolutePath.replace('\\', '/')

    private fun runGh(command: List<String>): Int = runCatching {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        process.inputStream.bufferedReader().forEachLine { println("gh: $it") }
        process.waitFor()
    }.getOrDefault(1)

    private companion object {
        const val REPO = "muntasimulhaque/quran"

        /** Two licenses of one pack read as one line. */
        const val SEPARATOR = " · "

        /** Only reached when a pack has no dataset recorded. */
        const val NO_LICENSE = "No license recorded"

        /** The release the word pronunciation pack is published to. */
        const val WORD_AUDIO_TAG = "word-audio-alafasy"
    }
}
