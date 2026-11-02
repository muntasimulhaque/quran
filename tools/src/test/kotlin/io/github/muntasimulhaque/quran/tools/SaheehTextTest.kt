package io.github.muntasimulhaque.quran.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaheehTextTest {

    @Test
    fun `al-fatihah opens on the first note`() {
        val notes = saheehNotes(
            "[2]- Allah is a proper name.\n" +
                "[3]- Ar-Rahman and ar-Raheem are two names.",
        )
        val local = localizeSaheeh(
            "In the name of Allah,[2] the Entirely Merciful, the Especially Merciful.[3]",
            notes,
        )
        assertEquals("[1]", local.text.substringAfter("Allah,").substringBefore(" the"))
        assertEquals("[2]", local.text.substringAfter("Merciful.").trim())
        assertEquals(2, local.notes)
        assertEquals(emptyList<Int>(), local.dropped)
        assertEquals(emptyList<Int>(), local.unused)
        assertTrue(local.footnotes.startsWith("[1]- Allah is a proper name."))
        assertTrue(local.footnotes.contains("[2]- Ar-Rahman and ar-Raheem are two names."))
    }

    @Test
    fun `a marker with no note is dropped and the sentence closes over it`() {
        val notes = saheehNotes(
            "[401]- That no one can see.\n" +
                "[402]- It crumbled to dust.",
        )
        val local = localizeSaheeh(
            "He said, \"You will not see Me,[401] but look at the mountain.[402] " +
                "And I am the first of the believers.[403]\"",
            notes,
        )
        assertEquals(listOf(403), local.dropped)
        assertEquals(2, local.notes)
        assertTrue("the dead marker must not be drawn", !local.text.contains("403"))
        assertTrue("the sentence must not lose its period", local.text.endsWith("believers.\""))
        assertEquals("[1]", local.text.substringAfter("Me,").substringBefore(" but"))
        assertEquals("[2]", local.text.substringAfter("mountain.").substringBefore(" And"))
    }

    @Test
    fun `a note with no marker is reported`() {
        val notes = saheehNotes("[1]- The first.\n[2]- The second.")
        val local = localizeSaheeh("Only the first.[1]", notes)
        assertEquals(listOf(2), local.unused)
        assertEquals(1, local.notes)
    }

    @Test
    fun `a repeated marker keeps its first number`() {
        val notes = saheehNotes("[7]- One body.")
        val local = localizeSaheeh("One,[7] and again.[7]", notes)
        assertEquals("One,[1] and again.[1]", local.text)
        assertEquals("[1]- One body.", local.footnotes)
    }

    @Test
    fun `markers are numbered in the order the sentence carries them`() {
        val notes = saheehNotes("[403]- Third.\n[401]- First.")
        val local = localizeSaheeh("First,[401] third,[403]", notes)
        assertEquals("First,[1] third,[2]", local.text)
        assertEquals("[1]- First. [2]- Third.", local.footnotes)
    }

    @Test
    fun `3-32 is corrected to the published reading`() {
        val text = "Say, \"Obey Allāh and the Messenger. But if you turn away - " +
            "then indeed, Allāh does not like the disbelievers.\""
        assertEquals(
            "Say, \"Obey Allāh and the Messenger. But if they turn away - " +
                "then indeed, Allāh does not like the disbelievers.\"",
            saheehCorrected(325, text),
        )
    }

    @Test
    fun `the other second-person turnings stay as they are`() {
        val text = "But if you turn away, then I have already conveyed that with which I was sent to you."
        assertEquals(text, saheehCorrected(111, text))
        assertEquals(text, saheehCorrected(325 + 1, text))
    }

    @Test
    fun `a source that no longer holds the corrected sentence fails`() {
        val failure = runCatching { saheehCorrected(325, "A different sentence.") }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure?.message.orEmpty().contains("3:32"))
    }
}
