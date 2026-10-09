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
 *
 * [text] is what the canvas draws. It is the word's own text, elongated with
 * as many tatweels as this line's slack asked of it: the print fills its line
 * by stretching its letters, and a face cannot stretch a letter without
 * distorting it, so the stroke the calligrapher lengthens is written out. The
 * Book's own text is never touched by this: [LayoutWord.text] is what stands
 * in the content database, and this is what is drawn at it today.
 */
data class PlacedWord(
    val word: LayoutWord,
    val text: String,
    val start: Float,
    val width: Float,
)

/**
 * One visual line the page is drawn with, and the canonical line it stands
 * for. A page of the Book is fifteen of these, and the engine never adds a
 * line to one: it draws the print's own lines, and only where a reader has
 * asked for type too large for a line to hold does that line flow onto a
 * second, the way any well-set page flows.
 */
data class LayoutSlot(
    val kind: SlotKind,
    /** The canonical line of the layout table this slot belongs to. */
    val line: Int,
    val surah: Int,
    val centered: Boolean,
    val words: List<PlacedWord>,
)

/**
 * A whole page, laid out: its slots in reading order, and the type it was
 * laid out at as a share of the page's own measure.
 */
data class PageLayout(val slots: List<LayoutSlot>, val scale: Float) {
    val lines: Int get() = slots.size
}

/**
 * Turns the print's own lines into the page's visual lines, at the page's own
 * type, filled the way the print fills them.
 *
 * The engine is pure arithmetic over measurements: it holds no font, no
 * pixel, and no platform, so the same words and the same measure always lay
 * out the same way, on every device and every version of it.
 *
 * Three rules, in order of what they protect:
 *
 * 1. **The print's lines are the page's lines.** The layout table says which
 *    words stand on which of the page's fifteen lines, and that is what is
 *    drawn. The page used to re-set a section whose lines were judged too
 *    empty, which packed the Book's fifteen lines into twelve or thirteen on
 *    nine pages in ten and lost the arrangement the reader came for. Nothing
 *    is re-set now. A line only flows onto a second when the reader has asked
 *    for a type too large for it, and every other line keeps its own words.
 *
 * 2. **The type is the page's own.** It is the largest at which that page's
 *    fullest line still stands inside the measure, because a line past the
 *    measure is a line whose words have been re-set. A dense page is
 *    therefore set smaller than a sparse one, which is what the print does:
 *    the hand shrinks to fit the words it was given.
 *
 * 3. **A line is filled as the print fills it.** The slack of a justified
 *    line goes into its letters, as tatweel, and only what the letters cannot
 *    carry is left to the word gaps. A centered line keeps its natural gaps
 *    and stands in the middle of what is left.
 */
object PageTextLayout {

    /**
     * A hair of slack at the measure's edge, so a line that fits by the width
     * of a rounding error is not thrown into flowing onto a second.
     */
    const val EPSILON = 0.005f

    /**
     * The share of a page's own fullest line the page is drawn at, and the
     * width of the air left under the fullest line's fit. One percent is
     * enough for the rounding in a rendered pixel and the engine's own hair,
     * and small enough that the print's own line is the line a reader sees.
     */
    const val FIT_MARGIN = 0.99f

    /**
     * Lays out one page.
     *
     * @param lines the layout table's lines for the page, in page order.
     * @param words every word of the page by id, so a line's own words are
     *   found by the ids the table names.
     * @param measure the width of any string, in ems of the page's text.
     * @param spaceEm the width of the word gap the text carries naturally.
     * @param roundelEm an ayah number's roundel, in ems of the *measure*, as
     *   wide as the drawer draws it ([PageFrame.roundelWidthEm]).
     * @param step the reader's own text size as a share of the page's type.
     */
    fun layout(
        lines: List<LayoutLine>,
        words: Map<Int, LayoutWord>,
        measure: (String) -> Float,
        spaceEm: Float,
        roundelEm: (Int) -> Float,
        step: Float = 1f,
    ): PageLayout {
        val fit = pageScale(lines, words, measure, spaceEm, roundelEm)
        val scale = (fit * step * FIT_MARGIN).coerceAtLeast(Float.MIN_VALUE)
        val capacity = PageFrame.EM_PER_LINE / scale
        val slots = ArrayList<LayoutSlot>(lines.size)
        for (line in lines) {
            when (line.type) {
                "ayah" -> {
                    val atoms = atomsOf(line, words)
                    slots += flow(line, atoms, capacity, measure, spaceEm, roundelEm, scale)
                }
                "basmallah" -> slots += LayoutSlot(
                    kind = SlotKind.Basmallah,
                    line = line.line,
                    surah = line.surah,
                    centered = true,
                    words = emptyList(),
                )
                else -> slots += LayoutSlot(
                    kind = SlotKind.SurahName,
                    line = line.line,
                    surah = line.surah,
                    centered = true,
                    words = emptyList(),
                )
            }
        }
        return PageLayout(slots, scale)
    }

