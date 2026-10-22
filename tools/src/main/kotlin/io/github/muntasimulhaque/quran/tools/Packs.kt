package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.sql.Connection

/**
 * Splits the built content database into packs, and describes them in a
 * catalog the app can read.
 *
 * A pack is one unit of content a reader can have, or not have: the core
 * text and layout, a translation, a tafsir, a word list, a script. The app
 * ships the core pack and nothing else; everything else is downloaded on the
 * reader's word, verified by SHA-256, and removable.
 *
 * The pack files are derived from the audited database, so there is exactly
 * one place where content is assembled and one set of gates that guard it.
 */
class Packs(private val root: File) {

    private val content = File(root, "content")
    private val database = File(content, "quran.db")
    private val directory = File(content, "packs")
    private val catalog = File(content, "catalog.json")

    private data class Pack(
        val id: String,
        val type: String,
        val name: String,
        val language: String,
        val credit: String,
        /** The pack's credit in Bangla, read beside the English one by a Bangla interface. */
        val creditBn: String,
        val version: String,
        val shipped: Boolean,
        val ayahs: Int,
    )

    /** The licenses the pack's own datasets carry, as the manifest records them. */
    private fun licenseOf(manifest: Manifest, id: String): String =
        PackSources.licenses(manifest, id).joinToString(SEPARATOR).ifEmpty { NO_LICENSE }

    /** The same licenses in Bangla, for the catalog's second line. */
    private fun licenseOfBn(manifest: Manifest, id: String): String =
        PackSources.licenses(manifest, id, bangla = true).joinToString(SEPARATOR).ifEmpty { NO_LICENSE }

