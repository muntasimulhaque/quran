package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A family is the unit the legend shows and the unit the reading colors, so
 * every rule belongs to exactly one of them, and the families in their own
 * order list the code's rules in the order the code runs. A rule that moved
 * family without moving its name would otherwise leave the legend naming it
 * under a color it is not drawn in.
 */
class TajweedFamilyTest {

    @Test
    fun everyRuleIsDrawnInExactlyOneFamily() {
        assertEquals(TajweedRule.entries.toList(), TajweedFamily.entries.flatMap { it.rules })
    }

    @Test
    fun theFamiliesHoldTheseManyRules() {
        assertEquals(listOf(3, 1, 1, 2, 1, 5, 5), TajweedFamily.entries.map { it.rules.size })
    }
}
