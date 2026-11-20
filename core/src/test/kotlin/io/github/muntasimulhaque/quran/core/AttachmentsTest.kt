package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The working set a surface hands its connection, and the budget it keeps.
 *
 * The library on the device and the library on the glass are two different
 * things: a reader may keep nine translations and read one, so the set is
 * built from the screen's own choices and never from the shelf. These legs
 * pin the shape; the catalog gate in `:tools` pins the worst case the
 * catalog can produce.
 */
class AttachmentsTest {

    @Test
    fun `the budget keeps one of sqlite's ten attachments spare`() {
        assertEquals(
            "SQLite attaches ten databases, and the ninth leaves one for the next change",
            9,
            Attachments.BUDGET,
        )
    }

    @Test
    fun `a reading attaches its columns once each and in priority order`() {
        val set = Attachments.screen(
            translations = listOf("translation-a", "translation-b"),
            tafsirs = listOf("tafsir-a"),
            words = "words-a",
            root = "word-root",
            voice = "reciter-a",
        )
        assertEquals(
            listOf(
                "translation-a",
                "translation-b",
                "words-a",
                "word-root",
                "reciter-a",
                "tafsir-a",
            ),
            set,
        )
    }

    @Test
    fun `the same pack is attached once`() {
        val set = Attachments.screen(
            translations = listOf("translation-a", "translation-a"),
            tafsirs = listOf("translation-a"),
        )
        assertEquals(listOf("translation-a"), set)
    }

    @Test
    fun `the columns cap keeps four on the page`() {
        val set = Attachments.screen(
            translations = (1..9).map { "translation-$it" },
            tafsirs = emptyList(),
        )
        assertEquals(
            "a page holds four columns; the fifth is a file, not a column",
            Attachments.COLUMNS,
            set.size,
        )
        assertEquals(
            listOf("translation-1", "translation-2", "translation-3", "translation-4"),
            set,
        )
    }

    @Test
    fun `a set past the budget keeps the page before the card`() {
        val translations = (1..Attachments.COLUMNS).map { "translation-$it" }
        val tafsirs = (1..10).map { "tafsir-$it" }
        val set = Attachments.screen(translations = translations, tafsirs = tafsirs)
        assertEquals(Attachments.BUDGET, set.size)
        assertEquals(
            "the page keeps its columns before an unopened card",
            translations,
            set.take(Attachments.COLUMNS),
        )
        assertEquals(tafsirs.take(Attachments.BUDGET - Attachments.COLUMNS), set.drop(Attachments.COLUMNS))
    }

    @Test
    fun `the reminder attaches only the translations it was given`() {
        assertEquals(
            listOf("translation-a"),
            Attachments.reminder(listOf("translation-a")),
        )
        assertEquals(emptyList<String>(), Attachments.reminder(emptyList()))
        assertEquals(
            "the reminder reads one, so a longer list is a fault the gate catches, not a set to attach",
            Attachments.BUDGET,
            Attachments.reminder((1..12).map { "translation-$it" }).size,
        )
    }

    @Test
    fun `a search attaches the packs it was given and nothing else`() {
        assertEquals(
            listOf("translation-a", "words-a", "tafsir-a"),
            Attachments.search(
                translations = listOf("translation-a"),
                tafsirs = listOf("tafsir-a"),
                words = "words-a",
            ),
        )
    }
}
