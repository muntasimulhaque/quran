package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The five scales are a choice the reader made once and expects to keep: the
 * list is pinned here so a session that trims or grows it must also say what
 * happens to every stored scale. The steps are the reader's own, and a stored
 * value keeps its meaning by landing on the nearest step, never on an index
 * that means something different under a new list.
 */
class TextSizeTest {

    @Test
    fun theStepsRunFrom75To120WithTheDefaultInTheMiddle() {
        assertEquals(listOf(0.75f, 0.85f, 1f, 1.1f, 1.2f), TextSize.STEPS)
        assertEquals(TextSize.DEFAULT, TextSize.STEPS[TextSize.STEPS.size / 2])
    }

    @Test
    fun aRetiredLargestStepLandsOnTheLargestOfToday() {
        assertEquals(1.2f, TextSize.step(1.4f))
        assertEquals(1.2f, TextSize.step(1.6f))
    }

    @Test
    fun aStoredStepThatIsStillHereKeepsItself() {
        for (step in TextSize.STEPS) assertEquals(step, TextSize.step(step))
    }

    @Test
    fun anUnknownValueLandsOnTheNearestStep() {
        assertEquals(1f, TextSize.step(0.95f))
        // The 0.65 step is retired (owner decision, the audio and type audit
        // session): a reader who stored it lands on the smallest step there
        // is, never on a step below the platform floor.
        assertEquals(0.75f, TextSize.step(0.6f))
    }

    @Test
    fun aLargerStepMeansLargerText() {
        val sizes = TextSize.STEPS.map { TextSize.sp(TypeRole.Arabic, it) }
        assertEquals(sizes.sorted(), sizes)
    }

    @Test
    fun theTafsirIsSetAtTheTranslationSize() {
        // One reading voice: the Ibn Kathir prose stands at the size the
        // translation stands at, both at 17 sp with the same 1.6 line
        // (owner decision, this session).
        assertEquals(TextSize.sp(TypeRole.Translation, 1f), TextSize.sp(TypeRole.Tafsir, 1f))
        assertEquals(TextSize.lineSp(TypeRole.Translation, 1f), TextSize.lineSp(TypeRole.Tafsir, 1f))
    }

    @Test
    fun theArabicLineHoldsTheMeasuredRatio() {
        // 1.9 (owner decision, the audio and type audit session): at 1.85 the
        // worst frame left about 0.7 dp of air at its tightest point, inside
        // the Hafs face's own 1.758 line box by a hair; 1.9 lifts it to about
        // 2.3 dp for about 2.6 percent of the page.
        assertEquals(32f * 1.9f, TextSize.lineSp(TypeRole.Arabic, 1f))
    }

    @Test
    fun theWordMeaningSitsAStepUnderTheTranslation() {
        // 16 sp against the translation's 17 (owner decision, the theme and
        // consistency session): the gloss is read at length under every word,
        // so it stays a small reading rather than a footnote.
        assertEquals(16f, TextSize.meaningSp(1f))
    }

    @Test
    fun theLadderIsPinned() {
        // The session's ladder, one line per relationship the reader was
        // promised: the Quran is the anchor, the word list reads one size
        // under it at 7/8, Arabic beside prose is one rule at 0.9 from the
        // Quran in both the translation and the tafsir, the prose stands at
        // the reading voice's own sizes, and the meaning sits under the word
        // it glosses (owner decision, the theme and consistency session).
        assertEquals(32f, TextSize.sp(TypeRole.Arabic, 1f))
        assertEquals(28f, TextSize.wordSp(1f))
        assertEquals(28.8f, TextSize.inlineArabic(1f), 0.001f)
        assertEquals(17f, TextSize.sp(TypeRole.Translation, 1f))
        assertEquals(17f, TextSize.sp(TypeRole.Tafsir, 1f))
        assertEquals(16f, TextSize.meaningSp(1f))
    }
}
