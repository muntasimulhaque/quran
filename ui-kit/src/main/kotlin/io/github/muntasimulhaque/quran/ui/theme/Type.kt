package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.content.R

/**
 * The voices of the app. Arabic is the Book's own: the QPC page fonts on the
 * the KFGQPC Hafs face in the reading, with Amiri for
 * ornaments. Everything else follows the reader's one language choice: the
 * interface speaks Inter in English and Noto Sans Bengali in Bangla, and the
 * reading (translation, tafsir, word meanings, notes) is Literata in English
 * and Noto Serif Bengali in Bangla. A Bangla sentence never falls through to
 * the phone's default Bengali font, because the whole voice is swapped, not
 * one glyph at a time.
 */

/** The interface voice: quiet, exact, never decorative. */
val Inter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Light),
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
)

/** The Bangla interface voice: the same job Inter does, in Bengali script. */
val NotoSansBengali = FontFamily(
    Font(R.font.noto_sans_bengali, FontWeight.Light),
    Font(R.font.noto_sans_bengali, FontWeight.Normal),
    Font(R.font.noto_sans_bengali, FontWeight.Medium),
    Font(R.font.noto_sans_bengali, FontWeight.SemiBold),
)

/**
 * The reading voice for translations and tafsir. The italic members keep an
 * emphasized run inside Literata: without them a span that asks for italic
 * falls through to the platform default, which breaks the page voice mid
 * sentence. The variable file carries one upright cut, so the italic here
 * is the skew of that cut until a true italic variable file joins the assets.
 */
val Literata = FontFamily(
    Font(R.font.literata_variable, FontWeight.Normal),
    Font(R.font.literata_variable, FontWeight.Medium),
    Font(R.font.literata_variable, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.literata_variable, FontWeight.Medium, FontStyle.Italic),
)

/**
 * The Bangla reading voice, for translations, tafsir, and notes. The
 * SemiBold member is what a bold run of the tafsir asks for; without it a
 * family is matched to the nearest declared weight and the emphasis would
 * render at Medium.
 */
val NotoSerifBengali = FontFamily(
    Font(R.font.noto_serif_bengali, FontWeight.Normal),
    Font(R.font.noto_serif_bengali, FontWeight.Medium),
    Font(R.font.noto_serif_bengali, FontWeight.SemiBold),
)

/** The display voice of the ornaments. */
val Amiri = FontFamily(Font(R.font.amiri_quran))

/**
 * The reading voice of the Quran's own text: the KFGQPC Hafs face, the same
 * one the study reading and the word by word aid are drawn in. It is a font
 * file rather than a resource, so it is loaded from the assets; use
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
 * Every style the app can ask for, in one interface voice. Nothing is left
 * to the Material default, because a style that falls through would draw in
 * another font on a page that has chosen its typefaces.
 */
private fun typographyFor(voice: FontFamily) = Typography(
    displayLarge = TextStyle(fontFamily = voice, fontWeight = FontWeight.SemiBold, fontSize = 48.sp, lineHeight = 56.sp),
    displayMedium = TextStyle(fontFamily = voice, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 48.sp),
    displaySmall = TextStyle(fontFamily = voice, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = voice, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = voice, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = voice, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = voice, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = voice, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = voice, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = voice, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = voice, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = voice, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = voice, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 19.sp),
    labelMedium = TextStyle(fontFamily = voice, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = voice, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

/** The interface in English. */
val QuranTypography = typographyFor(Inter)

/**
 * The interface in Bangla. The same sizes and line heights Inter gets: Noto
 * Sans Bengali's natural line box (1.325 of the em against Inter's 1.210)
 * sits inside them with room, so one scale serves both voices.
 */
val BanglaTypography = typographyFor(NotoSansBengali)

/**
 * Literata at the size it is about to be drawn, in the three weights a
 * paragraph of reading can ask for, upright and italic. The face's optical
 * size axis runs 7 to 72 with the default at 12, so a 17 sp body would
 * otherwise be set in the cut drawn for footnotes. Faces without the axis
 * (the Bengali voices) have one cut and never come through here, and below
 * API 26 the platform ignores the setting.
 *
 * The six members are the whole of what the reading requests: the regular
 * text, the Medium a footnote marker wears, and the SemiBold a bold run of
 * the tafsir wears, each upright and italic. A family is matched by the
 * weight and style each member declares,
 * so a member must exist for every weight a span can request, and each one
 * carries its own wght axis beside the opsz: Compose applies only the
 * variation settings a Font declares, and a family built with the opsz
 * alone loads every member at the file's default 400, so a bold run would
 * be drawn exactly like the regular text around it (the same silence a
 * static face has, made worse by the family looking correct).
 *
 * The opsz is the size the text is actually drawn at, the reader's own
 * scale included: a reader whose scale is doubled reads the cut Literata
 * drew for double size, not the small-text cut stretched over it.
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
    R.font.literata_variable,
    weight,
    style,
    variationSettings = FontVariation.Settings(
        FontVariation.Setting("opsz", sizeSp),
        FontVariation.weight(weight.weight),
    ),
)

private val literataCuts = java.util.concurrent.ConcurrentHashMap<Int, FontFamily>()

/** The Latin reading voice, for translations, tafsir, and notes. */
val LatinReading = TextStyle(
    fontFamily = Literata,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    lineHeight = 27.sp,
)

/**
 * The Bangla reading voice. The body is 18 sp against the Latin 17: Bengali
 * glyphs read smaller than Latin at the same nominal size, so parity needs
 * the extra point. The line is the face's own natural line plus the air
 * Literata's gets: Noto Serif Bengali's natural line is 1.594 of the em,
 * Literata's 1.485 is lifted to 1.588, so at 18 sp the Bangla line is 31.
 * Bengali script stacks its vowel marks above and below the line, and a
 * tighter box clips them.
 */
val BengaliReading = TextStyle(
    fontFamily = NotoSerifBengali,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 31.sp,
)

/**
 * The reading voice of the language being read: the style, the line a
 * paragraph of it gets per em of text (an Arabic line is its own, taller
 * thing, and never takes this one), and the face cut for an exact drawn
 * size. The theme sets it once from the reader's language; every reading
 * surface takes it from here.
 */
class ReadingVoice(
    val style: TextStyle,
    val lineRatio: Float,
    val atSize: @Composable (Float) -> FontFamily,
)

val LocalReadingVoice = staticCompositionLocalOf { ReadingVoice(LatinReading, 1.6f, ::literataAt) }
