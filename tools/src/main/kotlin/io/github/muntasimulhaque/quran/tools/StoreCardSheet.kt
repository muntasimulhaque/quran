package io.github.muntasimulhaque.quran.tools

import java.awt.Rectangle

/**
 * The store cards: the one place the store's own words, palette, and geometry
 * are written.
 *
 * Each card is a real capture from the app's screenshot test, laid on the
 * app's own paper inside an outline drawn in the app's ink, under one line of
 * type set in the app's own Literata. The card around the capture is the
 * store's advertising; the screen inside it is the app, untouched.
 *
 * The compositor lives here and not on the device. Composing on the device
 * was the first shape and it was set aside: a caption change is the thing a
 * listing iterates on most, and on the device every one of those changes costs
 * a clean install and a capture leg, while a JVM compositor re-renders the
 * whole set in seconds and sits inside `:tools:test`, which is already a
 * release gate. The device was also the only place that could shape a
 * complex script, and the set is English by the owner's word, so nothing was
 * lost by moving it (owner decision, 22.4).
 *
 * The palette is the theme's own, cited by constant so a card can never
 * drift into a color the app does not wear: the paper field, the ink, the
 * muted line, and the gold are [io.github.muntasimulhaque.quran.ui.theme]'s
 * `PaperBackground` `#FBF9F4`, `PaperInk` `#292622`, `PaperMuted` `#5D5A55`,
 * and `Gold` `#856E18`, and the night card keeps that page's own ground and
 * word, `NightBackground` `#1C1A18`, `NightText` `#E5E2DB`, and its gold
 * `NightGold` `#DCB870`. The gold rule is the feature graphic's rule, so the
 * icon, the feature graphic, and the cards read as one face.
 *
 * The type band is above the frame on every form, the ten inch card
 * included. A side-by-side column was drawn first and set aside: on a
 * landscape canvas a capture keeps its 1.42:1 shape, which leaves a type
 * column narrow enough to break a headline into four ragged lines, and the
 * wide card's own landscape reading is the argument, so it wants the widest
 * frame the band leaves (owner decision, 22.4).
 *
 * The copy is a sentence a reader would say, never a feature's label, and it
 * is held to the listing by `StoreCardSheetTest`, which reads the same words
 * out of `play-store/listing.md`.
 */
object StoreCardSheet {

    /**
     * One card: the capture it frames, its headline, and the quiet sub-line
     * that only the first card carries, because the first card has to make the
     * promise the rest of the set spends its frames keeping.
     */
    data class StoreCard(
        val frame: String,
        val headline: String,
        val sub: String?,
    )

    /** The three form factors Play asks for, each with its canvas. */
    enum class CardForm(val folder: String, val width: Int, val height: Int) {
        Phone("phone", 1080, 1920),
        Tablet7("tablet7", 1200, 1920),
        Tablet10("tablet10", 2560, 1800),
    }

    /**
     * One face of the card. Every value is an ARGB int whose RGB is the
     * theme's own color; the card's ground is opaque and the file carries no
     * alpha.
     */
    data class CardFace(
        val name: String,
        val ground: Int,
        val word: Int,
        val muted: Int,
        val rule: Int,
        val outline: Int,
    ) {
        /** Every color this face names, for the test that reads the theme. */
        val colors: List<Int> get() = listOf(ground, word, muted, rule, outline)
    }

    /** The day page: the paper field, the ink, and the ornament gold. */
    val paper = CardFace(
        name = "paper",
        ground = 0xFFFBF9F4.toInt(),
        word = 0xFF292622.toInt(),
        muted = 0xFF5D5A55.toInt(),
        rule = 0xFF856E18.toInt(),
        outline = 0xFF292622.toInt(),
    )

    /**
     * The night page. Its muted line is never drawn: no night card carries a
     * sub-line, and the field holds the night's own word so the face stays one
     * family rather than borrowing a color the page does not wear.
     */
    val night = CardFace(
        name = "night",
        ground = 0xFF1C1A18.toInt(),
        word = 0xFFE5E2DB.toInt(),
        muted = 0xFFE5E2DB.toInt(),
        rule = 0xFFDCB870.toInt(),
        outline = 0xFFE5E2DB.toInt(),
    )

    val faces: List<CardFace> = listOf(paper, night)

