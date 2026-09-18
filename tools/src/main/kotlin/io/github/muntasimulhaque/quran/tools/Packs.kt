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
        val license: String,
        val version: String,
        val shipped: Boolean,
        val ayahs: Int,
    )

    private val packs = listOf(
        Pack(
            id = "core",
            type = "script",
            name = "Quran text and page layout",
            language = "ar",
            credit = "KFGQPC Hafs word by word, QPC V2 layout, audited against Tanzil Uthmani",
            license = "See docs/content-sources.md",
            version = "1",
            shipped = true,
            ayahs = 6236,
        ),
        Pack(
            id = "translation-saheeh-en",
            type = "translation",
            name = "Saheeh International",
            language = "en",
            credit = "Noor International Center, via QuranEnc",
            license = "See docs/content-sources.md",
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
            license = "See docs/content-sources.md",
            version = "QUL",
            shipped = false,
            ayahs = 6236,
        ),
        Pack(
            id = "tafsir-as-sadi-ar",
            type = "tafsir",
            name = "As-Sa'di",
            language = "ar",
            credit = "Tafsir As-Sa'di, via QuranEnc",
            license = "See docs/content-sources.md",
            version = "1.0.0",
            shipped = false,
            ayahs = 6236,
        ),
        Pack(
            id = "translation-taisirul-quran-bn",
            type = "translation",
            name = "Taisirul Quran",
            language = "bn",
            credit = "Professor Muhammad Mozammel Haque, via the Quranic Universal Library",
            license = "See docs/content-sources.md",
            version = "QUL",
            shipped = false,
            ayahs = 6236,
        ),
        Pack(
            id = "tafsir-ibn-kathir-bn",
            type = "tafsir",
            name = "Ibn Kathir",
            language = "bn",
            credit = "Tafsir Ibn Kathir (Bengali), via the Quranic Universal Library",
            license = "See docs/content-sources.md",
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
            license = "See docs/content-sources.md",
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
            license = "See docs/content-sources.md",
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
            license = "See docs/content-sources.md",
            version = "QUL",
            shipped = false,
            ayahs = 6236,
        ),
        Pack(
            id = "words-bn",
            type = "words",
            name = "Word by word",
            language = "bn",
            credit = "Bengali word by word, via the Quranic Universal Library",
            license = "See docs/content-sources.md",
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
        directory.mkdirs()
        val entries = mutableListOf<JsonObject>()
        openSqlite(database).use { source ->
            for (pack in packs) {
                val file = File(directory, "${pack.id}.db")
                if (file.exists()) file.delete()
                buildPack(source, file, pack)
                entries += catalogEntry(pack, file)
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
                                "text, glyph, text_search, page, line, line_position FROM src.word",
                        )
                        statement.execute("CREATE TABLE page_line AS SELECT * FROM src.page_line")
                        statement.execute("CREATE INDEX word_ref ON word(surah, ayah, position)")
                        statement.execute("CREATE INDEX word_page ON word(page, line, line_position)")
                        statement.execute("CREATE INDEX ayah_page ON ayah(page)")
                        statement.execute("CREATE INDEX ayah_surah ON ayah(surah, ayah)")
                    }
                    "translation" -> {
                        statement.execute(
                            "CREATE TABLE translation AS SELECT ayah_number, text, footnotes, text_search " +
                                "FROM src.translation WHERE pack = '${pack.id}'",
                        )
                        statement.execute("CREATE INDEX translation_ayah ON translation(ayah_number)")
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
                    }
                    "words" -> {
                        statement.execute(
                            "CREATE TABLE word_meaning AS SELECT word_id, ayah_number, position, meaning, " +
                                "meaning_search FROM src.word_meaning WHERE language = '${pack.language}'",
                        )
                        statement.execute("CREATE INDEX word_meaning_ayah ON word_meaning(ayah_number, position)")
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
    private fun catalogEntry(pack: Pack, file: File): JsonObject {
        val hash = sha256(file)
        val tag = "pack-${pack.id}-${hash.substring(0, 8)}"
        return buildJsonObject {
            put("id", pack.id)
            put("type", pack.type)
            put("name", pack.name)
            put("language", pack.language)
            put("credit", pack.credit)
            put("license", pack.license)
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
    }
}
