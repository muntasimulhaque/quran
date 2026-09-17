package io.github.muntasimulhaque.quran.core

import java.text.Normalizer

/**
 * Arabic normalization, shared by the content pipeline and the app so that the
 * text indexed at build time and the query typed at runtime always meet the
 * same way. Two shapes are produced from the same rules:
 *
 * - [normalizeForSearch] keeps word boundaries and is what an ayah or a word
 *   is indexed with and what a query is normalized with.
 * - [skeleton] removes everything but letters, and is the audit's comparator:
 *   two editions of the text agree when their letter skeletons agree.
 */
object Arabic {

    private const val TATWEEL = 0x0640
    private const val ALEF = 0x0627
    private const val WAW = 0x0648
    private const val YEH = 0x064A
    private const val HEH = 0x0647
    private const val KAF = 0x0643

    /** Combining marks and format characters that carry no searchable meaning. */
    private val STRIPPED_RANGES = listOf(
        0x0610..0x061A, // Arabic signs
        0x064B..0x065F, // tanween and harakat
        0x0670..0x0670, // superscript alef
        0x06D6..0x06DC, // Quranic annotation signs and pause marks
        0x06DF..0x06E4, // small high marks
        0x06E7..0x06E8, // small high yeh and noon
        0x06EA..0x06ED, // empty centre marks and small low/high meem
        0x08D3..0x08FF, // Arabic Extended-A marks
        0xFE00..0xFE0F, // variation selectors
        0x200B..0x200F, // zero width and directional marks
        0x202A..0x202E,
        0x2066..0x2069,
        0xFEFF..0xFEFF,
        0x25CC..0x25CC, // dotted circle, a fallback artifact
    )

    /** Alef, waw, yeh and heh forms that fold to one base letter. */
    private val FOLDED_CODEPOINTS = mapOf(
        0x0622 to ALEF, // alef with madda
        0x0623 to ALEF, // alef with hamza above
        0x0625 to ALEF, // alef with hamza below
        0x0671 to ALEF, // alef wasla
        0x0672 to ALEF,
        0x0673 to ALEF,
        0x0675 to ALEF,
        0x0624 to WAW, // waw with hamza
        0x0626 to YEH, // yeh with hamza
        0x0649 to YEH, // alef maksura
        0x06CC to YEH, // farsi yeh
        0x0629 to HEH, // ta marbuta
        0x06BE to HEH,
        0x06C0 to HEH,
        0x06C1 to HEH,
        0x06C2 to HEH,
        0x06D5 to HEH,
        0x06A9 to KAF,
    )

    private fun isStripped(codePoint: Int): Boolean =
        codePoint == TATWEEL || STRIPPED_RANGES.any { codePoint in it }

    private fun fold(codePoint: Int): Int = FOLDED_CODEPOINTS[codePoint] ?: codePoint

    private fun toAsciiDigit(codePoint: Int): Int = when (codePoint) {
        in 0x0660..0x0669 -> codePoint - 0x0660 + '0'.code // Arabic-Indic
        in 0x06F0..0x06F9 -> codePoint - 0x06F0 + '0'.code // Extended Arabic-Indic
        else -> codePoint
    }

    /**
     * NFC, marks removed, letter forms folded, Arabic-Indic digits made ASCII,
     * whitespace collapsed to single spaces and trimmed. Suitable for indexing
     * and for normalizing a query.
     */
    fun normalizeForSearch(input: String): String {
        val nfc = Normalizer.normalize(input, Normalizer.Form.NFC)
        val out = StringBuilder(nfc.length)
        var pendingSpace = false
        var i = 0
        while (i < nfc.length) {
            val cp = nfc.codePointAt(i)
            i += Character.charCount(cp)
            if (isStripped(cp)) continue
            if (Character.isWhitespace(cp)) {
                pendingSpace = out.isNotEmpty()
                continue
            }
            if (pendingSpace) {
                out.append(' ')
                pendingSpace = false
            }
            out.appendCodePoint(toAsciiDigit(fold(cp)))
        }
        return out.toString().trim()
    }

    /**
     * Letters only: marks stripped, forms folded, every non-letter removed.
     * This is the audit comparator, not a search index.
     */
    fun skeleton(input: String): String {
        val nfc = Normalizer.normalize(input, Normalizer.Form.NFC)
        val out = StringBuilder(nfc.length)
        var i = 0
        while (i < nfc.length) {
            val cp = nfc.codePointAt(i)
            i += Character.charCount(cp)
            if (isStripped(cp)) continue
            val folded = toAsciiDigit(fold(cp))
            val type = Character.getType(folded)
            val isLetter = type == Character.OTHER_LETTER.toInt() ||
                type == Character.UPPERCASE_LETTER.toInt() ||
                type == Character.LOWERCASE_LETTER.toInt()
            if (isLetter) out.appendCodePoint(folded)
        }
        return out.toString()
    }
}
