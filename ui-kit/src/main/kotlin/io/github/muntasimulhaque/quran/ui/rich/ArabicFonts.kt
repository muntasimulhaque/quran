package io.github.muntasimulhaque.quran.ui.rich

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import io.github.muntasimulhaque.quran.ui.theme.Amiri

/**
 * The Arabic runs inside the reading are drawn in the Hafs face, the Quran's
 * own hand: the tafsir's quotations, and the Arabic inside a translation, are
 * the Book's words and read as the reading does (owner decision, the audio
 * and type audit session). A handful of codepoints inside the tafsir source
 * (an Urdu heh inside a blessing, one pre-shaped phrase) have no glyph in
 * Hafs, so those letters fall back to Amiri Quran, which ships beside it. The
 * choice is per codepoint and checked against the real typefaces, so no glyph
 * is ever left to the platform to guess and none can reach the screen as
 * tofu.
 */
class ArabicFonts(context: Context) {

    private val hafs: Typeface? =
        Typeface.createFromAsset(context.assets, "fonts/UthmanicHafs_V22.ttf")

    val hafsFamily: FontFamily =
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))

    private val paint = Paint()

    fun family(codepoint: Int): FontFamily =
        if (covers(hafs, codepoint)) hafsFamily else Amiri

    private fun covers(typeface: Typeface?, codepoint: Int): Boolean {
        if (typeface == null) return false
        paint.typeface = typeface
        return paint.hasGlyph(String(Character.toChars(codepoint)))
    }
}
