package io.github.muntasimulhaque.quran.core

import java.text.Normalizer

/**
 * Turns text into styled runs the reader can render with the right font.
 *
 * Two inputs matter here. The translation carries footnote markers inside its
 * sentences, and the tafsirs carry a small, controlled HTML subset. Both are
 * parsed into plain, testable runs; no Compose type is involved, so the logic
 * can be verified without a device.
 *
 * Arabic runs are marked so the app can draw them with the Arabic font; every
 * other run is Latin. A run is never restyled inside a word.
 */
data class TextRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val arabic: Boolean = false,
    val quote: Boolean = false,
    val marker: Int? = null,
)

enum class TextBlockKind { PARAGRAPH, HEADING }

data class TextBlock(val kind: TextBlockKind, val runs: List<TextRun>)

object RichText {

    /** Arabic script ranges, including the presentation forms used by ﷺ. */
    private val arabicRanges = listOf(
        0x0600..0x06FF,
        0x0750..0x077F,
        0x08A0..0x08FF,
        0xFB50..0xFDFF,
        0xFE70..0xFEFF,
    )

    private val marker = Regex("\\[(\\d+)]")

    /** A tag-shaped run: `<`, an optional slash, a letter, then to the `>`. */
    private val tag = Regex("</?[a-zA-Z][^>]*>")

    /**
     * True when a string still carries something tag-shaped, which means it
     * has not been through `plain` or `parseHtml` yet. A bare `<` or `>` in
     * prose is not markup: the sources carry both, escaped and unescaped, and
     * the content build decodes them.
     */
    fun hasMarkup(text: String): Boolean = tag.containsMatchIn(text)

    private val horizontalSpace = Regex("[ \\t\\r\\f\\u000B]+")

    /**
     * Removing a tag leaves a space in its place, which is right between two
     * words and wrong beside punctuation: no one writes "Al-Fatihah ."
     */
    private val spaceBeforePunctuation = Regex(" +([.,;:!?%)\\]])")
    private val spaceAfterOpening = Regex("([\\[(]) +")
    private val spaceBeforeBreak = Regex(" *\\n *")
    private val blankLines = Regex("\\n{3,}")

    private val heading = Regex("h[1-4]")

    fun isArabic(codepoint: Int): Boolean = arabicRanges.any { codepoint in it }

    /** Script-splits plain text; the caller supplies the styling. */
    fun runs(text: String): List<TextRun> = splitRuns(text)

    /**
     * Parses the sanitized HTML subset the tafsir build keeps: paragraphs,
     * headings, line breaks, bold, italic, and Arabic quotes. Links and any
     * unknown tags keep their text and lose their markup.
     */
    fun parseHtml(html: String): List<TextBlock> {
        val blocks = mutableListOf<TextBlock>()
        val runs = mutableListOf<TextRun>()
        val text = StringBuilder()
        var kind = TextBlockKind.PARAGRAPH
        var started = false
        var bold = 0
        var italic = 0
        var quote = false

        fun append(raw: String) {
            for (character in raw) {
                when (character) {
                    ' ', '\t', '\r', '\n' -> {
                        if (!started) continue
                        if (text.isEmpty() || (text.last() != ' ' && text.last() != '\n')) text.append(' ')
                    }
                    else -> {
                        started = true
                        text.append(character)
                    }
                }
            }
        }

        fun flushText() {
            if (text.isEmpty()) return
            runs += splitRuns(text.toString(), bold > 0, italic > 0, quote)
            text.clear()
        }

        fun flushBlock() {
            flushText()
            val trimmed = trimRuns(runs)
            if (trimmed.isNotEmpty()) blocks += TextBlock(kind, trimmed)
            runs.clear()
            kind = TextBlockKind.PARAGRAPH
            started = false
        }

        var index = 0
        while (index < html.length) {
            val open = html.indexOf('<', index)
            if (open < 0) {
                append(html.substring(index))
                break
            }
            if (open > index) append(html.substring(index, open))
            val close = html.indexOf('>', open)
            if (close < 0) {
                append(html.substring(open))
                break
            }
            val tag = html.substring(open + 1, close).trim().lowercase()
            when {
                tag == "p" || tag == "/p" -> flushBlock()
                heading.matches(tag) -> {
                    flushBlock()
                    kind = TextBlockKind.HEADING
                }
                tag.startsWith("/") && heading.matches(tag.removePrefix("/")) -> flushBlock()
                tag == "br" || tag == "br/" -> if (started && text.isNotEmpty() && text.last() != '\n') text.append('\n')
                tag == "strong" || tag == "b" -> {
                    flushText()
                    bold++
                }
                tag == "/strong" || tag == "/b" -> {
                    flushText()
                    if (bold > 0) bold--
                }
                tag == "em" || tag == "i" -> {
                    flushText()
                    italic++
                }
                tag == "/em" || tag == "/i" -> {
                    flushText()
                    if (italic > 0) italic--
                }
                tag == "q" -> {
                    flushText()
                    quote = true
                }
                tag == "/q" -> {
                    flushText()
                    quote = false
                }
                else -> Unit
            }
            index = close + 1
        }
        flushBlock()
        return blocks
    }

    /**
     * Splits a translation sentence into text and its [n] footnote markers, so
     * the reader sees a superscript number instead of brackets.
     */
    fun footnotes(text: String): List<TextRun> {
        val runs = mutableListOf<TextRun>()
        var last = 0
        for (match in marker.findAll(text)) {
            if (match.range.first > last) runs += splitRuns(text.substring(last, match.range.first))
            runs += TextRun(text = match.groupValues[1], marker = match.groupValues[1].toInt())
            last = match.range.last + 1
        }
        if (last < text.length) runs += splitRuns(text.substring(last))
        return coalesce(runs)
    }

