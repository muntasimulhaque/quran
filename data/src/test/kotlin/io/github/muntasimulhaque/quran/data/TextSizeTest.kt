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
}
