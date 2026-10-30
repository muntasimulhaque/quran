package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reading's sizes are the reader's script's, not one ladder borrowed by
 * both.
 *
 * A Bangla reading takes the translation, the tafsir, and the meaning at 18 sp
 * and their 1.722 line, so a Bengali paragraph reads at the size the script was
 * designed for; the Arabic and the word-by-word aid's own word stay where the
 * Quran's text stands, because the verse is data and its size is the reader's
 * own step. The two languages therefore share one ladder and keep their own
 * air, and the aid's word can never rise above the verse it glosses (owner
 * decision, this session: the metrics were language-blind, and the Bangla
 * reading was drawn at the Latin sizes).
 */
class ReadingMetricsTest {

    private fun settings(language: String?, step: Float = TextSize.DEFAULT): AppSettings =
        AppSettings(
            uiLanguage = language,
            arabicSize = step,
            translationSize = step,
            tafsirSize = step,
            wordsSize = step,
        )

    private fun metrics(language: String?, step: Float = TextSize.DEFAULT): ReadingMetrics =
        settings(language, step).readingMetrics()

    @Test
    fun englishProseKeepsTheLatinBase() {
        val english = settings(UiLanguage.English.tag)
        val reading = english.readingMetrics()
        assertEquals(15f, reading.translationSp, 0.001f)
        assertEquals(15f * 1.5f, reading.translationLineSp, 0.001f)
        assertEquals(15f, english.tafsirSp, 0.001f)
        assertEquals(15f * 1.5f, english.tafsirLineSp, 0.001f)
        assertEquals(13.5f, reading.wordsMeaningSp, 0.001f)
        assertEquals(24f, reading.arabicSp, 0.001f)
        assertEquals(22f, reading.wordsSp, 0.001f)
    }

    @Test
    fun banglaProseTakesTheScriptsOwnSize() {
        val bangla = settings(UiLanguage.Bangla.tag)
        val reading = bangla.readingMetrics()
        assertEquals(18f, reading.translationSp, 0.001f)
        assertEquals(31f, reading.translationLineSp, 0.001f)
        assertEquals(18f, bangla.tafsirSp, 0.001f)
        assertEquals(31f, bangla.tafsirLineSp, 0.001f)
        assertEquals(16.2f, reading.wordsMeaningSp, 0.001f)
        // The Quran's text is data, not a locale: it does not take the
        // script's step.
        assertEquals(24f, reading.arabicSp, 0.001f)
        // The aid's word is a copy of the verse, so it stays under it.
        assertEquals(22f, reading.wordsSp, 0.001f)
    }

    @Test
    fun theSmallestStepKeepsTheBanglaGlossReadable() {
        val bangla = settings(UiLanguage.Bangla.tag, 0.8f)
        val reading = bangla.readingMetrics()
        assertEquals(14.4f, reading.translationSp, 0.001f)
        assertEquals(12.96f, reading.wordsMeaningSp, 0.001f)
        // The word still stands under the verse's own step.
        assertTrue(reading.wordsSp < reading.arabicSp)
    }

    @Test
    fun anUnchosenLanguageReadsTheLatinVoice() {
        // A first launch has no language stored; the reading is English
        // until the welcome screen answers, so the metrics must not divide by
        // a language that is not there.
        val first = settings(null)
        assertEquals(15f, first.translationSp, 0.001f)
        assertEquals(15f * 1.5f, first.translationLineSp, 0.001f)
        assertEquals(15f, first.tafsirSp, 0.001f)
    }
}
