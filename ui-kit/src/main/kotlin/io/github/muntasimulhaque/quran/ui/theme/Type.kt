package io.github.muntasimulhaque.quran.ui.theme

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

/**
 * The voices of the app, under one rule: everything the reader reads is set
 * in the book's face, and everything the reader works is set in the plain one.
 *
 * The Book itself is the KFGQPC Hafs face: the reading, the basmalah, the
 * word-by-word aid, and the ayah's own medallion. Arabic outside the
 * canonical text speaks Amiri, the family's body face, which is drawn for
 * running prose: the tafsir's paragraphs and the quotations inside them.
 * Amiri Quran, the same design prepared for Quran pages, keeps the display
 * lines, the masthead and the surah names, whose tall marks need its tall
 * line box.
 *
 * The reader's language then chooses two pairs. English reads Literata and
 * works in Inter; Bangla reads Noto Serif Bengali and works in Noto Sans
 * Bengali. The pair is a pair: a title, a surah's name, a translation, and a
 * tafsir are all the reading face; a control, a label, a count, and a
 * footnote are all the plain one. Nothing the reader reads is set in the
 * control face, so a sheet opens inside the book instead of dropping out of
 * it (owner decision, this session: the titles and the surah names were Inter,
 * and the tafsir's Arabic and its quotations were Amiri Quran).
 */

/** The plain interface voice for English: quiet, exact, never decorative. */
val Inter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Light),
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
)

/** The plain interface voice for Bangla: the same job Inter does, in Bengali script. */
val NotoSansBengali = FontFamily(
    Font(R.font.noto_sans_bengali, FontWeight.Light),
    Font(R.font.noto_sans_bengali, FontWeight.Normal),
    Font(R.font.noto_sans_bengali, FontWeight.Medium),
    Font(R.font.noto_sans_bengali, FontWeight.SemiBold),
)

/**
 * The reading face for English: translations, tafsir, notes, and now the
 * titles and the surah names, which used to be Inter. The italic members come
 * from the face's own italic variable file (the roman-italic pair of the
 * same Literata 3.103 release, same axes, same metrics), so an emphasized
 * run keeps the page voice mid sentence instead of falling through to the
 * platform default.
 *
 * The static family is the fallback a surface gets without asking for a cut;
 * every reading surface asks through [literataAt], which sets the face's
 * optical size to the size being drawn.
 */
val Literata = FontFamily(
    Font(R.font.literata_variable, FontWeight.Normal),
    Font(R.font.literata_variable, FontWeight.Medium),
    Font(R.font.literata_italic_variable, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.literata_italic_variable, FontWeight.Medium, FontStyle.Italic),
)

/**
 * The Bangla reading face for translations, tafsir, notes, titles, and the
 * surah names. The SemiBold member is what a bold run of the tafsir asks for;
 * without it a family is matched to the nearest declared weight and the
 * emphasis would render at Medium.
 */
val NotoSerifBengali = FontFamily(
    Font(R.font.noto_serif_bengali, FontWeight.Normal),
    Font(R.font.noto_serif_bengali, FontWeight.Medium),
    Font(R.font.noto_serif_bengali, FontWeight.SemiBold),
)

/**
 * The display voice for Arabic: the masthead and the surah names. This is
 * Amiri Quran, the release prepared for Quran pages: its line box is 2.449 of
 * the em against the body face's 1.758, which is what the Quran's marks need
 * and what [amiriDisplay] trims back to the ink.
 */
val Amiri = FontFamily(Font(R.font.amiri_quran))

/**
 * The body voice for Arabic prose: the tafsir's paragraphs and the
 * quotations inside them. This is Amiri's running-text face, the same design
 * as [Amiri] with a normal 1.758 line box and 6,710 characters instead of
 * 1,446, so it draws every letter the content carries, including the Urdu heh
 * and the pre-shaped phrase the tafsir source keeps, without borrowing the
 * Hafs face.
 */
val AmiriText = FontFamily(Font(R.font.amiri_regular))

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
    arabicStyle(Amiri, sizeSp, sizeSp * 1.4f, color)

