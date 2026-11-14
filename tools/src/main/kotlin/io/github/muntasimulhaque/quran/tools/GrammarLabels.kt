package io.github.muntasimulhaque.quran.tools

import io.github.muntasimulhaque.quran.core.GrammaticalCase
import io.github.muntasimulhaque.quran.core.PartOfSpeech
import io.github.muntasimulhaque.quran.core.WordForm

/**
 * The corpus's Arabic labels, read as the app's grammar codes.
 *
 * The morphology export names a word's part of speech, its case or mood, and
 * its gender and number in Arabic, and the app draws them in the reader's
 * language. The translation from one to the other happens here, once, in the
 * pipeline, so the app never carries an Arabic label it has to match and a
 * label the build does not know is a build failure rather than a silent
 * blank line on the sheet (owner decision, this session).
 *
 * Only what the sheet shows is mapped. The export's other qualifiers describe
 * the word's place in its sentence (a pronoun's person, a noun's role as an
 * adjective, a verb's family), and the sheet is one short line about the word
 * itself, so they are left where they are. The counts beside each label are
 * from the current export and are there to show how much of the Book a label
 * covers (owner decision, this session).
 *
 * The 2026-11-14 export leaves nine rows' inline labels in the corpus's own
 * tag language, the same meaning under a different name. Those tags are read
 * here as the parts they name; where a tag is coarser than the Arabic label it
 * replaced (a noun that is a participle, a preposition that is a particle) the
 * build's GrammarErrata restores the fuller value the same word carried before
 * (owner decision, this session).
 */
object GrammarLabels {

    /** The verb's passive voice, one qualifier of the thirty the export carries. */
    const val PASSIVE = "مبني لما لم يُسمَّ فاعلُه"

