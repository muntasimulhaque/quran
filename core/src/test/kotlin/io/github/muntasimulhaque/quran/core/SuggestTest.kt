package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The suggester's arithmetic, apart from the database that feeds it.
 *
 * Every case here is a shape the sheet can actually meet: a finger landing
 * early, a word one letter short, a lexicon whose two nearest words are
 * equally common. The last one is the reason this file exists: a search that
 * answered differently on two runs would be a defect nobody could reproduce.
 */
class SuggestTest {

    private fun lexicon(vararg words: Pair<String, Int>): Suggest.Lexicon =
        Suggest.Lexicon(words.toMap())

    @Test
    fun `a swapped pair is one step away`() {
        val candidate = Suggest.suggest(listOf("mrecy"), lexicon("mercy" to 100))
        assertEquals("mrecy", candidate?.culprit)
        assertEquals("mercy", candidate?.word)
        assertEquals(1, candidate?.distance)
        assertEquals(1, Suggest.damerau("mrecy", "mercy", 2))
    }

    @Test
    fun `distance two needs five letters`() {
        // Three and four letters earn one edit; five and up earn two.
        assertNull(Suggest.suggest(listOf("mry"), lexicon("mercy" to 100)))
        assertEquals("mercy", Suggest.suggest(listOf("marci"), lexicon("mercy" to 100))?.word)
        assertTrue(Suggest.damerau("abcd", "wxyz", 1) > 1)
        assertEquals(3, Suggest.damerau("abc", "xyz", 3))
    }

    @Test
    fun `a word the lexicon holds is not the culprit`() {
        assertNull(Suggest.suggest(listOf("mercy"), lexicon("mercy" to 100)))
    }

    @Test
    fun `a punctuated term is never corrected`() {
        assertNull(Suggest.suggest(listOf("god's"), lexicon("god" to 100)))
    }

    @Test
    fun `a one letter term is never fixed`() {
        assertNull(Suggest.suggest(listOf("me"), lexicon("mercy" to 100)))
    }

    @Test
    fun `the likelier word wins`() {
        val candidate = Suggest.suggest(
            listOf("merc"),
            Suggest.Lexicon(mapOf("mercu" to 3, "mercy" to 100)),
        )
        assertEquals("mercy", candidate?.word)
        assertEquals(1, candidate?.distance)
    }

    @Test
    fun `the best fix is deterministic`() {
        // Equal distance, equal count, equal length: the alphabet decides, and
        // it decides the same way on every run.
        val counts = mapOf("mercy" to 7, "merci" to 7, "merca" to 7)
        val first = Suggest.suggest(listOf("merce"), Suggest.Lexicon(counts))
        val second = Suggest.suggest(listOf("merce"), Suggest.Lexicon(counts))
        assertEquals(first, second)
        assertEquals("merca", first?.word)
        assertEquals(1, first?.distance)
    }

    @Test
    fun `a correction rewrites the reader's own spelling`() {
        assertEquals(
            "mercy of God",
            Suggest.correction("Mrecy of God", "mrecy", "mercy", arabic = false),
        )
        assertEquals(
            "God's mercy",
            Suggest.correction("God's merci", "merci", "mercy", arabic = false),
        )
    }

    @Test
    fun `a word that cannot be located offers nothing`() {
        assertNull(Suggest.correction("mercy", "absent", "merci", arabic = false))
    }

    @Test
    fun `an arabic misspelling is one step away`() {
        val candidate = Suggest.suggest(
            listOf("\u0627\u0644\u0631\u062D\u0645\u0643"),
            lexicon("\u0627\u0644\u0631\u062D\u0645\u0646" to 57),
        )
        assertEquals("\u0627\u0644\u0631\u062D\u0645\u0646", candidate?.word)
        assertEquals(1, candidate?.distance)
    }

    @Test
    fun `an empty lexicon suggests nothing`() {
        assertNull(Suggest.suggest(listOf("mrecy"), Suggest.Lexicon(emptyMap())))
    }
}