    /**
     * The largest type, as a share of the measure, at which every one of
     * [lines] still stands its words inside the measure. A line's words are
     * its own in ems of the type and its roundels are the measure's own, so
     * the fit is a straight division per line and the page is the smallest of
     * them: the fullest line of the Book is what the Book is set at.
     */
    fun pageScale(
        lines: List<LayoutLine>,
        words: Map<Int, LayoutWord>,
        measure: (String) -> Float,
        spaceEm: Float,
        roundelEm: (Int) -> Float,
    ): Float {
        var smallest = Float.MAX_VALUE
        for (line in lines) {
            if (line.type != "ayah") continue
            val atoms = atomsOf(line, words)
            if (atoms.isEmpty()) continue
            var text = 0f
            var roundels = 0f
            for (atom in atoms) {
                if (atom.marker) roundels += roundelEm(atom.ayah) else text += measure(atom.text)
            }
            val denominator = text + spaceEm * (atoms.size - 1)
            if (denominator <= 0f) continue
            val fit = (PageFrame.EM_PER_LINE - roundels) / denominator
            if (fit < smallest) smallest = fit
        }
        return if (smallest == Float.MAX_VALUE) 1f else smallest
    }

    /** The words of one print line, in reading order, with every id it names. */
    private fun atomsOf(line: LayoutLine, words: Map<Int, LayoutWord>): List<LayoutWord> {
        val out = ArrayList<LayoutWord>()
        for (id in line.firstWordId..line.lastWordId) {
            val word = words[id] ?: continue
            out += word
        }
        return out
    }

    /**
     * One print line: drawn whole, or flowed onto as many lines as a reader's
     * larger type needs. The print's own line is never re-set for any other
     * reason.
     */
    private fun flow(
        line: LayoutLine,
        atoms: List<LayoutWord>,
        capacity: Float,
        measure: (String) -> Float,
        spaceEm: Float,
        roundelEm: (Int) -> Float,
        scale: Float,
    ): List<LayoutSlot> {
        if (atoms.isEmpty()) return emptyList()
        if (natural(atoms, measure, spaceEm, roundelEm, scale) <= capacity + EPSILON) {
            return listOf(
                slot(
                    line = line.line,
                    surah = line.surah,
                    atoms = atoms,
                    centered = line.centered || atoms.size == 1,
                    capacity = capacity,
                    measure = measure,
                    spaceEm = spaceEm,
                    roundelEm = roundelEm,
                    scale = scale,
                ),
            )
        }
        // The reader asked for type this line no longer holds: its words flow
        // onto the lines they need, the last of them centered where the print
        // centered its own.
        val slots = ArrayList<LayoutSlot>()
        var from = 0
        while (from < atoms.size) {
            var until = from + 1
            naturalRunning(atoms, from, until, measure, spaceEm, roundelEm, scale)
            while (until < atoms.size &&
                naturalRunning(atoms, from, until + 1, measure, spaceEm, roundelEm, scale) <= capacity + EPSILON
            ) {
                until++
            }
            val isLast = until >= atoms.size
            slots += slot(
                line = line.line,
                surah = line.surah,
                atoms = atoms.subList(from, until),
                centered = (isLast && line.centered) || until - from == 1,
                capacity = capacity,
                measure = measure,
                spaceEm = spaceEm,
                roundelEm = roundelEm,
                scale = scale,
            )
            from = until
        }
        return slots
    }

    private fun natural(
        atoms: List<LayoutWord>,
        measure: (String) -> Float,
        spaceEm: Float,
        roundelEm: (Int) -> Float,
        scale: Float,
    ): Float = naturalRunning(atoms, 0, atoms.size, measure, spaceEm, roundelEm, scale)

    private fun naturalRunning(
        atoms: List<LayoutWord>,
        from: Int,
        until: Int,
        measure: (String) -> Float,
        spaceEm: Float,
        roundelEm: (Int) -> Float,
        scale: Float,
    ): Float {
        var total = 0f
        for (i in from until until) {
            val atom = atoms[i]
            total += if (atom.marker) roundelEm(atom.ayah) / scale else measure(atom.text)
        }
        return total + spaceEm * (until - from - 1).coerceAtLeast(0)
    }

