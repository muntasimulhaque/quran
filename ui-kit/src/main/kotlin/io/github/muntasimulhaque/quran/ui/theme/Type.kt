package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.content.R
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole

/**
 * The voices of the app, under one rule: a face names its own world.
 *
 * The Book's voice carries the Book's words and the Book's proper names:
 * the translation, the tafsir, a footnote, the reader's own note, the surah
 * names, and a passage's reference. The app's voice carries everything the
 * app says about itself: a sheet's or a page's name, an empty state, a
 * row's name, a value, a label, a count, a button, a status, a credit. A
 * sheet therefore opens as a surface of the app and the page inside it is
 * the Book; the two never borrow each other's words (owner decision, this
 * session).
 *
 * The Book itself is the KFGQPC Hafs face: the reading, the basmalah, the
 * word-by-word aid, and the ayah's own medallion. Arabic outside the
 * canonical text speaks Kitab, the body Naskh drawn for long-form Arabic
 * books: the tafsir's paragraphs and the quotations inside them. Amiri Quran
 * keeps the display lines, the masthead and the surah names, whose tall marks
 * need its tall line box.
 *
 * The reader's language then chooses two pairs. English reads Literata and
 * works in Inter; Bangla reads Noto Serif Bengali and works in Noto Sans
 * Bengali. A title in the Book's voice, a surah's name, a translation, and a
 * tafsir are Literata; a sheet's name, a control, a label, a count, a row's
 * name, and a footnote are Inter. The two pairs are the same two jobs in two
 * scripts, and neither face does the other's work.
 */

/**
 * One member of a variable family, with the weight it declares set on the
 * file's own `wght` axis.
 *
 * A variable file draws its default instance (400) for every member unless
 * the axis is set: the declared [FontWeight] only tells Compose which member
 * to pick, and `fontSynthesis = None` forbids the platform from inventing
 * the missing weight. A family built without this drew every Bangla title,
 * row name, and bold run at Regular (owner report, this session).
 */
@OptIn(ExperimentalTextApi::class)
private fun variableCut(
    resId: Int,
    weight: FontWeight,
    style: FontStyle = FontStyle.Normal,
): Font = Font(
    resId,
    weight,
    style,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** The plain interface voice for English: quiet, exact, never decorative. */
val Inter = FontFamily(
    variableCut(R.font.inter_variable, FontWeight.Normal),
    variableCut(R.font.inter_variable, FontWeight.Medium),
    variableCut(R.font.inter_variable, FontWeight.SemiBold),
)

/** The plain interface voice for Bangla: the same job Inter does, in Bengali script. */
val NotoSansBengali = FontFamily(
    variableCut(R.font.noto_sans_bengali, FontWeight.Normal),
    variableCut(R.font.noto_sans_bengali, FontWeight.Medium),
    variableCut(R.font.noto_sans_bengali, FontWeight.SemiBold),
)

/**
 * The reading face for English: translations, tafsir, notes, and the Book's
 * own names (a surah's name, the reading's top bar, a passage's reference).
 * The italic members come from the face's own italic variable file (the
 * roman-italic pair of the same Literata 3.103 release, same axes, same
 * metrics), so an emphasized run keeps the page voice mid sentence instead
 * of falling through to the platform default.
 *
 * The static family is the fallback a surface gets without asking for a cut;
 * every reading surface asks through [literataAt], which sets the face's
 * optical size and weight to the size being drawn. The static members still
 * carry their own `wght` axis, so a surface that uses the family directly is
 * not drawn at 400 against its declared Medium.
 */
val Literata = FontFamily(
    variableCut(R.font.literata_variable, FontWeight.Normal),
    variableCut(R.font.literata_variable, FontWeight.Medium),
    variableCut(R.font.literata_italic_variable, FontWeight.Normal, FontStyle.Italic),
    variableCut(R.font.literata_italic_variable, FontWeight.Medium, FontStyle.Italic),
)

/**
 * The Bangla reading face for translations, tafsir, notes, and the Book's
 * own names. The Medium and SemiBold members are what a bold run and a title
 * ask for; each member carries its own `wght` axis, because a variable file
 * otherwise draws every member at 400 (owner report, this session).
 */
val NotoSerifBengali = FontFamily(
    variableCut(R.font.noto_serif_bengali, FontWeight.Normal),
    variableCut(R.font.noto_serif_bengali, FontWeight.Medium),
    variableCut(R.font.noto_serif_bengali, FontWeight.SemiBold),
)

/**
 * The display voice for Arabic: the masthead and the surah names. This is
 * Amiri Quran, the release prepared for Quran pages: its line box is 2.449 of
 * the em against a body face's 1.688, which is what the Quran's marks need
 * and what [amiriDisplay] trims back to the ink.
 */
val AmiriQuran = FontFamily(Font(R.font.amiri_quran))

/**
 * The body voice for Arabic prose: the tafsir's paragraphs and the
 * quotations inside them. Kitab is Khaled Hosny's classical Naskh for
 * long-form Arabic books, built on Scheherazade and extended to the full
 * Unicode Arabic repertoire. It is the face Quran.com and Quran for Android
 * draw their Arabic tafsir in, and it carries the prose punctuation that the
 * Quran's Hafs face has no glyphs for: U+060C, U+061B, U+061F, U+066A to
 * U+066C, and U+06D4. Its line box is 1.688 of the em, so the tafsir's
 * Arabic line can be the Quran's own 2.0 without the tall marks crowding
 * (owner decision, this session: Hafs drew every prose comma as a dotted
 * circle, and Amiri's tall marks crowded the 1.9 line).
 */
val KitabBody = FontFamily(Font(R.font.kitab_regular))

/**
 * The reading voice of the Quran's own text: the KFGQPC Hafs face, the same
 * one the reading, the basmalah, and the word-by-word aid are drawn in. It is
 * a font file rather than a resource, so it is loaded from the assets; use
 * [rememberHafs] to get the family on a surface that shows an ayah.
 */
@Composable
fun rememberHafs(): FontFamily {
    val context = LocalContext.current
    return remember(context) {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }
}

/**
 * One Arabic line's style, with the three rules every Arabic run keeps.
 *
 * **No synthesized weight or slant.** Android invents a bold by smearing the
 * regular letters thicker and an italic by slanting them; on a Naskh face
 * that eats the hairlines and slants a script that has no italic. The faces
 * ship with the weights the app asks for, so nothing is ever invented.
 *
 * **A paragraph that runs right to left.** The app's interface language is
 * English, so an Arabic paragraph would otherwise be laid out as a
 * left-to-right one: invisible while a line is pure Arabic, wrong the day a
 * line carries a Latin word, a bracket, or a reference beside the verse.
 *
 * **A line box measured by the script, not centred in it.** Arabic sets its
 * extra air above the letters, where the marks are, so the alignment is
 * proportional; [trim] is the one choice a caller makes, because a verse's
 * own rhythm is measured in its line box while a prose block's air is the
 * dp around it.
 */
fun arabicStyle(
    family: FontFamily,
    sizeSp: Float,
    lineSp: Float,
    color: Color = Color.Unspecified,
    trim: LineHeightStyle.Trim = LineHeightStyle.Trim.None,
): TextStyle = TextStyle(
    fontFamily = family,
    fontSize = sizeSp.sp,
    lineHeight = lineSp.sp,
    color = color,
    fontSynthesis = FontSynthesis.None,
    textDirection = TextDirection.Rtl,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Proportional,
        trim = trim,
    ),
)

