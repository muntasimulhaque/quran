package io.github.muntasimulhaque.quran.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The wash scale is an order, not four numbers: a door is the quietest fill, a
 * button above it, and a control's chosen step above that. A session that moves
 * one value past its neighbor changes what a reader sees without touching a
 * screen, and this is where that is caught.
 *
 * The two grounds are not on the scale: the chosen thing wears
 * [PaperSelected] and the found thing wears [PaperMatch], both opaque and both
 * in the accent's own family, at the lightness the warm grounds of 19.1
 * carried (owner decision, 21.0).
 */
class WashTest {

    @Test
    fun theScaleRisesFromQuietToLoud() {
        val scale = listOf(Wash.Door, Wash.Button, Wash.Control)
        assertTrue("a wash is not a share of the ground: $scale", scale.all { it > 0f && it < 1f })
        assertTrue("the wash scale is not in order: $scale", scale == scale.sorted())
        assertTrue("the loudest wash must stay a wash", scale.max() < 0.2f)
    }
}
