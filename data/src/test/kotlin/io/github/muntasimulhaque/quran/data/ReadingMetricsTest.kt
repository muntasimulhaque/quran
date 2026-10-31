package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reading's sizes are one ladder for both scripts; only the line is the
 * script's own.
 *
 * A Bangla reading takes the translation, the tafsir, and the meaning at the
 * same 16, 16, and 14.4 as English, but at Bengali's 1.722 line, because the
 * script stacks its vowel marks above and below the letters. The Arabic and
 * the word-by-word aid's own word stay where the Quran's text stands, because
 * the verse is data and its size is the reader's own step. The aid's word can
 * never rise above the verse it glosses (owner decision, this session).
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
        assertEquals(16f, reading.translationSp, 0.001f)
        assertEquals(16f * 1.5f, reading.translationLineSp, 0.001f)
        assertEquals(16f, english.tafsirSp, 0.001f)
        assertEquals(16f * 1.5f, english.tafsirLineSp, 0.001f)
        assertEquals(14.4f, reading.wordsMeaningSp, 0.001f)
        assertEquals(26f, reading.arabicSp, 0.001f)
        assertEquals(22f, reading.wordsSp, 0.001f)
    }

    @Test
    fun banglaProseTakesTheScriptsOwnLine() {
        val bangla = settings(UiLanguage.Bangla.tag)
        val reading = bangla.readingMetrics()
        assertEquals(16f, reading.translationSp, 0.001f)
        assertEquals(16f * TextSize.BENGALI_PROSE_RATIO, reading.translationLineSp, 0.001f)
        assertEquals(16f, bangla.tafsirSp, 0.001f)
        assertEquals(16f * TextSize.BENGALI_PROSE_RATIO, bangla.tafsirLineSp, 0.001f)
        assertEquals(14.4f, reading.wordsMeaningSp, 0.001f)
        // The Quran's text is data, not a locale: it does not take the
        // script's line.
        assertEquals(26f, reading.arabicSp, 0.001f)
        // The aid's word is a copy of the verse, so it stays under it.
        assertEquals(22f, reading.wordsSp, 0.001f)
    }

    @Test
    fun theSmallestStepKeepsTheBanglaGlossReadable() {
        val bangla = settings(UiLanguage.Bangla.tag, 0.8f)
        val reading = bangla.readingMetrics()
        assertEquals(12.8f, reading.translationSp, 0.001f)
        assertEquals(14.4f * 0.8f, reading.wordsMeaningSp, 0.001f)
        // The word still stands under the verse's own step.
        assertTrue(reading.wordsSp < reading.arabicSp)
    }

    @Test
    fun anUnchosenLanguageReadsTheLatinVoice() {
        // A first launch has no language stored; the reading is English
        // until the welcome screen answers, so the metrics must not divide by
        // a language that is not there.
        val first = settings(null)
        assertEquals(16f, first.translationSp, 0.001f)
        assertEquals(16f * 1.5f, first.translationLineSp, 0.001f)
        assertEquals(16f, first.tafsirSp, 0.001f)
    }
}
