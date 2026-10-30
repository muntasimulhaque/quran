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
 * The display and title ladder, in the book's face: 20, 24, 28, 34, 40, 56.
 * Nothing here is set in the interface face, because a screen's own name is
 * read, not operated (owner decision, this session).
 */
@Composable
private fun readingStyle(weight: FontWeight, sizeSp: Float, lineSp: Float): TextStyle =
    TextStyle(
        fontFamily = literataAt(sizeSp),
        fontWeight = weight,
        fontSize = sizeSp.sp,
        lineHeight = lineSp.sp,
        fontSynthesis = FontSynthesis.None,
        // A title is not a paragraph: the heading breaker keeps a short line
        // from ending in one orphaned word, and the body's high-quality
        // paragraph breaker would spend its work where a title has nothing to
        // optimize (owner decision, this session: titles keep Heading).
        lineBreak = LineBreak.Heading,
    )

/**
 * The plain ladder, in the interface's face: 12, 14, and 16 for words the
 * reader works with. [tabular] is on for every style that can carry a
 * number, so a reference, a count, a time, and a pace line up down a rail and
 * never jitter as the digits change.
 */
private fun interStyle(
    weight: FontWeight,
    sizeSp: Float,
    lineSp: Float,
    scale: Float,
    tabular: Boolean,
): TextStyle = TextStyle(
    fontFamily = interAt(weight, sizeSp * scale),
    fontWeight = weight,
    fontSize = sizeSp.sp,
    lineHeight = lineSp.sp,
    fontSynthesis = FontSynthesis.None,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

/**
 * The interface in English. The ladder is five sizes under the display
 * steps, and each is used for what it is: 12 for a label, a reference, a
 * count, and a caption; 14 for a row's second line; 16 for a row's name and
 * a field; 20 and up for a screen's title, in Literata. The near pairs the
 * ladder used to carry (12 and 13, 14 and 15) were two sizes eight and six
 * percent apart, which the eye cannot read as two steps; the meta line and
 * the label now share 12, and the hierarchy is tone, not a point of size
 * (owner decision, this session).
 *
 * The drawn size scales with the system font scale for the optical axis, the
 * way [literataAt] does, so a reader at 2.0 scale reads the cut for double
 * size.
 */
@Composable
internal fun quranTypography(): Typography {
    val scale = LocalDensity.current.fontScale
    return Typography(
        displayLarge = readingStyle(FontWeight.SemiBold, 56f, 64f),
        displayMedium = readingStyle(FontWeight.SemiBold, 48f, 56f),
        displaySmall = readingStyle(FontWeight.SemiBold, 40f, 48f),
        headlineLarge = readingStyle(FontWeight.SemiBold, 34f, 42f),
        headlineMedium = readingStyle(FontWeight.SemiBold, 28f, 36f),
        headlineSmall = readingStyle(FontWeight.SemiBold, 24f, 32f),
        titleLarge = readingStyle(FontWeight.SemiBold, 20f, 28f),
        titleMedium = readingStyle(FontWeight.Medium, 16f, 23f),
        titleSmall = readingStyle(FontWeight.Medium, 14f, 20f),
        bodyLarge = interStyle(FontWeight.Normal, 16f, 23f, scale, tabular = false),
        bodyMedium = interStyle(FontWeight.Normal, 14f, 20f, scale, tabular = false),
        bodySmall = interStyle(FontWeight.Normal, 12f, 17f, scale, tabular = true),
        labelLarge = interStyle(FontWeight.Medium, 14f, 19f, scale, tabular = true),
        labelMedium = interStyle(FontWeight.Medium, 12f, 16f, scale, tabular = true),
        labelSmall = interStyle(FontWeight.Medium, 12f, 16f, scale, tabular = true),
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
 * The interface in Bangla: the same six sizes as the English ladder, in the
 * script's own two faces. A title is Noto Serif Bengali, the reading face, and
 * carries 1.6 of its size in line, because that face's own line box is 1.594
 * of the em and a Bengali title's stacked marks need it; a label, a caption,
 * and a row's second line are Noto Sans Bengali at 1.45, which clears that
 * face's 1.403 ink. The two ratios are the script's, not a borrowed
 * compromise, so the Bangla ladder keeps its own air at every step (owner
 * decision, the audio and type audit session; the 1.6 titles this session).
 */
val BanglaTypography = Typography(
    displayLarge = banglaStyle(FontWeight.SemiBold, 56f, serif = true),
    displayMedium = banglaStyle(FontWeight.SemiBold, 48f, serif = true),
    displaySmall = banglaStyle(FontWeight.SemiBold, 40f, serif = true),
    headlineLarge = banglaStyle(FontWeight.SemiBold, 34f, serif = true),
    headlineMedium = banglaStyle(FontWeight.SemiBold, 28f, serif = true),
    headlineSmall = banglaStyle(FontWeight.SemiBold, 24f, serif = true),
    titleLarge = banglaStyle(FontWeight.SemiBold, 20f, serif = true),
    titleMedium = banglaStyle(FontWeight.Medium, 16f, serif = true),
    titleSmall = banglaStyle(FontWeight.Medium, 14f, serif = true),
    bodyLarge = banglaStyle(FontWeight.Normal, 16f),
    bodyMedium = banglaStyle(FontWeight.Normal, 14f),
    bodySmall = banglaStyle(FontWeight.Normal, 12f, tabular = true),
    labelLarge = banglaStyle(FontWeight.Medium, 14f, tabular = true),
    labelMedium = banglaStyle(FontWeight.Medium, 12f, tabular = true),
    labelSmall = banglaStyle(FontWeight.Medium, 12f, tabular = true),
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
)

/**
 * Literata at the size it is about to be drawn, in the three weights a page
 * can ask for, upright and italic. The face's optical size axis runs 7 to 72
 * with the default at 12, so a 17 sp body would otherwise be set in the cut
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
    fontSize = 16.2f.sp,
    lineHeight = 25.92f.sp,
)

/**
 * The Bangla reading voice at the middle step. The body is 18 sp against the
 * Latin 16.2: Bengali glyphs read smaller than Latin at the same nominal size,
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
     * The face's own line box, as a ratio of its size. A multi-line block is
     * never set tighter than this: below it one line's marks sit in the
     * next one's air. Literata's box is 1.485 of the em and Noto Serif
     * Bengali's 1.594.
     */
    val minimumLineRatio: Float,
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
 * Hyphenation follows the content's own language, not the phone's, so an
 * English translation hyphenates on an English dictionary even when the
 * interface is Bangla. The paragraph resolves its own direction from its first
 * strong character, so an Arabic paragraph in an English tafsir lays its
 * wrapped lines from the right and a Latin one from the left. The line's extra
 * air is trimmed from the first and last line, so the dp a surface puts above
 * and below a paragraph is the air the eye actually sees (owner decisions,
 * this session).
 *
 * The Arabic verse keeps its own line box and does not come through here: its
 * 1.9 leading is the measured rhythm of the marks, and trimming it would
 * tighten the verses toward each other (see [arabicStyle]).
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
    lineHeight = lineSp.sp,
    color = color,
    textDirection = TextDirection.Content,
    localeList = locale,
    lineBreak = LineBreak.Paragraph,
    hyphens = Hyphens.Auto,
    fontSynthesis = FontSynthesis.None,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Proportional,
        trim = LineHeightStyle.Trim.Both,
    ),
)

/** The reading's own locale, built once per language. */
internal val EnglishReadingLocale = LocaleList("en")
internal val BanglaReadingLocale = LocaleList("bn")

val LocalReadingVoice = staticCompositionLocalOf {
    ReadingVoice(
        LatinReading,
        TextSize.LATIN_PROSE_RATIO,
        1.485f,
        ::literataAt,
        EnglishReadingLocale,
    )
}