/**
 * A single line of display Arabic: the surah's name, the masthead, the name
 * beside a row's Latin one. The line box is 1.4 of the size, not the face's
 * 2.449, because the face's box is drawn for a Quran page's stacked marks and
 * would push every designed gap around a name out by most of an em. At 1.4
 * the ink still fits and the proportional alignment centres it, so the 14 dp
 * under a surah's name is 14 dp.
 */
fun amiriDisplay(sizeSp: Float, color: Color = Color.Unspecified): TextStyle =
    arabicStyle(AmiriQuran, sizeSp, sizeSp * 1.4f, color)

/**
 * The app's ladder, by the job a line does, in the two voices the app keeps.
 *
 * The reader's language chooses two pairs. The Book's voice (Literata and
 * Noto Serif Bengali) carries the Quran's meaning: the translation, the
 * tafsir, a footnote, the reader's own note, the names of the surahs, and a
 * passage's reference. The app's voice (Inter and Noto Sans Bengali)
 * carries everything the app says about itself: a sheet's or a page's
 * name, a row's name, a value, a label, a button, a count, a status, a
 * credit. A sheet opens as a surface of the app and the page inside it is
 * the Book; the two never borrow each other's words (owner decision, this
 * session).
 *
 * The sizes are the jobs, and the steps between them are visible:
 *
 * - **Page.** A sheet's, a page's, or an error's own name: 20 at SemiBold,
 *   Apple's Title 3, over the 15 interface body and the 16 reading body. It
 *   is the header of a utility sheet, not a display line and not a second
 *   article title.
 * - **Article.** A passage's title in the Book's voice: one point over the
 *   16 reading body at the default, 17, and capped at 19.5 so it never crosses
 *   the page's own 20 at the largest step. The weight and the title's
 *   tighter line carry the rest; a title is not a second ladder.
 * - **Subtitle.** An empty state's name, a credit's name, a language's
 *   name: 16 at Medium, one step over the interface body and one under the
 *   page.
 * - **Book name over a page.** A surah's name in a bar or a level header:
 *   16 at Medium in the Book's face, one point over the app's row name
 *   because it names the page. The opening's Latin name is 20, the page's
 *   own title.
 * - **Book name in a list.** A surah's name in a list of surahs: the row
 *   name's 15, carried by the Book's face rather than by a larger size.
 * - **Name.** A row's name: 15 at Medium, the interface body's size and one
 *   weight level up, so a name reads as a name without shouting.
 * - **Body.** The app's prose and a row: 15 at Regular. The Book's prose is
 *   16 at Regular in its own face, because Literata's smaller x-height
 *   needs the point to match Inter's 15 optically; the Book leads by its
 *   voice and by that point.
 * - **Field.** The search field: 16 at Regular,
 *   Material's bodyLarge. It is the one text the reader types, and the one
 *   page of prose in the chrome.
 * - **Meta.** A source line under a name, a count, a status: 13, Apple's
 *   footnote. A row's own value is the same size, tabular; the 12 value the
 *   sheet used to wear read as a footnote under the thing it described.
 * - **Label.** A group's name over a list: 12 at SemiBold with tracking, in
 *   the secondary ink. A label is smaller than the value under it on
 *   purpose: it steps back and lets the choices lead.
 * - **Button.** A word on a shape: 14 at Medium, Material's labelLarge.
 * - **Compact.** The playback pill's title and the download notice: 14, one
 *   step over meta and under the body. A compact surface is a control or a
 *   status, not a page.
 *
 * A document's own scale moves with the reader's step: the article title is
 * one point over the body at the reader's own size, capped at 19.5, and every
 * head is the body's own size at a weight, the way Apple's Headline is its
 * Body at Semibold. One title, body-size heads: a reading page, not a
 * magazine.
 */
