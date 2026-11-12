package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The grammar codes, pinned where both sides of the wire can read them: the
 * build writes one of these codes into the word pack and the sheet looks it
 * up in the reader's language. A code that does not round-trip is a label the
 * sheet cannot say, so the test is the contract, not a sample of it.
 */
class WordGrammarTest {

    @Test
    fun everyPartOfSpeechRoundTripsThroughItsCode() {
        for (value in PartOfSpeech.entries) {
            assertEquals(value, PartOfSpeech.ofOrNull(value.code))
        }
        assertEquals(
            "two parts of speech share a code",
            PartOfSpeech.entries.size,
            PartOfSpeech.entries.map { it.code }.toSet().size,
        )
    }

    @Test
    fun everyCaseRoundTripsThroughItsCode() {
        for (value in GrammaticalCase.entries) {
            assertEquals(value, GrammaticalCase.ofOrNull(value.code))
        }
        assertEquals(
            "two cases share a code",
            GrammaticalCase.entries.size,
            GrammaticalCase.entries.map { it.code }.toSet().size,
        )
    }

    @Test
    fun everyFormRoundTripsThroughItsCode() {
        for (value in WordForm.entries) {
            assertEquals(value, WordForm.ofOrNull(value.code))
        }
        assertEquals(
            "two forms share a code",
            WordForm.entries.size,
            WordForm.entries.map { it.code }.toSet().size,
        )
    }

    @Test
    fun aCodeTheAppDoesNotKnowIsNothing() {
        assertNull(PartOfSpeech.ofOrNull("a_label_that_is_not_ours"))
        assertNull(PartOfSpeech.ofOrNull(null))
        assertNull(GrammaticalCase.ofOrNull(""))
        assertNull(WordForm.ofOrNull("dual"))
    }
}