    private val packs = listOf(
        Pack(
            id = "core",
            type = "script",
            name = "Quran text",
            language = "ar",
            credit = "QPC Hafs, word by word, with tajweed; audited against the QUL QPC text",
            creditBn = "QPC \u09b9\u09be\u09ab\u09b8, \u09b6\u09ac\u09cd\u09a6\u09c7 \u09b6\u09ac\u09cd\u09a6\u09c7, \u09a4\u09be\u099c\u09ac\u09c0\u09a6\u09b8\u09b9; QUL-\u098f\u09b0 QPC \u09b2\u09c7\u0996\u09be\u09b0 \u09b8\u09be\u09a5\u09c7 \u09af\u09be\u099a\u09be\u0987\u0995\u09c3\u09a4",
            // The QPC switch replaced the core's content; the version moves
            // so a held core is replaced rather than kept (owner decision, this
            // session).
            version = "2",
            shipped = true,
            ayahs = 6236,
        ),
        Pack(
            id = "translation-saheeh-en",
            type = "translation",
            name = "Saheeh International",
            language = "en",
            credit = "Noor International Center, via QuranEnc",
            creditBn = "Noor International Center, QuranEnc-\u098f\u09b0 \u09ae\u09be\u09a7\u09cd\u09af\u09ae\u09c7",
            version = "1.1.2",
            shipped = false,
            ayahs = 6236,
        ),
        Pack(
            id = "tafsir-ibn-kathir-en",
            type = "tafsir",
            name = "Ibn Kathir",
            language = "en",
            credit = "Tafsir Ibn Kathir, via the Quranic Universal Library",
            creditBn = "\u09a4\u09be\u09ab\u09b8\u09c0\u09b0 \u0987\u09ac\u09a8\u09c7 \u0995\u09be\u09b8\u09c0\u09b0, \u0995\u09c1\u09b0\u0986\u09a8\u09bf\u0995 \u0987\u0989\u09a8\u09bf\u09ad\u09be\u09b0\u09cd\u09b8\u09be\u09b2 \u09b2\u09be\u0987\u09ac\u09cd\u09b0\u09c7\u09b0\u09bf \u09a5\u09c7\u0995\u09c7",
            version = "QUL",
            shipped = false,
            ayahs = 6236,
        ),
        Pack(
            id = "translation-taisirul-quran-bn",
            type = "translation",
            name = "Taisirul Quran",
            language = "bn",
            credit = "Professor Muhammad Mozammel Haque, via the Quranic Universal Library",
            creditBn = "\u09aa\u09cd\u09b0\u09ab\u09c7\u09b8\u09b0 \u09ae\u09c1\u09b9\u09be\u09ae\u09cd\u09ae\u09a6 \u09ae\u09cb\u099c\u09be\u09ae\u09cd\u09ae\u09c7\u09b2 \u09b9\u0995, \u0995\u09c1\u09b0\u0986\u09a8\u09bf\u0995 \u0987\u0989\u09a8\u09bf\u09ad\u09be\u09b0\u09cd\u09b8\u09be\u09b2 \u09b2\u09be\u0987\u09ac\u09cd\u09b0\u09c7\u09b0\u09bf \u09a5\u09c7\u0995\u09c7",
            version = "QUL",
            shipped = false,
            ayahs = 6236,
        ),
        Pack(
            id = "tafsir-ibn-kathir-bn",
            type = "tafsir",
            name = "Ibn Kathir",
            language = "bn",
            credit = "Tafsir Ibn Kathir (Bangla), via the Quranic Universal Library",
            creditBn = "\u09a4\u09be\u09ab\u09b8\u09c0\u09b0 \u0987\u09ac\u09a8\u09c7 \u0995\u09be\u09b8\u09c0\u09b0 (\u09ac\u09be\u0982\u09b2\u09be), \u0995\u09c1\u09b0\u0986\u09a8\u09bf\u0995 \u0987\u0989\u09a8\u09bf\u09ad\u09be\u09b0\u09cd\u09b8\u09be\u09b2 \u09b2\u09be\u0987\u09ac\u09cd\u09b0\u09c7\u09b0\u09bf \u09a5\u09c7\u0995\u09c7",
            version = "QUL",
            shipped = false,
            ayahs = 6236,
        ),
        Pack(
            id = "reciter-minshawi",
            type = "recitation",
            name = "Minshawi",
            language = "ar",
            credit = "Muhammad Siddiq Al-Minshawi, via the Quranic Universal Library",
            creditBn = "\u09ae\u09c1\u09b9\u09be\u09ae\u09cd\u09ae\u09a6 \u09b8\u09bf\u09a6\u09cd\u09a6\u09bf\u0995 \u0986\u09b2-\u09ae\u09bf\u09a8\u09b6\u09be\u09ac\u09bf, \u0995\u09c1\u09b0\u0986\u09a8\u09bf\u0995 \u0987\u0989\u09a8\u09bf\u09ad\u09be\u09b0\u09cd\u09b8\u09be\u09b2 \u09b2\u09be\u0987\u09ac\u09cd\u09b0\u09c7\u09b0\u09bf \u09a5\u09c7\u0995\u09c7",
            version = "QUL",
            shipped = false,
            ayahs = 6236,
        ),
        Pack(
            id = "reciter-husary",
            type = "recitation",
            name = "Husary",
            language = "ar",
            credit = "Mahmoud Khalil Al-Husary, via the Quranic Universal Library",
            creditBn = "\u09ae\u09be\u09b9\u09ae\u09c1\u09a6 \u0996\u09b2\u09bf\u09b2 \u0986\u09b2-\u09b9\u09c1\u09b8\u09be\u09b0\u09bf, \u0995\u09c1\u09b0\u0986\u09a8\u09bf\u0995 \u0987\u0989\u09a8\u09bf\u09ad\u09be\u09b0\u09cd\u09b8\u09be\u09b2 \u09b2\u09be\u0987\u09ac\u09cd\u09b0\u09c7\u09b0\u09bf \u09a5\u09c7\u0995\u09c7",
            version = "QUL",
            shipped = false,
            ayahs = 6236,
        ),
        Pack(
            id = "words-en",
            type = "words",
            name = "Word by word",
            language = "en",
            credit = "Quran.com word by word, via the Quranic Universal Library",
            creditBn = "Quran.com \u09b6\u09ac\u09cd\u09a6\u09c7 \u09b6\u09ac\u09cd\u09a6\u09c7, \u0995\u09c1\u09b0\u0986\u09a8\u09bf\u0995 \u0987\u0989\u09a8\u09bf\u09ad\u09be\u09b0\u09cd\u09b8\u09be\u09b2 \u09b2\u09be\u0987\u09ac\u09cd\u09b0\u09c7\u09b0\u09bf \u09a5\u09c7\u0995\u09c7",
            version = "QUL",
            shipped = false,
            ayahs = 6236,
        ),
        Pack(
            id = "words-bn",
            type = "words",
            name = "Word by word",
            language = "bn",
            credit = "Bangla word by word, via the Quranic Universal Library",
            creditBn = "\u09ac\u09be\u0982\u09b2\u09be \u09b6\u09ac\u09cd\u09a6\u09c7 \u09b6\u09ac\u09cd\u09a6\u09c7, \u0995\u09c1\u09b0\u0986\u09a8\u09bf\u0995 \u0987\u0989\u09a8\u09bf\u09ad\u09be\u09b0\u09cd\u09b8\u09be\u09b2 \u09b2\u09be\u0987\u09ac\u09cd\u09b0\u09c7\u09b0\u09bf \u09a5\u09c7\u0995\u09c7",
            version = "QUL",
            shipped = false,
            ayahs = 6236,
        ),
    )