object TypeLadder {

    /** A sheet's, a page's, or an error's own name: the interface face at SemiBold. */
    const val SHEET_SP = 20f

    /** An empty state's name, a credit's name, a language's name. */
    const val SUBTITLE_SP = 16f

    /** A Book name over a page, in a bar or a level header: a surah's own name. */
    const val BOOK_NAME_SP = 16f

    /**
     * A Book name in a list: a surah's own name in the row it is one of. It
     * is the row name's size, and the Book's face is what makes it a Book
     * name; a list of 114 needs a name, not 114 page titles (owner
     * decision, this session).
     */
    const val BOOK_ROW_SP = 15f

    /** The Latin name of the surah opening: the page's own title. */
    const val OPENING_NAME_SP = 20f

    /** The Arabic name at the surah opening: a display line, not the Quran's text. */
    const val OPENING_ARABIC_SP = 36f

    /** A surah's Arabic name in a list. */
    const val LIST_ARABIC_SP = 21f

    /** The masthead's Arabic title. */
    const val MASTHEAD_SP = 48f

    /** A row's name, in the interface face at Medium. */
    const val NAME_SP = 15f

    /** The interface's prose and a row. */
    const val BODY_SP = 15f

    /** The search field: Material's bodyLarge. */
    const val FIELD_SP = 16f

    /** A source line under a name: where it comes from, a place, a count. */
    const val META_SP = 13f

    /** A group's name over a list. */
    const val LABEL_SP = 12f

    /** A row's own value under its name: the meta size, tabular. */
    const val VALUE_SP = 13f

    /** A word on a shape. */
    const val BUTTON_SP = 14f

    /** A compact surface's body or label: the pill and the download notice. */
    const val COMPACT_SP = 14f

    /** The one sample word on the tajweed legend, at the reading's default Arabic size. */
    val TAJWEED_SAMPLE_SP: Float = TextSize.sp(TypeRole.Arabic, TextSize.DEFAULT)

    /** The theme swatch's letter. */
    const val SWATCH_SP = 18f

    /**
     * The share card's verse: the reading's own Arabic at its default step,
     * 26. The card is a specimen with the type fixed, because a picture is one
     * artifact shared with everyone, and the reader's own step does not reach
     * it; the ratio to the 16 translation, 1.625, is the reading's own at the
     * step it ships on. It stood at 24 from 13.4, when the reading stood at 24
     * too, and 13.7 raised the reading and left the card behind (owner report,
     * this version: the card and the page were two ratios). Read from
     * [TextSize] so the two cannot drift again (owner decision, this version).
     */
    val SHARE_ARABIC_SP: Float = TextSize.sp(TypeRole.Arabic, TextSize.DEFAULT)

    /** The surah's Arabic name on the share card. */
    const val SHARE_NAME_SP = 22f

    /**
     * The card's inline Arabic: the reading's one stand for Arabic beside
     * prose at the default step, 24 of the verse's 26, so the card's step
     * under its verse is the reading's and not a second ratio.
     */
    val SHARE_INLINE_SP: Float = TextSize.inlineArabic(TextSize.DEFAULT)

    /**
     * A title's line in the Latin reading face: 1.3, which is what Material's
     * 22/28 and Apple's 22/28 titles use. It is not the face's declared
     * 1.485 box: that box is the font's, and a UI title is set tighter.
     * Bengali keeps its own 1.6, carried by [ReadingVoice.titleRatio].
     */
    const val TITLE_LINE = 1.3f

