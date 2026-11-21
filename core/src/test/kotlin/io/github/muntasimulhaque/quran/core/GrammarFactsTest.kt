package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The facts a word's grammar line is built from, and the order a grammar
 * reads them in.
 *
 * The order is the rule: what the word is, its case or mood, its gender and
 * number, the passive voice, and a verb's pattern last. The words the facts
 * are drawn with are the settings module's, so this file pins the list and
 * never a string (owner decision, 21.5, moving 18.0 into the gate).
 */
class GrammarFactsTest {

    @Test
    fun `the parts of a grammar read in the grammar's own order`() {
        val facts = grammarFacts(
            WordGrammar(
                lemma = "رحم",
                partOfSpeech = PartOfSpeech.PAST_VERB,
                grammaticalCase = GrammaticalCase.NOMINATIVE,
                form = WordForm.MASCULINE_SINGULAR,
                passive = true,
                pattern = "فَعَلَ",
                lemmaWords = 12,
                lemmaForms = 4,
            ),
        )
        assertEquals(
            listOf(
                GrammarFact.PartOfSpeech(PartOfSpeech.PAST_VERB),
                GrammarFact.Case(GrammaticalCase.NOMINATIVE),
                GrammarFact.Form(WordForm.MASCULINE_SINGULAR),
                GrammarFact.Passive,
                GrammarFact.Pattern("فَعَلَ"),
            ),
            facts,
        )
    }

    @Test
    fun `an empty grammar is no line`() {
        val facts = grammarFacts(
            WordGrammar(
                lemma = null,
                partOfSpeech = null,
                grammaticalCase = null,
                form = null,
                passive = false,
                pattern = null,
                lemmaWords = 0,
                lemmaForms = 0,
            ),
        )
        assertTrue(facts.isEmpty())
    }

    @Test
    fun `the passive is a fact of its own even alone`() {
        val facts = grammarFacts(
            WordGrammar(
                lemma = null,
                partOfSpeech = null,
                grammaticalCase = null,
                form = null,
                passive = true,
                pattern = null,
                lemmaWords = 0,
                lemmaForms = 0,
            ),
        )
        assertEquals(listOf(GrammarFact.Passive), facts)
    }

    @Test
    fun `a pattern stands last`() {
        val facts = grammarFacts(
            WordGrammar(
                lemma = null,
                partOfSpeech = null,
                grammaticalCase = null,
                form = null,
                passive = false,
                pattern = "فَاعِل",
                lemmaWords = 0,
                lemmaForms = 0,
            ),
        )
        assertEquals(listOf(GrammarFact.Pattern("فَاعِل")), facts)
    }
}
