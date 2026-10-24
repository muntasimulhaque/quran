package io.github.muntasimulhaque.quran.tools

/**
 * One ayah of the QUL Al-Bayan text after its footnote has been moved out of
 * the sentence.
 *
 * QUL carries at most one note per ayah, after a blank line and a star. Some
 * ayahs also mark a second or third place with another star, but QUL does not
 * carry those notes. Those stars are removed rather than left pointing at
 * nothing. A star with no note body is removed with no marker. The note body
 * is kept as QUL has it, apart from its leading bullet and its whitespace.
 */
internal data class BayanAyah(
    val text: String,
    val note: String?,
)

/**
 * Splits one QUL Al-Bayan row into the ayah and its note. The first star
 * becomes the app's `[1]` marker; every other star is a reference QUL did not
 * fill. The note body is the text after the first blank line and its star.
 */
internal fun bayanAyah(raw: String): BayanAyah {
    // The QUL export carries U+00AC where a conjunct glyph was lost. It is
    // not a Bengali letter, and the publisher's text there has no such sign.
    val source = raw.replace("\r\n", "\n").replace("\u00AC", "").trim()
    val separator = source.indexOf("\n\n*")
    val translation = if (separator >= 0) source.substring(0, separator).trimEnd() else source
    val body = if (separator >= 0) source.substring(separator + 3) else ""
    val note = body.trim()
        .replaceFirst(Regex("^\\*+\\s*"), "")
        .replaceFirst(Regex("^[-\\u2013\\u2014]\\s*"), "")
        .replace("*", "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .takeIf { it.isNotEmpty() }
    val firstStar = translation.indexOf('*')
    val text = when {
        note != null && firstStar >= 0 -> {
            val before = translation.substring(0, firstStar)
            val after = translation.substring(firstStar + 1).replace("*", "")
            (before + "[1]" + after).trim()
        }
        // A general note on the ayah has no word to stand beside. It opens
        // from the end of the sentence, which is where the printed page
        // puts its reference for a note that belongs to the whole ayah.
        note != null -> translation.replace("*", "").trim() + "[1]"
        else -> translation.replace("*", "").trim()
    }
    return BayanAyah(text, note)
}