    /**
     * An article's title over its own text: one point over the body it
     * titles, 17 at the default 16. The title's Semibold and its 1.3 line
     * carry the hierarchy; the cap below keeps it under the page's own 20 at
     * the largest step (owner decision, this session).
     */
    const val ARTICLE_TITLE_LEAD = 1f

    /**
     * The title's ceiling: half a point under the page's own name, so the
     * content can never outgrow the chrome at the largest text step.
     */
    const val ARTICLE_TITLE_MAX = SHEET_SP - 0.5f

    /**
     * A section's head is the body's size at SemiBold; a sub-head is the same
     * size at Medium. The hierarchy is weight, not another size, because a
     * long reading cannot carry a ladder of big heads (owner decision, this
     * session).
     */
    const val HEADING = 1f
    const val SUBHEADING = 1f

    /** A passage's title at this body size: one point over it, under the page. */
    fun articleTitleSp(bodySp: Float): Float =
        minOf(bodySp + ARTICLE_TITLE_LEAD, ARTICLE_TITLE_MAX)

    /** A section's head at this body size: the body's own size. */
    fun headingSp(bodySp: Float): Float = bodySp * HEADING

    /** A sub-head at this body size: the body's own size, at Medium. */
    fun subheadingSp(bodySp: Float): Float = bodySp * SUBHEADING
}

/**
 * The title ladder, in the book's face: the Book's names. The app's own
 * names (a sheet, a page, an empty state) are in the interface face, because
 * a surface of the app names itself in the app's voice (owner decision, this
 * session). The display and headline slots are the interface's; the Book's
 * display lines are asked for by name.
 *
 * [trackingSp] is the optical tracking: a title is set a little tighter than
 * the face's raw spacing, and the larger the cut the more it closes, because
 * a display size that keeps body tracking reads loose. It is a number per
 * style rather than one global value, because the correction follows the
 * size.
 */
@Composable
private fun readingStyle(
    weight: FontWeight,
    sizeSp: Float,
    lineSp: Float,
    trackingSp: Float = 0f,
): TextStyle =
    TextStyle(
        fontFamily = literataAt(sizeSp),
        fontWeight = weight,
        fontSize = sizeSp.sp,
        lineHeight = lineSp.sp,
        letterSpacing = trackingSp.sp,
        fontSynthesis = FontSynthesis.None,
        // A title is not a paragraph: the heading breaker keeps a short line
        // from ending in one orphaned word, and the body's high-quality
        // paragraph breaker would spend its work where a title has nothing to
        // optimize (owner decision, this session: titles keep Heading). A
        // title never hyphenates either, at any size.
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None,
    )

/**
 * The plain ladder, in the interface's face, for the words the app itself
 * says. [tabular] is on for every style that can carry a number, so a
 * reference, a count, a time, and a pace line up down a rail and never
 * jitter as the digits change; a meta line of prose is not tabular, because
 * a sentence is not a column. [trackingSp] is the optical tracking: small
 * type opens a little, so a 12 sp label does not read as a body size shrunk.
 */
