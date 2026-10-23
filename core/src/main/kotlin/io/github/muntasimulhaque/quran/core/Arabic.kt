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
 *
 * The two shapes differ in one place: the QPC print writes the alef maksura
 * base as a dotless beh (U+066E), and its small waw and small yeh (U+06E5,
 * U+06E6) stand for a pronoun or a silent letter. The search folds the
 * dotless beh to the yeh a reader types and skips the small letters a reader
 * does not write, while the skeleton keeps the printed letters so the audit
 * can report the print's own spelling as a print variant. A reader who types
 * "هدى" must find "هَدَٮٰكُم", and "داود" must find "دَاوُۥدَ"
 * (owner report, the QPC rebuild audit).
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

    /**
     * Whether [codePoint] is a mark, a format character, or the tatweel
     * itself: the parts of a word that are not its letters. A justification line
     * engine reads this to find where a word's letters are, because a tatweel
     * belongs between two letters and never between a letter and its mark.
     */
    fun isMark(codePoint: Int): Boolean = isStripped(codePoint)

    /**
     * The print's letter bodies that a search query still spells the ordinary
     * way. They are not in [FOLDED_CODEPOINTS] because [skeleton] must keep
     * the printed letter for the audit.
     */
    private val SEARCH_FOLDED_CODEPOINTS = mapOf(
        0x066E to YEH, // QPC print's alef-maksura base
    )

    /**
     * The QPC print's small waw and small yeh (U+06E5, U+06E6) are letters
     * the print uses for a pronoun or a silent letter; a reader's query
     * spells the word without them. They are skipped in the search shape,
     * not folded to another letter, so "داود" finds "دَاوُۥدَ" and "به"
     * finds "بِهِۦ".
     *
     * The precomposed waw and yeh with hamza (U+0624, U+0626) are skipped for
     * the same reason. The print writes the hamza as a combining mark on the
     * bare seat, so its index holds one letter; a reader typing the ordinary
     * form writes a second letter that the print does not have. Dropping the
     * seat lets "شيئا" meet "شَيۡــًٔا" and "مسؤولا" meet "مَسۡـُٔولًا". The
     * skeleton keeps the folded seat, because the audit compares the print's
     * own letters.
     */
    fun isSearchIgnored(codePoint: Int): Boolean =
        codePoint == 0x06E5 || codePoint == 0x06E6 ||
            codePoint == 0x0624 || codePoint == 0x0626

    /**
     * The print's page signs: the end of the ayah (`U+06DD`), the rub el hizb
     * (`U+06DE`), and the sajdah (`U+06E9`). They stand beside a word and are
     * not part of it. The print glues the rub el hizb to the first word of
     * its ayah, so an index that keeps it makes that word unreachable through
     * the full text index, whose tokenizer keeps every non-ASCII code point
     * inside its token: a reader typing "قل" does not find "۞قُلۡ"
     * (owner report, this session). They are not in [STRIPPED_RANGES],
     * because [isMark] means a part of a word, and these stand beside one.
     */
    fun isPageSign(codePoint: Int): Boolean =
        codePoint == 0x06DD || codePoint == 0x06DE || codePoint == 0x06E9

    /**
     * True when the search shape does not carry [codePoint] at all: a mark, a
     * format character, one of the print's small letters, or a page sign.
     * [normalizeForSearch] and `Search.matchRanges` both read this one rule,
     * so a row and the highlight drawn from it cannot disagree.
     */
    fun isSkippedForSearch(codePoint: Int): Boolean =
        isStripped(codePoint) || isSearchIgnored(codePoint) || isPageSign(codePoint)

    private fun fold(codePoint: Int): Int = FOLDED_CODEPOINTS[codePoint] ?: codePoint

    private fun searchFold(codePoint: Int): Int =
        SEARCH_FOLDED_CODEPOINTS[codePoint] ?: fold(codePoint)

    private fun toAsciiDigit(codePoint: Int): Int = when (codePoint) {
        in 0x0660..0x0669 -> codePoint - 0x0660 + '0'.code // Arabic-Indic
        in 0x06F0..0x06F9 -> codePoint - 0x06F0 + '0'.code // Extended Arabic-Indic
        else -> codePoint
    }

    /**
     * NFC, marks and the print's page signs removed, letter forms folded,
     * Arabic-Indic digits made ASCII, whitespace collapsed to single spaces
     * and trimmed. Suitable for indexing and for normalizing a query.
     */
    fun normalizeForSearch(input: String): String {
        val nfc = Normalizer.normalize(input, Normalizer.Form.NFC)
        val out = StringBuilder(nfc.length)
        var pendingSpace = false
        var i = 0
        while (i < nfc.length) {
            val cp = nfc.codePointAt(i)
            i += Character.charCount(cp)
            if (isSkippedForSearch(cp)) continue
            if (Character.isWhitespace(cp)) {
                pendingSpace = out.isNotEmpty()
                continue
            }
            if (pendingSpace) {
                out.append(' ')
                pendingSpace = false
            }
            out.appendCodePoint(toAsciiDigit(searchFold(cp)))
        }
        return out.toString().trim()
    }

    /**
     * One code point in the shape the search index holds: marks and tatweel
     * folded away, letter forms folded, Arabic-Indic digits made ASCII. A
     * single code point needs no normalization pass of its own, because NFC
     * cannot compose a code point with itself; this exists so a caller that
     * walks a whole page of prose can fold one character at a time without
     * the normalizer on the hot path (owner decision, this session).
     */
    fun foldForSearch(codePoint: Int): Int = toAsciiDigit(searchFold(codePoint))

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
                type == Character.LOWERCASE_LETTER.toInt() ||
                // The QPC print's small waw and small yeh are modifier
                // letters; the skeleton is the audit's letter comparator,
                // so it must keep them (owner report, the QPC rebuild audit).
                type == Character.MODIFIER_LETTER.toInt()
            if (isLetter) out.appendCodePoint(folded)
        }
        return out.toString()
    }

    /**
     * The ayah as portable text: the print's own words, folded to the letters
     * and marks a reader's ordinary Arabic font can draw, for the surfaces
     * that cannot use the Hafs face.
     *
     * The reading draws the QPC print exactly, in the face the print was set
     * in. The share text and the daily reminder leave the app for another
     * font, and a screen reader hands the reading to a speech engine. Neither
     * can be assumed to draw the print's encoding as the print meant:
     *
     * - `U+066E`, the print's dotless alef-maksura base, becomes `U+0649`,
     *   so a text-to-speech engine reads the long vowel and not a beh.
     * - `U+06E5` and `U+06E6`, the small waw and small yeh, are dropped; a
     *   reader spells the word without them, and the search already does.
     * - `U+0640`, the print's madda kashida, and `U+200C`, its separator
     *   before a pause mark, are dropped.
     * - the page's signs are dropped: the rub el hizb (`U+06DE`), the waqf
     *   signs (`U+06D6` to `U+06DC`), the sajdah (`U+06E9`), and the empty
     *   centre stops; they are furniture of the printed page, not the
     *   verse's words, and another font draws several as a floating mark.
     * - the print's tajweed marks (`U+06D9`, `U+06E2`, `U+06E7`, `U+06E8`,
     *   `U+06E3`, and `U+06ED`) are dropped; they name a rule the receiving
     *   app does not know.
     * - the print's sukun (`U+06E1`) becomes the standard sukun (`U+0652`),
     *   which every Arabic font draws and every speech engine reads.
     *
     * Everything else stays, above all the harakat, the shadda, the tanween,
     * the madda, the superscript alef, and the hamzas, because those are the
     * pronunciation. This is not an edit of the text: the reading's bytes
     * are untouched, and every drop is a format or annotation character,
     * never a letter or a vowel (owner decision, the palette session).
     */
    fun portableText(input: String): String {
        val nfc = Normalizer.normalize(input, Normalizer.Form.NFC)
        val out = StringBuilder(nfc.length)
        var pendingSpace = false
        var i = 0
        while (i < nfc.length) {
            val cp = nfc.codePointAt(i)
            i += Character.charCount(cp)
            if (Character.isWhitespace(cp)) {
                pendingSpace = out.isNotEmpty()
                continue
            }
            if (cp == TATWEEL || cp == 0x200C || cp == 0x06E5 || cp == 0x06E6) continue
            if (isPortableDropped(cp)) continue
            if (pendingSpace) {
                out.append(' ')
                pendingSpace = false
            }
            val mapped = when (cp) {
                0x066E -> 0x0649 // the print's dotless alef-maksura base
                0x06E1 -> 0x0652 // the print's sukun, as the standard one
                else -> cp
            }
            out.appendCodePoint(mapped)
        }
        return out.toString().trim()
    }

    /** The print's page and annotation signs, which the export does not carry. */
    private fun isPortableDropped(codePoint: Int): Boolean = when (codePoint) {
        in 0x06D6..0x06E0 -> true // waqf signs, ayah end, rub el hizb, zeros
        0x06E2, 0x06E3 -> true // small high meem and small low seen
        in 0x06E7..0x06ED -> true // small high yeh and noon, sajdah, stops
        else -> false
    }

    /**
     * The ayah's own number in Arabic-Indic digits, "\u0661" for the first
     * verse. It is what the reading appends to close a verse: the KFGQPC Hafs
     * face draws the print's own medallion around a run of these digits, so
     * the number and the sign are one glyph and the app never types the sign
     * itself (owner decision, this session). A pure value, so its shape is
     * pinned without a device.
     */
    fun ayahNumber(number: Int): String = buildString {
        for (digit in number.toString()) {
            append('\u0660' + (digit - '0'))
        }
    }
}
