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
    fun theStepsRunFrom80To120WithTheDefaultInTheMiddle() {
        assertEquals(listOf(0.8f, 0.9f, 1f, 1.1f, 1.2f), TextSize.STEPS)
        assertEquals(TextSize.DEFAULT, TextSize.STEPS[TextSize.STEPS.size / 2])
    }

    @Test
    fun theStepsAreEvenlySpaced() {
        // 0.1 between every pair, so the step down from the shipped default is
        // the same size as every other step (owner decision, this session).
        val gaps = TextSize.STEPS.zipWithNext { a, b -> b - a }
        for (gap in gaps) assertEquals(0.1f, gap, 0.0001f)
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
    fun aRetiredSmallStepKeepsItsPlace() {
        // The old 0.75 and 0.85 are gone. 0.75 lands on 0.8, the smallest
        // step now, and 0.85, which sits exactly between 0.8 and 0.9, lands
        // on 0.9: the place it held, not the step below it (owner decision,
        // this session).
        assertEquals(0.8f, TextSize.step(0.75f))
        assertEquals(0.9f, TextSize.step(0.85f))
    }

    @Test
    fun anUnknownValueLandsOnTheNearestStep() {
        assertEquals(1f, TextSize.step(0.95f))
        // The 0.65 step is retired (owner decision, the audio and type audit
        // session): a reader who stored it lands on the smallest step there
        // is, never on a step below the platform floor.
        assertEquals(0.8f, TextSize.step(0.6f))
    }

    @Test
    fun aLargerStepMeansLargerText() {
        val sizes = TextSize.STEPS.map { TextSize.sp(TypeRole.Arabic, it) }
        assertEquals(sizes.sorted(), sizes)
    }

    @Test
    fun theTafsirIsSetAtTheTranslationSize() {
        // One reading voice: the Ibn Kathir prose stands at the size the
        // translation stands at, both at 15.3 sp with the same 1.6 line
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
        assertEquals(28.8f * 1.9f, TextSize.lineSp(TypeRole.Arabic, 1f))
    }

    @Test
    fun theShippedDefaultIsOneEvenStepSmaller() {
        // The default is 1, the middle of the five (owner decision, this
        // session). It renders 0.9 of the old base, so the Arabic stands at
        // 28.8 sp and the translation at 15.3 sp where they were 32 and 17.
        assertEquals(28.8f, TextSize.sp(TypeRole.Arabic, 1f))
        assertEquals(15.3f, TextSize.sp(TypeRole.Translation, 1f))
    }

    @Test
    fun theWordMeaningSitsAStepUnderTheTranslation() {
        // 14.4 sp against the translation's 15.3 (owner decision, this
        // session): the same stand under the translation that 16 held under
        // 17, so the gloss stays a small reading rather than a footnote.
        assertEquals(14.4f, TextSize.meaningSp(1f))
    }

    @Test
    fun theLadderIsPinned() {
        // The session's ladder, one line per relationship the reader was
        // promised: the Quran is the anchor, the word list reads one size
        // under it at 7/8, Arabic beside prose is one rule at 0.9 from the
        // Quran in both the translation and the tafsir, the prose stands at
        // the reading voice's own sizes, and the meaning sits under the word
        // it glosses (owner decision, the theme and consistency session).
        assertEquals(28.8f, TextSize.sp(TypeRole.Arabic, 1f))
        assertEquals(25.2f, TextSize.wordSp(1f), 0.001f)
        assertEquals(25.92f, TextSize.inlineArabic(1f), 0.001f)
        assertEquals(15.3f, TextSize.sp(TypeRole.Translation, 1f))
        assertEquals(15.3f, TextSize.sp(TypeRole.Tafsir, 1f))
        assertEquals(14.4f, TextSize.meaningSp(1f))
    }
}
