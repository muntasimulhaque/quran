package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The contrast rule, enforced by arithmetic instead of by memory: every tone
 * that carries words reads at 4.5:1 or better on every ground it is drawn
 * on, in each of the four themes. The pairs are the ones the schemes wire:
 * the reading ink, the muted voice, the gold of the page furniture, and the
 * lapis of an action. A palette edit that sinks a pair fails here, on the
 * machine, rather than on a reader's eyes.
 */
class ContrastTest {

    @Test
    fun paperHolds() {
        assertReadable(PaperInk, PaperBackground)
        assertReadable(PaperInk, PaperSurface)
        assertReadable(PaperMuted, PaperBackground)
        assertReadable(PaperMuted, PaperSurface)
        assertReadable(Gold, PaperBackground)
        assertReadable(Lapis, PaperBackground)
        assertReadable(Lapis, PaperSurface)
        assertReadable(Color(0xFFFDFBF6), Lapis)
    }

    @Test
    fun sepiaHolds() {
        assertReadable(SepiaInk, SepiaBackground)
        assertReadable(SepiaInk, SepiaSurface)
        assertReadable(SepiaMuted, SepiaBackground)
        assertReadable(SepiaMuted, SepiaSurface)
        assertReadable(SepiaGold, SepiaBackground)
        assertReadable(Lapis, SepiaBackground)
        assertReadable(Lapis, SepiaSurface)
        assertReadable(Color(0xFFFDF8ED), Lapis)
    }

    @Test
    fun nightHolds() {
        assertReadable(NightText, NightBackground)
        assertReadable(NightText, NightSurface)
        assertReadable(NightMuted, NightBackground)
        assertReadable(NightMuted, NightSurface)
        assertReadable(NightGold, NightBackground)
        assertReadable(LapisLight, NightBackground)
        assertReadable(LapisLight, NightSurface)
        assertReadable(Color(0xFF08243D), LapisLight)
    }

    @Test
    fun blackHolds() {
        assertReadable(BlackText, BlackBackground)
        assertReadable(BlackText, BlackSurface)
        assertReadable(BlackMuted, BlackBackground)
        assertReadable(BlackMuted, BlackSurface)
        assertReadable(BlackGold, BlackBackground)
        assertReadable(LapisLight, BlackBackground)
        assertReadable(LapisLight, BlackSurface)
        assertReadable(Color(0xFF061A2C), LapisLight)
    }

    private fun assertReadable(ink: Color, ground: Color) {
        val ratio = contrast(ink, ground)
        assertTrue(
            "0x${ink.value.toString(16).uppercase()} on 0x${ground.value.toString(16).uppercase()} " +
                "is ${"%.2f".format(ratio)}:1, under the 4.5:1 floor",
            ratio >= 4.5,
        )
    }

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
