package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The recent list's arithmetic: newest first, one spelling per query, five at
 * most, and a stored form that comes back the way it went in. A query is a
 * reader's own words, so the tests name real ones.
 */
class RecentQueriesTest {

    @Test
    fun theNewestQueryLeads() {
        val once = RecentQueries.record(emptyList(), "mercy")
        val twice = RecentQueries.record(once, "light")
        assertEquals(listOf("light", "mercy"), twice)
    }

    @Test
    fun oneWordIsOneRowWhateverItsCase() {
        val once = RecentQueries.record(emptyList(), "Mercy")
        val twice = RecentQueries.record(once, "mercy")
        assertEquals(listOf("mercy"), twice)
    }

    @Test
    fun theListStopsAtFive() {
        var list = emptyList<String>()
        listOf("one", "two", "three", "four", "five", "six").forEach {
            list = RecentQueries.record(list, it)
        }
        assertEquals(RecentQueries.LIMIT, list.size)
        assertEquals("six", list.first())
        assertEquals("two", list.last())
    }

    @Test
    fun aBlankQueryChangesNothing() {
        val list = RecentQueries.record(listOf("mercy"), "   ")
        assertEquals(listOf("mercy"), list)
    }

    @Test
    fun theStoredFormRoundTrips() {
        val queries = listOf("2:255", "the mercy of Allah", "সবর")
        assertEquals(
            queries,
            RecentQueries.decode(RecentQueries.encode(queries)),
        )
    }

    @Test
    fun theStoredFormIsCleanedOnTheWayIn() {
        val stored = " mercy \n\nmercy\nlight\n"
        assertEquals(listOf("mercy", "light"), RecentQueries.decode(stored))
        assertTrue(RecentQueries.decode(null).isEmpty())
    }

    @Test
    fun oneQueryLeavesAndTheOthersKeepTheirOrder() {
        val existing = listOf("mercy", "absolute", "light")
        assertEquals(
            listOf("absolute", "light"),
            RecentQueries.remove(existing, "mercy"),
        )
        assertEquals(
            listOf("mercy", "light"),
            RecentQueries.remove(existing, "absolute"),
        )
    }

    @Test
    fun theMatchDoesNotCareAboutCaseOrSpaces() {
        val existing = listOf("Mercy", "absolute")
        assertEquals(listOf("absolute"), RecentQueries.remove(existing, "  MERCY "))
    }

    @Test
    fun aQueryThatIsNotThereChangesNothing() {
        val existing = listOf("mercy", "absolute")
        assertEquals(existing, RecentQueries.remove(existing, "light"))
        assertEquals(existing, RecentQueries.remove(existing, " "))
    }

    @Test
    fun theListStillKeepsItsLimitAfterARemoval() {
        val existing = listOf("one", "two", "three", "four", "five", "six")
        val after = RecentQueries.remove(existing, "three")
        assertEquals(listOf("one", "two", "four", "five", "six"), after)
        assertEquals(RecentQueries.LIMIT, after.size)
    }
}