    private fun slot(
        line: Int,
        surah: Int,
        atoms: List<LayoutWord>,
        centered: Boolean,
        capacity: Float,
        measure: (String) -> Float,
        spaceEm: Float,
        roundelEm: (Int) -> Float,
        scale: Float,
    ): LayoutSlot {
        val texts = ArrayList<String>(atoms.size)
        val widths = ArrayList<Float>(atoms.size)
        for (atom in atoms) {
            texts += atom.text
            widths += if (atom.marker) roundelEm(atom.ayah) / scale else measure(atom.text)
        }
        var natural = widths.sum() + spaceEm * (atoms.size - 1).coerceAtLeast(0)
        if (!centered && atoms.size > 1) {
            val slack = capacity - natural
            if (slack > EPSILON) {
                val stretched = stretch(atoms, slack, measure)
                for (i in atoms.indices) {
                    if (atoms[i].marker) continue
                    texts[i] = stretched[i]
                    widths[i] = measure(texts[i])
                }
                natural = widths.sum() + spaceEm * (atoms.size - 1)
            }
        }
        // What the letters could not carry is shared out among the word gaps,
        // which is the air a page set from a face rather than a pen carries. A
        // centered line is the exception: it stands in the middle of what is
        // left, with the gaps it was written with, and the room it does not
        // fill is the room the print leaves it.
        val gaps = (atoms.size - 1).coerceAtLeast(0)
        val gapGrowth = if (centered || gaps == 0) {
            0f
        } else {
            (capacity - natural).coerceAtLeast(0f) / gaps
        }
        val placed = ArrayList<PlacedWord>(atoms.size)
        var cursor = if (centered) ((capacity - natural) / 2f).coerceAtLeast(0f) else 0f
        for (i in atoms.indices) {
            placed += PlacedWord(
                word = atoms[i],
                text = texts[i],
                start = cursor,
                width = widths[i],
            )
            cursor += widths[i] + spaceEm + gapGrowth
        }
        return LayoutSlot(
            kind = SlotKind.Text,
            line = line,
            surah = surah,
            centered = centered,
            words = placed,
        )
    }

    /**
     * The words of a line, each elongated by its own share of the line's
     * slack. The share follows the word's own width, so a line is stretched at
     * one density of stroke rather than at one number of strokes: the wider
     * the word, the more of the line's slack it is asked to carry.
     *
     * Every word of the line is stretched, the last one included: the print
     * fills a line by lengthening its words, not by opening the gaps between
     * them, and a last word left short is a line that stops short of the
     * measure. A roundel is drawn rather than stretched, so it takes no share.
     */
    private fun stretch(
        atoms: List<LayoutWord>,
        slack: Float,
        measure: (String) -> Float,
    ): List<String> {
        val stretchable = ArrayList<Int>()
        var stretchableWidth = 0f
        for (i in atoms.indices) {
            if (atoms[i].marker) continue
            stretchable += i
            stretchableWidth += measure(atoms[i].text)
        }
        val out = ArrayList<String>(atoms.size)
        if (stretchable.isEmpty() || stretchableWidth <= 0f) {
            for (atom in atoms) out += atom.text
            return out
        }
        for (i in atoms.indices) {
            val atom = atoms[i]
            if (i !in stretchable) {
                out += atom.text
                continue
            }
            val width = measure(atom.text)
            out += elongate(atom.text, slack * width / stretchableWidth, measure)
        }
        return out
    }

    /**
     * [word] elongated by [wanted] ems, as far as the tatweel can take it.
     *
     * The stroke goes where the word gains the most from one, and then keeps
     * going there while the word still gains and the line still wants it. A
     * word that cannot carry its share leaves what it could not to the word
     * gaps: a page with a little air reads better than a page with a stroke
     * run through the middle of a word.
     */
    private fun elongate(word: String, wanted: Float, measure: (String) -> Float): String {
        if (wanted <= EPSILON) return word
        val positions = Kashida.positions(word)
        if (positions.isEmpty()) return word
        val base = measure(word)
        var best = 0
        var bestGain = 0f
        for ((index, position) in positions.withIndex()) {
            val gain = measure(Kashida.insert(word, position)) - base
            if (gain > bestGain) {
                bestGain = gain
                best = index
            }
        }
        if (bestGain <= EPSILON) return word
        var out = word
        var count = 0
        while (count < Kashida.MAX_PER_WORD) {
            // The stroke is counted from the word's own width every time, so
            // the comparison is against what the word now is, not against a
            // running total of what it used to be.
            val next = Kashida.insert(word, positions[best], count + 1)
            val gain = measure(next) - base
            if (gain <= EPSILON || gain > wanted + EPSILON) break
            out = next
            count++
        }
        return out
    }
}
