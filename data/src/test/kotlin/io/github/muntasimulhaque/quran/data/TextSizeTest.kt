package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun everyStepHasItsOneBasedName() {
        assertEquals(TextSize.STEPS.size, TextSize.stepCount)
        TextSize.STEPS.forEachIndexed { index, step ->
            assertEquals(index + 1, TextSize.stepNumber(step))
        }
        assertEquals(TextSize.STEPS.size, TextSize.stepNumber(1.6f))
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
        // translation stands at, both at 15 sp with the same 1.5 line
        // (owner decision, this session).
        assertEquals(TextSize.sp(TypeRole.Translation, 1f), TextSize.sp(TypeRole.Tafsir, 1f))
        assertEquals(TextSize.lineSp(TypeRole.Translation, 1f), TextSize.lineSp(TypeRole.Tafsir, 1f))
    }

    @Test
    fun theArabicLineHoldsTheQuranFoundationsRatio() {
        // 2.0, the Quran Foundation's own QPC Hafs leading. At 28.8 the app
        // carried 1.9, measured against the marks; with the base at 24 the
        // official 2.0 gives the marks their room.
        assertEquals(24f * 2f, TextSize.lineSp(TypeRole.Arabic, 1f))
    }

    @Test
    fun theShippedDefaultIsOneEvenStepSmaller() {
        // The default is 1, the middle of the five (owner decision, this
        // session). The Arabic stands at 24 sp, 1.6 times the 15 sp prose:
        // the owner's eye asked for the Quran one step larger and the prose
        // one smaller, and the reader can still go down to 19.2.
        assertEquals(24f, TextSize.sp(TypeRole.Arabic, 1f))
        assertEquals(15f, TextSize.sp(TypeRole.Translation, 1f))
    }

    @Test
    fun theWordMeaningSitsAStepUnderTheTranslation() {
        // 13.5 sp against the translation's 15: nine tenths of it, a step
        // the eye reads as a gloss rather than as a second translation. The
        // five percent it stood at before read as one size used twice (owner
        // decision, this session).
        assertEquals(15f * 0.9f, TextSize.meaningSp(1f), 0.001f)
        assertEquals(13.5f * 0.8f, TextSize.meaningSp(0.8f), 0.001f)
        // The aid's Arabic word follows the verse and never the gloss, so a
        // bigger gloss cannot carry it past the Quran's own size.
        for (step in TextSize.STEPS) {
            assertTrue(
                "the aid's word must stay under the verse at $step",
                TextSize.wordSp(step) < TextSize.sp(TypeRole.Arabic, step),
            )
        }
    }

    @Test
    fun theLadderIsPinned() {
        // The session's ladder, one line per relationship the reader was
        // promised: the Quran is the anchor at 1.6 times the prose, the word
        // list reads two points under it, Arabic beside prose is one rule at
        // 0.9 from the Quran in both the translation and the tafsir, the
        // prose stands at the reading voice's own size, and the meaning sits
        // under the word it glosses (owner decision, this session).
        assertEquals(24f, TextSize.sp(TypeRole.Arabic, 1f))
        assertEquals(22f, TextSize.wordSp(1f), 0.001f)
        assertEquals(21.6f, TextSize.inlineArabic(1f), 0.001f)
        assertEquals(15f, TextSize.sp(TypeRole.Translation, 1f))
        assertEquals(15f, TextSize.sp(TypeRole.Tafsir, 1f))
        assertEquals(13.5f, TextSize.meaningSp(1f), 0.001f)
    }
}
