package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * The ayah of the day is one ayah per local day, drawn from a deck rather
 * than from the Book's own order, and every ayah still comes exactly once
 * before the deck is filled again.
 *
 * The deck is a pure function of the clock, so the numbers it deals are
 * pinned here by value: a change to the seed or to the shuffle is a change to
 * what every reader is given, and it should have to be written down rather
 * than arrive by accident. That is also what makes the coverage test below
 * cheap: the whole Book is one pass of the deck, and it can be walked in
 * memory without a phone.
 */
class DailyAyahTest {

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun theSameDayAlwaysShowsTheSameAyah() {
        val noon = utc.millisOf(2026, Calendar.SEPTEMBER, 25, 12)
        assertEquals(
            DailyAyah.numberFor(noon, utc),
            DailyAyah.numberFor(noon + 60_000L, utc),
        )
    }

    @Test
    fun theDeckIsDealtInThisOrder() {
        val noon = utc.millisOf(2026, Calendar.SEPTEMBER, 25, 12)
        assertEquals(
            "seven mornings, pinned: the deck is drawn from the day number alone",
            listOf(5117, 1719, 5757, 3120, 3920, 5882, 1312),
            (0 until 7).map { DailyAyah.numberFor(noon + it * 86_400_000L, utc) },
        )
    }

    @Test
    fun theFirstPassDealsTheseFirstCards() {
        // The epoch itself is the anchor of the first pass, so these seven
        // pin the shuffle rather than the day it was asked about.
        assertEquals(
            listOf(4903, 3752, 4801, 5884, 2231, 4025, 3032),
            (0 until 7).map { DailyAyah.numberFor(it * 86_400_000L, utc) },
        )
    }

    @Test
    fun theWalkIsNotTheBookSOrder() {
        val start = utc.millisOf(2026, Calendar.SEPTEMBER, 25, 12)
        var previous = 0
        for (day in 0 until 60) {
            val number = DailyAyah.numberFor(start + day * 86_400_000L, utc)
            if (day == 0) continue
            val next = if (previous == DailyAyah.TOTAL_AYAHS) 1 else previous + 1
            assertTrue(
                "day $day brought the ayah after the day before, which is the walk again",
                number != next,
            )
            previous = number
        }
    }

    @Test
    fun everyAyahIsInRangeAndTheWholeBookIsCovered() {
        // Walked from the start of a pass, not from an arbitrary day: 6,236
        // days are one pass only when they begin on its first slot, and two
        // partial passes can honestly hold the same verse twice.
        val firstSlot = utc.millisOf(1970, Calendar.JANUARY, 1, 12)
        val seen = HashSet<Int>(DailyAyah.TOTAL_AYAHS)
        for (slot in 0 until DailyAyah.TOTAL_AYAHS) {
            val number = DailyAyah.numberFor(firstSlot + slot * 86_400_000L, utc)
            assertTrue("$number is outside 1..${DailyAyah.TOTAL_AYAHS}", number in 1..DailyAyah.TOTAL_AYAHS)
            // Nothing kept, so nothing can repeat: this is the whole reason
            // for a deck rather than a draw.
            assertTrue("$number came twice inside one pass", seen.add(number))
        }
        assertEquals(
            "a full pass must reach every ayah exactly once",
            DailyAyah.TOTAL_AYAHS,
            seen.size,
        )
    }

    @Test
    fun theDeckIsFilledAgainOnTheNextPass() {
        val noon = utc.millisOf(2026, Calendar.SEPTEMBER, 25, 12)
        assertEquals(
            "the same day, one pass on",
            5117,
            DailyAyah.numberFor(noon, utc),
        )
        assertEquals(
            "and the same day seventeen years later is a different verse",
            5814,
            DailyAyah.numberFor(noon + DailyAyah.TOTAL_AYAHS * 86_400_000L, utc),
        )
    }

    @Test
    fun aDayBeforeTheEpochIsStillADayOfTheBook() {
        // A pre-epoch instant floors to a negative day and lands inside the
        // Book rather than on a negative index, which is what floorDiv and
        // floorMod are in the production line for.
        assertEquals(5836, DailyAyah.numberFor(-86_400_000L, utc))
    }

    @Test
    fun midnightIsWhereTheDayTurns() {
        val justBefore = utc.millisOf(2026, Calendar.SEPTEMBER, 25, 23) + 59 * 60_000L
        val justAfter = justBefore + 2 * 60_000L
        val before = DailyAyah.numberFor(justBefore, utc)
        val after = DailyAyah.numberFor(justAfter, utc)
        assertTrue("the turn is a different ayah", before != after)
        assertEquals(
            "and the one after midnight is that day's own",
            DailyAyah.numberFor(justAfter + 60_000L, utc),
            after,
        )
    }

    @Test
    fun theReaderSTimezoneDecidesTheDay() {
        // Two zones whose local days differ at one instant: the reader's
        // today is their own, not UTC's, so the two readers are dealt from
        // two slots of the deck rather than from one.
        val instant = utc.millisOf(2026, Calendar.SEPTEMBER, 25, 20)
        val east = TimeZone.getTimeZone("Asia/Dhaka")
        val west = TimeZone.getTimeZone("America/Los_Angeles")
        assertEquals(1719, DailyAyah.numberFor(instant, east))
        assertEquals(5117, DailyAyah.numberFor(instant, west))
        assertTrue(
            "one local day apart is two slots, not one ayah apart",
            DailyAyah.numberFor(instant, east) != DailyAyah.numberFor(instant, west),
        )
    }

    private fun TimeZone.millisOf(year: Int, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance(this).apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis
}