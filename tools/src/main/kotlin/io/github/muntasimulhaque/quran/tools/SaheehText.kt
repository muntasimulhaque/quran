package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The publisher's notes and the reader's numbering.
 *
 * The Saheeh International edition numbers its footnotes across the whole
 * book, and its first note is the surah title's. QuranEnc's export is keyed
 * by ayah, so it cannot carry a title note: Al-Fatihah opens on note 2, and
 * every surah start after it leaves one number unused. The reading numbers
 * each ayah's notes from one instead, the rule the Bangla pack already
 * follows and the rule Quran.com presents this same translation with (owner
 * decision, this session).
 *
 * The publisher's words are not touched. The localizer rewrites only the
 * bracketed numbers the sentence draws; the note bodies keep their text. A
 * marker whose body the export lost is dropped, because a number that opens
 * nothing is a dead door. The only such marker in version 1.1.2 is 7:143's
 * [403], whose note the publisher's export does not carry and whose 1997
 * print does not have.
 *
 * The export also has one typo: 3:32 reads "if you turn away", while the
 * 1997 and 2023 printed editions and the Arabic (فَإِن تَوَلَّوْا, third
 * person) read "if they turn away". [saheehCorrections] carries that one
 * correction with its source and reason, and the build fails if the source
 * no longer holds the sentence the correction expects (owner decision, this
 * session).
 */
internal data class SaheehLocal(
    val text: String,
    val footnotes: String,
    /** Source numbers of markers whose notes the export does not carry. */
    val dropped: List<Int>,
    /** Source numbers of notes whose markers the export does not carry. */
    val unused: List<Int>,
    /** The number of notes the ayah now carries. */
    val notes: Int,
)

internal data class SaheehCorrection(
    /** The global ayah number, the id the publisher's export uses. */
    val ayah: Int,
    /** The reference a reader and a maintainer know, for example 3:32. */
    val reference: String,
    val from: String,
    val to: String,
    val reason: String,
)

internal val saheehCorrections = listOf(
    SaheehCorrection(
        // 3:32. The sentence is quoted whole so a shorter phrase that also
        // occurs at 11:3 and 11:57, where "you" is right, cannot match.
        ayah = 325,
        reference = "3:32",
        from = "But if you turn away - then indeed, Allāh does not like the disbelievers.",
        to = "But if they turn away - then indeed, Allāh does not like the disbelievers.",
        reason = "The 1997 and 2023 printed editions and the Arabic read \"they\"; " +
            "QuranEnc 1.1.2 has a typo.",
    ),
)

private val saheehMarker = Regex("\\[(\\d+)]")
private val saheehNote = Regex("\\[(\\d+)]-\\s*")

/**
 * The source's footnotes, by their printed number. The export carries one
 * block per ayah, each note beginning with `[n]- `; the bodies are the text
 * between two beginnings.
 */
internal fun saheehNotes(raw: String): LinkedHashMap<Int, String> {
    val matches = saheehNote.findAll(raw).toList()
    val notes = LinkedHashMap<Int, String>(matches.size)
    matches.forEachIndexed { index, match ->
        val start = match.range.last + 1
        val end = matches.getOrNull(index + 1)?.range?.first ?: raw.length
        notes[match.groupValues[1].toInt()] = raw.substring(start, end).trim()
    }
    return notes
}

/** [text] with [ayah]'s one known correction applied, or unchanged. */
internal fun saheehCorrected(ayah: Int, text: String): String {
    val correction = saheehCorrections.firstOrNull { it.ayah == ayah } ?: return text
    val at = text.indexOf(correction.from)
    require(at >= 0) {
        "the source no longer holds the sentence ${correction.reference} corrects: ${correction.reason}"
    }
    require(text.indexOf(correction.from, at + 1) < 0) {
        "the correction target appears twice in ${correction.reference}"
    }
    return text.replaceRange(at, at + correction.from.length, correction.to)
}

/**
 * The sentence with its markers numbered within the ayah. The first marker
 * the sentence carries is 1, the second 2, and a repeated marker keeps the
 * number it was given. The note bodies are emitted in the same order, so a
 * caller that stores the pair has nothing left to number.
 */
internal fun localizeSaheeh(text: String, notes: Map<Int, String>): SaheehLocal {
    val numbers = LinkedHashMap<Int, Int>(notes.size)
    val dropped = ArrayList<Int>()
    val out = StringBuilder(text.length)
    var last = 0
    for (match in saheehMarker.findAll(text)) {
        val source = match.groupValues[1].toInt()
        if (source !in notes) {
            dropped += source
            // The brackets are not part of the sentence: remove them and
            // keep the words on either side.
            out.append(text, last, match.range.first)
            last = match.range.last + 1
            continue
        }
        val number = numbers.getOrPut(source) { numbers.size + 1 }
        out.append(text, last, match.range.first)
        out.append('[').append(number).append(']')
        last = match.range.last + 1
    }
    out.append(text, last, text.length)
    val footnotes = numbers.entries.joinToString(" ") { (source, number) ->
        "[$number]- ${notes.getValue(source)}"
    }
    return SaheehLocal(
        text = out.toString(),
        footnotes = footnotes,
        dropped = dropped,
        unused = notes.keys.filterNot { it in numbers },
        notes = numbers.size,
    )
}

/**
 * The notes the built database stores as JSON (`[{"n":"2","text":"..."}]`),
 * read back into the source-number map the pack builder localizes. The
 * database keeps the publisher's numbers; the pack gets the reader's.
 */
internal fun notesFromJson(raw: String): LinkedHashMap<Int, String> {
    val array = Json.parseToJsonElement(raw).jsonArray
    val notes = LinkedHashMap<Int, String>(array.size)
    for (entry in array) {
        val note = entry.jsonObject
        val number = note["n"]?.jsonPrimitive?.content?.toIntOrNull()
            ?: error("a stored footnote has no number")
        notes[number] = note["text"]?.jsonPrimitive?.content.orEmpty()
    }
    return notes
}

/** The stored JSON shape of a translation's notes, from the `[n]- body` form. */
internal fun footnotesJson(raw: String): String {
    val entries = Regex("\\[(\\d+)]-\\s*").findAll(raw).toList()
    val array = StringBuilder("[")
    entries.forEachIndexed { index, match ->
        val start = match.range.last + 1
        val end = entries.getOrNull(index + 1)?.range?.first ?: raw.length
        val text = raw.substring(start, end).trim()
        if (index > 0) array.append(',')
        array.append(JsonObject(mapOf(
            "n" to JsonPrimitive(match.groupValues[1]),
            "text" to JsonPrimitive(text),
        )))
    }
    array.append(']')
    return array.toString()
}

