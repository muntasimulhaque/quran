package io.github.muntasimulhaque.quran.core

import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * The same text with its ASCII digits written in [locale]'s own digits, so a
 * reference the reader meets keeps the language they chose: "56:59" becomes
 * "৫৬:৫৯" in a Bangla interface.
 *
 * Content is stored once and stays ASCII. The content database's `verse_key`
 * column is the surah and the ayah in Latin digits, and it is also the key
 * search parses and the reading navigates with, so it is never written in
 * another script. The number the reader sees changes at the edge, where a
 * reference is drawn, and nowhere earlier.
 *
 * Only the ten ASCII digits move. The colon, the range's hyphen, a surah
 * name, and any Latin word beside them are left exactly as they were, which
 * is what a reference line is made of. A locale whose digits are already
 * ASCII, English among them, gets the same string back untouched.
 *
 * The digits come from the locale's own [DecimalFormatSymbols], never a
 * hand-written table: every script Android carries is already described
 * there, and a language added later names its own digits without a change
 * here (owner decision, this session).
 */
fun localizedDigits(text: String, locale: Locale): String {
    val zero = DecimalFormatSymbols.getInstance(locale).zeroDigit
    if (zero == '0') return text
    var changed = false
    val out = StringBuilder(text.length)
    for (character in text) {
        if (character in '0'..'9') {
            out.append(zero + (character - '0'))
            changed = true
        } else {
            out.append(character)
        }
    }
    return if (changed) out.toString() else text
}

/**
 * The same text with every decimal digit written as ASCII, so a query typed in
 * the reader's own digits ("٢:٧", "২:৭") is read as the one form the
 * content stores. [Character.digit] knows every script's ten digits, so no
 * table is kept here, and a code point that is not a decimal digit is left
 * exactly as it was.
 *
 * This is the query's own fold only: the indexed columns keep the shape
 * [Arabic.normalizeForSearch] gives them, and a reference the reader meets is
 * still drawn in their language's digits (owner decision, this session).
 */
fun asciiDigits(input: String): String {
    var changed = false
    val out = StringBuilder(input.length)
    var index = 0
    while (index < input.length) {
        val codePoint = input.codePointAt(index)
        index += Character.charCount(codePoint)
        val digit = Character.digit(codePoint, 10)
        if (digit in 0..9) {
            out.append('0' + digit)
            changed = true
        } else {
            out.appendCodePoint(codePoint)
        }
    }
    return if (changed) out.toString() else input
}
