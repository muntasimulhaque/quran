package io.github.muntasimulhaque.quran.tools

/**
 * One ayah of the Al-Bayan translation after its footnotes have been moved out
 * of the sentence.
 *
 * QUL carries at most one note per ayah, after a blank line and a star. The
 * publisher's Word edition carries every note the printed page has, so its
 * list decides how many markers an ayah gets and what each note says. QUL's
 * body is the fallback for the one note QUL added (9:1) and for the one note
 * whose Arabic the Word file still holds in a legacy font (33:37).
 */
internal data class BayanAyah(
    val text: String,
    val notes: List<String>,
)

/**
 * The two ayahs whose note has no star in the QUL sentence but does have a
 * place in the publisher's sentence. 15:76's note belongs after its last word;
 * 31:12's note belongs beside জ্ঞান, the word it explains, where QUL's revised
 * sentence no longer says হিকমাত (owner decision, this session).
 */
internal val bayanAnchors = mapOf(
    "15:76" to "বিদ্যমান",
    "31:12" to "জ্ঞান",
)

/**
 * Splits one QUL Al-Bayan row into the ayah and its notes.
 *
 * The QUL sentence marks note n with n stars, so `*` is `[1]`, `**` is `[2]`,
 * and so on. The body after the blank line carries the notes separated by
 * stars; when [sourceNotes] is present, the publisher's notes replace the
 * body's, because QUL keeps only the first note of most ayahs. A body note
 * whose text the publisher places on another ayah is a QUL misplacement and
 * is dropped. [anchorAfter] places a general note beside the word it explains
 * instead of at the end of the sentence.
 */
internal fun bayanAyah(
    raw: String,
    sourceNotes: List<String>? = null,
    allSourceNotes: Set<String> = emptySet(),
    anchorAfter: String? = null,
): BayanAyah {
    // The QUL export carries U+00AC where a conjunct glyph was lost. It is
    // not a Bengali letter, and the publisher's text there has no such sign.
    val source = raw.replace("\r\n", "\n").replace("\u00AC", "").trim()
    val separator = source.indexOf("\n\n*")
    val translation = bayanSpelling(
        if (separator >= 0) source.substring(0, separator).trimEnd() else source,
    )
    val body = if (separator >= 0) source.substring(separator + 3) else ""
    val bodyNotes = noteBodies(body)
    val notes = when {
        sourceNotes != null -> sourceNotes.mapIndexed { index, note ->
            // The Word file stores 33:37's Arabic quotation in a legacy font.
            // The QUL body has the same note in Unicode, so it stands in for
            // that one note.
            val replacement = bodyNotes.getOrNull(index)
            if (note.any(::isLegacyArabicGlyph) && replacement != null) {
                replacement
            } else {
                cleanNote(note)
            }
        }
        // A general note QUL kept but the publisher places on another ayah is
        // a row that slipped (34:54 carries 34:2's note). The publisher's
        // placement wins; 9:1's note is unique and stays.
        bodyNotes.isNotEmpty() && normalizeNote(bodyNotes.first()) in allSourceNotes -> emptyList()
        else -> bodyNotes
    }
    return BayanAyah(withMarkers(translation, notes, anchorAfter), notes)
}

/** The legacy Arabic presentation forms, which only 33:37 uses in the Word file. */
private fun isLegacyArabicGlyph(char: Char): Boolean =
    char in '\uFB50'..'\uFDFF' || char in '\uFE70'..'\uFEFF'

/**
 * The notes in a QUL body, in order. The leading star starts the first note;
 * each later star run starts the next. QUL uses a star only as a note
 * separator, so a star run inside a note is the next note.
 */
private fun noteBodies(body: String): List<String> =
    body.split(Regex("\\*+"))
        .map(::cleanNote)
        .filter { it.isNotEmpty() }

/** One note as the reading stores it: one line, no marker, no invisible signs. */
internal fun cleanNote(raw: String): String {
    val oneLine = raw.replace("\r\n", "\n")
        .replace(Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF]"), "")
        .replaceFirst(Regex("^\\*+\\s*"), "")
        .replaceFirst(Regex("^[-\\u2013\\u2014]\\s*"), "")
        .replace("*", "")
        .replace(Regex("\\s+"), " ")
        .trim()
    // The publisher's 18:80 note begins with a stray period. A note never
    // starts with one, so it is not part of the note (owner decision, this
    // session).
    val withoutStrayPeriod = oneLine.replaceFirst(Regex("^\\.\\s*"), "")
    return bayanSpelling(withoutStrayPeriod).trim()
}

/**
 * One spelling the publisher's file and the print both get wrong: ধরন is a
 * misspelling of ধরণ, and the owner asked for the correct form everywhere it
 * appears, in the notes and in the sentence (owner decision, this session).
 */
internal fun bayanSpelling(text: String): String = text.replace("ধরন", "ধরণ")

/** A note text with its spacing and invisible signs gone, for comparing. */
internal fun normalizeNote(text: String): String =
    cleanNote(text).replace(Regex("[^\\u0980-\\u09FFA-Za-z0-9]+"), " ").trim()

/**
 * Replaces the sentence's star runs with `[n]` markers and appends a marker
 * for a general note. A run with no note leaves nothing behind, the old QUL
 * rule that kept a star from opening an empty sheet.
 */
private fun withMarkers(translation: String, notes: List<String>, anchorAfter: String?): String {
    val runs = Regex("\\*+").findAll(translation).toList()
    val builder = StringBuilder(translation.length + notes.size * 3)
    var last = 0
    for (run in runs) {
        builder.append(translation, last, run.range.first)
        val number = run.value.length
        if (number <= notes.size) builder.append('[').append(number).append(']')
        last = run.range.last + 1
    }
    builder.append(translation, last, translation.length)
    var text = builder.toString().replace("*", "").trim()
    if (runs.isEmpty() && notes.size == 1) {
        text = appendMarker(text, anchorAfter)
    } else {
        val placed = runs.maxOfOrNull { it.value.length } ?: 0
        for (number in placed + 1..notes.size) text += "[$number]"
    }
    return text
}

/**
 * Puts `[1]` after [anchorAfter] when the sentence has it, and after the
 * sentence's last mark otherwise. The text itself is never otherwise changed.
 */
private fun appendMarker(text: String, anchorAfter: String?): String {
    if (anchorAfter != null) {
        val at = text.indexOf(anchorAfter)
        if (at >= 0) {
            val end = at + anchorAfter.length
            return text.substring(0, end) + "[1]" + text.substring(end)
        }
    }
    return text + "[1]"
}
