package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The switch's own rule, enforced by arithmetic instead of by memory: on every
 * ground, a knob reads against the track it sits in, and against the sheet the
 * whole control stands on, at 3:1 or better.
 *
 * A control boundary owes 3:1, which is the same rule the app's choice rows
 * already hold their marks to in `ui.kit.ChoiceRow`, and it is a rule about a
 * shape rather than about words, which is why it has its own class beside
 * `ContrastTest` rather than inside it.
 *
 * This test exists because the switch's colors were nobody's choice. Material
 * read the unchecked knob from `outline` and the unchecked track from
 * `surfaceContainerHighest`, which on the night ground are four levels
 * apart: 1.03:1 on Night, so an off switch had no knob in
 * it at all. Every pair here is declared in `Switch.kt`, and a ground whose
 * knob or ring is moved back toward its own track fails here.
 */
class SwitchContrastTest {

    @Test
    fun lightHolds() {
        assertControl(LightSwitch)
        assertOutOfReach(LightSwitch)
    }

    @Test
    fun darkHolds() {
        assertControl(DarkSwitch)
        assertOutOfReach(DarkSwitch)
    }

    private fun assertControl(palette: SwitchPalette) {
        assertBoundary("the off knob on its own track", palette.offKnob, palette.offTrack)
        assertBoundary("the off knob on the sheet", palette.offKnob, palette.ground)
        assertBoundary("the on knob on its own track", palette.onKnob, palette.onTrack)
    }

    /**
     * The switch the reader cannot reach.
     *
     * A disabled control owes no contrast rule, so this is not the 3:1 above.
     * It is the weaker promise the app makes instead: the knob still stands
     * out of its track, and the two states are still told apart, because the
     * one out of reach switch in the app says "written as your answer,
     * nothing to draw yet" rather than nothing at all. The track is declared
     * in [Switch.kt]; Material's own alphas are not this app's to draw.
     */
    private fun assertOutOfReach(palette: SwitchPalette) {
        assertVisible("the out of reach off knob", palette.offKnob, palette.offTrackOutOfReach)
        assertVisible("the out of reach on knob", palette.onKnob, palette.onTrackOutOfReach)
        assertTrue(
            "the out of reach track is the enabled track, so the state cannot be read",
            palette.offTrackOutOfReach != palette.offTrack || palette.onTrackOutOfReach != palette.onTrack,
        )
        assertTrue(
            "the two states of the out of reach switch are one tone",
            contrast(palette.onTrackOutOfReach, palette.offTrackOutOfReach) > 1.05,
        )
    }
    private fun assertVisible(what: String, knob: Color, behind: Color) {
        val ratio = contrast(knob, behind)
        assertTrue(
            "$what: ${hex(knob)} on ${hex(behind)} is ${"%.2f".format(ratio)}:1, " +
                "which is not a knob a reader can find",
            ratio >= 1.5,
        )
    }

    private fun assertBoundary(what: String, knob: Color, behind: Color) {
        val ratio = contrast(knob, behind)
        assertTrue(
            "$what: ${hex(knob)} on ${hex(behind)} is ${"%.2f".format(ratio)}:1, " +
                "under the 3:1 a control boundary owes",
            ratio >= 3.0,
        )
    }

    private fun hex(color: Color): String = "0x${color.value.toString(16).uppercase()}"

    /** The WCAG 2.x ratio: (L1 + 0.05) / (L2 + 0.05) over linearized sRGB. */
    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun linear(channel: Float): Double =
            if (channel <= 0.04045f) channel / 12.92 else Math.pow((channel + 0.055) / 1.055, 2.4)
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }
}
