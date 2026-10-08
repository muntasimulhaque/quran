package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The page's own line setting, pinned here because it is the part of the
 * Mushaf a reader meets first and the part a text engine can most easily get
 * quietly wrong: a line wider than its measure, a word lost between two
 * lines, a section that forgets it was centered.
 *
 * The measurements are the shape of the real thing: a line of the Book sums
 * to a median 19.4 of the face's ems against the print's 15.6 measure, so a
 * capacity in the low twenties keeps the print's lines and a capacity in the
 * teens re-sets them, which is exactly the pair of behaviours under test.
 */
class PageTextLayoutTest {

    private val space = 0.22f

    /** Four ems a word, so the arithmetic is readable in the failures. */
    private fun measure(text: String): Float = text.length * 1f

    private fun word(id: Int, ayah: Int, position: Int, marker: Boolean = false): LayoutWord =
        LayoutWord(
            id = id,
            ayah = ayah,
            position = position,
            marker = marker,
            text = if (marker) "" else "x".repeat(4),
        )

    private fun lineOf(
        line: Int,
        type: String = "ayah",
        centered: Boolean = false,
        ids: IntRange = 0..0,
        surah: Int = 0,
    ) = LayoutLine(
        line = line,
        type = type,
        centered = centered,
        firstWordId = ids.first,
        lastWordId = ids.last,
        surah = surah,
    )

    private fun layout(
        lines: List<LayoutLine>,
        words: Map<Int, LayoutWord>,
        capacity: Float,
        marker: Float = 1f,
    ) = PageTextLayout.layout(lines, words, capacity, ::measure, space, marker)

