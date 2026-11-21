package io.github.muntasimulhaque.quran.core

/**
 * One fact of a word's grammar line: what the word is, its case or mood, its
 * gender and number, its voice, and a verb's pattern.
 *
 * The facts and their order are decided here, so the JVM suite can pin them
 * without a window; the words they are drawn with stay in the module that
 * draws them, because they are the reader's own language's. The codes come
 * from the corpus through the word pack, and the words are the app's own:
 * they are the settled names of Arabic grammar in English and in Bangla, not
 * a translation of scripture (owner decision, 18.0).
 *
 * The parts are separated by the app's middot where they are joined, which is
 * the drawing module's business; this file only says which facts stand, and
 * in which order.
 */
sealed interface GrammarFact {

    /**
     * What the word is grammatically. The enum lives in this package; the
     * fact's own property names it in full so the fact's name does not hide
     * the type.
     */
    data class PartOfSpeech(
        val value: io.github.muntasimulhaque.quran.core.PartOfSpeech,
    ) : GrammarFact

    /** The word's case, or the mood of its verb. */
    data class Case(val value: GrammaticalCase) : GrammarFact

    /** The word's gender and number, when the corpus names both. */
    data class Form(val value: WordForm) : GrammarFact

    /** The passive voice, which the corpus marks on its own. */
    data object Passive : GrammarFact

    /** A verb's pattern, as the Arabic letters themselves. */
    data class Pattern(val text: String) : GrammarFact
}

/**
 * The facts of one grammar, in the order a grammar reads left to right: what
 * the word is, its case or mood, its gender and number, its voice, and a
 * verb's pattern last. A word the corpus gives nothing is no line at all.
 *
 * A pattern is not a code: it is the Arabic pattern itself, which is what a
 * reader of Arabic grammar already reads (owner decision, 18.0).
 */
fun grammarFacts(grammar: WordGrammar): List<GrammarFact> = buildList {
    grammar.partOfSpeech?.let { add(GrammarFact.PartOfSpeech(it)) }
    grammar.grammaticalCase?.let { add(GrammarFact.Case(it)) }
    grammar.form?.let { add(GrammarFact.Form(it)) }
    if (grammar.passive) add(GrammarFact.Passive)
    grammar.pattern?.let { add(GrammarFact.Pattern(it)) }
}
