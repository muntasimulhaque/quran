package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The contrast rule, enforced by arithmetic instead of by memory: every tone
 * that carries words reads at 4.5:1 or better on every ground it is drawn
 * on, in each of the two themes. The pairs are the ones the schemes wire:
 * the reading ink, the muted voice, the gold of the page furniture, and the
 * lapis of an action. The muted voice is held against every ground a sheet
 * or a floating control stands on, not only against the sheet's own tone,
 * because the quiet words of a label travel with the surfaces they sit on.
 * A palette edit that sinks a pair fails here, on the machine, rather than
 * on a reader's eyes.
 *
 * The search result's matched line is the one ground the families are not
 * measured on: that line is drawn in the reading's ink and not in the rule
 * colors, because a result list is a scan surface and the match wash is not
 * a reading ground (owner decision, the palette session). The ink's own
 * pair on that wash is measured here, so the decision stays written.
 */
class ContrastTest {

    @Test
    fun lightHolds() {
        assertReadable(PaperInk, PaperBackground)
        assertReadable(PaperInk, PaperSurface)
        assertReadable(PaperMuted, PaperBackground)
        assertReadable(PaperMuted, PaperSurface)
        assertSheetGrounds(LightScheme)
        assertReadable(Gold, PaperBackground)
        assertReadable(Lapis, PaperBackground)
        assertReadable(Lapis, PaperSurface)
        assertReadable(PaperOnPrimary, Lapis)
        // The error roles are the quiet voice, so a Material control that
        // asks for one can never paint a stock red (owner decision, this
        // session).
        assertReadable(LightScheme.onError, LightScheme.error)
        assertReadable(LightScheme.onErrorContainer, LightScheme.errorContainer)
        assertReadable(PaperTajweed.silent, PaperBackground)
        assertReadable(PaperTajweed.ghunnah, PaperBackground)
        assertReadable(PaperTajweed.qalaqah, PaperBackground)
        assertReadable(PaperTajweed.ikhafa, PaperBackground)
        assertReadable(PaperTajweed.iqlab, PaperBackground)
        assertReadable(PaperTajweed.idgham, PaperBackground)
        assertReadable(PaperTajweed.madda, PaperBackground)
        // The rules page draws its swatches on the sheet, and the reading
        // draws the words on the selected and reciter washes as well as on
        // the plain page.
        assertTajweedOn(PaperTajweed, PaperSurface)
        assertTajweedOnWash(PaperTajweed, PaperBackground, LightScheme.primary, Wash.Selected)
        assertTajweedOnWash(
            PaperTajweed,
            PaperBackground,
            LightPage.highlight,
            LightPage.highlight.alpha * 0.55f,
        )
        // The search result's match wash: the result line is ink, not a
        // family, for the reason the class notes. The sheet is the ground
        // the wash is laid over.
        assertReadable(PaperInk, blend(LightScheme.primary, PaperSurface, Wash.Match))
        // Browse's current row wears the selected wash under its quiet words,
        // so the muted voice is measured on that ground too (owner decision,
        // this session).
        assertReadable(PaperMuted, blend(LightScheme.primary, PaperSurface, Wash.Selected))
    }

    @Test
    fun darkHolds() {
        assertReadable(NightText, NightBackground)
        assertReadable(NightText, NightSurface)
        // The reading's own ink, which is softer than the interface's word so
        // a long night read does not bloom: it is still past the floor on
        // both grounds the reading is drawn on.
        assertReadable(NightInk, NightBackground)
        assertReadable(NightInk, NightSurface)
        assertReadable(NightMuted, NightBackground)
        assertReadable(NightMuted, NightSurface)
        assertSheetGrounds(DarkScheme)
        assertReadable(NightGold, NightBackground)
        assertReadable(LapisLight, NightBackground)
        assertReadable(LapisLight, NightSurface)
        assertReadable(NightOnPrimary, LapisLight)
        // The error roles are the quiet voice on this ground too (owner
        // decision, this session).
        assertReadable(DarkScheme.onError, DarkScheme.error)
        assertReadable(DarkScheme.onErrorContainer, DarkScheme.errorContainer)
        assertReadable(NightTajweed.silent, NightBackground)
        assertReadable(NightTajweed.ghunnah, NightBackground)
        assertReadable(NightTajweed.qalaqah, NightBackground)
        assertReadable(NightTajweed.ikhafa, NightBackground)
        assertReadable(NightTajweed.iqlab, NightBackground)
        assertReadable(NightTajweed.idgham, NightBackground)
        assertReadable(NightTajweed.madda, NightBackground)
        assertTajweedOn(NightTajweed, NightSurface)
        assertTajweedOnWash(NightTajweed, NightBackground, DarkScheme.primary, Wash.Selected)
        assertTajweedOnWash(
            NightTajweed,
            NightBackground,
            DarkPage.highlight,
            DarkPage.highlight.alpha * 0.55f,
        )
        assertReadable(NightInk, blend(DarkScheme.primary, NightSurface, Wash.Match))
        // The night sheet's current row, on the same selected wash (owner
        // decision, this session).
        assertReadable(NightMuted, blend(DarkScheme.primary, NightSurface, Wash.Selected))
    }

    /**
     * The quiet words of a sheet, on every ground a sheet or a floating
     * control stands on: the sheet's own surface, the variant a component
     * asks for, and the two containers a lifted control wears. These pairs
     * are read off the scheme itself, so a test can never pin a value the
     * app no longer draws with.
     */
    private fun assertSheetGrounds(scheme: ColorScheme) {
        val quiet = scheme.onSurfaceVariant
        assertReadable(quiet, scheme.surfaceVariant)
        assertReadable(quiet, scheme.surfaceContainer)
        assertReadable(quiet, scheme.surfaceContainerHigh)
        assertReadable(quiet, scheme.surfaceContainerHighest)
    }

    private fun assertTajweedOn(colors: TajweedColors, ground: Color) {
        listOf(
            colors.silent,
            colors.ghunnah,
            colors.qalaqah,
            colors.ikhafa,
            colors.iqlab,
            colors.idgham,
            colors.madda,
        ).forEach { assertReadable(it, ground) }
    }

    private fun assertTajweedOnWash(
        colors: TajweedColors,
        ground: Color,
        tint: Color,
        alpha: Float,
    ) = assertTajweedOn(colors, blend(tint, ground, alpha))

    private fun blend(tint: Color, ground: Color, alpha: Float): Color = Color(
        red = tint.red * alpha + ground.red * (1f - alpha),
        green = tint.green * alpha + ground.green * (1f - alpha),
        blue = tint.blue * alpha + ground.blue * (1f - alpha),
        alpha = 1f,
    )

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
