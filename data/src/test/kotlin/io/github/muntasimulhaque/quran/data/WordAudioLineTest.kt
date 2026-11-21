package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The pronunciation line under the words switch, as a fact rather than a
 * sentence. The settings module maps the fact to its words; the rule that
 * decides it is pinned here, where the release gates run (owner decision,
 * 21.5).
 *
 * The four answers are the four states a transfer has, and the order between
 * them is the whole of the rule: a failure outranks bytes already read, and a
 * pack that is installed is never announced as arriving.
 */
class WordAudioLineTest {

    @Test
    fun aFailedTransferSaysFailedEvenWhenBytesHaveArrived() {
        assertEquals(
            WordAudioLine.Failed,
            wordAudioLine(WordAudioState(surah = 1, bytes = 900, read = 400, failed = true)),
        )
    }

    @Test
    fun aTransferThatIsReadingButNotLandedSaysDownloading() {
        assertEquals(
            WordAudioLine.Downloading,
            wordAudioLine(WordAudioState(surah = 1, bytes = 900, read = 400)),
        )
    }

    @Test
    fun anInstalledPackKeepsTheLineQuiet() {
        assertEquals(
            WordAudioLine.None,
            wordAudioLine(WordAudioState(surah = 1, bytes = 900, read = 900, installed = true)),
        )
    }

    @Test
    fun aWaitingTransferThatHasReadNothingKeepsTheLineQuiet() {
        assertEquals(
            WordAudioLine.None,
            wordAudioLine(WordAudioState(surah = 1, bytes = 900, read = 0, waiting = true)),
        )
        assertEquals(WordAudioLine.None, wordAudioLine(WordAudioState()))
    }
}
