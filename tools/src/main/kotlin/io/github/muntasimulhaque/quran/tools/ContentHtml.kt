package io.github.muntasimulhaque.quran.tools

import io.github.muntasimulhaque.quran.core.ReferenceLink

/**
 * The small HTML subset the content database keeps, normalized for the app.
 *
 * Keeps the structure the reading draws, drops every attribute except a link
 * target, and turns the Arabic quote spans into `q` elements. Letters are
 * never touched; gray spans in Ibn Kathir are parenthetical inline text and
 * keep their content with the span dropped.
 *
 * A link survives only when [ReferenceLink] can open it. The source writes
 * its targets both quoted and bare, and the app's parser reads a quoted
 * target, so every kept link is written quoted here. A target the reading
 * cannot open, an external address or a quran.com footnote, loses the anchor
 * and keeps its words, the rule every surface already keeps: no door that
 * answers nothing (owner decision, 23.5).
 *
 * This is the one normalizer for the tafsir, the translation, and the surah
 * introductions, so the three cannot drift into three HTML vocabularies.
 */
internal object ContentHtml {

    private val decodeEntities = mapOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&#39;" to "'",
        "&nbsp;" to " ",
    )

    /**
     * The target of an anchor, whether the source quoted it with one mark,
     * the other, or wrote it bare. The source that carried 261 bare targets
     * was the reason this reads all three.
     */
    private val href = Regex(
        """(?<![-\w])href\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""",
        RegexOption.IGNORE_CASE,
    )

    /** A kept anchor with no target left, unwrapped to the words it held. */
    private val bareAnchor = Regex("<a>([\\s\\S]*?)</a>")

    fun sanitize(html: String): String {
        var out = html
        val span = Regex("<span([^>]*)>(.*?)</span>", RegexOption.DOT_MATCHES_ALL)
        while (true) {
            val next = span.replace(out) { match ->
                val attributes = match.groupValues[1]
                val content = match.groupValues[2]
                if (attributes.contains("arabic")) "<q>$content</q>" else content
            }
            if (next == out) break
            out = next
        }
        out = out.replace(Regex("<(div|span)[^>]*>"), "")
        out = out.replace("</div>", "").replace("</span>", "")
        val tag = Regex("<(/?)([a-zA-Z0-9]+)([^>]*)>")
        val keep = setOf("p", "br", "h1", "h2", "h3", "h4", "strong", "b", "em", "i", "q", "a")
        out = tag.replace(out) { match ->
            val closing = match.groupValues[1]
            val name = match.groupValues[2].lowercase()
            val attributes = match.groupValues[3]
            if (name !in keep) return@replace ""
            if (name == "a" && closing.isEmpty()) {
                val target = href.find(attributes)
                    ?.groupValues
                    ?.drop(1)
                    ?.firstOrNull { it.isNotBlank() }
                    ?.trim()
                if (target != null && ReferenceLink.parse(target) != null) {
                    "<a href=\"$target\">"
                } else {
                    "<a>"
                }
            } else {
                "<$closing$name>"
            }
        }
        out = bareAnchor.replace(out) { match -> match.groupValues[1] }
        for ((entity, character) in decodeEntities) out = out.replace(entity, character)
        return out
    }
}
