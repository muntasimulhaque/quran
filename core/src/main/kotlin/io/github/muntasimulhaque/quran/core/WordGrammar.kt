package io.github.muntasimulhaque.quran.core

/**
 * What a word is, grammatically, in the shape the word sheet shows it.
 *
 * The codes are the contract between the build and the app: the build reads
 * the corpus's Arabic labels and writes one of these codes into the content
 * database, and the app turns the code into the reader's own language. A
 * code is written here once, in the module both sides can read, so a label
 * the build writes and a string the app looks up can never drift apart
 * (owner decision, this session).
 *
 * The corpus labels 41 parts of speech, four cases, nine gender and number
 * forms, and the passive voice; the rest of its qualifiers describe the
 * word's role in the sentence and are not shown. The verb's pattern is not a
 * code: it is the Arabic pattern itself, which is what a reader of Arabic
 * grammar already reads (owner decision, this session).
 */
enum class PartOfSpeech(val code: String) {
    NOUN("noun"),
    PAST_VERB("past_verb"),
    PRESENT_VERB("present_verb"),
    IMPERATIVE_VERB("imperative_verb"),
    REQUEST_VERB("request_verb"),
    PREPOSITION("preposition"),
    NEGATIVE_PARTICLE("negative_particle"),
    ACCUSATIVE_PARTICLE("accusative_particle"),
    PROHIBITIVE_PARTICLE("prohibitive_particle"),
    CONJUNCTION("conjunction"),
    SUBORDINATING_PARTICLE("subordinating_particle"),
    CERTAINTY_PARTICLE("certainty_particle"),
    RECTIFICATION_PARTICLE("rectification_particle"),
    DETAIL_PARTICLE("detail_particle"),
    CORRECTION_PARTICLE("correction_particle"),
    ANSWER_PARTICLE("answer_particle"),
    OPENING_PARTICLE("opening_particle"),
    EXPLANATION_PARTICLE("explanation_particle"),
    FUTURE_PARTICLE("future_particle"),
    URGING_PARTICLE("urging_particle"),
    SURPRISE_PARTICLE("surprise_particle"),
    ATTENTION_PARTICLE("attention_particle"),
    DETERRENCE_PARTICLE("deterrence_particle"),
    CONNECTIVE_PARTICLE("connective_particle"),
    INVOCATION_PARTICLE("invocation_particle"),
    RELATIVE_NOUN("relative_noun"),
    PRONOUN("pronoun"),
    ACTIVE_PARTICIPLE("active_participle"),
    ALLAH("allah"),
    PROPER_NOUN("proper_noun"),
    TIME_ADVERB("time_adverb"),
    DEMONSTRATIVE("demonstrative"),
    CONDITIONAL_PARTICLE("conditional_particle"),
    PLACE_ADVERB("place_adverb"),
    VERBAL_NOUN("verbal_noun"),
    RESTRICTION_PARTICLE("restriction_particle"),
    PASSIVE_PARTICIPLE("passive_participle"),
    INTERROGATIVE_PARTICLE("interrogative_particle"),
    DISJOINT_LETTERS("disjoint_letters"),
    VERB_NOUN("verb_noun"),
    EXCEPTION_PARTICLE("exception_particle"),
    ;

    companion object {
        fun ofOrNull(code: String?): PartOfSpeech? =
            entries.firstOrNull { it.code == code }
    }
}

/** The word's case, or the mood of its verb. */
enum class GrammaticalCase(val code: String) {
    NOMINATIVE("nominative"),
    ACCUSATIVE("accusative"),
    GENITIVE("genitive"),
    JUSSIVE("jussive"),
    ;

    companion object {
        fun ofOrNull(code: String?): GrammaticalCase? =
            entries.firstOrNull { it.code == code }
    }
}

/** The word's gender and number, when the corpus names both. */
enum class WordForm(val code: String) {
    MASCULINE("masculine"),
    FEMININE("feminine"),
    MASCULINE_SINGULAR("masculine_singular"),
    FEMININE_SINGULAR("feminine_singular"),
    MASCULINE_DUAL("masculine_dual"),
    FEMININE_DUAL("feminine_dual"),
    MASCULINE_PLURAL("masculine_plural"),
    FEMININE_PLURAL("feminine_plural"),
    PLURAL("plural"),
    ;

    companion object {
        fun ofOrNull(code: String?): WordForm? =
            entries.firstOrNull { it.code == code }
    }
}

/**
 * One word's grammar, as the sheet reads it.
 *
 * The lemma is the dictionary form and its two counts: how many words in the
 * Quran carry it, and how many different forms those words take. It is null
 * for the words the corpus gives none, a pronoun or a disjoint letter, and
 * the sheet then draws the grammar line without a lemma line. [pattern] is
 * the verb's Arabic pattern, or null for a word that is not a verb. All of it
 * comes from the word pack that opens the sheet, so an older pack on the
 * phone simply has no grammar to draw (owner decision, this session).
 */
data class WordGrammar(
    val lemma: String?,
    val partOfSpeech: PartOfSpeech?,
    val grammaticalCase: GrammaticalCase?,
    val form: WordForm?,
    val passive: Boolean,
    val pattern: String?,
    val lemmaWords: Int,
    val lemmaForms: Int,
)
