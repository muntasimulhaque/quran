package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The page's own line setting, pinned here because it is the part of the
 * Mushaf a reader meets first and the part a text engine can most easily get
 * quietly wrong: a line re-set when the print set it, a line past its measure,
 * a word lost between two lines, a section that forgets it was centered.
 *
 * The measurement is the shape of the real thing. A letter is one em wide and
 * a tatweel is half an em, which is the order of the face's own: a word of the
 * Book is a handful of ems and a stroke through it half of one.
 */
class PageTextLayoutTest {

    private val space = 0.22f

    /** A letter, an em; a tatweel, half of one. */
    private fun measure(text: String): Float = text.sumOf { char ->
        if (char == Kashida.TATWEEL) 0.5 else 1.0
    }.toFloat()

    /** An ayah number's roundel: a digit's worth, then two. */
    private fun roundel(ayah: Int): Float = if (ayah <= 9) 0.6f else if (ayah <= 99) 0.65f else 0.84f

    private fun word(id: Int, ayah: Int, position: Int, marker: Boolean = false): LayoutWord =
        LayoutWord(
            id = id,
            ayah = ayah,
            position = position,
            marker = marker,
            text = if (marker) "" else "abcd",
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
        step: Float = 1f,
    ) = PageTextLayout.layout(lines, words, ::measure, space, ::roundel, step)

