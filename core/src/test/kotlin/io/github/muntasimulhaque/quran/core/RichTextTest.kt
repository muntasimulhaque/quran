package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextTest {

    @Test
    fun `paragraphs and headings become blocks`() {
        val blocks = RichText.parseHtml("<h2>The Virtues</h2><p>First line.</p><p>Second.</p>")
        assertEquals(
            listOf(TextBlockKind.HEADING, TextBlockKind.PARAGRAPH, TextBlockKind.PARAGRAPH),
            blocks.map { it.kind },
        )
        assertEquals("The Virtues", blocks[0].runs.single().text)
        assertEquals("Second.", blocks[2].runs.single().text)
    }

    @Test
    fun `bold italic and bold italic are marked`() {
        val runs = RichText.parseHtml("<p>plain <strong>bold</strong> <em>italic</em> <b><i>both</i></b> end</p>")
            .single().runs
        assertEquals("bold", runs.first { it.bold }.text)
        assertEquals("italic", runs.first { it.italic }.text)
        val both = runs.first { it.bold && it.italic }
        assertEquals("both", both.text)
    }

    @Test
    fun `line breaks survive and other whitespace collapses`() {
        val runs = RichText.parseHtml("<p>one   two<br>three\n\nfour</p>").single().runs
        assertEquals("one two\nthree four", runs.joinToString("") { it.text })
    }

    @Test
    fun `arabic is split from latin and quotes are marked`() {
        val runs = RichText.parseHtml("<p>Allah said <q>إِنَّ اللَّهَ</q> indeed.</p>").single().runs
        assertEquals(listOf(false, true, false), runs.map { it.arabic })
        val quote = runs.single { it.quote }
        assertEquals("إِنَّ اللَّهَ", quote.text)
        assertFalse(runs.first().quote)
    }

    @Test
    fun `the prophet ligature counts as arabic`() {
        val runs = RichText.runs("the Prophet \uFDFA said")
        assertEquals("\uFDFA", runs.single { it.arabic }.text)
    }

    @Test
    fun `footnote markers split out of the sentence`() {
        val runs = RichText.footnotes("In the name of Allah,[2] the Merciful.[3]")
        assertEquals("In the name of Allah,", runs[0].text)
        assertEquals(2, runs[1].marker)
        assertEquals(" the Merciful.", runs[2].text)
        assertEquals(3, runs[3].marker)
    }

    @Test
    fun `bracketed words are not footnotes`() {
        val runs = RichText.footnotes("what is [presently] before them and what will be after them,[100] and")
        assertEquals(listOf(100), runs.filter { it.marker != null }.map { it.marker })
        assertEquals(
            "what is [presently] before them and what will be after them,100 and",
            runs.joinToString("") { it.text },
        )
    }

    @Test
    fun `plain drops footnote markers and keeps the sentence`() {
        assertEquals(
            "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
            RichText.plain("In the name of Allah,[2] the Entirely Merciful, the Especially Merciful.[3]"),
        )
        assertEquals(
            "what is [presently] before them and what will be after them, and",
            RichText.plain("what is [presently] before them and what will be after them,[100] and"),
        )
    }

    @Test
    fun `presentation forms unfold into real letters`() {
        // The tafsir source carries a pre-shaped phrase; NFKC restores letters.
        val runs = RichText.runs("\u0627\uFEDF\uFEE0\uFEE7\uFEE2")
        assertEquals("اللنم", runs.joinToString("") { it.text })
        assertTrue(runs.none { run -> run.text.codePoints().anyMatch { it in 0xFE70..0xFEFF } })
    }

    @Test
    fun `isolated shadda forms lose their artifact space`() {
        assertEquals("\u064E\u0651", RichText.runs("\uFC60").single().text)
    }

    @Test
    fun `the prophet ligature is preserved`() {
        assertEquals("\uFDFA", RichText.runs("\uFDFA").single().text)
    }

    @Test
    fun `quoted passages are marked`() {
        val runs = RichText.quotes("هو {الثناء على الله} بصفات الكمال")
        assertEquals(1, runs.count { it.quote })
        assertEquals("{الثناء على الله}", runs.single { it.quote }.text)
        assertTrue(runs.filter { it.text.isNotBlank() }.all { it.arabic })
    }

    @Test
    fun `unclosed quote keeps its text`() {
        val runs = RichText.quotes("قال {الحمد")
        assertEquals("{الحمد", runs.single { it.quote }.text)
    }

    @Test
    fun `adjacent runs with equal styling merge`() {
        val runs = RichText.parseHtml("<p>one<em></em> two</p>").single().runs
        assertEquals("one two", runs.single().text)
    }

    @Test
    fun `unknown tags keep their text`() {
        val runs = RichText.parseHtml("<p>see <a href=\"https://example.org\">this</a> note</p>").single().runs
        assertEquals("see this note", runs.joinToString("") { it.text })
    }

    /**
     * The stored tafsir is a small HTML subset for its own panels to parse.
     * A place with no parser behind it (a share, a search excerpt, a surah's
     * introduction) reads the text through `plain`, so a reader can never be
     * shown `</p><h2>` where a sentence should be.
     */
    @Test
    fun `plain removes the tafsir markup`() {
        assertEquals(
            "The Meaning of Al-Fatihah & its Various Names This Surah is called Al-Fatihah.",
            RichText.plain(
                "<h2>The Meaning of Al-Fatihah & its Various Names</h2>" +
                    "<p>This Surah is called <strong>Al-Fatihah</strong>.</p>",
            ),
        )
        assertEquals(
            "Al Fatiha & its names",
            RichText.plain("<h2>Al Fatiha & its names</h2>"),
        )
    }

    @Test
    fun `plain leaves no space where a tag stood beside punctuation`() {
        assertEquals(
            "The Entirely Merciful, the Especially Merciful.",
            RichText.plain("The <em>Entirely</em> Merciful, the <em>Especially</em> Merciful."),
        )
        assertEquals("(The Cow)", RichText.plain("(The Cow)"))
    }

    @Test
    fun `plain separates the words a removed tag stood between`() {
        assertEquals("one two", RichText.plain("one<strong>two</strong>"))
        assertEquals("one two three", RichText.plain("<p>one</p><p>two</p><p>three</p>"))
        assertEquals("before after", RichText.plain("before<br>after").replace("\n", " "))
    }

    @Test
    fun `plain keeps a real less-than and a word list's own brackets`() {
        assertEquals("1 < 2 and 3 > 2", RichText.plain("1 < 2 and 3 > 2"))
        assertEquals("disbelieve[d]", RichText.plain("disbelieve[d]"))
        assertEquals("[the] Last", RichText.plain("[the] Last"))
    }

    @Test
    fun `plain keeps a paragraph break as a break`() {
        val plain = RichText.plain("<h2>Name</h2>\n<p>First paragraph.</p>\n<p>Second.</p>")
        assertTrue("paragraphs must stay apart: $plain", plain.contains("\n"))
        assertTrue(plain.startsWith("Name"))
        assertTrue(plain.endsWith("Second."))
        assertFalse("no run of blank lines: $plain", plain.contains("\n\n\n"))
    }
}