/**
 * The app's ladder, by the job a line does, and the ratios a document's own
 * scale is built from.
 *
 * The Material slots held four real sizes over fifteen names: 20 for every
 * title, 16 for a name and a body, 12 for six different labels, and 14 for a
 * title nothing drew. A card could therefore set its own name at 12 and a
 * heading inside its text above its title, which is the report this ladder
 * answers (owner decision, this session). The sizes below are the jobs:
 *
 * - **Sheet.** A sheet's or a page's own name, 18 in the book's face. It is
 *   the largest fixed line in a sheet, under Apple's Title 3 (20), where a
 *   quiet utility header belongs.
 * - **Name.** A row's name, 15 at Medium: the same size as the body and a
 *   step above it in weight, so a name reads as a name without shouting.
 * - **Body.** The interface's prose and a field, 15 at Regular.
 * - **Meta.** The line under a name that says where it comes from: a source,
 *   a place, a count: 13. Apple's footnote, and the one size this compact
 *   ladder leaves where it was.
 * - **Caption.** A row's own value under its name, 12 at Regular. It is the
 *   line the settings list is measured by, so it sits at Material's body
 *   small and Apple's Caption 1, never at a sentence's size.
 * - **Label.** A group's name over a list, 12 at Medium with tracking, in
 *   the secondary ink. Material's own label, one weight down from a title.
 * - **Button.** A word on a shape, 13 at Medium.
 *
 * A document's own scale moves with the reader's step: the article title is
 * 18 at the default body, floored at 18 and capped at 22, and every head is
 * the body's own size, a weight level the way Apple's Headline is 17 semibold
 * against a 17 Body. At the default body the article title is 18, the section
 * head is 15 semibold, the sub-head 15 medium, and the body 15. One title,
 * body-size heads: a reading page, not a magazine.
 */
object TypeLadder {

    /** A sheet's or a page's own name. */
    const val SHEET_SP = 18f

    /** A row's name, in the interface face at Medium. */
    const val NAME_SP = 15f

    /** The interface's prose and a field. */
    const val BODY_SP = 15f

    /** A source line under a name: where it comes from, a place, a count. */
    const val META_SP = 13f

    /** A group's name over a list. */
    const val LABEL_SP = 12f

    /** A row's own value under its name. */
    const val CAPTION_SP = 12f

    /** A word on a shape. */
    const val BUTTON_SP = 13f

    /**
     * A title's line in the Latin reading face: 1.3, which is what Material's
     * 22/28 and Apple's 22/28 titles use. It is not the face's declared
     * 1.485 box: that box is the font's, and a UI title is set tighter.
     * Bengali keeps its own 1.6, carried by [ReadingVoice.titleRatio].
     */
    const val TITLE_LINE = 1.3f

    /**
     * An article's title over its own text: 18 at the default body, floored at
     * 18 and capped at 22 at the reader's steps. The card is a chapter, not a
     * page, and the owner's eye puts a card's title at one step over its
     * prose; a floor keeps the title the largest line when the prose is set to
     * the compact step.
     */
    const val ARTICLE_TITLE = 18f / 15f
    const val ARTICLE_TITLE_MIN = 18f
    const val ARTICLE_TITLE_MAX = 22f

    /**
     * A section's head is the body's size at SemiBold; a sub-head is the same
     * size at Medium. The hierarchy is weight, not another size, because a
     * long reading cannot carry a ladder of big heads (owner decision, this
     * session).
     */
    const val HEADING = 1f
    const val SUBHEADING = 1f

    /** A passage's title at this body size, between its floor and cap. */
    fun articleTitleSp(bodySp: Float): Float =
        (bodySp * ARTICLE_TITLE).coerceIn(ARTICLE_TITLE_MIN, ARTICLE_TITLE_MAX)

    /** A section's head at this body size: the body's own size. */
    fun headingSp(bodySp: Float): Float = bodySp * HEADING

    /** A sub-head at this body size: the body's own size, at Medium. */
    fun subheadingSp(bodySp: Float): Float = bodySp * SUBHEADING
}

/**
 * The display and title ladder, in the book's face. Nothing here is set in
 * the interface face, because a screen's own name is read, not operated
 * (owner decision, this session).
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
 * The plain ladder, in the interface's face, for words the reader works with.
 * [tabular] is on for every style that can carry a number, so a reference, a
 * count, a time, and a pace line up down a rail and never jitter as the digits
 * change; a meta line of prose is not tabular, because a sentence is not a
 * column. [trackingSp] is the optical tracking: small type opens a little, so
 * a 12 sp label does not read as a body size shrunk.
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
 * The three "title" slots carry three different jobs rather than three
 * heights: [TypeLadder.SHEET_SP] names a sheet or a page, [TypeLadder.NAME_SP]
 * names a row or a text in the book's face, and the same size in the plain
 * face names a control. A title is never smaller than the body it titles; the
 * slot Material calls `titleSmall` is folded onto the name for that reason,
 * because the app has no title under its body.
 *
 * The meta line is 13, not 12: a source line, a place, and a count at 12
 * read as footnotes under the thing they describe. A row's own value is the
 * caption, 12 at Regular, and the group's name is the label, 12 at Medium
 * with a touch of tracking, because Inter at 12 in the raw spacing reads as
 * a shrunken body.
 *
 * The drawn size scales with the system font scale for the optical axis, the
 * way [literataAt] does, so a reader at 2.0 scale reads the cut for double
 * size.
 */
