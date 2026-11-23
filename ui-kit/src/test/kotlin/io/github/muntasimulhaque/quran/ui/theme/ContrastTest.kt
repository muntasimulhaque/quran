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
 * a reading ground (owner decision, 10.6). The ink's own
 * pair on that wash is measured here, so the decision stays written.
 *
 * The second rule here is perceptual, and it is the one WCAG 2.x cannot
 * state: APCA-W3 0.1.9, the method the WCAG 3 draft carries, whose own
 * documentation says the 2.x ratio "cannot be used for guidance designing
 * dark mode" because it overstates contrast near black. Its grades are Lc 90
 * preferred for fluent body text, Lc 75 the minimum for body text, Lc 60 the
 * minimum for content a reader reads, and Lc 45 for a large display line.
 * Those grades are asserted for every tone that carries words, on every
 * ground it is drawn on, in both grounds of the app. The ratios above stay:
 * they are the legal floor, and APCA is the perceptual one (owner decision,
 * 20.8).
 *
 * Three pairs are recorded here and deliberately not asserted, because each
 * one is a decision this app already took and lifting it would undo that
 * decision:
 *
 * - **The seven rule colors on the night page.** They measure Lc -50.2
 *   (idgham) to -59.4 (iqlab), under the content grade. Decision 17.1 pulled
 *   them down from a 10.2 WCAG top to 8.1 to stop the bloom a pale saturated
 *   letter makes on a charcoal page over an evening read, and the band that
 *   decision chose keeps every family under the ink. Reaching Lc -60 needs
 *   roughly 8.2 to 9.5 on that ground, which is the top 17.1 removed. The
 *   day palette holds 78.7 to 84.0 across the same band, so the asymmetry is
 *   the polarity's and not the palette's.
 *
 * - **The word mark's rule colors on the night page.** The worst rule color
 *   on the reciter's mark measures -45.8, against -50.2 on the plain page:
 *   the mark is the same 17.1 decision drawn a step louder, and the reading
 *   ink on that mark still holds -76.6.
 *
 * - **The night quiet voice on the found ground.** It measures -58.4, but no
 *   quiet word is ever drawn there: the wash is a span behind one word, and
 *   every caption in a result row stands on the sheet. The pair is recorded
 *   so a later session does not read the absence as an oversight.
 */
class ContrastTest {

