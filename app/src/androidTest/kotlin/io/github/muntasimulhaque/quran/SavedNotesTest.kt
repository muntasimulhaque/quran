package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.SavedStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * A note is written on a kept ayah, so it lives in Saved, previewed under its
 * place: one list for the reader's own work, and the note is recognised by
 * what the reader wrote rather than by the place alone (owner decision).
 * Removing a save that carries a note asks first, because the note
 * goes with it. This test writes a note on 2:255 before the activity starts,
 * reads it back in the list, and walks the removal through its question.
 */
@RunWith(AndroidJUnit4::class)
class SavedNotesTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                }
                SavedStore(context).apply {
                    setNote(AYAT_AL_KURSI, NOTE)
                    close()
                }
            }
        }

        override fun after() {
            runBlocking {
                SavedStore(context).apply {
                    unsave(AYAT_AL_KURSI)
                    close()
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun aNoteIsReadableInTheSavedList() {
        openSaved()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText(NOTE).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun removingASaveWithANoteAsksFirst() {
        openSaved()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText(NOTE).fetchSemanticsNodes().isNotEmpty()
        }
        // The row's Remove opens the question, and the question's own Remove
        // takes the save and the note with it.
        compose.onNodeWithText("Remove").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("remove-saved-confirm", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("remove-saved-confirm").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText(NOTE).fetchSemanticsNodes().isEmpty()
        }
    }

    /**
     * The ayah stands under its surah on the step Browse's Surahs and Juz rows
     * keep between a name and its own label, and the note below them keeps the
     * row's own line gap.
     *
     * A place row used to stand every line under its name 8 dp apart, which
     * unified the row and put the ayah further from its surah than the same
     * pair sits one tab away in Surahs: there the step is 4 dp (owner report,
     * this session). The row now reads as a name and its label, and then the
     * row's own line about it.
     */
    @Test
    fun theAyahStandsUnderItsNameOnTheBrowsRowGap() {
        openSaved()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText(NOTE).fetchSemanticsNodes().isNotEmpty()
        }
        // Unmerged: the row is one node for the finger and for TalkBack, so
        // the merged tree hands back the row itself for every line in it and
        // only the unmerged tree can be asked where a line stands.
        val name = compose.onNodeWithText(SURAH_NAME, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val ayah = compose.onNodeWithText(AYAH_LABEL, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val note = compose.onNodeWithText(NOTE, useUnmergedTree = true).getUnclippedBoundsInRoot()

        val step = (ayah.top - name.bottom).value
        assertEquals(
            "the ayah is the surah's own label and stands ${Space.Tight} under it",
            Space.Tight.value,
            step,
            0.5f,
        )
        assertTrue(
            "the reader's note is the row's own line and keeps the row's line gap",
            (note.top - ayah.bottom).value > Space.Line.value,
        )
    }

    private fun openSaved() {
        openBrowse()
        compose.onNodeWithText("Saved").performClick()
        compose.waitForIdle()
    }

    private fun openBrowse() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        // The chrome comes up on a tap on the paper; the reading below it
        // keeps its own gestures, so the tap is the same one a reader makes.
        compose.onAllNodesWithContentDescription(readingPage).onFirst()
            .tapThePaper()
        val browse = compose.activity.getString(R.string.action_browse)
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription(browse).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(browse).performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("browse-sheet", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private companion object {
        /** The global ayah number of 2:255, the ayah the note is written on. */
        const val AYAT_AL_KURSI = 7 + 255

        const val NOTE = "Ayat Al-Kursi, the greatest ayah"

        /** Ayat Al-Kursi's surah, and the label the row draws under its name. */
        const val SURAH_NAME = "Al-Baqarah"
        const val AYAH_LABEL = "Ayah 255"
    }
}
