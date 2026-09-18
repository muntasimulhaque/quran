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
}
