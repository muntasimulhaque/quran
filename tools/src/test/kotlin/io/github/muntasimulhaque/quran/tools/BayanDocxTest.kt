package io.github.muntasimulhaque.quran.tools

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The publisher's Word edition keeps its footnotes outside the sentence, one
 * entry per note and a reference where the note belongs. These tests pin that
 * read and the way a note paragraph finds its ayah.
 */
class BayanDocxTest {

    private val namespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"

    private fun document(body: String) =
        """<?xml version="1.0" encoding="UTF-8"?>
        <w:document xmlns:w="$namespace"><w:body>$body</w:body></w:document>"""

    private fun run(text: String) = "<w:r><w:t>$text</w:t></w:r>"

    @Test
    fun aParagraphKeepsItsTextAndItsFootnoteIds() {
        val xml = document(
            "<w:p>${run("আলিফ-লাম-মীম।")}<w:r><w:footnoteReference w:id=\"2\"/></w:r></w:p>" +
                "<w:p>${run("এই সেই কিতাব।")}</w:p>",
        )
        val paragraphs = readDocxParagraphs(ByteArrayInputStream(xml.toByteArray()))
        assertEquals(2, paragraphs.size)
        assertEquals("আলিফ-লাম-মীম।", paragraphs[0].text)
        assertEquals(listOf(2), paragraphs[0].notes)
        assertEquals("এই সেই কিতাব।", paragraphs[1].text)
        assertTrue(paragraphs[1].notes.isEmpty())
    }

    @Test
    fun aSeparatorIsNotANote() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
            <w:footnotes xmlns:w="$namespace">
              <w:footnote w:type="separator" w:id="-1"><w:p>${run("--")}</w:p></w:footnote>
              <w:footnote w:id="2"><w:p>${run("এ অক্ষরগুলো কুরআনের একটি মু‘জিযা।")}</w:p></w:footnote>
            </w:footnotes>"""
        val notes = readDocxNotes(ByteArrayInputStream(xml.toByteArray()))
        assertEquals(setOf(2), notes.keys)
        assertEquals("এ অক্ষরগুলো কুরআনের একটি মু‘জিযা।", notes[2])
    }

    @Test
    fun aNoteParagraphFindsItsAyahAndKeepsItsNotesInOrder() {
        val paragraphs = listOf(
            DocxParagraph("আলিফ-লাম-মীম।", listOf(2)),
            DocxParagraph(
                "আর যমীনে বিচরণকারী প্রতিটি প্রাণীর রিয্কের দায়িত্ব আল্লাহরই।",
                listOf(50, 51),
            ),
        )
        val notes = mapOf(
            2 to "এ অক্ষরগুলো কুরআনের একটি মু‘জিযা।",
            50 to "প্রথম নোট।",
            51 to "দ্বিতীয় নোট।",
        )
        val qul = listOf(
            QulVerse("1:1", "পরম করুণাময় অতি দয়ালু আল্লাহর নামে।"),
            QulVerse("2:1", "আলিফ-লাম-মীম।"),
            QulVerse(
                "11:6",
                "আর যমীনে বিচরণকারী প্রতিটি প্রাণীর রিয্কের দায়িত্ব আল্লাহরই এবং তিনি জানেন তাদের আবাসস্থল।",
            ),
        )
        val matched = matchBayanNotes(paragraphs, notes, qul)
        assertEquals(listOf("এ অক্ষরগুলো কুরআনের একটি মু‘জিযা।"), matched.byAyah["2:1"])
        assertEquals(listOf("প্রথম নোট।", "দ্বিতীয় নোট।"), matched.byAyah["11:6"])
        assertEquals(3, matched.allTexts.size)
    }
}