    @Test
    fun thePrintsLinesStandWhileTheyFit() {
        // Two lines of four words each: 16 ems of words, 3 gaps of 0.22, so
        // 16.66 against a measure of 20.
        val words = (1..8).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, ids = 1..4), lineOf(2, ids = 5..8))
        val page = layout(lines, words, capacity = 20f)
        assertEquals("the print's two lines stand", 2, page.lines)
        for (slot in page.slots) {
            assertEquals("four words on the print's line", 4, slot.words.size)
        }
    }

    @Test
    fun aJustifiedLineSharesItsSlackAmongItsGaps() {
        val words = (1..3).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, ids = 1..3))
        val page = layout(lines, words, capacity = 20f)
        val slot = page.slots.single()
        // Natural: 12 ems of words and 0.44 of gaps, so 7.56 of slack shared
        // between two gaps: each gap grows by 3.78.
        val first = slot.words[0]
        val second = slot.words[1]
        assertEquals(0f, first.start, 0.001f)
        assertEquals(4f + 0.22f + 3.78f, second.start, 0.001f)
        // The last word ends exactly at the measure.
        val last = slot.words.last()
        assertEquals(20f, last.start + last.width, 0.001f)
    }

    @Test
    fun aCenteredLineKeepsItsGapsAndStandsInTheMiddle() {
        val words = (1..3).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, centered = true, ids = 1..3))
        val page = layout(lines, words, capacity = 20f)
        val slot = page.slots.single()
        assertTrue("the print centered this line", slot.centered)
        // 12.44 natural against 20: 3.78 of slack splits in two, and the gaps
        // stay the natural 0.22.
        assertEquals(3.78f, slot.words[0].start, 0.001f)
        assertEquals(0.22f, slot.words[1].start - slot.words[0].start - 4f, 0.001f)
    }

    @Test
    fun aLineThatNoLongerFitsReSetsItsSection() {
        // Six words of 4 ems on one print line: 24 against a measure of 10,
        // so the section re-sets into lines that fit.
        val words = (1..6).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, ids = 1..6))
        val page = layout(lines, words, capacity = 10f)
        assertEquals("two words to a line at this size", 3, page.lines)
        for (slot in page.slots) {
            val last = slot.words.last()
            assertTrue(
                "a re-set line never passes the measure: ${last.start + last.width}",
                last.start + last.width <= 10f + 0.001f,
            )
        }
    }

    @Test
    fun aReSetSectionsLastLineIsCenteredWhereThePrintCenteredIts() {
        val words = (1..6).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, centered = true, ids = 1..6))
        val page = layout(lines, words, capacity = 10f)
        assertEquals(3, page.lines)
        assertTrue("the last re-set line carries the print's centering", page.slots.last().centered)
        assertTrue("the full re-set lines are justified", page.slots.dropLast(1).none { it.centered })
    }

    @Test
    fun everyWordOfThePageIsLaidOutExactlyOnceInOrder() {
        val words = (1..24).associateWith { word(it, it / 6 + 1, it) }
        val lines = (0..3).map { lineOf(it + 1, ids = it * 6 + 1..it * 6 + 6) }
        // A measure of 13 holds three words to a line, so the section re-sets
        // from four print lines of six into eight lines of three.
        val page = layout(lines, words, capacity = 13f)
        val laid = page.slots.flatMap { it.words.map { placed -> placed.word.id } }
        assertEquals("every word is on the page once", (1..24).toList(), laid)
    }

    @Test
    fun aSurahsNameAndItsBasmallahStartFreshLines() {
        val words = (1..4).associateWith { word(it, 1, it) }
        val lines = listOf(
            lineOf(1, ids = 1..4),
            lineOf(2, type = "surah_name", surah = 2),
            lineOf(3, type = "basmallah", surah = 2),
            lineOf(4, ids = 1..4),
        )
        val page = layout(lines, words, capacity = 20f)
        assertEquals(4, page.lines)
        assertEquals(SlotKind.Text, page.slots[0].kind)
        assertEquals(SlotKind.SurahName, page.slots[1].kind)
        assertEquals(2, page.slots[1].surah)
        assertEquals(SlotKind.Basmallah, page.slots[2].kind)
        assertEquals(SlotKind.Text, page.slots[3].kind)
    }

    @Test
    fun theSameWordsAlwaysLayOutTheSameWay() {
        val words = (1..10).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, ids = 1..10))
        val first = layout(lines, words, capacity = 14f)
        val second = layout(lines, words, capacity = 14f)
        assertEquals(first.slots.size, second.slots.size)
        first.slots.zip(second.slots).forEach { (a, b) ->
            assertEquals(a.words.map { it.word.id }, b.words.map { it.word.id })
            a.words.zip(b.words).forEach { (x, y) ->
                assertEquals(x.start, y.start, 0f)
                assertEquals(x.width, y.width, 0f)
            }
        }
    }

    @Test
    fun anAyahsNumberTakesItsRoomAndKeepsItsPlace() {
        val words = mapOf(
            1 to word(1, 1, 1),
            2 to word(2, 1, 2),
            3 to word(3, 1, 3, marker = true),
        )
        val lines = listOf(lineOf(1, ids = 1..3))
        val page = layout(lines, words, capacity = 20f)
        val slot = page.slots.single()
        // The marker stands at the line's left end, after the words.
        assertEquals(3, slot.words.last().word.id)
        assertTrue("the marker is a marker", slot.words.last().word.marker)
        assertEquals(1f, slot.words.last().width, 0.001f)
    }

    @Test
    fun aMeasureTooTightForOneWordNeverLosesIt() {
        val words = (1..4).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, ids = 1..4))
        // A measure that cannot hold one word: each word still gets its line.
        val page = layout(lines, words, capacity = 1f)
        assertEquals(4, page.lines)
        assertEquals((1..4).toList(), page.slots.flatMap { it.words.map { p -> p.word.id } })
        assertTrue("a lone word is centered, not marooned", page.slots.all { it.centered })
    }

    @Test
    fun anEmptySectionDrawsNothing() {
        val words = emptyMap<Int, LayoutWord>()
        val lines = listOf(lineOf(1, type = "surah_name", surah = 1))
        val page = layout(lines, words, capacity = 20f)
        assertEquals(1, page.lines)
        assertTrue(page.slots.single().words.isEmpty())
    }

    @Test
    fun theCapacityThePageUsesIsTheMeasureOverTheReadersSize() {
        // At the page's own stand the measure holds more of the face's ems
        // than at a larger size: the reader's size is a share of the em, and
        // the measure is the page's own.
        assertEquals(
            PageFrame.EM_PER_LINE / MushafText.scale(1f),
            MushafText.capacityEm(1f),
            0.0001f,
        )
        assertTrue(
            "a larger step holds fewer of the face's ems",
            MushafText.capacityEm(1f) < MushafText.capacityEm(0.65f),
        )
    }
}