private fun interStyle(
    weight: FontWeight,
    sizeSp: Float,
    lineSp: Float,
    scale: Float,
    tabular: Boolean,
    trackingSp: Float = 0f,
): TextStyle = TextStyle(
    fontFamily = interAt(weight, sizeSp * scale),
    fontWeight = weight,
    fontSize = sizeSp.sp,
    lineHeight = lineSp.sp,
    letterSpacing = trackingSp.sp,
    fontSynthesis = FontSynthesis.None,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

/**
 * The interface in English, by job, from [TypeLadder].
 *
 * The page is the app's own name: 20 at SemiBold (titleLarge), Apple's
 * Title 3, over the 15 interface body and the 16 reading body. The Book's
 * name over a page is 16 at Medium in the Book's face (titleMedium); a
 * list's Book name is 15 in the same face ([bookRowName]); a row's name is
 * 15 at Medium (titleSmall); the app's prose and a row are 15 (bodyMedium);
 * a field is 16 (bodyLarge); a source, a value, a
 * count and a status are 13; a group's label is 12 at SemiBold with
 * tracking; a button's word is 14; a compact surface is 14 through
 * [compactBody] or [compactLabel].
 *
 * The meta and the value are one size now. The 13 meta line and the 12
 * caption were two names for the same quiet fact, and the 12 value read as a
 * footnote under the thing it described. A value is a caption no more: it
 * is the meta line that belongs to its row, tabular (owner decision, this
 * session).
 *
 * The display and headline slots are the interface's, because no surface
 * asks a theme slot for a Book display line: the Book's display lines are
 * amiriDisplay and [ReadingVoice.title], and a stock Material surface
 * should never fall into Literata.
 *
 * The drawn size scales with the system font scale for the optical axis, the
 * way [literataAt] does, so a reader at 2.0 scale reads the cut for double
 * size.
 */
@Composable
internal fun quranTypography(): Typography {
    val scale = LocalDensity.current.fontScale
    return Typography(
        displayLarge = interStyle(
            FontWeight.SemiBold,
            56f,
            64f,
            scale,
            tabular = false,
            trackingSp = -0.5f,
        ),
        displayMedium = interStyle(
            FontWeight.SemiBold,
            48f,
            56f,
            scale,
            tabular = false,
            trackingSp = -0.4f,
        ),
        displaySmall = interStyle(
            FontWeight.SemiBold,
            40f,
            48f,
            scale,
            tabular = false,
            trackingSp = -0.3f,
        ),
        headlineLarge = interStyle(
            FontWeight.SemiBold,
            34f,
            42f,
            scale,
            tabular = false,
            trackingSp = -0.3f,
        ),
        headlineMedium = interStyle(
            FontWeight.SemiBold,
            28f,
            36f,
            scale,
            tabular = false,
            trackingSp = -0.2f,
        ),
        headlineSmall = interStyle(
            FontWeight.SemiBold,
            24f,
            36f,
            scale,
            tabular = false,
            trackingSp = -0.2f,
        ),
        titleLarge = interStyle(
            FontWeight.SemiBold,
            TypeLadder.SHEET_SP,
            TypeLadder.SHEET_SP * TypeLadder.TITLE_LINE,
            scale,
            tabular = false,
            trackingSp = -0.2f,
        ),
        titleMedium = readingStyle(
            FontWeight.Medium,
            TypeLadder.BOOK_NAME_SP,
            TypeLadder.BOOK_NAME_SP * TypeLadder.TITLE_LINE,
        ),
        titleSmall = interStyle(
            FontWeight.Medium,
            TypeLadder.NAME_SP,
            20f,
            scale,
            tabular = false,
        ),
        bodyLarge = interStyle(
            FontWeight.Normal,
            TypeLadder.FIELD_SP,
            24f,
            scale,
            tabular = false,
        ),
        bodyMedium = interStyle(FontWeight.Normal, TypeLadder.BODY_SP, 22f, scale, tabular = false),
        bodySmall = interStyle(FontWeight.Normal, TypeLadder.META_SP, 19f, scale, tabular = false),
        labelLarge = interStyle(
            FontWeight.Medium,
            TypeLadder.BUTTON_SP,
            20f,
            scale,
            tabular = true,
            trackingSp = 0.1f,
        ),
        labelMedium = interStyle(
            FontWeight.SemiBold,
            TypeLadder.LABEL_SP,
            16f,
            scale,
            tabular = true,
            trackingSp = 0.4f,
        ),
        // The value: a row's own line under its name, 13 at Regular, the
        // same size as a source line. A name and its value are told apart by
        // weight and tone, not by a footnote's size (owner decision, this
        // session).
        //
        // The M3 slot names are read by job here, not by M3's baseline sizes:
        // this app's value is 13 and its group label is 12, so a stock
        // Material surface that asks for labelSmall gets the value size.
        // Scope one as the time picker is scoped rather than assuming the
        // baseline scale (owner decision, this session).
        labelSmall = interStyle(
            FontWeight.Normal,
            TypeLadder.VALUE_SP,
            18f,
            scale,
            tabular = true,
        ),
    )
}

/**
 * A Book name in a list: the row name's 15, in the Book's own face and at
 * the line its script needs.
 *
 * The size comes from `titleMedium`, the Book's named style, so the family
 * and the script's line ratio follow the reader's language: Literata on a
 * 1.3 line, Noto Serif Bengali on its 1.6. The one point off the page's
 * Book name is the difference between a row and a page, and the face is
 * what still says the name belongs to the Book (owner decision, this
 * session).
 */
@Composable
fun bookRowName(): TextStyle {
    val base = MaterialTheme.typography.titleMedium
    val lineRatio = base.lineHeight.value / base.fontSize.value
    return base.copy(
        fontSize = TypeLadder.BOOK_ROW_SP.sp,
        lineHeight = (TypeLadder.BOOK_ROW_SP * lineRatio).sp,
    )
}

/**
 * A small page's own name: an empty state, a credit, a language's name.
 * 16 at Medium in both scripts, one step over the interface body and under
 * the page title. It is the app's voice, so it never borrows the Book's
 * face, and it takes the theme's own interface family, so a Bangla empty
 * state is Noto Sans Bengali and an English one is Inter (owner decision,
 * this session).
 *
 * The line is the base style's own, not a second number: the English title
 * style's 1.333 and the Bangla 1.45 are the scripts' boxes, and a hard-coded
 * 1.375 set the Bangla name at 22 where its face asked for 23.2.
 */
@Composable
fun screenSubtitle(): TextStyle {
    val base = MaterialTheme.typography.titleSmall
    val size = TypeLadder.SUBTITLE_SP
    val baseSize = base.fontSize.value
    val ratio = if (baseSize > 0f && base.lineHeight.value > 0f) {
        base.lineHeight.value / baseSize
    } else {
        1.375f
    }
    return base.copy(
        fontSize = size.sp,
        lineHeight = (size * ratio).sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    )
}

/**
 * A compact surface's body line: the download notice, and the pill's status
 * if a caller wants it at 14. 14 at the base body style's own line, so the
 * notice is one step over meta and one under the body, and the Bangla line
 * stays the script's 1.45 (owner decision, this session).
 */
@Composable
fun compactBody(): TextStyle {
    val base = MaterialTheme.typography.bodySmall
    val size = TypeLadder.COMPACT_SP
    val baseSize = base.fontSize.value
    val ratio = if (baseSize > 0f && base.lineHeight.value > 0f) {
        base.lineHeight.value / baseSize
    } else {
        1.46f
    }
    return base.copy(
        fontSize = size.sp,
        lineHeight = (size * ratio).sp,
    )
}

/**
 * A compact surface's label: the playback pill's title at 14 Medium. It is
 * separate from [compactBody] because the pill's title is a name at Medium,
 * not a sentence at Regular (owner decision, this session).
 */
@Composable
fun compactLabel(): TextStyle {
    val base = MaterialTheme.typography.titleSmall
    val size = TypeLadder.COMPACT_SP
    val baseSize = base.fontSize.value
    val ratio = if (baseSize > 0f && base.lineHeight.value > 0f) {
        base.lineHeight.value / baseSize
    } else {
        1.333f
    }
    return base.copy(
        fontSize = size.sp,
        lineHeight = (size * ratio).sp,
    )
}

/**
 * Inter cut at the drawn size and weight: the file's optical axis set to the
 * size it is drawn at, clamped to the axis's own 14 to 32 range, beside the
 * weight the style asks for. Set once per cut and cached, the way the
 * Literata cuts are; a family built without the settings would draw every
 * size in the file's default 14 cut.
 *
 * The cache key is the pair itself, as a string: it used to be a hash of the
 * two numbers, which can collide, and a collision would hand one size the
 * cut another size had already cached (owner report, this session).
 */
@OptIn(ExperimentalTextApi::class)
private fun interAt(weight: FontWeight, drawnSp: Float): FontFamily =
    interCuts.getOrPut("$drawnSp/${weight.weight}") {
        FontFamily(
            Font(
                R.font.inter_variable,
                weight,
                variationSettings = FontVariation.Settings(
                    FontVariation.Setting("opsz", drawnSp.coerceIn(14f, 32f)),
                    FontVariation.weight(weight.weight),
                ),
            ),
        )
    }

private val interCuts = java.util.concurrent.ConcurrentHashMap<String, FontFamily>()

/**
 * The interface in Bangla: the same jobs and the same sizes as the English
 * ladder, read from [TypeLadder] so the two can never drift, in the script's
 * own two voices. The app's voice is Noto Sans Bengali; the Book's voice is
 * Noto Serif Bengali. The sizes are the same in both scripts; only the line
 * is the script's own (owner decision, this session).
 *
 * The display and headline slots are the interface's, because no surface
 * asks a theme slot for a Book display line. The Book's display lines are
 * `amiriDisplay` and [ReadingVoice.title].
 *
 * A title and a reading name carry 1.6 of their size in line, because the
 * serif face's own line box is 1.594 of the em and a Bengali title's stacked
 * marks need it; a label, a meta line, a row's name, and a control's word
 * are Noto Sans Bengali at 1.45, which clears that face's 1.325 ink. The two
 * ratios are the script's, not a borrowed compromise, so the Bangla ladder
 * keeps its own air at every step.
 *
 * Each member carries its own `wght` axis: the family used to declare Medium
 * and SemiBold on one variable file without setting the axis, so every
 * Bangla weight drew at 400 and the titles and heads had no weight hierarchy
 * at all (owner report, this session).
 *
 * Bengali is not letterspaced: the script's conjuncts and matras are drawn as
 * clusters, and the optical tracking the Latin ladder carries would pull them
 * apart.
 */
val BanglaTypography = Typography(
    displayLarge = banglaStyle(FontWeight.SemiBold, 56f),
    displayMedium = banglaStyle(FontWeight.SemiBold, 48f),
    displaySmall = banglaStyle(FontWeight.SemiBold, 40f),
    headlineLarge = banglaStyle(FontWeight.SemiBold, 34f),
    headlineMedium = banglaStyle(FontWeight.SemiBold, 28f),
    headlineSmall = banglaStyle(FontWeight.SemiBold, 24f),
    titleLarge = banglaStyle(FontWeight.SemiBold, TypeLadder.SHEET_SP),
    titleMedium = banglaStyle(FontWeight.Medium, TypeLadder.BOOK_NAME_SP, serif = true),
    titleSmall = banglaStyle(FontWeight.Medium, TypeLadder.NAME_SP),
    bodyLarge = banglaStyle(FontWeight.Normal, TypeLadder.FIELD_SP),
    bodyMedium = banglaStyle(FontWeight.Normal, TypeLadder.BODY_SP),
    bodySmall = banglaStyle(FontWeight.Normal, TypeLadder.META_SP),
    labelLarge = banglaStyle(FontWeight.Medium, TypeLadder.BUTTON_SP, tabular = true),
    labelMedium = banglaStyle(FontWeight.SemiBold, TypeLadder.LABEL_SP, tabular = true),
    labelSmall = banglaStyle(FontWeight.Normal, TypeLadder.VALUE_SP, tabular = true),
)

/** One style of the Bangla ladder, at the line its face needs. */
private fun banglaStyle(
    weight: FontWeight,
    sizeSp: Float,
    serif: Boolean = false,
    tabular: Boolean = false,
): TextStyle = TextStyle(
    fontFamily = if (serif) NotoSerifBengali else NotoSansBengali,
    fontWeight = weight,
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp * if (serif) 1.6f else 1.45f).sp,
    fontSynthesis = FontSynthesis.None,
    fontFeatureSettings = if (tabular) "tnum" else null,
    lineBreak = if (serif) LineBreak.Heading else LineBreak.Simple,
    hyphens = Hyphens.None,
)

