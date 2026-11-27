package io.github.muntasimulhaque.quran.tools

import io.github.muntasimulhaque.quran.core.ReferenceLink
import io.github.muntasimulhaque.quran.core.RichText

/**
 * The surah introductions, as the reading draws them.
 *
 * The source writes verse references in every shape its editors used: `v. 9`,
 * `vv. 46-47`, `verse 35`, a bare `1-10`. The reader meets one vocabulary, so
 * the build expands the two abbreviations to the words a general reader
 * knows: `v.` to `verse` and `vv.` to `verses`. The letters of the source are
 * not otherwise touched; the raw export stays as QUL shipped it, and the
 * manifest records this presentation (owner decision, 23.5).
 *
 * A link is kept only when [ReferenceLink] can open it. The source writes
 * most of its targets unquoted, which the old normalizer dropped, and four as
 * quran.com footnote URLs, which no reading surface can open. The first now
 * arrive as doors; the second keep their words and lose their target, the
 * rule the reading already keeps (owner decision, 19.7).
 */
internal object SurahInfo {

    /**
     * One source defect the build corrects, with its evidence. A correction
     * whose exact sentence the source no longer holds fails the build rather
     * than silently doing nothing, the rule `saheehCorrections` keeps.
     */
    private data class Correction(
        val surah: Int,
        val from: String,
        val to: String,
        val reason: String,
    )

    /**
     * Surah 17's introduction names the Night Journey and v. 1, but its href
     * is the printed edition's older Bani Israil target, v. 4. The sentence
     * was updated and the old link was left behind, so the door opened a
     * verse the words do not name. The correction says both halves, so a
     * future source that fixes either half stops the build for review instead
     * of being corrected twice (owner decision, 23.5).
     */
    private val corrections = listOf(
        Correction(
            surah = 17,
            from = "<a href=\"/17/4\">v. 1</a>",
            to = "<a href=\"/17/1\">v. 1</a>",
            reason = "The sentence names the Night Journey and v. 1; the href is the " +
                "printed edition's old Bani Israil target, v. 4.",
        ),
    )

    /**
     * A range the printed introduction names past its surah's last verse.
     * The link opens the first verse, which is real, so the reading is not
     * sent anywhere false; the note keeps the defect visible instead of
     * hiding it (owner decision, 23.5).
     */
    private data class KnownOverrun(val href: String, val reason: String)

    private val knownRangeOverruns = mapOf(
        13 to KnownOverrun(
            href = "/13/34-48",
            reason = "Ar-Ra'd ends at 43, but the printed introduction names 34-48; " +
                "the link opens the first verse, 34.",
        ),
    )

    /** A tag, so the abbreviation rule never edits an href or another attribute. */
    private val tag = Regex("</?[a-zA-Z][^>]*>")

    /**
     * `v.` or `vv.` and the space after it, before a verse number. The
     * lookahead leaves a `v.` that does not introduce a number, such as the
     * abbreviation defined in a sentence, alone.
     */
    private val abbreviatedVerse = Regex("""\b(vv?)\.\s*(?=\d)""")

    /**
     * A `vv.` that introduces a linked list of verses: `vv. <a>41</a>, ...`.
     * The number is inside the anchor, so the abbreviation is not directly
     * before a digit and [abbreviatedVerse] cannot see it.
     */
    private val abbreviatedLinkedVerse = Regex("""\b(vv?)\.\s*(?=<a\b[^>]*>\s*\d)""")

    /**
     * A `vv.` whose anchor already states the same abbreviation:
     * `vv. <a>vv. 75-76</a>`. The inner one is expanded first, then the outer
     * is dropped rather than leaving `vv. verses 75-76` on the page.
     */
    private val doubledAbbreviation = Regex("""\b(vv?)\.\s*(?=<a\b[^>]*>\s*verses?\s)""")

    /**
     * Any abbreviation the expansion should have removed. The audit reads the
     * drawn text, so `vv. 41` and `vv. verses 75-76` both name a leftover.
     */
    private val survivingAbbreviation = Regex("""\b(vv?)\.\s*(?=\d|verses?\s+\d)""")

    /** An anchor and the words it wraps. */
    private val anchor = Regex("""<a\b([^>]*)>([\s\S]*?)</a>""")

