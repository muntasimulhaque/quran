package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The word pronunciation pack's manifest, report, and bundled Al-Fatiha.
 *
 * The audio build itself reads `content/raw` and writes the ZIPs, and it is
 * the owner's machine that runs it. What must hold everywhere, including CI,
 * is the shape of what was published: one package for each of the 114 surahs,
 * every word count accounted for, the Al-Fatiha files carried in the app, and
 * the catalog and the manifest naming the same total and the same hash.
 */
class BundledWordAudioTest {

    @Test
    fun theManifestNamesEverySurahAndAccountsForEveryWord() {
        val root = projectRoot()
        val manifest = Json.parseToJsonElement(
            File(root, "content/word-audio/manifest.json").readText(),
        ).jsonObject
        val packs = manifest["packs"]?.jsonArray ?: error("the manifest has no packs")
        assertEquals(114, packs.size)
        val surahs = packs.map {
            it.jsonObject["surah"]?.jsonPrimitive?.longOrNull?.toInt()
        }
        assertEquals((1..114).toSet(), surahs.toSet())

        val total = manifest["totalBytes"]?.jsonPrimitive?.longOrNull ?: 0L
        var bytes = 0L
        var words = 0L
        for (pack in packs) {
            val row = pack.jsonObject
            val surah = row["surah"]?.jsonPrimitive?.longOrNull?.toInt() ?: 0
            val asset = row["asset"]?.jsonPrimitive?.content.orEmpty()
            val packBytes = row["bytes"]?.jsonPrimitive?.longOrNull ?: 0L
            val sha = row["sha256"]?.jsonPrimitive?.content.orEmpty()
            val packWords = row["words"]?.jsonPrimitive?.longOrNull ?: 0L
            assertEquals("words-${"%03d".format(surah)}.zip", asset)
            assertTrue("$surah has a positive size", packBytes > 0)
            assertTrue("$surah has a 64-hex hash", Regex("^[0-9a-f]{64}$").matches(sha))
            assertTrue("$surah has a positive word count", packWords > 0)
            bytes += packBytes
            words += packWords
        }
        assertEquals("the manifest's total is its packs' sum", total, bytes)
        assertEquals(77_338L, words)

        val report = Json.parseToJsonElement(
            File(root, "content/word-audio/report.json").readText(),
        ).jsonObject
        assertEquals(77_429L, report["words"]?.jsonPrimitive?.longOrNull)
        assertEquals(77_338L, report["files"]?.jsonPrimitive?.longOrNull)
        assertEquals(91L, report["skipped"]?.jsonPrimitive?.longOrNull)
        assertEquals(0L, report["failed"]?.jsonPrimitive?.longOrNull)
        assertEquals(114L, report["packs"]?.jsonPrimitive?.longOrNull)
        assertEquals(total, report["bytes"]?.jsonPrimitive?.longOrNull)
        assertEquals(0, report["failedWords"]?.jsonArray?.size)
        assertEquals(
            "every drawn word is either a file or a named skip",
            report["files"]?.jsonPrimitive?.longOrNull!! +
                report["skipped"]?.jsonPrimitive?.longOrNull!!,
            report["words"]?.jsonPrimitive?.longOrNull!!,
        )
    }

    @Test
    fun alFatihaIsCarriedWholeAndInTheManifest() {
        val root = projectRoot()
        val folder = File(root, "content/bundled/word-audio/001")
        assertTrue("the bundled Al-Fatiha words are missing", folder.isDirectory)
        val files = folder.listFiles { file ->
            file.isFile && Regex("""^[0-9]{3}_[0-9]{3}_[0-9]{3}\.opus$""").matches(file.name)
        }?.sortedBy { it.name }.orEmpty()
        assertEquals(29, files.size)
        assertTrue(files.any { it.name == "001_001_001.opus" })
        assertTrue(files.all { it.length() > 0 })

        val manifest = Json.parseToJsonElement(
            File(root, "content/word-audio/manifest.json").readText(),
        ).jsonObject
        val fatiha = manifest["packs"]?.jsonArray
            ?.map { it.jsonObject }
            ?.firstOrNull { it["surah"]?.jsonPrimitive?.longOrNull == 1L }
            ?: error("the manifest has no Al-Fatiha pack")
        assertEquals(29L, fatiha["words"]?.jsonPrimitive?.longOrNull)
    }

    @Test
    fun theCatalogAndTheManifestNameTheSamePack() {
        val root = projectRoot()
        val manifestFile = File(root, "content/word-audio/manifest.json")
        val manifest = Json.parseToJsonElement(manifestFile.readText()).jsonObject
        val total = manifest["totalBytes"]?.jsonPrimitive?.longOrNull ?: 0L

        val catalog = Json.parseToJsonElement(
            File(root, "content/catalog.json").readText(),
        ).jsonObject
        val entry = catalog["packs"]?.jsonArray
            ?.map { it.jsonObject }
            ?.firstOrNull { it["id"]?.jsonPrimitive?.content == "word-pronunciation-alafasy" }
            ?: error("the catalog has no word pronunciation pack")
        assertEquals("word-audio", entry["type"]?.jsonPrimitive?.content)
        assertEquals("word-audio-alafasy", entry["tag"]?.jsonPrimitive?.content)
        assertEquals(total, entry["bytes"]?.jsonPrimitive?.longOrNull)
        assertEquals(sha256(manifestFile), entry["sha256"]?.jsonPrimitive?.content)
        assertFalse(entry["license"]?.jsonPrimitive?.content.isNullOrBlank())
        assertFalse(entry["credit"]?.jsonPrimitive?.content.isNullOrBlank())

        val content = Json.parseToJsonElement(
            File(root, "content/manifest.json").readText(),
        ).jsonObject
        val dataset = content["datasets"]?.jsonArray
            ?.map { it.jsonObject }
            ?.firstOrNull { it["id"]?.jsonPrimitive?.content == "word-pronunciation-alafasy" }
            ?: error("the content manifest has no word pronunciation dataset")
        assertEquals(
            "content/word-audio/manifest.json",
            dataset["path"]?.jsonPrimitive?.content,
        )
        assertEquals(manifestFile.length(), dataset["bytes"]?.jsonPrimitive?.longOrNull)
        assertEquals(sha256(manifestFile), dataset["sha256"]?.jsonPrimitive?.content)
    }

    private fun projectRoot(): File {
        var directory = File(".").canonicalFile
        while (directory.parentFile != null && !File(directory, "content/quran.db").isFile) {
            directory = directory.parentFile
        }
        assertTrue("the project root was not found", File(directory, "content/quran.db").isFile)
        return directory
    }
}
