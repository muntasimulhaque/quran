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
    fun theStepsRunFrom65To120() {
        assertEquals(listOf(0.65f, 0.75f, 0.85f, 1f, 1.2f), TextSize.STEPS)
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
        assertEquals(0.65f, TextSize.step(0.6f))
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
    fun theArabicLineHoldsTheFrameDecidedRatio() {
        // 1.85: a twelfth of an em behind the Hafs face's own 1.758 line
        // box, decided on the tour's reading frame (owner approval, this
        // session). Anything less overcrowds the marks; anything more was
        // unmeasured headroom the page no longer pays for.
        assertEquals(32f * 1.85f, TextSize.lineSp(TypeRole.Arabic, 1f))
    }
}