    @Test
    fun thePrintsLinesStandWhileTheyFit() {
        // Two lines of four words each: 16 ems of words, 3 gaps of 0.22, so
        // 16.66 against the page's own measure.
        val words = (1..8).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, ids = 1..4), lineOf(2, ids = 5..8))
        val page = layout(lines, words)
        assertEquals("the print's two lines stand", 2, page.lines)
        for (slot in page.slots) {
            assertEquals("four words on the print's line", 4, slot.words.size)
        }
    }

    @Test
    fun thePrintsLinesStandEvenWhenTheyAreEmpty() {
        // The page used to re-set a section whose lines were judged too empty,
        // which packed the Book's fifteen lines into twelve or thirteen. A
        // line the print set short is a line the print set short: it stands.
        val words = (1..4).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, ids = 1..4))
        // One line of four short words against a measure that holds six: the
        // line is half empty and it is still the print's line.
        val page = PageTextLayout.layout(
            lines = lines,
            words = words,
            measure = { 1f },
            spaceEm = 0.01f,
            roundelEm = ::roundel,
            step = 1f / 6f,
        )
        assertEquals("the print's own line stands", 1, page.lines)
        assertEquals("every word is on it", 4, page.slots.single().words.size)
    }

    @Test
    fun aJustifiedLineStretchesItsLettersBeforeItsGaps() {
        // The page is set at its fullest line, so a second, emptier line on the
        // same page is the line with slack to fill.
        val words = mapOf(
            1 to word(1, 1, 1, ).copy(text = "aaaa"),
            2 to word(2, 1, 2).copy(text = "aaaa"),
            3 to word(3, 1, 3).copy(text = "aaaa"),
            4 to word(4, 2, 1).copy(text = "ab"),
            5 to word(5, 2, 2).copy(text = "ab"),
            6 to word(6, 2, 3).copy(text = "ab"),
        )
        val lines = listOf(lineOf(1, ids = 1..3), lineOf(2, ids = 4..6))
        val page = layout(lines, words)
        val slot = page.slots[1]
        // The emptier line's words are 6 ems against a measure set by the
        // fuller line's 12.44, so there is room for strokes in every word of
        // it, and what the strokes cannot reach falls to the gaps.
        val strokes = slot.words.map { placed -> placed.text.count { it == Kashida.TATWEEL } }
        assertTrue("the first word takes strokes", strokes[0] >= 1)
        assertTrue("the last word takes strokes too", strokes[2] >= 1)
        // What the strokes could not reach, the gaps carry.
        val first = slot.words[0]
        val second = slot.words[1]
        val gap = second.start - first.start - first.width
        assertTrue("the leftover is in the gap", gap > space + 0.05f)
        // And the line reaches the measure it was given.
        val last = slot.words.last()
        assertEquals(
            "the line fills the measure",
            PageFrame.EM_PER_LINE / page.scale,
            last.start + last.width,
            0.01f,
        )
    }

    @Test
    fun theWiderWordIsAskedToCarryTheWiderStretch() {
        val words = mapOf(
            1 to word(1, 1, 1).copy(text = "aaaa"),
            2 to word(2, 1, 2).copy(text = "aaaa"),
            3 to word(3, 1, 3).copy(text = "aaaa"),
            4 to word(4, 2, 1).copy(text = "ab"),
            5 to word(5, 2, 2).copy(text = "abcdefgh"),
        )
        val lines = listOf(lineOf(1, ids = 1..3), lineOf(2, ids = 4..5))
        val page = layout(lines, words)
        val slot = page.slots[1]
        val shortBy = slot.words[0].width - 2f
        val longBy = slot.words[1].width - 8f
        assertTrue(
            "the wider word gains at least as much: $shortBy against $longBy",
            longBy >= shortBy,
        )
    }

    @Test
    fun aWordThatCannotCarryAStrokeLeavesItToTheGaps() {
        // One letter has no boundary to put a tatweel at, so its share falls to
        // the word gap: a page with a little air, not a stroke through a word.
        val words = mapOf(
            1 to word(1, 1, 1).copy(text = "aaaa"),
            2 to word(2, 1, 2).copy(text = "aaaa"),
            3 to word(3, 1, 3).copy(text = "aaaa"),
            4 to word(4, 2, 1).copy(text = "a"),
            5 to word(5, 2, 2).copy(text = "ab"),
        )
        val lines = listOf(lineOf(1, ids = 1..3), lineOf(2, ids = 4..5))
        val page = layout(lines, words)
        val slot = page.slots[1]
        assertEquals("the lone letter takes no stroke", 0, slot.words[0].text.length - 1)
        val gap = slot.words[1].start - slot.words[0].start - slot.words[0].width
        assertTrue("its share is in the gap", gap > space + 0.5f)
    }

    @Test
    fun aCenteredLineKeepsItsGapsAndStandsInTheMiddle() {
        val words = (1..3).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, centered = true, ids = 1..3))
        val page = layout(lines, words)
        val slot = page.slots.single()
        assertTrue("the print centered this line", slot.centered)
        // 12.44 natural: the line stands in the middle of what is left and its
        // gaps stay the natural 0.22, because a centered line is not filled.
        assertTrue("the line stands off the right edge", slot.words[0].start > 0f)
        assertEquals(
            "a centered line is not elongated",
            0,
            slot.words.sumOf { placed -> placed.text.count { char -> char == Kashida.TATWEEL } },
        )
        assertEquals(
            "a centered line keeps the gap it was written with",
            space,
            slot.words[1].start - slot.words[0].start - slot.words[0].width,
            0.001f,
        )
    }

    @Test
    fun aCenteredLineWithRoomLeftKeepsThatRoom() {
        // The page is set at its fullest line, so a centered line beside it has
        // room the print leaves it: the line stands in the middle and is not
        // stretched out to the measure.
        val words = mapOf(
            1 to word(1, 1, 1),
            2 to word(2, 1, 2),
            3 to word(3, 1, 3),
            4 to word(4, 2, 1),
            5 to word(5, 2, 2),
        )
        val lines = listOf(
            lineOf(1, ids = 1..3),
            lineOf(2, centered = true, ids = 4..5),
        )
        val page = layout(lines, words)
        val slot = page.slots[1]
        val capacity = PageFrame.EM_PER_LINE / page.scale
        val end = slot.words.last().start + slot.words.last().width
        assertTrue("the centered line stands short of the measure: $end of $capacity", end < capacity)
        assertEquals(
            "it stands in the middle of what is left",
            (capacity - (end - slot.words[0].start)) / 2f,
            slot.words[0].start,
            0.01f,
        )
        assertEquals(
            "and keeps the gap it was written with",
            space,
            slot.words[1].start - slot.words[0].start - slot.words[0].width,
            0.001f,
        )
    }

    @Test
    fun aLineNoLongerHeldByTheReadersSizeFlowsOntoASecond() {
        val words = (1..6).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, ids = 1..6))
        // Four ems a word and the line's own measure is far smaller: the
        // reader has asked for type the print's line no longer holds.
        val page = PageTextLayout.layout(
            lines = lines,
            words = words,
            measure = ::measure,
            spaceEm = space,
            roundelEm = ::roundel,
            step = 6f,
        )
        assertTrue("the line flows", page.lines > 1)
        val full = page.slots.filter { it.words.size > 1 }
        assertTrue("the full flowed lines are justified", full.none { it.centered && it.words.size > 1 })
        assertTrue("the last flowed line carries the print's centering", page.slots.last().centered)
        assertEquals(
            "every word is on the page once",
            (1..6).toList(),
            page.slots.flatMap { it.words.map { placed -> placed.word.id } },
        )
    }

    @Test
    fun aLinesLastCenteredWhereThePrintCenteredIts() {
        val words = (1..6).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, centered = true, ids = 1..6))
        val page = PageTextLayout.layout(
            lines = lines,
            words = words,
            measure = ::measure,
            spaceEm = space,
            roundelEm = ::roundel,
            step = 6f,
        )
        assertTrue("the last flowed line carries the print's centering", page.slots.last().centered)
        val full = page.slots.dropLast(1).filter { it.words.size > 1 }
        assertTrue("a full flowed line is not centered", full.none { it.centered })
    }

    @Test
    fun anAyahsNumberTakesItsOwnRoundelsRoom() {
        // A three-digit number carries a wider roundel than a one-digit one,
        // and the engine reserves the one the drawer draws, so the line a
        // number stands on is the line a finger can touch.
        val words = mapOf(
            1 to word(1, 7, 1),
            2 to word(2, 7, 2, marker = true),
            3 to word(3, 42, 1),
            4 to word(4, 42, 2, marker = true),
        )
        val page = layout(listOf(lineOf(1, ids = 1..2), lineOf(2, ids = 3..4)), words)
        val oneDigit = page.slots[0].words.last()
        val twoDigit = page.slots[1].words.last()
        assertEquals("the marker stands at the line's left end", true, oneDigit.word.marker)
        assertEquals(
            "a one digit number fills the roundel's own height",
            roundel(7) / page.scale,
            oneDigit.width,
            0.001f,
        )
        assertTrue("the wider number takes a wider roundel", twoDigit.width > oneDigit.width)
    }

    @Test
    fun everyWordOfThePageIsLaidOutExactlyOnceInOrder() {
        val words = (1..24).associateWith { word(it, it / 6 + 1, it) }
        val lines = (0..3).map { lineOf(it + 1, ids = it * 6 + 1..it * 6 + 6) }
        val page = layout(lines, words)
        assertEquals(
            "every word is on the page once",
            (1..24).toList(),
            page.slots.flatMap { it.words.map { placed -> placed.word.id } },
        )
        assertEquals("the print's four lines are the page's four lines", 4, page.lines)
    }

    @Test
    fun theSameWordsAlwaysLayOutTheSameWay() {
        val words = (1..10).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, ids = 1..10))
        val first = layout(lines, words)
        val second = layout(lines, words)
        assertEquals(first.slots.size, second.slots.size)
        first.slots.zip(second.slots).forEach { (a, b) ->
            assertEquals(a.words.map { it.word.id }, b.words.map { it.word.id })
            a.words.zip(b.words).forEach { (x, y) ->
                assertEquals(x.start, y.start, 0f)
                assertEquals(x.width, y.width, 0f)
                assertEquals(x.text, y.text)
            }
        }
    }

    @Test
    fun aSurahsNameAndItsBasmallahStandOnTheirOwnLines() {
        val words = (1..4).associateWith { word(it, 1, it) }
        val lines = listOf(
            lineOf(1, ids = 1..4),
            lineOf(2, type = "surah_name", surah = 2),
            lineOf(3, type = "basmallah", surah = 2),
            lineOf(4, ids = 1..4),
        )
        val page = layout(lines, words)
        assertEquals(4, page.lines)
        assertEquals(SlotKind.Text, page.slots[0].kind)
        assertEquals(SlotKind.SurahName, page.slots[1].kind)
        assertEquals(2, page.slots[1].surah)
        assertEquals(SlotKind.Basmallah, page.slots[2].kind)
        assertEquals(SlotKind.Text, page.slots[3].kind)
    }

    @Test
    fun thePagesOwnTypeIsItsFullestLine() {
        // A page whose fullest line is fuller than its emptiest is set at the
        // fullest: a line past the measure is a line whose words have been
        // re-set, and that is the one thing the page must never do.
        val words = mapOf(
            1 to word(1, 1, 1),
            2 to word(2, 1, 2),
            3 to word(3, 2, 1),
        )
        val lines = listOf(lineOf(1, ids = 1..2), lineOf(2, ids = 3..3))
        val page = layout(lines, words)
        // Line one holds two four-em words, line two one: the fuller line is
        // eight ems and one gap, and the page is set so that it stands.
        val fullest = 8f + space
        assertTrue(
            "the page's own type holds its fullest line: ${page.scale}",
            page.scale <= PageFrame.EM_PER_LINE / fullest + 0.001f,
        )
        // And the emptier line is filled from what is left of it.
        assertTrue("the fuller line is the tighter one", page.slots[1].words.single().start > 0f)
    }

    @Test
    fun aReadersStepIsAShareOfThePagesOwnType() {
        val words = (1..10).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, ids = 1..10))
        val usual = layout(lines, words).scale
        val larger = layout(lines, words, step = 1.2f).scale
        assertEquals("a larger step is a larger share of the same hand", 1.2f, larger / usual, 0.0001f)
    }

    @Test
    fun aMeasureTooTightForOneWordNeverLosesIt() {
        val words = (1..4).associateWith { word(it, 1, it) }
        val lines = listOf(lineOf(1, ids = 1..4))
        val page = layout(lines, words, step = 20f)
        assertTrue("the line flows", page.lines >= 4)
        assertEquals(
            (1..4).toList(),
            page.slots.flatMap { it.words.map { placed -> placed.word.id } },
        )
    }

    @Test
    fun anEmptyPageDrawsNothing() {
        val page = layout(listOf(lineOf(1, type = "surah_name", surah = 1)), emptyMap())
        assertEquals(1, page.lines)
        assertTrue(page.slots.single().words.isEmpty())
    }
}