    /** Marks the Arabic passages As-Sa'di wraps in braces, braces included. */
    fun quotes(text: String): List<TextRun> {
        val runs = mutableListOf<TextRun>()
        var index = 0
        while (index < text.length) {
            val open = text.indexOf('{', index)
            if (open < 0) {
                runs += splitRuns(text.substring(index))
                break
            }
            if (open > index) runs += splitRuns(text.substring(index, open))
            val close = text.indexOf('}', open + 1)
            val end = if (close < 0) text.length else close + 1
            runs += splitRuns(text.substring(open, end), quote = true)
            index = end
        }
        return coalesce(runs)
    }

    /**
     * The readable form of a marked-up document read from its first word to
     * its last: every heading and every paragraph of the source becomes its
     * own paragraph here, one blank line between them, so a heading stands
     * where the source put it instead of running into the sentence it heads.
     * [plain] welds the blocks into one run of prose, which is right for an
     * excerpt cut from the middle of a text and wrong for the surah's
     * introduction, whose headings (Name, Period of Revelation, Theme) are
     * half of what it says.
     */
    fun paragraphs(html: String): String =
        marker.replace(
            parseHtml(html).joinToString("\n\n") { block ->
                block.runs.joinToString("") { run -> run.text }
            },
            "",
        )

    /**
     * The text as readable prose, for a place that cannot draw runs: the
     * tafsir's tags go, the translation's [n] footnote markers go, runs of
     * spaces become one, and a paragraph break stays a break. Sharing and a
     * search excerpt read through here, so no surface can show the raw
     * stored form by accident. A whole document whose headings matter reads
     * through [paragraphs] instead, which keeps the blocks apart.
     *
     * A tag is only what looks like one: a `<` that introduces a letter or a
     * slash. A real less-than sign in prose survives, and the words on either
     * side of a removed tag keep a space between them instead of being welded
     * together. The brackets of a translation's footnotes are removed, but the
     * square brackets of a word list's implied words (`disbelieve[d]`) are
     * part of the meaning and stay.
     */
    fun plain(text: String): String =
        tag.replace(text, " ")
            .let { marker.replace(it, "") }
            .replace(horizontalSpace, " ")
            .replace(spaceBeforePunctuation, "$1")
            .replace(spaceAfterOpening, "$1")
            .replace(spaceBeforeBreak, "\n")
            .replace(blankLines, "\n\n")
            .trim()

    private fun splitRuns(
        text: String,
        bold: Boolean = false,
        italic: Boolean = false,
        quote: Boolean = false,
    ): List<TextRun> {
        val runs = mutableListOf<TextRun>()
        val builder = StringBuilder()
        var arabic = false

        fun flush() {
            if (builder.isEmpty()) return
            runs += TextRun(builder.toString(), bold, italic, arabic, quote)
            builder.clear()
        }

        var index = 0
        while (index < text.length) {
            val codepoint = text.codePointAt(index)
            val isArabic = quote || isArabic(codepoint)
            if (builder.isNotEmpty() && isArabic != arabic) flush()
            arabic = isArabic
            builder.append(display(codepoint))
            index += Character.charCount(codepoint)
        }
        flush()
        return runs
    }

    /**
     * The display form of one codepoint. Arabic presentation forms are pre-shaped
     * glyphs, not letters; the tafsir source carries a few of them. NFKC unfolds
     * them back to real letters that shape correctly in any Arabic font. Two
     * exceptions are deliberate: the Prophet's ligature ﷺ is kept as the one
     * glyph it is, and the space that isolated forms decompose to is dropped when
     * it would sit before a combining mark.
     */
    private fun display(codepoint: Int): String {
        if (codepoint == 0xFDFA) return String(Character.toChars(codepoint))
        val single = String(Character.toChars(codepoint))
        val normalized = Normalizer.normalize(single, Normalizer.Form.NFKC)
        return if (normalized.length > 1 && normalized[0] == ' ' &&
            Character.getType(normalized.codePointAt(1)) == Character.NON_SPACING_MARK.toInt()
        ) {
            normalized.substring(1)
        } else {
            normalized
        }
    }

    private fun trimRuns(runs: List<TextRun>): List<TextRun> {
        if (runs.isEmpty()) return runs
        val out = runs.toMutableList()
        while (out.isNotEmpty()) {
            val first = out.first()
            val trimmed = first.text.trimStart()
            if (trimmed.isEmpty()) {
                out.removeAt(0)
                continue
            }
            out[0] = first.copy(text = trimmed)
            break
        }
        while (out.isNotEmpty()) {
            val last = out.last()
            val trimmed = last.text.trimEnd()
            if (trimmed.isEmpty()) {
                out.removeAt(out.size - 1)
                continue
            }
            out[out.size - 1] = last.copy(text = trimmed)
            break
        }
        return coalesce(out)
    }

    private fun coalesce(runs: List<TextRun>): List<TextRun> {
        val out = mutableListOf<TextRun>()
        for (run in runs) {
            val last = out.lastOrNull()
            val mergeable = last != null &&
                last.bold == run.bold &&
                last.italic == run.italic &&
                last.arabic == run.arabic &&
                last.quote == run.quote &&
                last.marker == run.marker
            if (mergeable) {
                out[out.size - 1] = last.copy(text = last.text + run.text)
            } else {
                out += run
            }
        }
        return out
    }
}