    /** The target of an anchor in the normalized HTML, which is always quoted. */
    private val anchorHref = Regex("""href\s*=\s*"([^"]*)"""")

    /** The surah, first verse, and optional range end a target names. */
    private val linkTarget = Regex("""^/?(\d+)(?:/(\d+)(?:-(\d+))?)?$""")

    /**
     * A label that states its verse at the start: `verse 9`, `verses 46-47`,
     * `1-10`, `1 - 10`. A label whose first word is not a number, such as
     * `first verse` or `Surah Al-Hashr: 2-7`, is left to the reader's eye.
     */
    private val verseLabel = Regex(
        """(?:verses?\s+)?(\d+)(?:\s*-\s*(\d+))?""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * The introduction the database stores for one surah: the source's HTML,
     * its links normalized, the one correction applied, and its verse
     * abbreviations expanded (owner decision, 23.5).
     */
    fun drawn(surah: Int, raw: String): String {
        var html = ContentHtml.sanitize(raw)
        for (correction in corrections.filter { it.surah == surah }) {
            val found = Regex(Regex.escape(correction.from)).findAll(html).count()
            require(found == 1) {
                "surah $surah: expected one ${correction.from} in the source, found $found; " +
                    correction.reason
            }
            html = html.replace(correction.from, correction.to)
        }
        return expandVerses(html)
    }

    /** Expands `v.` and `vv.` in the text between tags, never inside one. */
    private fun expandVerses(html: String): String {
        val expanded = expandText(html)
        val linked = abbreviatedLinkedVerse.replace(expanded) { match ->
            if (match.groupValues[1] == "vv") "verses " else "verse "
        }
        return doubledAbbreviation.replace(linked) { "" }
    }

    /** The direct form, applied to one text node at a time. */
    private fun expandText(html: String): String {
        val out = StringBuilder(html.length)
        var last = 0
        for (match in tag.findAll(html)) {
            out.append(expand(html.substring(last, match.range.first)))
            out.append(match.value)
            last = match.range.last + 1
        }
        out.append(expand(html.substring(last)))
        return out.toString()
    }

    private fun expand(text: String): String =
        abbreviatedVerse.replace(text) { match ->
            if (match.groupValues[1] == "vv") "verses " else "verse "
        }

    /**
     * The rules a built introduction must keep: every anchor has a target the
     * reading can open, the target exists, and a label that states its verse
     * states the verse the target opens. The printed range that overruns its
     * surah is a note, because the source says it and the first verse is
     * true (owner decision, 23.5).
     */
    fun audit(surah: Int, html: String, verses: Map<Int, Int>): SurahInfoAudit {
        val problems = mutableListOf<String>()
        val notes = mutableListOf<String>()
        for (match in anchor.findAll(html)) {
            val href = anchorHref.find(match.groupValues[1])?.groupValues?.get(1)
            if (href == null) {
                problems += "surah $surah: a link has no target: " +
                    RichText.plain(match.value).take(60)
                continue
            }
            val parsed = ReferenceLink.parse(href)
            if (parsed == null) {
                problems += "surah $surah: a link the reading cannot open: $href"
                continue
            }
            val target = linkTarget.matchEntire(href)
            if (target == null) {
                problems += "surah $surah: a link shape the reading does not know: $href"
                continue
            }
            val targetSurah = target.groupValues[1].toIntOrNull()
            if (targetSurah == null) {
                problems += "surah $surah: a link with no surah: $href"
                continue
            }
            val first = target.groupValues[2].toIntOrNull()
            val last = target.groupValues[3].toIntOrNull() ?: first
            if (targetSurah != parsed.surah || first != parsed.ayah) {
                problems += "surah $surah: the target parser and the reading disagree on $href"
            }
            val count = verses[targetSurah]
            if (count == null) {
                problems += "surah $surah: $href names surah $targetSurah, which is not in the Book"
                continue
            }
            if (first != null && first !in 1..count) {
                problems += "surah $surah: $href names verse $first, but surah $targetSurah has $count"
            }
            if (last != null && last > count) {
                val known = knownRangeOverruns[targetSurah]
                if (known != null && known.href == href) {
                    notes += "surah $surah: known source defect, $href: ${known.reason}"
                } else {
                    problems += "surah $surah: $href names verse $last, " +
                        "but surah $targetSurah ends at $count"
                }
            }
            val label = RichText.plain(match.groupValues[2]).trim()
            if (label.isEmpty()) {
                problems += "surah $surah: a link carries no words: $href"
                continue
            }
            val stated = verseLabel.matchAt(label, 0) ?: continue
            val statedFirst = stated.groupValues[1].toIntOrNull()
            val statedLast = stated.groupValues[2].toIntOrNull()
            if (statedFirst != null && first != null && statedFirst != first) {
                problems += "surah $surah: the link says \"$label\" but opens $targetSurah:$first"
            }
            if (statedLast != null && last != null && statedLast != last) {
                problems += "surah $surah: the link says \"$label\" but its range ends at $last"
            }
        }
        val leftover = survivingAbbreviation.find(RichText.paragraphs(html))
        if (leftover != null) {
            problems += "surah $surah: an abbreviated verse label survived: ${leftover.value.trim()}"
        }
        return SurahInfoAudit(problems = problems, notes = notes)
    }
}

/**
 * What one introduction's audit found. [problems] fail the content gate;
 * [notes] are source defects the gate reports without failing, so a printed
 * error stays visible and a reviewer can decide whether to correct it.
 */
internal data class SurahInfoAudit(
    val problems: List<String>,
    val notes: List<String>,
)
