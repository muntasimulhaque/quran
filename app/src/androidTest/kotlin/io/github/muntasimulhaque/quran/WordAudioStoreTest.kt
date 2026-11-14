package io.github.muntasimulhaque.quran

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.WordAudioStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

/**
 * The word pronunciation files the app carries, read the way the player reads
 * them.
 *
 * Two things are pinned here. The store answers by the ayah inside the surah
 * and the word's position, because that is what a file is named and what the
 * sheet knows when it opens; a position that exists in one ayah must not make
 * the same position in another ayah look hearable. And every bundled file is a
 * real Opus stream the platform can demux, so the first page's speaker cannot
 * be a control over a file Android refuses.
 *
 * The class lives in the app's own tests because the files it reads are the
 * app's assets: the data module carries no content.
 */
@RunWith(AndroidJUnit4::class)
class WordAudioStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun positionsAreKeyedByTheAyahInsideTheSurah() {
        val store = WordAudioStore(context)
        val fatiha = store.positions(1)
        assertEquals("Al-Fatiha's first ayah has four words", setOf(1, 2, 3, 4), fatiha[1])
        assertEquals("Al-Fatiha's second ayah has four words", setOf(1, 2, 3, 4), fatiha[2])
        assertEquals("Al-Fatiha's last ayah has nine words", (1..9).toSet(), fatiha[7])
        assertEquals(29, fatiha.values.sumOf { it.size })
        assertTrue(store.isInstalled(1))
        assertTrue(store.isBundled(1))
    }

    @Test
    fun aSurahWithNoFileHasNoPositionsAndNoUri() {
        val store = WordAudioStore(context)
        assertTrue(store.positions(3).isEmpty())
        assertNull(store.uri(3, 1, 1))
        assertNull(store.uri(1, 1, 99))
        assertNull(store.uri(1, 1, 0))
        assertNull(store.uri(0, 1, 1))
    }

    /**
     * The page's own count: the surahs the reader downloaded and their bytes.
     * The bundled Al-Fatiha is the app's asset and is never counted, and a
     * folder the unpack left behind is not a surah a reader can play.
     */
    @Test
    fun onDeviceCountsTheDownloadedSurahsAndIgnoresTheUnpackSLeftovers() {
        val store = WordAudioStore(context)
        val before = store.onDevice()
        val folder = File(context.filesDir, "word-audio/003").apply { mkdirs() }
        File(folder, "003_001_001.opus").writeBytes(ByteArray(100))
        // The three names the unpack leaves beside a committed folder.
        File(context.filesDir, "word-audio/.staging/004.new").mkdirs()
        File(context.filesDir, "word-audio/003.new").mkdirs()
        File(context.filesDir, "word-audio/003.old").mkdirs()
        try {
            val after = store.onDevice()
            assertEquals(before.surahs + 1, after.surahs)
            assertEquals(before.bytes + 100, after.bytes)
        } finally {
            folder.deleteRecursively()
            File(context.filesDir, "word-audio/.staging").deleteRecursively()
            File(context.filesDir, "word-audio/003.new").deleteRecursively()
            File(context.filesDir, "word-audio/003.old").deleteRecursively()
        }
    }

    @Test
    fun everyBundledFileIsAnOpusStreamThePlatformCanRead() {
        val store = WordAudioStore(context)
        val fatiha = store.positions(1)
        var checked = 0
        for ((ayah, positions) in fatiha) {
            for (position in positions) {
                val uri = store.uri(1, ayah, position)
                assertNotNull("1:$ayah word $position has no file", uri)
                val bytes = context.assets.open("word-audio/001/${name(ayah, position)}")
                    .use { it.readBytes() }
                assertTrue("1:$ayah word $position is empty", bytes.isNotEmpty())
                val probe = File(context.cacheDir, "probe-${ayah}-${position}.opus")
                probe.writeBytes(bytes)
                try {
                    val extractor = MediaExtractor()
                    try {
                        extractor.setDataSource(probe.absolutePath)
                        var mime: String? = null
                        var duration = 0L
                        for (index in 0 until extractor.trackCount) {
                            val format = extractor.getTrackFormat(index)
                            if (format.getString(MediaFormat.KEY_MIME) == MIME_OPUS) {
                                mime = MIME_OPUS
                                duration = format.getLong(MediaFormat.KEY_DURATION)
                                break
                            }
                        }
                        assertEquals("1:$ayah word $position is not Opus", MIME_OPUS, mime)
                        assertTrue("1:$ayah word $position has no duration", duration > 0)
                    } finally {
                        extractor.release()
                    }
                } finally {
                    probe.delete()
                }
                checked++
            }
        }
        assertEquals("every bundled word was checked", 29, checked)
    }

    private fun name(ayah: Int, position: Int): String =
        String.format(Locale.ROOT, "001_%03d_%03d.opus", ayah, position)

    private companion object {
        const val MIME_OPUS = "audio/opus"
    }
}
