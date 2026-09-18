package io.github.muntasimulhaque.quran.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The manifest is the contract between the packaging tool and the app: a URL,
 * a size, and a SHA-256 per surah per reciter. Getting it wrong means the app
 * would ask for the wrong file or accept a wrong one, so it is pinned here.
 */
@RunWith(AndroidJUnit4::class)
class RecitationManifestTest {

    private val sample = """
        {
          "baseUrl": "https://example.test/download",
          "recitations": [
            {
              "id": "minshawi",
              "tag": "recitation-minshawi",
              "surahs": [
                {"surah": 2, "asset": "surah-002.zip", "bytes": 123456, "sha256": "AABB"}
              ]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun packagesResolveToTheirReleaseUrl() {
        val manifest = RecitationManifest.parse(sample)
        val packageToFetch = manifest.packageFor("minshawi", 2)
        assertNotNull(packageToFetch)
        assertEquals(
            "https://example.test/download/recitation-minshawi/surah-002.zip",
            packageToFetch!!.url,
        )
        assertEquals(123456, packageToFetch.bytes)
        assertEquals("AABB", packageToFetch.sha256)
        assertEquals(setOf(2), manifest.surahs("minshawi"))
    }

    @Test
    fun unknownRecitationsAndSurahsAreAbsent() {
        val manifest = RecitationManifest.parse(sample)
        assertNull(manifest.packageFor("husary", 2))
        assertNull(manifest.packageFor("minshawi", 3))
        assertTrue(manifest.surahs("husary").isEmpty())
    }

    @Test
    fun brokenJsonYieldsAnEmptyManifest() {
        assertTrue(RecitationManifest.parse("{not json").surahs("minshawi").isEmpty())
    }

    @Test
    fun storeCountsAndRemovesOnlyTheSurahFiles() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = RecitationStore(context)
        val folder = "test-folder"
        val directory = File(context.filesDir, "recitations/$folder").apply { mkdirs() }
        File(directory, "002001.mp3").writeBytes(ByteArray(10))
        File(directory, "002002.mp3").writeBytes(ByteArray(20))
        File(directory, "003001.mp3").writeBytes(ByteArray(30))

        assertEquals(30, store.bytesForSurah(folder, 2))
        assertEquals(2, store.removeSurah(folder, 2))
        assertEquals(0, store.bytesForSurah(folder, 2))
        assertEquals(30, store.bytesForSurah(folder, 3))

        directory.deleteRecursively()
    }
}
