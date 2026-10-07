package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.ui.settings.ListeningPage
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Listening page carries the pace and the one answer list.
 *
 * The answers are one list with one radio mark (owner decision, 4.5), so each
 * row reports exactly the answer it names, and the page draws the value it
 * was given with one row chosen and the rest not. The exclusivity itself is
 * the plan in `core`, which the JVM suite pins.
 */
@RunWith(AndroidJUnit4::class)
class ListeningSettingsTest {

    @get:Rule
    val compose = createComposeRule()

    private fun showPage(
        end: EndOfAudio = EndOfAudio.CONTINUE_AYAH,
        onEndOfAudio: (EndOfAudio) -> Unit = {},
    ) {
        compose.setContent {
            QuranTheme(theme = AppTheme.Paper) {
                ListeningPage(
                    settings = AppSettings(endOfAudio = end),
                    onSpeed = {},
                    onEndOfAudio = onEndOfAudio,
                )
            }
        }
    }

    @Test
    fun eachRowReportsItsOwnAnswer() {
        val reported = ArrayList<EndOfAudio>()
        showPage { reported += it }
        compose.onNodeWithText("Stop after this ayah").performClick()
        compose.onNodeWithText("Repeat the ayah").performClick()
        compose.onNodeWithText("Continue to the next surah").performClick()
        assertEquals(
            listOf(
                EndOfAudio.STOP_AFTER_AYAH,
                EndOfAudio.REPEAT_AYAH,
                EndOfAudio.CONTINUE_SURAH,
            ),
            reported,
        )
    }

    @Test
    fun theAnswerThePageShowsIsTheOnlyOneChosen() {
        showPage(end = EndOfAudio.REPEAT_SURAH)
        compose.onNodeWithText("Repeat the surah").assertIsSelected()
        compose.onNodeWithText("Continue to the next ayah").assertIsNotSelected()
        compose.onNodeWithText("Repeat the ayah").assertIsNotSelected()
        compose.onNodeWithText("Stop after this ayah").assertIsNotSelected()
        compose.onNodeWithText("Continue to the next surah").assertIsNotSelected()
    }

    @Test
    fun theDefaultIsTheOnlyOneChosenOutOfTheBox() {
        showPage()
        compose.onNodeWithText("Continue to the next ayah").assertIsSelected()
        compose.onNodeWithText("Repeat the ayah").assertIsNotSelected()
        compose.onNodeWithText("Stop after this ayah").assertIsNotSelected()
        compose.onNodeWithText("Continue to the next surah").assertIsNotSelected()
        compose.onNodeWithText("Repeat the surah").assertIsNotSelected()
    }

    /**
     * The five answers are one list, so they keep one gap. The page and the
     * pill both drew a wider break between the ayah answers and the surah
     * answers, and the two breaks were different widths, so one setting read
     * as two groups in two shapes (owner decision, forty-eighth session).
     */
    @Test
    fun theFiveAnswersKeepOneGap() {
        showPage()
        val titles = listOf(
            "Continue to the next ayah",
            "Repeat the ayah",
            "Stop after this ayah",
            "Continue to the next surah",
            "Repeat the surah",
        )
        val tops = titles.map { compose.onNodeWithText(it).getUnclippedBoundsInRoot().top.value }
        val pitch = tops[1] - tops[0]
        for (index in 1 until tops.size) {
            assertEquals(
                "answer $index must stand one pitch under answer ${index - 1}",
                pitch,
                tops[index] - tops[index - 1],
                0.5f,
            )
        }
    }

    /**
     * The mark leads the name on every answer, and each row is a full 48 dp
     * target. The pill used to break both: its rows carried the mark at the
     * far end and stood 40 dp tall (owner decision, forty-eighth session).
     */
    @Test
    fun theMarkLeadsAndTheRowHoldsTheTarget() {
        showPage()
        compose.onAllNodesWithTag("choice-mark", useUnmergedTree = true).assertCountEquals(5)
        val mark = compose.onAllNodesWithTag("choice-mark", useUnmergedTree = true).onFirst()
            .getUnclippedBoundsInRoot()
        val name = compose.onNodeWithText("Continue to the next ayah", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "the mark must lead the name: mark ${mark.left}, name ${name.left}",
            mark.left.value < name.left.value,
        )
        compose.onNodeWithText("Continue to the next ayah").assertHeightIsAtLeast(48.dp)
    }
}
