package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.content.R

/** The interface voice: quiet, exact, never decorative. */
val Inter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Light),
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
)

/** The reading voice for translations and tafsir. */
val Literata = FontFamily(
    Font(R.font.literata_variable, FontWeight.Normal),
    Font(R.font.literata_variable, FontWeight.Medium),
)

/** The display voice of the Mushaf ornaments. */
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
 * Every style the app can ask for, in the interface's own voice. Nothing is
 * left to the Material default, because a style that falls through would draw
 * in another font on a page that has chosen its typefaces.
 */
val QuranTypography = Typography(
    displayLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 48.sp, lineHeight = 56.sp),
    displayMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 48.sp),
    displaySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 19.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

/** The Latin reading voice, for translations, tafsir, and notes. */
val LatinReading = TextStyle(
    fontFamily = Literata,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    lineHeight = 27.sp,
)
