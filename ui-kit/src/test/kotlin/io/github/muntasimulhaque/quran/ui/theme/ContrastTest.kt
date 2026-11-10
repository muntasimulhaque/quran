package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import io.github.muntasimulhaque.quran.ui.kit.readingSelectionColors
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
        // The reading's own ink, one step under the interface's word on the
        // day page as the night's is under its own: the verse, the
        // translation, and the tafsir are read for an hour, and a label is
        // not (owner decision, the long read session).
        assertReadable(LightPage.ink, PaperBackground)
        assertReadable(LightPage.ink, PaperSurface)
        assertReadable(LightPage.ink, blend(LightScheme.primary, PaperBackground, Wash.Selected))
        assertReadable(PaperMuted, PaperBackground)
        assertReadable(PaperMuted, PaperSurface)
        assertSheetGrounds(LightScheme)
        assertFloatingGrounds(LightScheme, day = true)
        assertReadable(Gold, PaperBackground)
        assertReadable(Lapis, PaperBackground)
        assertReadable(Lapis, PaperSurface)
        assertReadable(PaperOnPrimary, Lapis)
        // The four container roles a Material component may ask for, so a
        // stock surface can never fall back to the baseline purple (owner
        // decision, this session).
        assertReadable(LightScheme.onSecondaryContainer, LightScheme.secondaryContainer)
        assertReadable(LightScheme.onTertiaryContainer, LightScheme.tertiaryContainer)
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
        // draws the words on the selected wash and on the word mark as well
        // as on the plain page. The word mark is a wash and not a recolored
        // word: the letters keep their own ink and rule colors, so every one
        // of them is measured on the full mark, which is the strongest ground
        // the palette allows (owner decision, this session).
        assertTajweedOn(PaperTajweed, PaperSurface)
        assertTajweedOnWash(PaperTajweed, PaperBackground, LightScheme.primary, Wash.Selected)
        assertTajweedOnWash(
            PaperTajweed,
            PaperBackground,
            LightPage.highlight,
            LightPage.highlight.alpha,
        )
        assertReadable(
            LightPage.ink,
            blend(LightPage.highlight, PaperBackground, LightPage.highlight.alpha),
        )
        // The search result's match wash: the result line is ink, not a
        // family, for the reason the class notes. The sheet is the ground
        // the wash is laid over.
        assertReadable(PaperInk, blend(LightScheme.primary, PaperSurface, Wash.Match))
        // Browse's current row wears the selected wash under its quiet words,
        // so the muted voice is measured on that ground too (owner decision,
        // this session).
        assertReadable(PaperMuted, blend(LightScheme.primary, PaperSurface, Wash.Selected))
        // The quiet ground every button, off chip, and typed field stands on:
        // the muted voice is measured on the ink wash itself, not only on the
        // scheme's named slots (owner decision, this session).
        assertReadable(PaperMuted, blend(PaperInk, PaperSurface, Wash.Door))
        // A chosen search source wears the ayah door's open state: the
        // accent's ink on the accent's own tint, where the full-strength fill
        // read as a pressed button times seven (owner decision, this
        // session).
        assertReadable(Lapis, blend(Lapis, PaperSurface, Wash.Control))
        // The two text tones outside the scheme, measured the way they are drawn:
        // the opened surah introduction at 0.78 of the reading ink, and the
        // masthead at 0.9 of the interface word.
        assertReadable(blend(LightPage.introInk, PaperBackground, LightPage.introInk.alpha), PaperBackground)
        assertReadable(
            blend(LightPage.introInk, blend(LightScheme.primary, PaperBackground, Wash.Selected), LightPage.introInk.alpha),
            blend(LightScheme.primary, PaperBackground, Wash.Selected),
        )
        assertReadable(blend(LightPage.mastheadInk, PaperBackground, LightPage.mastheadInk.alpha), PaperBackground)
        // The reading's copy selection, which is the page's own word mark and
        // not Material's forty percent accent: every family and the reading's
        // ink are read on it (owner decision, the long read session).
        val lightSelection = readingSelectionColors(LightScheme.primary, LightPage.highlight)
        assertTrue(
            "the reading selects in the page's word mark",
            lightSelection.backgroundColor == LightPage.highlight,
        )
        // The mark is translucent, so it is measured the way it is drawn: over
        // the page and over a sheet, the two grounds a selected word can stand
        // on.
        val selectedOnPage = blend(
            lightSelection.backgroundColor,
            PaperBackground,
            lightSelection.backgroundColor.alpha,
        )
        val selectedOnSheet = blend(
            lightSelection.backgroundColor,
            PaperSurface,
            lightSelection.backgroundColor.alpha,
        )
        // A rule color is drawn on the page, never on a sheet: the ayah card
        // repeats nothing of the verse. The reading's ink is read on both.
        assertTajweedOn(PaperTajweed, selectedOnPage)
        assertReadable(LightPage.ink, selectedOnPage)
        assertReadable(LightPage.ink, selectedOnSheet)
    }

    /**
     * The seven rule colors are one weight on the page, not seven.
     *
     * A long read made the old palette flicker: its families spanned 1.76 in
     * contrast on paper, so the lightest read as the only colored words and
     * the darkest read as emphasis on the ink. The band is the decision the
     * long read session made, and it is pinned here so a later tone cannot
     * quietly walk out of it (owner decision, the long read session).
     */
    @Test
    fun readingWeightsHoldOneBand() {
        assertBand(PaperTajweed, PaperBackground, 6.0, 7.8)
        assertBand(NightTajweed, NightBackground, 6.5, 8.2)
        // The ink is above every family on both grounds, so a colored letter
        // never outweighs the plain word beside it.
        assertTrue(contrast(LightPage.ink, PaperBackground) > 10.0)
        assertTrue(contrast(NightInk, NightBackground) > 10.0)
    }

    /**
     * The reading ink's own band: soft enough for an hour of paper and still
     * far past every floor. The pair a reader actually looks at is pinned the
     * same way the rule colors are.
     */
    @Test
    fun readingInkHoldsItsBand() {
        val day = contrast(LightPage.ink, PaperBackground)
        assertTrue("the day reading ink is ${"%.2f".format(day)}:1, outside 11 to 13", day in 11.0..13.0)
        val night = contrast(NightInk, NightBackground)
        assertTrue(
            "the night reading ink is ${"%.2f".format(night)}:1, outside 11 to 13",
            night in 11.0..13.0,
        )
    }

    private fun assertBand(colors: TajweedColors, ground: Color, low: Double, high: Double) {
        val ratios = listOf(
            colors.silent,
            colors.ghunnah,
            colors.qalaqah,
            colors.ikhafa,
            colors.iqlab,
            colors.idgham,
            colors.madda,
        ).map { contrast(it, ground) }
        ratios.forEach { ratio ->
            assertTrue(
                "a family is ${"%.2f".format(ratio)}:1 on the page, outside the $low to $high band",
                ratio in low..high,
            )
        }
        val spread = ratios.max() / ratios.min()
        assertTrue(
            "the seven span ${"%.2f".format(spread)}, over the 1.30 the band allows",
            spread <= 1.30,
        )
    }

    @Test
    fun controlBoundariesHold() {
        // A control's edge is not a hairline: a swatch, a door, or any shape
        // whose boundary identifies it owes 3:1. The edge is the ground's own
        // ink at [Edge.Control], so the four pairs are the four grounds a
        // sheet or a page can stand on.
        assertBoundary(PaperInk, PaperBackground)
        assertBoundary(PaperInk, PaperSurface)
        assertBoundary(NightText, NightBackground)
        assertBoundary(NightText, NightSurface)
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
        assertFloatingGrounds(DarkScheme, day = false)
        assertReadable(NightGold, NightBackground)
        assertReadable(LapisLight, NightBackground)
        assertReadable(LapisLight, NightSurface)
        assertReadable(NightOnPrimary, LapisLight)
        assertReadable(DarkScheme.onSecondaryContainer, DarkScheme.secondaryContainer)
        assertReadable(DarkScheme.onTertiaryContainer, DarkScheme.tertiaryContainer)
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
            DarkPage.highlight.alpha,
        )
        assertReadable(NightInk, blend(DarkPage.highlight, NightBackground, DarkPage.highlight.alpha))
        assertReadable(NightInk, blend(DarkScheme.primary, NightSurface, Wash.Match))
        // The night sheet's current row, on the same selected wash (owner
        // decision, this session).
        assertReadable(NightMuted, blend(DarkScheme.primary, NightSurface, Wash.Selected))
        // The quiet ground and the chosen search source, as on the day sheet
        // (owner decision, this session).
        assertReadable(NightMuted, blend(NightText, NightSurface, Wash.Door))
        assertReadable(LapisLight, blend(LapisLight, NightSurface, Wash.Control))
        // The same two tones on the night page.
        assertReadable(blend(DarkPage.introInk, NightBackground, DarkPage.introInk.alpha), NightBackground)
        assertReadable(
            blend(DarkPage.introInk, blend(DarkScheme.primary, NightBackground, Wash.Selected), DarkPage.introInk.alpha),
            blend(DarkScheme.primary, NightBackground, Wash.Selected),
        )
        assertReadable(blend(DarkPage.mastheadInk, NightBackground, DarkPage.mastheadInk.alpha), NightBackground)
        // The night reading's copy selection, the same word mark the reciter's
        // word wears on that ground (owner decision, the long read session).
        val darkSelection = readingSelectionColors(DarkScheme.primary, DarkPage.highlight)
        assertTrue(
            "the night reading selects in the page's word mark",
            darkSelection.backgroundColor == DarkPage.highlight,
        )
        val nightSelectedOnPage = blend(
            darkSelection.backgroundColor,
            NightBackground,
            darkSelection.backgroundColor.alpha,
        )
        val nightSelectedOnSheet = blend(
            darkSelection.backgroundColor,
            NightSurface,
            darkSelection.backgroundColor.alpha,
        )
        assertTajweedOn(NightTajweed, nightSelectedOnPage)
        assertReadable(NightInk, nightSelectedOnPage)
        assertReadable(NightInk, nightSelectedOnSheet)
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

    /**
     * The floating tone is a step of its own, not the page and not the sheet.
     *
     * The pill, the offer, the snackbar, the pack row, and the popup all wear
     * `surfaceContainerHigh`, and the page is visible around every one of
     * them: a menu is a separate surface over a reading whose own words are
     * still on the glass. The tone has to say that at a glance, without a hard
     * border, so it owes a perceptible step from the page it floats over and
     * from the sheet a panel rises in. The day ground used to stand at 1.07:1
     * from its page while the night stood at 1.23:1, which is why a light pill
     * went missing and a night pill did not (owner report).
     *
     * The two grounds take two rules, because they have two amounts of room.
     * The night lifts its tone 8.3 L* above its page, and a step of lightness
     * on a dark ground is a step the eye reads on its own, so the floor there
     * stays a ratio. The day has two points of headroom to white and a chosen
     * block at L* 92.9, so its step has to be a warmth, and its rules are
     * CIELAB ones: the warmth itself, the band the step may take, and the
     * distance from the block a menu is actually drawn on. All of them are
     * pinned here, so a palette edit fails on the machine and not on a
     * reader's eyes (owner decision, this session).
     */
    private fun assertFloatingGrounds(scheme: ColorScheme, day: Boolean) {
        val floating = scheme.surfaceContainerHigh
        assertReadable(scheme.onSurface, floating)
        assertFloating("the floating tone on the sheet", floating, scheme.surface, 1.10)
        if (day) {
            assertWarmthIsTheStep(floating, scheme.background)
            assertClearOfTheChosenBlock(
                floating,
                blend(scheme.primary, scheme.background, Wash.Selected),
            )
        } else {
            assertFloating("the night floating tone on the page", floating, scheme.background, 1.20)
        }
    }

    /**
     * The day's step is warmth, not shade (owner decision, this session).
     *
     * The page stands at L* 98.0 and the block a menu is raised over stands at
     * L* 92.9, so the lightness has no room to give, and both tones that asked
     * it for the whole step read wrong: the 17.2 paper at 8.0 L* down with a
     * warm tint read as a sepia over the page, and the 17.3 paper at the same
     * 8.0 L* down with the page's own chroma read as a smudge (owner reports).
     * The floating paper is therefore the page's own paper: three times its
     * chroma at its hue to within thirty degrees, dropped 3.0 to 6.5 L* and no
     * further. That band keeps the step off both failures, since a shade is
     * what 8.0 L* down is and a card with no tone of its own is what 0.4 L*
     * down and no chroma was (the white pill before 17.0).
     */
    private fun assertWarmthIsTheStep(floating: Color, page: Color) {
        val drop = lightness(floating) - lightness(page)
        assertTrue(
            "the floating paper stands ${"%.1f".format(-drop)} L* under the page, outside the 3.0 to " +
                "6.5 a warmth step allows",
            drop <= -3.0 && drop >= -6.5,
        )
        val times = chroma(floating) / chroma(page)
        assertTrue(
            "the floating paper carries ${"%.2f".format(times)} times the page's chroma, under the 3.0 " +
                "that makes the step a hue",
            times >= 3.0,
        )
        val turned = hueGap(floating, page)
        assertTrue(
            "the floating paper stands ${"%.1f".format(turned)} degrees of hue from the page, outside " +
                "the 30 that keep it the page's own paper",
            turned <= 30.0,
        )
    }

    /**
     * The distance a menu's edge needs, measured from the ground the menu is
     * actually drawn on.
     *
     * A menu is never raised over the plain page: the ayah whose menu is open
     * paints its own block at [Wash.Selected] of the accent, which lands at
     * L* 92.9 and chroma 0.71, so the edge is read against that block. The
     * floating tone that read as a veil stood dE76 4.2 from it while holding
     * every ratio it was asked for (owner report), because the two tones sat at
     * one lightness and differed only in a chroma that no WCAG ratio can see.
     * A CIELAB distance is the honest measure of that pair, and 6.0 is the
     * distance at which the edge reads as a card at a glance.
     */
    private fun assertClearOfTheChosenBlock(floating: Color, block: Color) {
        val distance = deltaE(floating, block)
        assertTrue(
            "the floating paper stands dE76 ${"%.1f".format(distance)} from the chosen block, under " +
                "the 6.0 a menu's edge is read at",
            distance >= 6.0,
        )
    }

    private fun assertFloating(what: String, floating: Color, behind: Color, floor: Double) {
        val ratio = contrast(floating, behind)
        assertTrue(
            "$what: ${"%.2f".format(ratio)}:1, under the ${"%.2f".format(floor)}:1 " +
                "the floating paper is drawn at",
            ratio >= floor,
        )
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

    private fun assertBoundary(ink: Color, ground: Color) {
        val edge = blend(ink, ground, Edge.Control)
        val ratio = contrast(edge, ground)
        assertTrue(
            "the control edge 0x${edge.value.toString(16).uppercase()} on " +
                "0x${ground.value.toString(16).uppercase()} is ${"%.2f".format(ratio)}:1, " +
                "under the 3:1 non-text floor",
            ratio >= 3.0,
        )
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

    /**
     * A tone in CIELAB, in the D65 white the palette is chosen in.
     *
     * CIELAB is where an adjacent surface's step is decided, because the
     * numbers a step needs are not the numbers a ratio carries: two tones can
     * hold every ratio asked of them and still sit at one lightness, which is
     * what the eye said of the 17.2 paper and again of the 17.3 paper (owner
     * reports). The lightness, the chroma, and the hue of a tone are read from
     * here, and so is the distance between two of them.
     */
    private data class Lab(val lightness: Double, val a: Double, val b: Double) {
        val chroma: Double get() = Math.hypot(a, b)
        val hue: Double get() = (Math.toDegrees(Math.atan2(b, a)) + 360.0) % 360.0
    }

    private fun lab(color: Color): Lab {
        fun linear(channel: Float): Double =
            if (channel <= 0.04045f) channel / 12.92 else Math.pow((channel + 0.055) / 1.055, 2.4)
        val red = linear(color.red)
        val green = linear(color.green)
        val blue = linear(color.blue)
        val x = red * 0.4124564 + green * 0.3575761 + blue * 0.1804375
        val y = red * 0.2126729 + green * 0.7151522 + blue * 0.0721750
        val z = red * 0.0193339 + green * 0.1191920 + blue * 0.9503041
        fun f(t: Double): Double = if (t > 0.008856) Math.cbrt(t) else 7.787 * t + 16.0 / 116.0
        val fx = f(x / 0.95047)
        val fy = f(y)
        val fz = f(z / 1.08883)
        return Lab(116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz))
    }

    /** A tone's CIELAB lightness, which is what a step of paper is measured by. */
    private fun lightness(color: Color): Double = lab(color).lightness

    /** A tone's CIELAB chroma, the distance from the gray axis. */
    private fun chroma(color: Color): Double = lab(color).chroma

    /**
     * The shortest turn between two tones' hues, in degrees: the one measure of
     * "the same paper, warmed" that no step of lightness can give.
     */
    private fun hueGap(a: Color, b: Color): Double {
        val gap = Math.abs(lab(a).hue - lab(b).hue) % 360.0
        return if (gap > 180.0) 360.0 - gap else gap
    }

    /** CIE76, the straight-line distance between two tones in CIELAB. */
    private fun deltaE(a: Color, b: Color): Double {
        val first = lab(a)
        val second = lab(b)
        return Math.sqrt(
            Math.pow(first.lightness - second.lightness, 2.0) +
                Math.pow(first.a - second.a, 2.0) +
                Math.pow(first.b - second.b, 2.0),
        )
    }
}
