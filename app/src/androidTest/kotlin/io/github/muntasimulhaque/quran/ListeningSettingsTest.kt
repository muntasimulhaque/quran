package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
 * The Listening page carries the pace and the four answers.
 *
 * The answers are four switches over one value (owner decision), so each row
 * reports the answer it names and nothing else: the exclusivity itself is the
 * plan in `core`, which the JVM suite pins, and what this proves is that the
 * page sends that answer rather than a set of booleans that could be turned on
 * together. The default row, continuing to the next ayah, is the one switch
 * whose off is the stop, so it reports the stop rather than a choice
 * (owner decision).
 *
 * The switch is looked for in the unmerged tree, and it has to be: the row
 * itself is the door and the switch is its own control inside it, so the
 * merged tree reads one control for the pair and the switch's own tag is not
 * in it. Every test that names a switch in a row that opens a page asks for
 * the unmerged tree, and this one did not, so it was reading a node that is
 * not there (thirty-seventh session).
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
    fun theDefaultSwitchReportsTheStopWhenTurnedOff() {
        var reported: EndOfAudio? = null
        showPage { reported = it }
        compose.onNodeWithTag("switch-continue-ayah", useUnmergedTree = true).performClick()
        assertEquals(
            "turning the default off is the reader's stop",
            EndOfAudio.STOP_AFTER_AYAH,
            reported,
        )
    }

    @Test
    fun theContinuationSwitchReportsItsOwnChoice() {
        var reported: EndOfAudio? = null
        showPage { reported = it }
        compose.onNodeWithTag("switch-continue", useUnmergedTree = true).performClick()
        assertEquals(
            "the continue switch must report the choice",
            EndOfAudio.CONTINUE_SURAH,
            reported,
        )
    }

    @Test
    fun theSurahRepeatRowIsThereAndReportsItsOwnChoice() {
        var reported: EndOfAudio? = null
        showPage { reported = it }
        compose.onNodeWithText("Repeat the surah").assertExists()
        compose.onNodeWithText("Repeat the surah").performClick()
        assertEquals(
            "the surah repeat reports its own answer, and never the ayah's as well",
            EndOfAudio.REPEAT_SURAH,
            reported,
        )
    }

    @Test
    fun theAnswerThePageShowsIsTheOnlyOneChecked() {
        showPage(end = EndOfAudio.REPEAT_SURAH)
        // The page is a view of one value, so exactly one of the four rows can
        // read on. Two of them on is the state the player cannot keep, and a
        // page that drew it would be lying before the reader pressed anything.
        compose.onNodeWithText("Repeat the surah").assertIsOn()
        compose.onNodeWithText("Continue to the next ayah").assertIsOff()
        compose.onNodeWithText("Continue to the next surah").assertIsOff()
        compose.onNodeWithText("Repeat the ayah").assertIsOff()
    }

    @Test
    fun theDefaultIsTheOnlyOneCheckedOutOfTheBox() {
        showPage()
        compose.onNodeWithText("Continue to the next ayah").assertIsOn()
        compose.onNodeWithText("Continue to the next surah").assertIsOff()
        compose.onNodeWithText("Repeat the ayah").assertIsOff()
        compose.onNodeWithText("Repeat the surah").assertIsOff()
    }
}
