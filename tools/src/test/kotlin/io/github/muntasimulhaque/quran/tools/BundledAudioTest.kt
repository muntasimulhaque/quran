package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The fourteen Fatiha files the app carries are the published recording, not
 * a hand edit and not a second encode: every bundled byte must match its pin
 * in `content/audio-sources.json`, and no other file may stand beside them.
 *
 * The check runs without a content database or a network, so a fresh clone
 * and CI can prove the bundle before the app is built. `checkdb` pins the
 * same set against the database's own surah 1 paths.
 */
class BundledAudioTest {

    private val reciters = listOf("minshawyMurattal", "husary")

    @Test
    fun theBundledFatihaIsThePinnedSourceAudio() {
        val root = root()
        val sources = Json.parseToJsonElement(
            File(root, "content/audio-sources.json").readText(),
        ).jsonObject
        val expected = reciters.flatMap { folder ->
            (1..7).map { ayah -> "$folder/00100$ayah.mp3" }
        }.toSet()
        val bundle = File(root, "content/bundled/audio/recitation")
        assertTrue("the bundled audio folder is missing", bundle.isDirectory)
        val found = bundle.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(bundle).invariantSeparatorsPath }
            .toSet()
        assertEquals("the bundle must carry exactly the two Fatiha reciters", expected, found)
        for (path in expected) {
            val pin = sources[path]?.jsonPrimitive?.content
            assertTrue("$path has no source pin", !pin.isNullOrBlank())
            assertEquals("$path does not match its source pin", pin, sha256(File(bundle, path)))
        }
    }

    private fun root(): File {
        var directory = File(".").absoluteFile
        while (!File(directory, "settings.gradle.kts").isFile) {
            directory = directory.parentFile
                ?: error("no repository root above ${File(".").absolutePath}")
        }
        return directory
    }
}