/**
 * Literata at the size it is about to be drawn, in the three weights a page
 * can ask for, upright and italic. The face's optical size axis runs 7 to 72
 * with the default at 12, so a 16 sp body would otherwise be set in the cut
 * drawn for footnotes. Faces without the axis (the Bengali voices) have one
 * cut and never come through here, and below API 26 the platform ignores the
 * setting.
 *
 * The six members are the whole of what the reading requests: the regular
 * text, the Medium a footnote marker and a row's name wear, and the SemiBold
 * a bold run of the tafsir and a title wear, each upright and italic. A family
 * is matched by the weight and style each member declares, so a member must
 * exist for every weight a span can request, and each one carries its own
 * wght axis beside the opsz: Compose applies only the variation settings a
 * Font declares, and a family built with the opsz alone loads every member at
 * the file's default 400, so a bold run would be drawn exactly like the
 * regular text around it (the same silence a static face has, made worse by
 * the family looking correct).
 *
 * The opsz is the size the text is actually drawn at, the reader's own scale
 * included: a reader whose scale is doubled reads the cut Literata drew for
 * double size, not the small-text cut stretched over it.
 */
@Composable
fun literataAt(sizeSp: Float): FontFamily {
    val drawn = sizeSp * LocalDensity.current.fontScale
    return literataCuts.getOrPut(drawn.toBits()) {
        FontFamily(
            literataCut(drawn, FontWeight.Normal, FontStyle.Normal),
            literataCut(drawn, FontWeight.Medium, FontStyle.Normal),
            literataCut(drawn, FontWeight.SemiBold, FontStyle.Normal),
            literataCut(drawn, FontWeight.Normal, FontStyle.Italic),
            literataCut(drawn, FontWeight.Medium, FontStyle.Italic),
            literataCut(drawn, FontWeight.SemiBold, FontStyle.Italic),
        )
    }
}

