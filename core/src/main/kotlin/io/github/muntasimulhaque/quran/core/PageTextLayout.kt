package io.github.muntasimulhaque.quran.core

/**
 * One word of a page as the layout engine sees it: the word's own text, the
 * ayah it belongs to, and whether it is the ayah's number rather than a word
 * of the Book.
 */
data class LayoutWord(
    val id: Int,
    val ayah: Int,
    val position: Int,
    val marker: Boolean,
    val text: String,
)

/**
 * One line of the layout table, as the engine sees it: the print's own line,
 * with the words the print set on it.
 */
data class LayoutLine(
    val line: Int,
    /** One of `ayah`, `basmallah`, or `surah_name`. */
    val type: String,
    val centered: Boolean,
    val firstWordId: Int,
    val lastWordId: Int,
    val surah: Int,
)

/** What a visual line of the page is. */
enum class SlotKind {
    /** A surah's name in its band: the drawer paints it, the engine places it. */
    SurahName,

    /** The basmallah line: the drawer paints its four words. */
    Basmallah,

    /** Words of the Book, laid out by the engine. */
    Text,
}

/**
 * One word placed on a visual line. [start] is the distance from the line's
 * right edge to this word's own right edge, in ems of the page's text: the
 * Book is read right to left, so the line's first word stands at its right
 * edge and every word after it lays out to the left.
 */
data class PlacedWord(
    val word: LayoutWord,
    val start: Float,
    val width: Float,
)

/**
 * One visual line the page is drawn with, and the canonical line it stands
 * for. A page usually has fifteen: the engine only adds a line when the
 * words the print set on one no longer fit it at the reader's own text size.
 */
data class LayoutSlot(
    val kind: SlotKind,
    /** The canonical line of the layout table this slot belongs to. */
    val line: Int,
    val surah: Int,
    val centered: Boolean,
    val words: List<PlacedWord>,
)

/** A whole page, laid out: its slots in reading order. */
data class PageLayout(val slots: List<LayoutSlot>) {
    val lines: Int get() = slots.size
}

/**
 * Turns the print's own lines into the page's visual lines at the reader's
 * text size.
 *
 * The engine is pure arithmetic over measurements: it holds no font, no
 * pixel, and no platform, so the same words and the same measure always lay
 * out the same way, on every device and every version of it. That is the
 * whole point of writing it here rather than handing the page to the
 * platform's text layout, which breaks Arabic lines where it likes and
 * differently on every device.
 *
 * The rule it follows is the print's own: the layout table says which words
 * stand on which of the page's fifteen lines, and those lines are kept
 * exactly as long as the words still fit them at the reader's size. A line
 * whose words fit is drawn justified, as the print justifies it, or centered
 * where the print centers it. When the words no longer fit, the section they
 * belong to is re-set: its words flow onto as many lines as they now need,
 * the way any well-set page flows, and the section's last line is centered
 * if the print centered it. A page's ayahs, its fifteen slots, and its
 * furniture keep their place either way: what moves is only where a line
 * ends, and only when the reader has asked for text that no longer allows
 * it to stand still.
 */
object PageTextLayout {

    /**
     * How full of the measure a justified line must be to be left as the
     * print set it. Below this the section is re-set: a justified line with
     * half the measure as word gaps reads as a row of loose words, not as a
     * line of the Book.
     */
    const val KEEP_FILL = 0.75f

    /**
     * A hair of slack at the measure's edge, so a line that fits by the
     * width of a rounding error is not thrown into re-setting its whole
     * section: a line that fits by this much justifies a hair tight, which
     * no eye can see, and keeps the print's own line.
     */
    const val EPSILON = 0.005f

    /**
     * Lays out one page.
     *
     * @param lines the layout table's lines for the page, in page order.
     * @param words every word of the page by id, so a line's own words are
     *   found by the ids the table names.
     * @param capacityEm the line's measure in ems of the page's text at the
     *   reader's size: the words of one line, their word gaps included, must
     *   fit inside it.
     * @param measure the width of any string, in the same ems.
     * @param spaceEm the width of the word gap the text carries naturally.
     * @param markerEm the room an ayah's number takes on a line, in the same
     *   ems: the number is drawn in its roundel at display time, so the
     *   engine only reserves its width.
     */
    fun layout(
        lines: List<LayoutLine>,
        words: Map<Int, LayoutWord>,
        capacityEm: Float,
        measure: (String) -> Float,
        spaceEm: Float,
        markerEm: Float,
    ): PageLayout {
        val slots = ArrayList<LayoutSlot>(lines.size + 2)
        // A section is a run of ayah lines between the page's own furniture:
        // a surah's name or a basmallah starts a fresh run, because the print
        // never runs a surah's opening words onto the line before it.
        var section = ArrayList<LayoutLine>()
        fun closeSection() {
            if (section.isNotEmpty()) {
                slots += layoutSection(section, words, capacityEm, measure, spaceEm, markerEm)
                section = ArrayList()
            }
        }
        for (line in lines) {
            when (line.type) {
                "ayah" -> section += line
                "basmallah" -> {
                    closeSection()
                    slots += LayoutSlot(SlotKind.Basmallah, line.line, line.surah, true, emptyList())
                }
                else -> {
                    closeSection()
                    slots += LayoutSlot(SlotKind.SurahName, line.line, line.surah, true, emptyList())
                }
            }
        }
        closeSection()
        return PageLayout(slots)
    }