@Composable
internal fun quranTypography(): Typography {
    val scale = LocalDensity.current.fontScale
    return Typography(
        displayLarge = readingStyle(FontWeight.SemiBold, 56f, 64f, -0.5f),
        displayMedium = readingStyle(FontWeight.SemiBold, 48f, 56f, -0.4f),
        displaySmall = readingStyle(FontWeight.SemiBold, 40f, 48f, -0.3f),
        headlineLarge = readingStyle(FontWeight.SemiBold, 34f, 42f, -0.3f),
        headlineMedium = readingStyle(FontWeight.SemiBold, 28f, 36f, -0.2f),
        headlineSmall = readingStyle(FontWeight.SemiBold, 24f, 36f, -0.2f),
        titleLarge = readingStyle(
            FontWeight.SemiBold,
            TypeLadder.SHEET_SP,
            TypeLadder.SHEET_SP * TypeLadder.TITLE_LINE,
            -0.2f,
        ),
        titleMedium = readingStyle(
            FontWeight.Medium,
            TypeLadder.NAME_SP,
            TypeLadder.NAME_SP * TypeLadder.TITLE_LINE,
        ),
        titleSmall = interStyle(
            FontWeight.Medium,
            TypeLadder.NAME_SP,
            20f,
            scale,
            tabular = false,
        ),
        bodyLarge = interStyle(FontWeight.Normal, TypeLadder.BODY_SP, 22f, scale, tabular = false),
        bodyMedium = interStyle(FontWeight.Normal, 14f, 21f, scale, tabular = false),
        bodySmall = interStyle(FontWeight.Normal, TypeLadder.META_SP, 19f, scale, tabular = false),
        labelLarge = interStyle(
            FontWeight.Medium,
            TypeLadder.BUTTON_SP,
            18f,
            scale,
            tabular = true,
            trackingSp = 0.1f,
        ),
        labelMedium = interStyle(
            FontWeight.Medium,
            TypeLadder.LABEL_SP,
            16f,
            scale,
            tabular = true,
            trackingSp = 0.4f,
        ),
        // The caption: a row's own value, 12 at Regular, so a name and its
        // value are told apart by weight and tone and not by a sentence's
        // size. It is the line the settings hub fits by (owner decision, this
        // session).
        labelSmall = interStyle(
            FontWeight.Normal,
            TypeLadder.CAPTION_SP,
            16f,
            scale,
            tabular = true,
        ),
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
 * The interface in Bangla: the same jobs as the English ladder, in the
 * script's own two faces. A title and a reading name are Noto Serif Bengali,
 * and carry 1.6 of their size in line, because that face's own line box is
 * 1.594 of the em and a Bengali title's stacked marks need it; a label, a
 * meta line, a row's name, and a control's word are Noto Sans Bengali at
 * 1.45, which clears that face's 1.403 ink. The two ratios are the script's,
 * not a borrowed compromise, so the Bangla ladder keeps its own air at every
 * step (owner decision, the audio and type audit session; the 18 sp sheet
 * title and the folded name this session).
 *
 * Bengali is not letterspaced: the script's conjuncts and matras are drawn as
 * clusters, and the optical tracking the Latin ladder carries would pull them
 * apart.
 */
val BanglaTypography = Typography(
    displayLarge = banglaStyle(FontWeight.SemiBold, 56f, serif = true),
    displayMedium = banglaStyle(FontWeight.SemiBold, 48f, serif = true),
    displaySmall = banglaStyle(FontWeight.SemiBold, 40f, serif = true),
    headlineLarge = banglaStyle(FontWeight.SemiBold, 34f, serif = true),
    headlineMedium = banglaStyle(FontWeight.SemiBold, 28f, serif = true),
    headlineSmall = banglaStyle(FontWeight.SemiBold, 24f, serif = true),
    titleLarge = banglaStyle(FontWeight.SemiBold, TypeLadder.SHEET_SP, serif = true),
    titleMedium = banglaStyle(FontWeight.Medium, TypeLadder.NAME_SP, serif = true),
    titleSmall = banglaStyle(FontWeight.Medium, TypeLadder.NAME_SP),
    bodyLarge = banglaStyle(FontWeight.Normal, TypeLadder.BODY_SP),
    bodyMedium = banglaStyle(FontWeight.Normal, 14f),
    bodySmall = banglaStyle(FontWeight.Normal, TypeLadder.META_SP),
    labelLarge = banglaStyle(FontWeight.Medium, TypeLadder.BUTTON_SP, tabular = true),
    labelMedium = banglaStyle(FontWeight.Medium, TypeLadder.LABEL_SP, tabular = true),
    labelSmall = banglaStyle(FontWeight.Normal, TypeLadder.CAPTION_SP, tabular = true),
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
 * with the default at 12, so a 15 sp body would otherwise be set in the cut
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
    fontSize = 15.sp,
    lineHeight = 22.5.sp,
)

/**
 * The Bangla reading voice at the middle step. The body is 18 sp against the
 * Latin 16: Bengali glyphs read smaller than Latin at the same nominal size,
 * so parity needs the extra point. The line is the face's own natural 1.722
 * (Noto Serif Bengali's natural box is 1.594 of the em, and the script stacks
 * vowel marks above and below the line).
 */
val BengaliReading = TextStyle(
    fontFamily = NotoSerifBengali,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 31.sp,
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
