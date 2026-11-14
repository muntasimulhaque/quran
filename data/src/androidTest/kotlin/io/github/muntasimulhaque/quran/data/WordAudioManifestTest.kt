package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordAudioManifestTest {

    private val base = "https://github.com/muntasimulhaque/quran/releases/download"
    private val hash = "a".repeat(64)

    @Test
    fun oneSurahIsAddressedByItsOwnName() {
        val manifest = WordAudioManifest.parse(
            """
            {
              "version": 1,
              "baseUrl": "$base",
              "tag": "word-audio-alafasy",
              "totalBytes": 160401,
              "packs": [
                {"surah": 1, "asset": "words-001.zip", "bytes": 160401,
                 "sha256": "$hash", "words": 29}
              ]
            }
            """.trimIndent(),
        )
        val pack = manifest.packageFor(1)
        assertEquals("$base/word-audio-alafasy/words-001.zip", pack?.url)
        assertEquals(160401L, pack?.bytes)
        assertEquals(29, pack?.words)
        assertEquals(160401L, manifest.totalBytes())
        assertEquals(setOf(1), manifest.surahs())
    }

    @Test
    fun aBadHashIsDropped() {
        val manifest = WordAudioManifest.parse(
            """
            {
              "version": 1,
              "baseUrl": "$base",
              "tag": "word-audio-alafasy",
              "totalBytes": 10,
              "packs": [
                {"surah": 1, "asset": "words-001.zip", "bytes": 10,
                 "sha256": "abc", "words": 29}
              ]
            }
            """.trimIndent(),
        )
        assertNull(manifest.packageFor(1))
        assertEquals(0L, manifest.totalBytes())
    }

    @Test
    fun aNonHttpsBaseIsRefused() {
        val manifest = WordAudioManifest.parse(
            """
            {
              "version": 1,
              "baseUrl": "http://example.com",
              "tag": "word-audio-alafasy",
              "totalBytes": 10,
              "packs": [
                {"surah": 1, "asset": "words-001.zip", "bytes": 10,
                 "sha256": "$hash", "words": 29}
              ]
            }
            """.trimIndent(),
        )
        assertNull(manifest.packageFor(1))
        assertEquals(0L, manifest.totalBytes())
    }

    @Test
    fun anUnsafeAssetIsDropped() {
        val manifest = WordAudioManifest.parse(
            """
            {
              "version": 1,
              "baseUrl": "$base",
              "tag": "word-audio-alafasy",
              "totalBytes": 10,
              "packs": [
                {"surah": 1, "asset": "../words-001.zip", "bytes": 10,
                 "sha256": "$hash", "words": 29}
              ]
            }
            """.trimIndent(),
        )
        assertNull(manifest.packageFor(1))
        assertEquals(0L, manifest.totalBytes())
    }
}