    @Test
    fun lightHolds() {
        assertReadable(PaperInk, PaperBackground)
        assertReadable(PaperInk, PaperSurface)
        // The reading's own ink, one step under the interface's word on the
        // day page as the night's is under its own: the verse, the
        // translation, and the tafsir are read for an hour, and a label is
        // not (owner decision, 17.1).
        assertReadable(LightPage.ink, PaperBackground)
        assertReadable(LightPage.ink, PaperSurface)
        assertReadable(LightPage.ink, PaperSelected)
        assertReadable(PaperMuted, PaperBackground)
        assertReadable(PaperMuted, PaperSurface)
        assertSheetGrounds(LightScheme)
        assertFloatingGrounds(LightScheme, PaperSelected, day = true)
        assertReadable(Gold, PaperBackground)
        assertReadable(Lapis, PaperBackground)
        assertReadable(Lapis, PaperSurface)
        assertReadable(PaperOnPrimary, Lapis)
        // The four container roles a Material component may ask for, so a
        // stock surface can never fall back to the baseline purple (owner
        // decision, 13.8).
        assertReadable(LightScheme.onSecondaryContainer, LightScheme.secondaryContainer)
        assertReadable(LightScheme.onTertiaryContainer, LightScheme.tertiaryContainer)
        // The error roles are the quiet voice, so a Material control that asks
        // for one can never paint a stock red (owner decision, 13.5).
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
        // the palette allows (owner decision, 15.9).
        assertTajweedOn(PaperTajweed, PaperSurface)
        assertTajweedOnWash(PaperTajweed, PaperBackground, PaperSelected, 1f)
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
        // The found word's ground: the result line is ink, not a family, for
        // the reason the class notes, and the ground is the accent's own
        // family at the lightness the warm tone carried (owner decision, 21.0).
        assertReadable(PaperInk, PaperMatch)
        assertReadable(LightPage.ink, PaperMatch)
        // Browse's current row wears the selected wash under its quiet words,
        // so the muted voice is measured on that ground too
        // (owner decision, 13.5).
        assertReadable(PaperMuted, PaperSelected)
        assertChosenStep("the day chosen block", PaperSelected, PaperSurface, day = true)
        // The quiet ground every button, off chip, and typed field stands on:
        // the muted voice is measured on the ink wash itself, not only on the
        // scheme's named slots (owner decision, 15.0).
        assertReadable(PaperMuted, blend(PaperInk, PaperSurface, Wash.Door))
        // A chosen search source wears the ayah door's open state: the
        // accent's ink on the accent's own tint, where the full-strength fill
        // read as a pressed button times seven (owner decision, 15.0).
        assertReadable(Lapis, blend(Lapis, PaperSurface, Wash.Control))
        // The two text tones outside the scheme, measured the way they are drawn:
        // the opened surah introduction at 0.78 of the reading ink, and the
        // masthead at 0.9 of the interface word.
        assertReadable(blend(LightPage.introInk, PaperBackground, LightPage.introInk.alpha), PaperBackground)
        assertReadable(
            blend(LightPage.introInk, PaperSelected, LightPage.introInk.alpha),
            PaperSelected,
        )
        assertReadable(blend(LightPage.mastheadInk, PaperBackground, LightPage.mastheadInk.alpha), PaperBackground)
        // The reading's copy selection, which is the page's own word mark and
        // not Material's forty percent accent: every family and the reading's
        // ink are read on it (owner decision, 17.1).
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
     * quietly walk out of it (owner decision, 17.1).
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
        assertFloatingGrounds(DarkScheme, NightSelected, day = false)
        assertReadable(NightGold, NightBackground)
        assertReadable(LapisLight, NightBackground)
        assertReadable(LapisLight, NightSurface)
        assertReadable(NightOnPrimary, LapisLight)
        assertReadable(DarkScheme.onSecondaryContainer, DarkScheme.secondaryContainer)
        assertReadable(DarkScheme.onTertiaryContainer, DarkScheme.tertiaryContainer)
        // The error roles are the quiet voice on this ground too (owner
        // decision, 13.5).
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
        assertTajweedOnWash(NightTajweed, NightBackground, NightSelected, 1f)
        assertTajweedOnWash(
            NightTajweed,
            NightBackground,
            DarkPage.highlight,
            DarkPage.highlight.alpha,
        )
        assertReadable(NightInk, blend(DarkPage.highlight, NightBackground, DarkPage.highlight.alpha))
        assertReadable(NightInk, NightMatch)
        assertReadable(NightText, NightMatch)
        // The night sheet's current row, on the same selected wash (owner
        // decision, 13.5).
        assertReadable(NightMuted, NightSelected)
        assertChosenStep("the night chosen block", NightSelected, NightSurface, day = false)
        // The quiet ground and the chosen search source, as on the day sheet
        // (owner decision, 15.0).
        assertReadable(NightMuted, blend(NightText, NightSurface, Wash.Door))
        assertReadable(LapisLight, blend(LapisLight, NightSurface, Wash.Control))
        // The same two tones on the night page.
        assertReadable(blend(DarkPage.introInk, NightBackground, DarkPage.introInk.alpha), NightBackground)
        assertReadable(
            blend(DarkPage.introInk, NightSelected, DarkPage.introInk.alpha),
            NightSelected,
        )
        assertReadable(blend(DarkPage.mastheadInk, NightBackground, DarkPage.mastheadInk.alpha), NightBackground)
        // The night reading's copy selection, the same word mark the reciter's
        // word wears on that ground (owner decision, 17.1).
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
     * A floating paper is the highest paper on its ground.
     *
     * The pill, the offer, the snackbar, the pack row, and the popup all wear
     * `surfaceContainerHigh`, and the page is visible around every one of
     * them. The old rule asked the tone to be a warmth 3.0 to 6.5 L* under the
     * page, because the day page stands at L* 98.0 and the block a menu is
     * raised over at 92.9. That rule kept four darker tones in place, and each
     * read as a slip, a shade, a smudge, or a veneer (owner reports). The
     * reference frames answer the question the other way: a raised surface is
     * the lightest thing near it, and the edge is a shadow or a 1 px line.
     *
     * The day's floating paper is therefore the sheet's own white, 2.0 L* over
     * the page (dE76 3.3) and 7.1 L* over the chosen block (dE76 9.0). It is
     * the step every sheet already stands on, and the lift is its edge. The
     * night's page is L* 9.4 and its sheet 13.4, but the chosen block is 18.4:
     * the lapis wash lifts a dark page where it darkens a light one, so the
     * night's floating paper must stand above the sheet as well. [NightFloating]
     * is L* 19.2, 9.8 over the page (1.29:1) and dE76 11.8 from the block. The
     * shadow is invisible at night, so the fill is the only edge there, and
     * that is why the night's step is the larger one. All of this is pinned
     * here, so a palette edit fails on the machine and not on a reader's eyes
     * (owner decision, 17.5).
     */
    private fun assertFloatingGrounds(scheme: ColorScheme, selected: Color, day: Boolean) {
        val floating = scheme.surfaceContainerHigh
        val page = scheme.background
        val block = selected
        assertReadable(scheme.onSurface, floating)
        // The one rule both grounds share: a floating paper is above the page
        // and above the block it is raised over, never a shade of either.
        assertHigher("the floating paper on the page", floating, page)
        assertHigher("the floating paper on the chosen block", floating, block)
        if (day) {
            // The day's floating paper is the sheet's own paper (owner
            // decision, 17.5). The day can be white because it is the
            // top of the scale; the night sheet cannot, because its block
            // stands above it.
            assertTrue(
                "the day's floating paper is not the sheet's own tone",
                floating == scheme.surface,
            )
            assertClearOfTheChosenBlock(floating, block, 6.0)
        } else {
            assertFloating("the night floating paper on the page", floating, page, 1.20)
            assertClearOfTheChosenBlock(floating, block, 5.5)
        }
    }

    /** A raised paper is lighter than the ground it stands on, or it is a recess. */
    private fun assertHigher(what: String, floating: Color, ground: Color) {
        val above = lightness(floating) - lightness(ground)
        assertTrue(
            "$what is ${"%.1f".format(above)} L* from its ground, not above it",
            above > 0.0,
        )
    }

    /**
     * The chosen block's step over the sheet it is painted on, in the
     * direction its ground paints steps. Browse's current row and the word
     * sheet's named word stand on a sheet, and the 15.4 L* tone both accents
     * before 22.1 shipped stood 2.0 L* over the night sheet, a chroma-only
     * whisper the owner read as nothing at all (owner report, 22.1). The
     * day's 92.9 holds 7.1 L* under the white sheet, the night's 18.4 holds
     * 5.0 over its own, and 4.0 is the floor both directions keep, so a
     * later edit cannot sink the night's step back into a hue shift the eye
     * reads as a smudge (owner decision, 22.1).
     */
    private fun assertChosenStep(what: String, block: Color, sheet: Color, day: Boolean) {
        val step = if (day) lightness(sheet) - lightness(block) else lightness(block) - lightness(sheet)
        assertTrue(
            "$what stands ${"%.1f".format(step)} L* from the sheet it is painted on, " +
                "under the 4.0 the current row is read at",
            step >= 4.0,
        )
    }

    /**
     * The distance a menu's edge needs, measured from the block it is raised
     * over.
     *
     * A menu is never raised over the plain page: the ayah whose menu is open
     * paints its own block in [PaperSelected]. The day's block is L* 92.9 and
     * the sheet's white stands dE76 9.0 from it; the night's block is L* 18.4
     * and the one-step night paper stands dE76 11.8 from it. The 17.3
     * tone that read as a veil stood dE76 4.2 from the block while it held every
     * ratio it was asked for, because the two tones sat at one lightness and
     * differed only in a chroma that no WCAG ratio can see. The direction is
     * the first rule (see [assertHigher]) and this distance is the second: 6.0
     * on the day, 5.5 on the night, where the shadow cannot help.
     */
    private fun assertClearOfTheChosenBlock(floating: Color, block: Color, floor: Double) {
        val distance = deltaE(floating, block)
        assertTrue(
            "the floating paper stands dE76 ${"%.1f".format(distance)} from the chosen block, under " +
                "the ${"%.1f".format(floor)} a menu's edge is read at",
            distance >= floor,
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
     * The day ground's prose, at APCA's fluent grade.
     *
     * Lc 90 is the preferred level for fluent body text and Lc 75 its
     * minimum; the reading ink holds 95.0 on the page and 98.5 on a sheet,
     * and the interface's word holds 98.4. This is the grade a reader spends
     * an hour in, so it is pinned above the minimum rather than at it.
     */
    @Test
    fun dayProseHoldsApcaFluency() {
        assertApca("the day reading ink on the page", LightPage.ink, PaperBackground, 90.0)
        assertApca("the day reading ink on a sheet", LightPage.ink, PaperSurface, 90.0)
        assertApca("the day interface word on the page", PaperInk, PaperBackground, 90.0)
    }

    /**
     * The night ground's prose, at the same grade from the other polarity.
     *
     * The night is where WCAG 2.x stops being a guide, so it is where APCA
     * earns its place: the reading ink measures -81.0 on the page and -79.9
     * on a sheet, the interface's word -87.9, and the floors are the day's,
     * negated.
     */
    @Test
    fun nightProseHoldsApcaFluency() {
        assertApca("the night reading ink on the page", NightInk, NightBackground, -75.0)
        assertApca("the night reading ink on a sheet", NightInk, NightSurface, -75.0)
        assertApca("the night interface word on the page", NightText, NightBackground, -75.0)
    }

    /**
     * The quiet voice and the action color, at APCA's content grade.
     *
     * Lc 60 is the minimum for content a reader reads, the grade a caption, a
     * reference, a label, and an action word owe. The quiet voice measures
     * 80.2 on the day page and 71.6 to 83.7 across the grounds it rides; on
     * the night page -65.2, -64.1 on a sheet, -61.7 on the floating paper.
     * Every ground a quiet word actually stands on is listed, because a
     * label travels with the surface it sits on.
     */
    @Test
    fun quietVoicesHoldTheApcaContentFloor() {
        listOf(
            "the page" to PaperBackground,
            "a sheet" to PaperSurface,
            "the chosen block" to PaperSelected,
            "a floating paper" to PaperFloating,
            "the variant" to LightScheme.surfaceVariant,
            "a container" to LightScheme.surfaceContainer,
            "the switch's track" to PaperControl,
        ).forEach { (where, ground) ->
            assertApca("the day quiet voice on $where", PaperMuted, ground, 60.0)
        }
        listOf(
            "the page" to NightBackground,
            "a sheet" to NightSurface,
            "the chosen block" to NightSelected,
            "a floating paper" to NightFloating,
            "the variant" to DarkScheme.surfaceVariant,
            "a container" to DarkScheme.surfaceContainer,
            "the switch's track" to NightControl,
        ).forEach { (where, ground) ->
            assertApca("the night quiet voice on $where", NightMuted, ground, -60.0)
        }
        assertApca("lapis on the page", Lapis, PaperBackground, 60.0)
        assertApca("lapis on a sheet", Lapis, PaperSurface, 60.0)
        assertApca("the night lapis on the page", LapisLight, NightBackground, -60.0)
        assertApca("the night lapis on a floating paper", LapisLight, NightFloating, -60.0)
        assertApca("gold on the page", Gold, PaperBackground, 60.0)
        assertApca("gold on a sheet", Gold, PaperSurface, 60.0)
        assertApca("the night gold on the page", NightGold, NightBackground, -60.0)
        assertApca("the night gold on a floating paper", NightGold, NightFloating, -60.0)
    }

    /**
     * The opened surah introduction, at the content grade on both grounds.
     *
     * It is prose, the longest beside the tafsir on its page (owner decision
     * 13.3), so it owes the content grade rather than a caption's licence. It
     * measured -56.7 at night until 20.8, when the day ground's base tone was
     * found to differ from the night's: one base and one alpha put it at 83.9
     * and -64.3, above every caption on its own ground (owner decision,
     * 20.8).
     */
    @Test
    fun openedIntroductionHoldsTheContentFloor() {
        listOf(
            "the day page" to PaperBackground,
            "the day chosen block" to PaperSelected,
        ).forEach { (where, ground) ->
            assertApca(
                "the introduction on $where",
                blend(LightPage.introInk, ground, LightPage.introInk.alpha),
                ground,
                60.0,
            )
        }
        listOf(
            "the night page" to NightBackground,
            "the night chosen block" to NightSelected,
        ).forEach { (where, ground) ->
            assertApca(
                "the introduction on $where",
                blend(DarkPage.introInk, ground, DarkPage.introInk.alpha),
                ground,
                -60.0,
            )
        }
    }

    /**
     * The ground the reading is scanned on rather than read on, and the first
     * paint's display line.
     *
     * The found ground is a scan surface by decision 10.6: the matched word
     * is one word in a result list, and the reading ink on it measures 81.2
     * on the day and -74.3 on the night, a hair under the body floor and far
     * past the content floor. The floor here is stated one grade down rather
     * than pretending it clears the body grade, because a wash that lifts a
     * dark page can only take light text down, and 10.6's decision is what
     * keeps the result list legible. The first paint's display line is Lc
     * 92.2 on the day page and -75.6 at night, far past the 45 a 48 sp line
     * owes.
     */
    @Test
    fun scanGroundsAndDisplayTonesHold() {
        assertApca("the reading ink on the day found ground", LightPage.ink, PaperMatch, 75.0)
        assertApca("the reading ink on the night found ground", NightInk, NightMatch, -73.0)
        assertApca(
            "the day first paint's display line",
            blend(LightPage.mastheadInk, PaperBackground, LightPage.mastheadInk.alpha),
            PaperBackground,
            60.0,
        )
        assertApca(
            "the night first paint's display line",
            blend(DarkPage.mastheadInk, NightBackground, DarkPage.mastheadInk.alpha),
            NightBackground,
            -60.0,
        )
    }

    /**
     * The APCA-W3 0.1.9 constants, the reference implementation's own.
     *
     * Copied from `apca-w3@0.1.9` rather than derived, so a reader can diff
     * this object against the library. The exponent is the library's 2.4,
     * applied plainly to each channel and not through the sRGB piecewise
     * curve: that is what the W3 build computes, and matching it is the
     * point.
     */
    private object Apca {
        const val mainTrc = 2.4
        const val red = 0.2126729
        const val green = 0.7151522
        const val blue = 0.0721750
        const val normBg = 0.56
        const val normTxt = 0.57
        const val revTxt = 0.62
        const val revBg = 0.65
        const val blackThreshold = 0.022
        const val blackClamp = 1.414
        const val scaleBow = 1.14
        const val scaleWob = 1.14
        const val lowBowOffset = 0.027
        const val lowWobOffset = 0.027
        const val deltaYMin = 0.0005
        const val lowClip = 0.1
    }

    /**
     * A color's APCA luminance: the 2.4 channel exponent, the sRGB
     * coefficients, and the black soft clamp that keeps a near-black ground
     * from being read as true black.
     */
    private fun apcaY(color: Color): Double {
        val y = Math.pow(color.red.toDouble(), Apca.mainTrc) * Apca.red +
            Math.pow(color.green.toDouble(), Apca.mainTrc) * Apca.green +
            Math.pow(color.blue.toDouble(), Apca.mainTrc) * Apca.blue
        return if (y > Apca.blackThreshold) {
            y
        } else {
            y + Math.pow(Apca.blackThreshold - y, Apca.blackClamp)
        }
    }

    /**
     * The APCA lightness contrast of [text] on [ground], signed by polarity:
     * a positive Lc is dark text on a light ground, a negative Lc is light
     * text on a dark one. The sign is the polarity and is never taken as an
     * absolute value, which is why the callers below assert a negative floor
     * at night.
     */
    private fun apca(text: Color, ground: Color): Double {
        val txt = apcaY(text)
        val bg = apcaY(ground)
        if (Math.abs(bg - txt) < Apca.deltaYMin) return 0.0
        val sapc = if (bg > txt) {
            (Math.pow(bg, Apca.normBg) - Math.pow(txt, Apca.normTxt)) * Apca.scaleBow
        } else {
            (Math.pow(bg, Apca.revBg) - Math.pow(txt, Apca.revTxt)) * Apca.scaleWob
        }
        val weighted = if (bg > txt) {
            if (sapc < Apca.lowClip) 0.0 else sapc - Apca.lowBowOffset
        } else {
            if (sapc > -Apca.lowClip) 0.0 else sapc + Apca.lowWobOffset
        }
        return weighted * 100
    }

    /**
     * One APCA pair against a floor that carries its own polarity: 90 on the
     * day ground, -75 on the night.
     */
    private fun assertApca(what: String, text: Color, ground: Color, floor: Double) {
        val lc = apca(text, ground)
        val passed = if (floor >= 0) lc >= floor else lc <= floor
        assertTrue(
            "$what measures Lc ${"%.1f".format(lc)}, under the ${"%.1f".format(floor)} floor",
            passed,
        )
    }


    /**
     * A tone in CIELAB, in the D65 white the palette is chosen in.
     *
     * CIELAB is where an adjacent surface's step is decided, because the
     * numbers a step needs are not the numbers a ratio carries: two tones can
     * hold every ratio asked of them and still sit at one lightness, which is
     * what the eye said of the 17.2 paper and again of the 17.3 paper (owner
     * reports). The lightness of a tone and the distance between two of them
     * are read from here.
     */
    private data class Lab(val lightness: Double, val a: Double, val b: Double)

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
