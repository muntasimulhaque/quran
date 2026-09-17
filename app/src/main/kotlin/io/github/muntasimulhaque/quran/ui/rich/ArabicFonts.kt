package io.github.muntasimulhaque.quran.ui.rich

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.ui.theme.Amiri

/**
 * Arabic outside the canonical text is drawn in Amiri Quran. A handful of
 * codepoints inside the tafsir source (an Urdu heh inside a blessing, one
 * pre-shaped phrase) have no glyph there, so those letters are drawn with the
 * Hafs font that already ships with the app. The choice is per codepoint and
 * checked against the real typefaces, so no glyph is ever left to the
 * platform to guess and none can reach the screen as tofu.
 */
class ArabicFonts(context: Context) {

    private val amiri: Typeface? = ResourcesCompat.getFont(context, R.font.amiri_quran)
    private val hafs: Typeface? =
        Typeface.createFromAsset(context.assets, "fonts/UthmanicHafs_V22.ttf")

    val hafsFamily: FontFamily =
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))

    private val paint = Paint()

    fun family(codepoint: Int): FontFamily =
        if (covers(amiri, codepoint)) Amiri else hafsFamily

    private fun covers(typeface: Typeface?, codepoint: Int): Boolean {
        if (typeface == null) return false
        paint.typeface = typeface
        return paint.hasGlyph(String(Character.toChars(codepoint)))
    }
}
