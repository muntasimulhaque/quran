package io.github.muntasimulhaque.quran.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The wash scale is an order, not five numbers: a door is the quietest fill,
 * a selection reads above it, a button above that, a control's chosen step
 * above that, and a matched word is the loudest quiet fill. A session that
 * moves one value past its neighbour changes what a reader sees without
 * touching a screen, and this is where that is caught.
 */
class WashTest {

    @Test
    fun theScaleRisesFromQuietToLoud() {
        val scale = listOf(Wash.Door, Wash.Selected, Wash.Button, Wash.Control, Wash.Match)
        assertTrue("a wash is not a share of the ground: $scale", scale.all { it > 0f && it < 1f })
        assertTrue("the wash scale is not in order: $scale", scale == scale.sorted())
        assertTrue("the loudest wash must stay a wash", Wash.Match < 0.2f)
    }
}
