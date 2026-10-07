package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.ui.settings.ListeningPage
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Assert.assertEquals
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
}
