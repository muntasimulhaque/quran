package io.github.muntasimulhaque.quran.tools

import io.github.muntasimulhaque.quran.core.GrammaticalCase
import io.github.muntasimulhaque.quran.core.PartOfSpeech
import io.github.muntasimulhaque.quran.core.WordForm

/**
 * The nine rows the 2026-11-14 morphology export leaves in the corpus's raw
 * tag language, and the fuller labels those rows carried before.
 *
 * Quranpedia's export is the same Quranic Arabic Corpus data, and its lemma
 * set, its forms, and its 77,429 rows are the ones the committed database was
 * built from. Two verses, 70:1 and 78:30, are the exception: their inline
 * translation was not written in this export, so the part of speech, the case,
 * the gender, and the verb's pattern stand as the corpus's own tags
 * (`morphology.tags.*`, `morphology.cases.*`) and as its pattern code `o`.
 * Reading those tags alone would call two active participles nouns and two
 * particles prepositions, and it would put the code `o` where the sheet shows
 * a verb's pattern.
 *
 * The values here are the export's own previous labels for those nine words,
 * which the committed database was built from. They are a source erratum, not
 * a hand edit of the data: the build reads the new export, and where the
 * export's inline translation is missing, it reads the values the corpus
 * itself gave the same words before (owner decision, this session). The
 * database the app ships is expected to rebuild to the same SHA-256 as the
 * committed one; a difference is a change in the source's data rather than in
 * the shape of this table, and it stops the content build for an audit.
 */
internal data class GrammarErratum(
    val partOfSpeech: PartOfSpeech,
    val grammaticalCase: GrammaticalCase?,
    val form: WordForm?,
    val pattern: String?,
)

/** The errata by the source's own location, surah:ayah:position. */
internal object GrammarErrata {

    operator fun get(key: String): GrammarErratum? = byKey[key]

    private val byKey: Map<String, GrammarErratum> = mapOf(
        // Al-Ma'arij 70:1.
        "70:1:1" to GrammarErratum(
            partOfSpeech = PartOfSpeech.PAST_VERB,
            grammaticalCase = null,
            form = null,
            pattern = "فَعَلَ",
        ),
        "70:1:2" to GrammarErratum(
            partOfSpeech = PartOfSpeech.ACTIVE_PARTICIPLE,
            grammaticalCase = GrammaticalCase.NOMINATIVE,
            form = WordForm.MASCULINE,
            pattern = null,
        ),
        "70:1:3" to GrammarErratum(
            partOfSpeech = PartOfSpeech.NOUN,
            grammaticalCase = GrammaticalCase.GENITIVE,
            form = WordForm.MASCULINE,
            pattern = null,
        ),
        "70:1:4" to GrammarErratum(
            partOfSpeech = PartOfSpeech.ACTIVE_PARTICIPLE,
            grammaticalCase = GrammaticalCase.GENITIVE,
            form = WordForm.MASCULINE,
            pattern = null,
        ),
        // An-Naba 78:30.
        "78:30:1" to GrammarErratum(
            partOfSpeech = PartOfSpeech.IMPERATIVE_VERB,
            grammaticalCase = null,
            form = null,
            pattern = "فَعَلَ",
        ),
        "78:30:2" to GrammarErratum(
            partOfSpeech = PartOfSpeech.NEGATIVE_PARTICLE,
            grammaticalCase = null,
            form = null,
            pattern = null,
        ),
        "78:30:3" to GrammarErratum(
            partOfSpeech = PartOfSpeech.PRESENT_VERB,
            grammaticalCase = GrammaticalCase.ACCUSATIVE,
            form = null,
            pattern = "فَعَلَ",
        ),
        "78:30:4" to GrammarErratum(
            partOfSpeech = PartOfSpeech.RESTRICTION_PARTICLE,
            grammaticalCase = null,
            form = null,
            pattern = null,
        ),
        "78:30:5" to GrammarErratum(
            partOfSpeech = PartOfSpeech.NOUN,
            grammaticalCase = GrammaticalCase.ACCUSATIVE,
            form = WordForm.MASCULINE,
            pattern = null,
        ),
    )
}
