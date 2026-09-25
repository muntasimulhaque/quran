package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.SavedStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.study.R as StudyR
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Notes are recognised by what the reader wrote, not by the place alone.
 *
 * Saved and Notes drew the same row (a surah, an ayah, when), so a reader
 * looking for the note they took on Ayat Al-Kursi had to open the notes one
 * by one. Notes now previews the reader's own words under the place, two
 * lines at most; this test writes a note on 2:255 before the activity starts
 * and reads it back in the list.
 */
@RunWith(AndroidJUnit4::class)
class NotesPreviewTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setMode(ReadingMode.Study)
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
                    clearNote(AYAT_AL_KURSI)
                    close()
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun aNoteIsReadableInTheList() {
        openBrowse()
        compose.onNodeWithText("Notes").performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText(NOTE).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openBrowse() {
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(studyPage).fetchSemanticsNodes().isNotEmpty()
        }
        // The chrome comes up on a tap on the paper; the reading below it
        // keeps its own gestures, so the tap is the same one a reader makes.
        compose.onAllNodesWithContentDescription(studyPage).onFirst()
            .performTouchInput { click(center) }
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
    }
}