    /**
     * The eight, in the order a reader meets them. Each headline is under
     * eight words and ends a sentence; the first card also says, in the sub,
     * the three things the app is free of.
     */
    val cards: List<StoreCard> = listOf(
        StoreCard("01-reading", "Built to be read.", "Offline. No ads. No account."),
        StoreCard("02-tafsir", "Tafsir, a tap away.", null),
        StoreCard("03-search", "Go to any ayah directly.", null),
        StoreCard("04-saved", "Save ayah and take notes.", null),
        StoreCard("05-tajweed", "Tajweed colors with rules.", null),
        StoreCard("06-dark-reading", "The same page, gentle at night.", null),
        StoreCard("07-share-card", "Share any ayah as screenshot.", null),
        StoreCard("08-listen-offer", "Play recitation with one tap.", null),
    )

    /**
     * Where a card's parts stand.
     *
     * A card is a stack of fixed bands: the headline, the sub-line, the gold
     * rule, and the frame's slot. The bands keep their heights, so the frame
     * sits at the same place on every card of a form however a caption wraps,
     * and a one-line caption is centered in the band a two-line caption fills.
     */
    data class StoreCardLayout(
        val form: CardForm,
        val displaySize: Int,
        val displayLeading: Int,
        val displayLines: Int,
        val labelSize: Int,
        val ruleWidth: Int,
        val ruleHeight: Int,
        val frame: Rectangle,
        val pad: Int,
        val stroke: Int,
        val radius: Int,
        val textX: Int,
        val textWidth: Int,
        val displayTop: Int,
        val displayHeight: Int,
        val labelTop: Int,
        val labelHeight: Int,
        val ruleY: Int,
    )

    /**
     * The 1080 x 1920 phone card, the shape Play's phone set has. The corner
     * radius is 40 on every form because the status bar's clock starts 40 card
     * pixels from the capture's edge on the ten inch frame and 41 on the
     * phone's: a larger round clips the clock, which reads as a broken status
     * bar rather than as a device's corner (owner decision, 22.4).
     */
    val phoneLayout = StoreCardLayout(
        form = CardForm.Phone,
        displaySize = 100,
        displayLeading = 120,
        displayLines = 2,
        labelSize = 46,
        ruleWidth = 168,
        ruleHeight = 6,
        frame = Rectangle(96, 526, 888, 1298),
        pad = 12,
        stroke = 6,
        radius = 40,
        textX = 96,
        textWidth = 888,
        displayTop = 128,
        displayHeight = 240,
        labelTop = 368,
        labelHeight = 72,
        ruleY = 456,
    )

    /** The 1200 x 1920 seven inch card: the same page at 1.11 times the width. */
    val tablet7Layout = StoreCardLayout(
        form = CardForm.Tablet7,
        displaySize = 110,
        displayLeading = 132,
        displayLines = 2,
        labelSize = 50,
        ruleWidth = 184,
        ruleHeight = 6,
        frame = Rectangle(112, 552, 976, 1272),
        pad = 12,
        stroke = 6,
        radius = 40,
        textX = 112,
        textWidth = 976,
        displayTop = 128,
        displayHeight = 264,
        labelTop = 392,
        labelHeight = 74,
        ruleY = 482,
    )

    /**
     * The 2560 x 1800 ten inch card: the band above, the wide reading below.
     * The frame's slot is the capture's own 1.42:1 shape, so the whole tablet
     * page stands in it at one scale, and the outline keeps 48 pixels of air
     * under the rule and 64 at the card's foot.
     */
    val tablet10Layout = StoreCardLayout(
        form = CardForm.Tablet10,
        displaySize = 160,
        displayLeading = 190,
        displayLines = 2,
        labelSize = 64,
        ruleWidth = 200,
        ruleHeight = 6,
        frame = Rectangle(532, 660, 1496, 1052),
        pad = 16,
        stroke = 8,
        radius = 40,
        textX = 128,
        textWidth = 2304,
        displayTop = 88,
        displayHeight = 380,
        labelTop = 476,
        labelHeight = 84,
        ruleY = 582,
    )

    val layouts: List<StoreCardLayout> = listOf(phoneLayout, tablet7Layout, tablet10Layout)

    fun layout(form: CardForm): StoreCardLayout = layouts.first { it.form == form }

    /** The face a card is drawn on: the night card is the night page. */
    fun faceOf(card: StoreCard): CardFace =
        if (card.frame == "06-dark-reading") night else paper
}
