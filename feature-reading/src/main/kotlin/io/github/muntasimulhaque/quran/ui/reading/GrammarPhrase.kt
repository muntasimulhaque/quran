package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.core.GrammaticalCase
import io.github.muntasimulhaque.quran.core.PartOfSpeech
import io.github.muntasimulhaque.quran.core.WordForm
import io.github.muntasimulhaque.quran.core.WordGrammar
import io.github.muntasimulhaque.quran.feature.reading.R

/**
 * One word's grammar as the sheet says it: what the word is, its case or
 * mood, its gender and number, its voice, and a verb's pattern, in the
 * reader's own language and in the order a grammar reads left to right.
 *
 * The codes come from the corpus through the word pack, and the words are
 * the app's own: they are the settled names of Arabic grammar in English
 * and in Bangla, not a translation of scripture (owner decision, this
 * session).
 */
@Composable
internal fun grammarPhrase(grammar: WordGrammar): String? {
    val parts = buildList {
        grammar.partOfSpeech?.let { add(stringResource(partOfSpeechLabel(it))) }
        grammar.grammaticalCase?.let { add(stringResource(grammaticalCaseLabel(it))) }
        grammar.form?.let { add(stringResource(wordFormLabel(it))) }
        if (grammar.passive) add(stringResource(R.string.reading_grammar_passive))
        grammar.pattern?.let { add(stringResource(R.string.reading_grammar_pattern, it)) }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
}

private fun partOfSpeechLabel(value: PartOfSpeech): Int = when (value) {
    PartOfSpeech.NOUN -> R.string.reading_grammar_pos_noun
    PartOfSpeech.PAST_VERB -> R.string.reading_grammar_pos_past_verb
    PartOfSpeech.PRESENT_VERB -> R.string.reading_grammar_pos_present_verb
    PartOfSpeech.IMPERATIVE_VERB -> R.string.reading_grammar_pos_imperative_verb
    PartOfSpeech.REQUEST_VERB -> R.string.reading_grammar_pos_request_verb
    PartOfSpeech.PREPOSITION -> R.string.reading_grammar_pos_preposition
    PartOfSpeech.NEGATIVE_PARTICLE -> R.string.reading_grammar_pos_negative_particle
    PartOfSpeech.ACCUSATIVE_PARTICLE -> R.string.reading_grammar_pos_accusative_particle
    PartOfSpeech.PROHIBITIVE_PARTICLE -> R.string.reading_grammar_pos_prohibitive_particle
    PartOfSpeech.CONJUNCTION -> R.string.reading_grammar_pos_conjunction
    PartOfSpeech.SUBORDINATING_PARTICLE -> R.string.reading_grammar_pos_subordinating_particle
    PartOfSpeech.CERTAINTY_PARTICLE -> R.string.reading_grammar_pos_certainty_particle
    PartOfSpeech.RECTIFICATION_PARTICLE -> R.string.reading_grammar_pos_rectification_particle
    PartOfSpeech.DETAIL_PARTICLE -> R.string.reading_grammar_pos_detail_particle
    PartOfSpeech.CORRECTION_PARTICLE -> R.string.reading_grammar_pos_correction_particle
    PartOfSpeech.ANSWER_PARTICLE -> R.string.reading_grammar_pos_answer_particle
    PartOfSpeech.OPENING_PARTICLE -> R.string.reading_grammar_pos_opening_particle
    PartOfSpeech.EXPLANATION_PARTICLE -> R.string.reading_grammar_pos_explanation_particle
    PartOfSpeech.FUTURE_PARTICLE -> R.string.reading_grammar_pos_future_particle
    PartOfSpeech.URGING_PARTICLE -> R.string.reading_grammar_pos_urging_particle
    PartOfSpeech.SURPRISE_PARTICLE -> R.string.reading_grammar_pos_surprise_particle
    PartOfSpeech.ATTENTION_PARTICLE -> R.string.reading_grammar_pos_attention_particle
    PartOfSpeech.DETERRENCE_PARTICLE -> R.string.reading_grammar_pos_deterrence_particle
    PartOfSpeech.CONNECTIVE_PARTICLE -> R.string.reading_grammar_pos_connective_particle
    PartOfSpeech.INVOCATION_PARTICLE -> R.string.reading_grammar_pos_invocation_particle
    PartOfSpeech.RELATIVE_NOUN -> R.string.reading_grammar_pos_relative_noun
    PartOfSpeech.PRONOUN -> R.string.reading_grammar_pos_pronoun
    PartOfSpeech.ACTIVE_PARTICIPLE -> R.string.reading_grammar_pos_active_participle
    PartOfSpeech.ALLAH -> R.string.reading_grammar_pos_allah
    PartOfSpeech.PROPER_NOUN -> R.string.reading_grammar_pos_proper_noun
    PartOfSpeech.TIME_ADVERB -> R.string.reading_grammar_pos_time_adverb
    PartOfSpeech.DEMONSTRATIVE -> R.string.reading_grammar_pos_demonstrative
    PartOfSpeech.CONDITIONAL_PARTICLE -> R.string.reading_grammar_pos_conditional_particle
    PartOfSpeech.PLACE_ADVERB -> R.string.reading_grammar_pos_place_adverb
    PartOfSpeech.VERBAL_NOUN -> R.string.reading_grammar_pos_verbal_noun
    PartOfSpeech.RESTRICTION_PARTICLE -> R.string.reading_grammar_pos_restriction_particle
    PartOfSpeech.PASSIVE_PARTICIPLE -> R.string.reading_grammar_pos_passive_participle
    PartOfSpeech.INTERROGATIVE_PARTICLE -> R.string.reading_grammar_pos_interrogative_particle
    PartOfSpeech.DISJOINT_LETTERS -> R.string.reading_grammar_pos_disjoint_letters
    PartOfSpeech.VERB_NOUN -> R.string.reading_grammar_pos_verb_noun
    PartOfSpeech.EXCEPTION_PARTICLE -> R.string.reading_grammar_pos_exception_particle
}

private fun grammaticalCaseLabel(value: GrammaticalCase): Int = when (value) {
    GrammaticalCase.NOMINATIVE -> R.string.reading_grammar_case_nominative
    GrammaticalCase.ACCUSATIVE -> R.string.reading_grammar_case_accusative
    GrammaticalCase.GENITIVE -> R.string.reading_grammar_case_genitive
    GrammaticalCase.JUSSIVE -> R.string.reading_grammar_case_jussive
}

private fun wordFormLabel(value: WordForm): Int = when (value) {
    WordForm.MASCULINE -> R.string.reading_grammar_form_masculine
    WordForm.FEMININE -> R.string.reading_grammar_form_feminine
    WordForm.MASCULINE_SINGULAR -> R.string.reading_grammar_form_masculine_singular
    WordForm.FEMININE_SINGULAR -> R.string.reading_grammar_form_feminine_singular
    WordForm.MASCULINE_DUAL -> R.string.reading_grammar_form_masculine_dual
    WordForm.FEMININE_DUAL -> R.string.reading_grammar_form_feminine_dual
    WordForm.MASCULINE_PLURAL -> R.string.reading_grammar_form_masculine_plural
    WordForm.FEMININE_PLURAL -> R.string.reading_grammar_form_feminine_plural
    WordForm.PLURAL -> R.string.reading_grammar_form_plural
}