@OptIn(ExperimentalTextApi::class)
private fun literataCut(sizeSp: Float, weight: FontWeight, style: FontStyle): Font = Font(
    // The roman and italic cuts are the face's two variable files, a matched
    // pair of the same release; the italic members load the italic file, so
    // an italic run draws the true italic shapes rather than the platform's
    // skew of the roman.
    if (style == FontStyle.Italic) R.font.literata_italic_variable else R.font.literata_variable,
    weight,
    style,
    variationSettings = FontVariation.Settings(
        FontVariation.Setting("opsz", sizeSp),
        FontVariation.weight(weight.weight),
    ),
)

private val literataCuts = java.util.concurrent.ConcurrentHashMap<Int, FontFamily>()

/** The Latin reading voice at the middle step, for a surface with no size of its own. */
val LatinReading = TextStyle(
    fontFamily = Literata,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
)

/**
 * The Bangla reading voice at the middle step. The body is 16 sp, the same
 * size as the Latin reading, because the two scripts share one ladder. The
 * line is the face's own natural 1.722 (Noto Serif Bengali's natural box is
 * 1.594 of the em, and the script stacks vowel marks above and below the
 * line).
 */
val BengaliReading = TextStyle(
    fontFamily = NotoSerifBengali,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = (16f * 31f / 18f).sp,
)

/**
 * The reading voice of the language being read: the style, the line a
 * paragraph of it gets per em of text, the face cut for an exact drawn size,
 * and the language's own locale, which decides the hyphenation dictionary and
 * the local forms of the script. The theme sets it once from the reader's
 * language; every reading surface takes it from here.
 *
 * It carried the face's measured average advance for as long as the app
 * capped a column at 65 characters. The column is the page now, and the page
 * is a width in dp rather than a count of characters, so nothing here is
 * asked how wide a character is (owner decision: the character measure stays
 * gone, and the page is `Space.Measure`).
 */