    fun run(): Int {
        if (!database.exists()) {
            println("packs: content/quran.db is missing; run tools build first")
            return 2
        }
        val manifest = loadManifest(root)
        directory.mkdirs()
        val entries = mutableListOf<JsonObject>()
        openSqlite(database).use { source ->
            for (pack in packs) {
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
        val json = buildJsonObject {
            put("version", 1)
            put("baseUrl", "https://github.com/muntasimulhaque/quran/releases/download")
            put("packs", JsonArray(entries))
        }
        catalog.writeText(
            Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), json),
        )
        println("packs: ${packs.size} pack(s) written, catalog at content/catalog.json")
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

    private fun buildPack(source: Connection, file: File, pack: Pack) {
        openSqlite(file).use { target ->
            target.createStatement().use { statement ->
                statement.execute("PRAGMA journal_mode=OFF")
                statement.execute("PRAGMA synchronous=OFF")
                statement.execute("ATTACH DATABASE '${sourcePath()}' AS src")
            }
            target.autoCommit = false
            target.createStatement().use { statement ->
                when (pack.type) {
                    "script" -> {
                        statement.execute("CREATE TABLE meta AS SELECT * FROM src.meta")
                        statement.execute("CREATE TABLE surah AS SELECT * FROM src.surah")
                        statement.execute("CREATE TABLE ayah AS SELECT * FROM src.ayah")
                        statement.execute(
                            "CREATE TABLE word AS SELECT id, ayah_number, surah, ayah, position, marker, " +
                                "text, text_search FROM src.word",
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
                        createFts(statement, "ayah_fts", "text_search", "ayah")
                    }
                    "translation" -> {
                        statement.execute(
                            "CREATE TABLE translation AS SELECT ayah_number, text, footnotes, text_search " +
                                "FROM src.translation WHERE pack = '${pack.id}'",
                        )
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
                        // report, this session).
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
     * keystroke was the one search a reader could feel (owner decision, this
     * session).
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

    private fun catalogEntry(pack: Pack, file: File, license: String, licenseBn: String): JsonObject {
        val hash = sha256(file)
        val tag = "pack-${pack.id}-${hash.substring(0, 8)}"
        return buildJsonObject {
            put("id", pack.id)
            put("type", pack.type)
            put("name", pack.name)
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
    }
}
