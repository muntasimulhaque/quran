package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {

    @Test
    fun `blank and one letter queries are refused`() {
        assertNull(Search.parse(""))
        assertNull(Search.parse("   "))
        assertNull(Search.parse("a"))
        assertNull(Search.parse("م"))
    }

    @Test
    fun `arabic queries fold letter forms and drop marks`() {
        val query = Search.parse("الرَّحْمَٰنِ")!!
        assertTrue(query.arabic)
        assertEquals(listOf("الرحمن"), query.terms)
    }

    @Test
    fun `english queries fold diacritics and transliteration marks`() {
        assertEquals("allah", Search.normalizeEnglish("All\u0101h"))
        assertEquals("isa", Search.normalizeEnglish("\u02BF\u012As\u0101"))
        assertEquals("god's", Search.normalizeEnglish("God\u2019s"))
        assertEquals("musa", Search.normalizeEnglish("M\u016Bs\u0101"))
        assertEquals("a-b", Search.normalizeEnglish("a\u2013b"))
    }

    @Test
    fun `english queries lowercase and keep inner punctuation`() {
        val query = Search.parse("God's Mercy.")!!
        assertFalse(query.arabic)
        assertEquals(listOf("god's", "mercy"), query.terms)
    }

    @Test
    fun `multiple terms are kept in order and bounded`() {
        val query = Search.parse("one two three four five six seven eight")!!
        assertEquals(listOf("one", "two", "three", "four", "five", "six"), query.terms)
    }

    @Test
    fun `duplicate terms collapse`() {
        assertEquals(listOf("mercy"), Search.parse("mercy mercy")!!.terms)
    }

    @Test
    fun `a query with any arabic letter is arabic`() {
        assertTrue(Search.parse("Allah الرحمن")!!.arabic)
    }

    @Test
    fun `wildcards in a term are escaped`() {
        assertEquals("%100\\%%", Search.pattern("100%"))
        assertEquals("%a\\_b%", Search.pattern("a_b"))
        assertEquals("%a\\\\b%", Search.pattern("a\\b"))
    }

    @Test
    fun `references are read from every form a reader types`() {
        assertEquals(Search.Reference(2, 255), Search.reference("2:255"))
        assertEquals(Search.Reference(2, 255), Search.reference("2.255"))
        assertEquals(Search.Reference(2, 255), Search.reference("2 255"))
        assertEquals(Search.Reference(2, 255), Search.reference("surah 2:255"))
        assertEquals(Search.Reference(2, 255), Search.reference("Sura 2 255"))
        assertEquals(Search.Reference(36, null), Search.reference("36"))
        assertEquals(Search.Reference(36, null), Search.reference("surah 36"))
        assertEquals(Search.Reference(1, 1), Search.reference("\u0661:\u0661"))
        assertNull(Search.reference("115"))
        assertNull(Search.reference("mercy"))
        assertNull(Search.reference("255"))
        assertNull(Search.reference("2:"))
    }

    @Test
    fun `arabic matches ignore diacritics and letter forms`() {
        val text = "\u0671\u0644\u0631\u0651\u064e\u062d\u0652\u0645\u064e\u0670\u0646\u0650 \u0671\u0644\u0631\u0651\u064e\u062d\u0650\u064a\u0645\u0650"
        val ranges = Search.matchRanges(text, listOf("\u0627\u0644\u0631\u062d\u0645\u0646"), arabic = true)
        assertEquals(1, ranges.size)
        assertEquals(0, ranges.first().first)
    }

    @Test
    fun `english matches ignore transliteration marks`() {
        val text = "In the name of All\u0101h, the Entirely Merciful"
        val ranges = Search.matchRanges(text, listOf("allah"), arabic = false)
        assertEquals(1, ranges.size)
        assertEquals("All\u0101h", text.substring(ranges.first().first, ranges.first().last + 1))
    }

    @Test
    fun `match ranges cover whole visible words and merge`() {
        val text = "mercy, mercy and mercy"
        val ranges = Search.matchRanges(text, listOf("mercy"), arabic = false)
        assertEquals(3, ranges.size)
        assertEquals("mercy", text.substring(ranges[1].first, ranges[1].last + 1))
    }

    @Test
    fun `an excerpt centers the match and rebases the ranges`() {
        val text = "a ".repeat(400) + "needle" + " b".repeat(400)
        val (excerpt, ranges) = Search.excerpt(text, listOf("needle"), arabic = false, window = 60)
        assertTrue(excerpt.contains("needle"))
        assertEquals(1, ranges.size)
        assertEquals("needle", excerpt.substring(ranges.first().first, ranges.first().last + 1))
        assertTrue(excerpt.length < 200)
    }

    @Test
    fun `the index shape answers both scripts`() {
        assertEquals("\u0628\u0633\u0645 \u0627\u0644\u0644\u0647", Search.normalizeForIndex("\u0628\u0650\u0633\u0652\u0645\u0650 \u0671\u0644\u0644\u0651\u064e\u0647\u0650"))
        assertEquals("allah", Search.normalizeForIndex("All\u0101h"))
        assertEquals("allah", Search.normalizeForIndex("Allah"))
    }

    @Test
    fun `an excerpt never starts or ends inside a word`() {
        val text = (1..80).joinToString(" ") { "before$it" } +
            " needle " +
            (1..80).joinToString(" ") { "after$it" }
        val (excerpt, ranges) = Search.excerpt(text, listOf("needle"), arabic = false, window = 20)
        assertTrue("the window must be a window", excerpt.length < text.length)
        assertEquals(1, ranges.size)
        assertEquals("needle", excerpt.substring(ranges.first().first, ranges.first().last + 1))
        val body = excerpt.removePrefix("... ").removeSuffix(" ...")
        val firstWord = body.substringBefore(' ')
        val lastWord = body.substringAfterLast(' ')
        assertTrue("the window starts mid-word: $firstWord", firstWord.matches(Regex("before\\d+")))
        assertTrue("the window ends mid-word: $lastWord", lastWord.matches(Regex("after\\d+")))
    }

    /**
     * A tafsir is stored as a small HTML subset for its own panels to parse,
     * and a search result has no parser behind it. The excerpt therefore comes
     * from the readable form, or the reader is shown `</p><h2>` inside a
     * sentence. The highlight is computed on that same form, so it still lands
     * on the words the reader can see.
     */
    @Test
    fun `a tafsir excerpt is readable prose and its highlight still lands`() {
        val passage = "<h2>The Meaning of Al-Fatihah</h2>" +
            "<p>This Surah is called Al-Fatihah because of its subject matter, and " +
            "the <strong>mercy</strong> of Allah is mentioned in it more than once.</p>"
        val (excerpt, ranges) = Search.excerpt(passage, listOf("mercy"), arabic = false, window = 20)
        assertFalse("the excerpt must carry no markup: $excerpt", excerpt.contains('<'))
        assertTrue("the excerpt must carry the match: $excerpt", excerpt.contains("mercy"))
        assertEquals(1, ranges.size)
        assertEquals("mercy", excerpt.substring(ranges.first().first, ranges.first().last + 1))
        assertTrue(
            "a removed tag must not weld two words together: $excerpt",
            !excerpt.contains("subject matter,and"),
        )
    }

    @Test
    fun `a translation excerpt drops footnote markers instead of showing them`() {
        val (excerpt, ranges) = Search.excerpt(
            "In the name of Allah,[2] the Entirely Merciful, the Especially Merciful.[3]",
            listOf("merciful"),
            arabic = false,
            window = 5,
        )
        assertFalse("a marker must not reach the reader: $excerpt", excerpt.contains("[2]"))
        assertEquals("Merciful", excerpt.substring(ranges.first().first, ranges.first().last + 1))
    }
}
