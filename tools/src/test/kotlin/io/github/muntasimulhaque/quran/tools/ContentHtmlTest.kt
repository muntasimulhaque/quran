package io.github.muntasimulhaque.quran.tools

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The one normalizer the tafsir, the translation's notes, and the surah
 * introductions pass through: the tags the app draws survive, a link keeps
 * its target whether the source quoted it or not, and a target the reading
 * cannot open leaves the words without the promise.
 */
class ContentHtmlTest {

    @Test
    fun aLinkKeepsItsTargetWhetherTheSourceQuotesItOrNot() {
        assertEquals(
            "<a href=\"/7/46-47\">vv. 46-47</a>",
            ContentHtml.sanitize("<a href=/7/46-47>vv. 46-47</a>"),
        )
        assertEquals(
            "<a href=\"/18/9\">v. 9</a>",
            ContentHtml.sanitize(
                "<a href=\"/18/9\" rel=\"noopener noreferrer\" target=\"_blank\">v. 9</a>",
            ),
        )
        assertEquals(
            "<a href=\"/2/67-73\">67-73</a>",
            ContentHtml.sanitize("<a href='/2/67-73'>67-73</a>"),
        )
    }

    @Test
    fun aLinkTheReadingCannotOpenKeepsItsWords() {
        assertEquals(
            "footnote 3",
            ContentHtml.sanitize(
                "<a href=\"/44:5?font=v2&amp;translations=95\">footnote 3</a>",
            ),
        )
        assertEquals(
            "this",
            ContentHtml.sanitize("<a href=\"https://example.org\">this</a>"),
        )
        assertEquals("bare", ContentHtml.sanitize("<a>bare</a>"))
    }

    @Test
    fun aDataHrefIsNotALinkTarget() {
        assertEquals("words", ContentHtml.sanitize("<a data-href=\"/2/1\">words</a>"))
    }

    @Test
    fun theArabicQuoteSpanBecomesAQElement() {
        assertEquals(
            "<p><q>الحمد</q></p>",
            ContentHtml.sanitize("<p><span class=\"arabic\">الحمد</span></p>"),
        )
    }

    @Test
    fun aGraySpanKeepsItsTextAndLosesTheSpan() {
        assertEquals(
            "<p>note</p>",
            ContentHtml.sanitize("<p><span class=\"gray\">note</span></p>"),
        )
    }

    @Test
    fun aTagTheAppDoesNotDrawIsDroppedAndItsWordsStay() {
        assertEquals("<p>a1b</p>", ContentHtml.sanitize("<p>a<sup>1</sup>b</p>"))
    }

    @Test
    fun theSourcesEntitiesBecomeTheLettersTheyName() {
        assertEquals(
            "<p>a & b, \"c\", d's</p>",
            ContentHtml.sanitize("<p>a &amp; b, &quot;c&quot;, d&#39;s</p>"),
        )
    }
}
