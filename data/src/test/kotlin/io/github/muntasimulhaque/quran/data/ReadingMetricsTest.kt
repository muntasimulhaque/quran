package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The reading's sizes are one ladder for both scripts; only the line is the
 * script's own.
 *
 * A Bangla reading takes the translation and the tafsir at the same 16 as
 * English, but at Bengali's 1.722 line, because the script stacks its vowel
 * marks above and below the letters. The Arabic stays where the Quran's text
 * stands, because the verse is data and its size is the reader's own step.
 * The word meaning sheet draws the Quran's word at the Arabic size and its
 * gloss at the translation size, so the metrics no longer carry a size of
 * their own for the aid (owner decision, this session).
 */
class ReadingMetricsTest {

    private fun settings(language: String?, step: Float = TextSize.DEFAULT): AppSettings =
        AppSettings(
            uiLanguage = language,
            arabicSize = step,
            translationSize = step,
            tafsirSize = step,
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
        assertEquals(26f, reading.arabicSp, 0.001f)
    }

    @Test
    fun banglaProseTakesTheScriptsOwnLine() {
        val bangla = settings(UiLanguage.Bangla.tag)
        val reading = bangla.readingMetrics()
        assertEquals(16f, reading.translationSp, 0.001f)
        assertEquals(16f * TextSize.BENGALI_PROSE_RATIO, reading.translationLineSp, 0.001f)
        assertEquals(16f, bangla.tafsirSp, 0.001f)
        assertEquals(16f * TextSize.BENGALI_PROSE_RATIO, bangla.tafsirLineSp, 0.001f)
        // The Quran's text is data, not a locale: it does not take the
        // script's line.
        assertEquals(26f, reading.arabicSp, 0.001f)
    }

    @Test
    fun theSmallestStepKeepsTheBanglaTranslationReadable() {
        val bangla = settings(UiLanguage.Bangla.tag, 0.8f)
        val reading = bangla.readingMetrics()
        assertEquals(12.8f, reading.translationSp, 0.001f)
        assertEquals(12.8f * TextSize.BENGALI_PROSE_RATIO, reading.translationLineSp, 0.001f)
    }

    @Test
    fun anUnchosenLanguageReadsTheLatinVoice() {
        // A fresh install has no language stored until the first open seeds
        // it from the phone; the reading is English until then, so the
        // metrics must not divide by a language that is not there.
        val first = settings(null)
        assertEquals(16f, first.translationSp, 0.001f)
        assertEquals(16f * 1.5f, first.translationLineSp, 0.001f)
        assertEquals(16f, first.tafsirSp, 0.001f)
    }

    @Test
    fun theMetricsCarryTheLanguageTheTranslationColumnsLeadWith() {
        assertEquals("en", metrics(null).readingLanguage)
        assertEquals("en", metrics(UiLanguage.English.tag).readingLanguage)
        assertEquals("bn", metrics(UiLanguage.Bangla.tag).readingLanguage)
    }
}