    /** One section: the print's lines kept, or re-set when they no longer fit. */
    private fun layoutSection(
        section: List<LayoutLine>,
        words: Map<Int, LayoutWord>,
        capacityEm: Float,
        measure: (String) -> Float,
        spaceEm: Float,
        markerEm: Float,
    ): List<LayoutSlot> {
        // The section's words, flattened in reading order, with each word's
        // natural width beside it.
        val atoms = ArrayList<LayoutWord>()
        val widths = ArrayList<Float>()
        for (line in section) {
            for (id in line.firstWordId..line.lastWordId) {
                val word = words[id] ?: continue
                atoms += word
                widths += widthOf(word, measure, markerEm)
            }
        }
        if (atoms.isEmpty()) return emptyList()

        // Where each of the print's lines begins and ends in the flattened
        // words, so the print's own lines are kept exactly when they fit.
        val ranges = ArrayList<Triple<LayoutLine, Int, Int>>(section.size)
        var index = 0
        for (line in section) {
            var count = 0
            for (id in line.firstWordId..line.lastWordId) if (words.containsKey(id)) count++
            ranges += Triple(line, index, index + count)
            index += count
        }

        if (keepsThePrintsLines(ranges, widths, capacityEm, spaceEm)) {
            return ranges.map { (line, from, until) ->
                placeLine(
                    from = from,
                    until = until,
                    centered = line.centered || until - from == 1,
                    atoms = atoms,
                    widths = widths,
                    capacityEm = capacityEm,
                    spaceEm = spaceEm,
                    kind = SlotKind.Text,
                    line = line.line,
                    surah = line.surah,
                )
            }
        }
        return reSetSection(section, atoms, widths, capacityEm, spaceEm)
    }

    /**
     * Whether every line the print set still stands: each fits the measure,
     * and each line the print justifies is still full enough of it that
     * justifying it does not open it into word gaps wider than its words.
     */
    private fun keepsThePrintsLines(
        ranges: List<Triple<LayoutLine, Int, Int>>,
        widths: List<Float>,
        capacityEm: Float,
        spaceEm: Float,
    ): Boolean {
        for ((line, from, until) in ranges) {
            val count = until - from
            if (count == 0) continue
            var natural = spaceEm * (count - 1)
            for (i in from until until) natural += widths[i]
            if (natural > capacityEm + EPSILON) return false
            if (!line.centered && natural < capacityEm * KEEP_FILL) return false
        }
        return true
    }

    /**
     * The section re-set: its words flow onto as many lines as they need at
     * the reader's size. Every line is justified; the last is centered where
     * the print centered its own last line, and a line left with a single
     * word is centered, because one word justified to a full measure is a
     * word marooned at the right of an empty line.
     */
    private fun reSetSection(
        section: List<LayoutLine>,
        atoms: List<LayoutWord>,
        widths: List<Float>,
        capacityEm: Float,
        spaceEm: Float,
    ): List<LayoutSlot> {
        val lastLine = section.last()
        val firstLine = section.first().line
        val slots = ArrayList<LayoutSlot>()
        var from = 0
        while (from < atoms.size) {
            var used = widths[from]
            var until = from + 1
            while (until < atoms.size) {
                val next = used + spaceEm + widths[until]
                if (next > capacityEm + EPSILON) break
                used = next
                until++
            }
            slots += placeLine(
                from = from,
                until = until,
                centered = (until == atoms.size && lastLine.centered) || until - from == 1,
                atoms = atoms,
                widths = widths,
                capacityEm = capacityEm,
                spaceEm = spaceEm,
                kind = SlotKind.Text,
                line = firstLine,
                surah = 0,
            )
            from = until
        }
        return slots
    }

    /**
     * Places one line's words across the measure, from its right edge. A
     * justified line's slack is shared among its word gaps, the way the print
     * shares it; a centered line keeps its natural gaps and stands in the
     * middle of what is left.
     */
    private fun placeLine(
        from: Int,
        until: Int,
        centered: Boolean,
        atoms: List<LayoutWord>,
        widths: List<Float>,
        capacityEm: Float,
        spaceEm: Float,
        kind: SlotKind,
        line: Int,
        surah: Int,
    ): LayoutSlot {
        val count = until - from
        var natural = 0f
        for (i in from until until) natural += widths[i]
        val withGaps = natural + spaceEm * (count - 1)
        val gap = when {
            count <= 1 -> spaceEm
            centered -> spaceEm
            else -> spaceEm + (capacityEm - withGaps) / (count - 1)
        }
        var cursor = if (centered) (capacityEm - withGaps) / 2f else 0f
        val placed = ArrayList<PlacedWord>(count)
        for (i in from until until) {
            placed += PlacedWord(word = atoms[i], start = cursor, width = widths[i])
            cursor += widths[i] + gap
        }
        return LayoutSlot(
            kind = kind,
            line = line,
            surah = surah,
            centered = centered,
            words = placed,
        )
    }

    private fun widthOf(word: LayoutWord, measure: (String) -> Float, markerEm: Float): Float =
        if (word.marker) markerEm else measure(word.text)
}