    val partOfSpeech: Map<String, PartOfSpeech> = mapOf(
        "اسم" to PartOfSpeech.NOUN, // 22940
        "فعل ماضٍ" to PartOfSpeech.PAST_VERB, // 9151
        "فعل مضارع" to PartOfSpeech.PRESENT_VERB, // 8329
        "حرف جر" to PartOfSpeech.PREPOSITION, // 7673
        "اسم موصول" to PartOfSpeech.RELATIVE_NOUN, // 3340
        "ضمير منفصل" to PartOfSpeech.PRONOUN, // 3301
        "اسم فاعل" to PartOfSpeech.ACTIVE_PARTICIPLE, // 2956
        "لفظ الجلالة" to PartOfSpeech.ALLAH, // 2699
        "حرف نفي" to PartOfSpeech.NEGATIVE_PARTICLE, // 2645
        "حرف نصب" to PartOfSpeech.ACCUSATIVE_PARTICLE, // 2290
        "فعل أمر" to PartOfSpeech.IMPERATIVE_VERB, // 1759
        "اسم علم" to PartOfSpeech.PROPER_NOUN, // 1212
        "ظرف زمان" to PartOfSpeech.TIME_ADVERB, // 1142
        "اسم إشارة" to PartOfSpeech.DEMONSTRATIVE, // 1062
        "أداة شرط" to PartOfSpeech.CONDITIONAL_PARTICLE, // 1028
        "حرف عطف" to PartOfSpeech.CONJUNCTION, // 756
        "ظرف مكان" to PartOfSpeech.PLACE_ADVERB, // 688
        "مصدر" to PartOfSpeech.VERBAL_NOUN, // 668
        "حرف مصدري" to PartOfSpeech.SUBORDINATING_PARTICLE, // 667
        "أداة حصر" to PartOfSpeech.RESTRICTION_PARTICLE, // 557
        "اسم مفعول" to PartOfSpeech.PASSIVE_PARTICIPLE, // 550
        "حرف تحقيق" to PartOfSpeech.CERTAINTY_PARTICLE, // 415
        "أداة استفهام" to PartOfSpeech.INTERROGATIVE_PARTICLE, // 389
        "حرف نهي" to PartOfSpeech.PROHIBITIVE_PARTICLE, // 316
        "حرف إضراب" to PartOfSpeech.RECTIFICATION_PARTICLE, // 122
        "فعل طلبٍ ودعاء" to PartOfSpeech.REQUEST_VERB, // 114
        "أداة استثناء" to PartOfSpeech.EXCEPTION_PARTICLE, // 105
        "حرف تفصيل" to PartOfSpeech.DETAIL_PARTICLE, // 69
        "حرف استدراك" to PartOfSpeech.CORRECTION_PARTICLE, // 65
        "حرف جواب" to PartOfSpeech.ANSWER_PARTICLE, // 58
        "حرف ابتداء" to PartOfSpeech.OPENING_PARTICLE, // 51
        "حرف تفسير" to PartOfSpeech.EXPLANATION_PARTICLE, // 47
        "حرف استقبال" to PartOfSpeech.FUTURE_PARTICLE, // 42
        "حرف تحضيض" to PartOfSpeech.URGING_PARTICLE, // 41
        "حرف فجاءة" to PartOfSpeech.SURPRISE_PARTICLE, // 40
        "حرف تنبيه" to PartOfSpeech.ATTENTION_PARTICLE, // 39
        "حرف ردع" to PartOfSpeech.DETERRENCE_PARTICLE, // 33
        "حروف مقطعة" to PartOfSpeech.DISJOINT_LETTERS, // 30
        "حرف صلة" to PartOfSpeech.CONNECTIVE_PARTICLE, // 15
        "اسم فعل" to PartOfSpeech.VERB_NOUN, // 13
        "حرف دعاء" to PartOfSpeech.INVOCATION_PARTICLE, // 12
        // The 2026-11-14 export's raw tags on the nine rows at 70:1 and
        // 78:30. The tag names the broader part; GrammarErrata restores the
        // finer label the same word carried in the previous export (owner
        // decision, this session).
        "morphology.tags.N" to PartOfSpeech.NOUN,
        "morphology.tags.P" to PartOfSpeech.PREPOSITION,
        "morphology.tags.V morphology.tenses.PERF" to PartOfSpeech.PAST_VERB,
        "morphology.tags.V morphology.tenses.IMPF" to PartOfSpeech.PRESENT_VERB,
        "morphology.tags.V morphology.tenses.IMPV" to PartOfSpeech.IMPERATIVE_VERB,
    )

    val cases: Map<String, GrammaticalCase> = mapOf(
        "مرفوع" to GrammaticalCase.NOMINATIVE, // 14388
        "مجرور" to GrammaticalCase.GENITIVE, // 12625
        "منصوب" to GrammaticalCase.ACCUSATIVE, // 11860
        "مجزوم" to GrammaticalCase.JUSSIVE, // 1419
        "morphology.cases.NOM" to GrammaticalCase.NOMINATIVE,
        "morphology.cases.GEN" to GrammaticalCase.GENITIVE,
        "morphology.cases.ACC" to GrammaticalCase.ACCUSATIVE,
        "morphology.moods.SUBJ" to GrammaticalCase.ACCUSATIVE,
    )

    /**
     * Gender and number, from the qualifiers that name both. A qualifier that
     * names only one of them is used when the pair is not there, so a word is
     * never left without the part the sheet can state.
     */
    val forms: Map<String, WordForm> = mapOf(
        "مذكر" to WordForm.MASCULINE, // 13195
        "جمع مذكر" to WordForm.MASCULINE_PLURAL, // 5998
        "مذكر مفرد" to WordForm.MASCULINE_SINGULAR, // 4028
        "مؤنث" to WordForm.FEMININE, // 3102
        "جمع مؤنث" to WordForm.FEMININE_PLURAL, // 1637
        "مؤنث مفرد" to WordForm.FEMININE_SINGULAR, // 982
        "جمع" to WordForm.PLURAL, // 390
        "مثنى مذكر" to WordForm.MASCULINE_DUAL, // 149
        "مثنى مؤنث" to WordForm.FEMININE_DUAL, // 94
    )
}