class ReadingVoice(
    val style: TextStyle,
    val lineRatio: Float,
    /**
     * The face's own line box, as a ratio of its size, is the floor for a
     * *paragraph* of this face. Below it one line's marks sit in the next
     * one's air. Literata's box is 1.485 of the em and Noto Serif Bengali's
     * 1.594.
     */
    val minimumLineRatio: Float,
    /**
     * A title's line, which is the platform's and not the face's prose box:
     * 1.3 in Literata (Material's 22/28 and Apple's 22/28 are 1.27) and 1.6
     * in Noto Serif Bengali, whose matras need the script's own box. A title
     * is not a paragraph, so the two leading decisions are two numbers.
     */
    val titleRatio: Float,
    val atSize: @Composable (Float) -> FontFamily,
    /** The content's own language, for hyphenation and the script's local forms. */
    val locale: LocaleList,
)

/**
 * One paragraph of reading, with the app's one policy for prose.
 *
 * The high-quality paragraph breaker instead of the default simple one: a
 * page of two or three lines is where a greedy breaker leaves a short last
 * line and a loose one above it, and the reader feels it as an uneven page.
 * The paragraph resolves its own direction from its first strong character,
 * so an Arabic paragraph in an English tafsir lays its wrapped lines from the
 * right and a Latin one from the left. The line's extra air is trimmed from
 * the first and last line, so the dp a surface puts above and below a
 * paragraph is the air the eye actually sees.
 *
 * Nothing here hyphenates. The app sets ragged right in both languages, and
 * hyphenation is a tool for setting a justified column: on a ragged line at
 * the page's 45 to 70 characters it splits words the eye did not need split
 * (the owner's own capture carried "what-ever" and "Here-after"). The
 * content's locale stays, because it shapes the script's local forms; it no
 * longer decides a break (owner decision, this session).
 *
 * The Arabic verse keeps its own line box and does not come through here: its
 * 2.0 leading is the Quran Foundation's own for the QPC Hafs face, and
 * trimming it would tighten the verses toward each other (see [arabicStyle]).
 */
@Composable
fun ReadingVoice.prose(
    sizeSp: Float,
    lineSp: Float,
    color: Color = Color.Unspecified,
    weight: FontWeight = FontWeight.Normal,
): TextStyle = style.copy(
    fontFamily = atSize(sizeSp),
    fontWeight = weight,
    fontSize = sizeSp.sp,
    // A paragraph is never tighter than the face's own box: the caller's
    // line is the script's ratio and already clears it, but a future surface
    // that asks for less cannot put one line's marks in the next one's air.
    lineHeight = maxOf(lineSp, sizeSp * minimumLineRatio).sp,
    color = color,
    textDirection = TextDirection.Content,
    localeList = locale,
    lineBreak = LineBreak.Paragraph,
    hyphens = Hyphens.None,
    fontSynthesis = FontSynthesis.None,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Proportional,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/**
 * One title in the reading's own face: a passage's reference at the head of a
 * tafsir, or a section's head inside one.
 *
 * It is [prose]'s sibling and its opposite in three decisions. A title is
 * broken by the heading breaker, not the paragraph one, because a title has
 * nothing to justify and one orphaned word in it is the whole line. It never
 * hyphenates: a hyphen in a title is a word split where the eye is reading
 * for a name. And its line is [titleRatio], the platform's title leading
 * (1.3 Latin, 1.6 Bengali), not the face's 1.485 prose box: a 22/28 title is
 * Material's and Apple's own, and the box that keeps a paragraph of marks
 * apart would add about 6 dp of air above and below every one-line title.
 *
 * Its tracking closes by [TitleTracking] of its size: a 24 sp title with body
 * tracking reads loose, the same optical correction the theme's fixed titles
 * carry. The trim is none, so the air around a title belongs to the surface
 * that places it and not to the line box.
 */
@Composable
fun ReadingVoice.title(
    sizeSp: Float,
    lineSp: Float = sizeSp * titleRatio,
    weight: FontWeight = FontWeight.SemiBold,
    color: Color = Color.Unspecified,
): TextStyle = style.copy(
    fontFamily = atSize(sizeSp),
    fontWeight = weight,
    fontSize = sizeSp.sp,
    lineHeight = lineSp.sp,
    color = color,
    letterSpacing = (sizeSp * TitleTracking).sp,
    fontSynthesis = FontSynthesis.None,
    textDirection = TextDirection.Content,
    localeList = locale,
    lineBreak = LineBreak.Heading,
    hyphens = Hyphens.None,
)

/** The share of its size a title closes by: an eighth of a percent a point. */
private const val TitleTracking = -0.008f

/** The reading's own locale, built once per language. */
internal val EnglishReadingLocale = LocaleList("en")
internal val BanglaReadingLocale = LocaleList("bn")

val LocalReadingVoice = staticCompositionLocalOf {
    ReadingVoice(
        LatinReading,
        TextSize.LATIN_PROSE_RATIO,
        1.485f,
        TypeLadder.TITLE_LINE,
        ::literataAt,
        EnglishReadingLocale,
    )
}
