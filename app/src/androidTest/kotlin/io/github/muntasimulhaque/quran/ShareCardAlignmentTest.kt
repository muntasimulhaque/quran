package io.github.muntasimulhaque.quran

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.core.TextRun
import io.github.muntasimulhaque.quran.ui.reader.ShareCard
import io.github.muntasimulhaque.quran.ui.reader.ShareCardPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The card's two axes: the labels on the center, each body of text on the
 * edge its script reads from.
 *
 * The card was one centered plaque, which put a four-line translation on a
 * ragged axis with no straight edge to return the eye to; before that, four
 * alignments met with the labels alternating too (owner report). The rule now
 * is what the reading itself does: the ayah stands at the right, because
 * Arabic is read from the right; its translation begins at the start, because
 * English and Bangla are read from the left; and the card's labels, which
 * belong to neither body, stand on the center (owner decision, this
 * session).
 *
 * The test reads the laid-out lines, not the source, so a change of alignment
 * in the card or in the paragraph it hands its runs fails here.
 */
@RunWith(AndroidJUnit4::class)
class ShareCardAlignmentTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theAyahEndsAtTheRightAndItsTranslationBeginsAtTheLeft() {
        compose.setContent { ShareCardPreview(CARD) }

        val arabic = layoutOf("قُلْ هُوَ ٱللَّهُ أَحَدٌ")
        val width = arabic.size.width.toFloat()
        assertEquals(
            "the ayah must end at the card's right edge",
            width,
            arabic.getLineRight(0),
            1f,
        )
        assertTrue(
            "a one-line ayah must not fill the line",
            arabic.getLineLeft(0) > width / 4f,
        )

        val translation = layoutOf(TRANSLATION)
        val body = translation.size.width.toFloat()
        assertEquals(
            "the translation must begin at the card's left edge",
            0f,
            translation.getLineLeft(0),
            1f,
        )
        assertTrue(
            "the translation must not run to the right edge on its first line",
            translation.getLineRight(0) < body,
        )
    }

    @Test
    fun theCardsLabelsStandOnTheCenter() {
        compose.setContent { ShareCardPreview(CARD) }

        val reference = layoutOf(CARD.reference)
        val width = reference.size.width.toFloat()
        val center = (reference.getLineLeft(0) + reference.getLineRight(0)) / 2f
        assertEquals(
            "the reference is a label of the whole card, so it stands on the center",
            width / 2f,
            center,
            1f,
        )
    }

    /** The lines the text drew, read off the node the reader would meet. */
    private fun layoutOf(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val node = compose.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
        val action = node.config[SemanticsActions.GetTextLayoutResult].action
            ?: throw AssertionError("no laid-out text for $text")
        action(results)
        return results.first()
    }

    private companion object {
        /** Al-Ikhlas 112:1, one line, so the alignment is the only variable. */
        const val TRANSLATION = "Say, \"He is Allah, the One."

        val CARD = ShareCard(
            arabic = "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
            translation = listOf(TextRun(TRANSLATION)),
            reference = "Al-Ikhlas 112:1",
            text = "plain text",
            surahArabic = "الإخلاص",
        )
    }
}
