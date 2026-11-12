package io.github.muntasimulhaque.quran.tools

import io.github.muntasimulhaque.quran.core.GrammaticalCase
import io.github.muntasimulhaque.quran.core.PartOfSpeech
import io.github.muntasimulhaque.quran.core.WordForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The corpus's labels and the app's codes, pinned as one set: every code the
 * app can draw has a label the build can read, and no two labels claim the
 * same code. A new code with no label is a build that fails on the first word
 * of it, which is exactly the sheet's blank line this test exists to prevent.
 */
class GrammarLabelsTest {

    @Test
    fun everyPartOfSpeechIsNamedByALabel() {
        assertEquals(
            PartOfSpeech.entries.toSet(),
            GrammarLabels.partOfSpeech.values.toSet(),
        )
        assertTrue(GrammarLabels.partOfSpeech.size >= PartOfSpeech.entries.size)
    }

    @Test
    fun everyCaseIsNamedByALabel() {
        assertEquals(
            GrammaticalCase.entries.toSet(),
            GrammarLabels.cases.values.toSet(),
        )
    }

    @Test
    fun everyFormIsNamedByALabel() {
        assertEquals(
            WordForm.entries.toSet(),
            GrammarLabels.forms.values.toSet(),
        )
    }

    @Test
    fun everyLabelIsReal() {
        val labels = GrammarLabels.partOfSpeech.keys +
            GrammarLabels.cases.keys +
            GrammarLabels.forms.keys +
            GrammarLabels.PASSIVE
        for (label in labels) {
            assertTrue("a label is blank", label.isNotBlank())
        }
        assertEquals(
            "a label is written twice",
            labels.size,
            labels.toSet().size,
        )
    }

    @Test
    fun thePassiveVoiceIsNotAForm() {
        assertFalse(GrammarLabels.forms.containsKey(GrammarLabels.PASSIVE))
    }
}
