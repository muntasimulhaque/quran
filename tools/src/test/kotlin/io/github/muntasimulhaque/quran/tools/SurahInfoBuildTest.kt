package io.github.muntasimulhaque.quran.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The introductions' two build rules and the gate that keeps them.
 *
 * The source writes a reference in ten shapes and writes most of its targets
 * without quotes. The build expands the two abbreviations, accepts both
 * target shapes, drops a target the reading cannot open, and corrects the one
 * stale Night Journey link. The audit then reads every built introduction as
 * the app would and refuses a target, a verse, or a label that disagrees with
 * the others (owner decision, 23.5).
 */
class SurahInfoBuildTest {

    @Test
    fun theTwoAbbreviationsBecomeTheWordsAGeneralReaderKnows() {
        assertEquals("<p>verse 9</p>", SurahInfo.drawn(18, "<p>v. 9</p>"))
        assertEquals("<p>verses 46-47</p>", SurahInfo.drawn(7, "<p>vv. 46-47</p>"))
        assertEquals("<p>verse 9</p>", SurahInfo.drawn(18, "<p>v.9</p>"))
        assertEquals(
            "<p>verses 66-69 of Surah Ta Ha</p>",
            SurahInfo.drawn(114, "<p>vv. 66-69 of Surah Ta Ha</p>"),
        )
    }

    @Test
    fun aLinkedListOfVersesKeepsOneWord() {
        assertEquals(
            "<p>(verses <a href=\"/22/41\">41</a>, " +
                "<a href=\"/22/77\">77</a>, <a href=\"/22/78\">78</a>.)</p>",
            SurahInfo.drawn(
                22,
                "<p>(vv. <a href=\"/22/41\">41</a>, " +
                    "<a href=\"/22/77\">77</a>, <a href=\"/22/78\">78</a>.)</p>",
            ),
        )
    }

    @Test
    fun aDoubledAbbreviationIsDroppedRatherThanStuttered() {
        assertEquals(
            "<p>(<a href=\"/23/75-76\">verses 75-76</a>)</p>",
            SurahInfo.drawn(23, "<p>(vv. <a href=\"/23/75-76\">vv. 75-76</a>)</p>"),
        )
    }

    @Test
    fun theNightJourneyReferenceOpensTheVerseTheSentenceNames() {
        val source = "<p>This Surah takes its name (The Night Journey) from " +
            "<a href=\"/17/4\" rel=\"noopener noreferrer\" target=\"_blank\">v. 1</a>.</p>"
        assertEquals(
            "<p>This Surah takes its name (The Night Journey) from " +
                "<a href=\"/17/1\">verse 1</a>.</p>",
            SurahInfo.drawn(17, source),
        )
    }

    @Test
    fun theBuildStopsWhenTheSourceNoLongerHoldsTheCorrection() {
        assertThrows(IllegalArgumentException::class.java) {
            SurahInfo.drawn(
                17,
                "<p>This Surah takes its name (The Night Journey) from " +
                    "<a href=\"/17/1\">v. 1</a>.</p>",
            )
        }
    }

    @Test
    fun aTargetTheReadingCannotOpenLeavesTheWordsWithoutAnAnchor() {
        assertEquals(
            "footnote 3",
            SurahInfo.drawn(
                97,
                "<a href=\"/44:5?font=v2&amp;translations=95\">footnote 3</a>",
            ),
        )
    }

    @Test
    fun theAuditPassesARealIntroduction() {
        val html = SurahInfo.drawn(
            18,
            "<p>This Surah takes its name from " +
                "<a href=\"/18/9\" rel=\"noopener noreferrer\" target=\"_blank\">v. 9</a> " +
                "in which the word (A<em>l-Kahf</em>) occurs.</p>",
        )
        val audit = SurahInfo.audit(18, html, mapOf(18 to 110))
        assertEquals(emptyList<String>(), audit.problems)
    }

    @Test
    fun theAuditFindsALabelThatDoesNotMatchItsTarget() {
        val audit = SurahInfo.audit(
            17,
            "<a href=\"/17/4\">verse 1</a>",
            mapOf(17 to 111),
        )
        assertTrue(audit.problems.any { "opens 17:4" in it })
    }

    @Test
    fun theAuditAllowsTheKnownPrintedOverrunAndReportsIt() {
        val audit = SurahInfo.audit(
            13,
            "<a href=\"/13/34-48\">verses 34-48</a>",
            mapOf(13 to 43),
        )
        assertEquals(emptyList<String>(), audit.problems)
        assertTrue(audit.notes.any { "34-48" in it })
    }

    @Test
    fun theAuditFindsARangeBeyondTheSurah() {
        val audit = SurahInfo.audit(
            13,
            "<a href=\"/13/34-49\">verses 34-49</a>",
            mapOf(13 to 43),
        )
        assertTrue(audit.problems.any { "ends at 43" in it })
    }

    @Test
    fun theAuditFindsAVerseBeyondTheSurah() {
        val audit = SurahInfo.audit(
            1,
            "<a href=\"/1/8\">verse 8</a>",
            mapOf(1 to 7),
        )
        assertTrue(audit.problems.any { "has 7" in it })
    }

    @Test
    fun theAuditFindsALinkWithoutATarget() {
        val audit = SurahInfo.audit(18, "<a>verse 9</a>", mapOf(18 to 110))
        assertTrue(audit.problems.any { "has no target" in it })
    }

    @Test
    fun theAuditFindsATargetTheReadingCannotOpen() {
        val audit = SurahInfo.audit(
            18,
            "<a href=\"/44:5?font=v2\">footnote 3</a>",
            mapOf(18 to 110),
        )
        assertTrue(audit.problems.any { "cannot open" in it })
    }

    @Test
    fun theAuditFindsAnAbbreviationThatSurvived() {
        val audit = SurahInfo.audit(18, "v. 9", mapOf(18 to 110))
        assertTrue(audit.problems.any { "abbreviated verse label" in it })
    }

    @Test
    fun theAuditPassesASurahOnlyLink() {
        val audit = SurahInfo.audit(2, "<a href=\"/2\">Al-Baqarah</a>", mapOf(2 to 286))
        assertEquals(emptyList<String>(), audit.problems)
    }
}
