package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.ReadingMode
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
 * The deeper door names what is behind it. In the study reading the ayah card
 * holds only the tafsir doors, so the pill's last action reads Tafsir with
 * its scroll mark, and that is the assertion this test pins. From the Mushaf
 * the same card holds the word meanings, the translation, and the tafsirs
 * together, so the action stays More.
 *
 * The library is prepared before the activity starts, the order a reader
 * creates it in, and the two long presses are the only thing the test
 * touches.
 */
@RunWith(AndroidJUnit4::class)
class AyahActionsTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    private val library = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setMode(ReadingMode.Study)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theDeeperDoorNamesTheReadingItWasRaisedOver() {
        waitForStudy()

        // Study reading: the card behind the door is only the tafsirs.
        longPressStudyAyah()
        compose.onNodeWithContentDescription("Tafsir").assertExists()

        // A tap on the paper puts the pill away and brings the chrome back,
        // then the mode door offers the Mushaf. Nothing from the study pill
        // can survive into the next assertion: the pill is raised again by a
        // real press on the Mushaf ayah.
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.onAllNodesWithContentDescription(studyPage).onFirst()
            .tapThePaper()
        compose.onNodeWithContentDescription("Switch to the Mushaf page").performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodes(hasContentDescription("Mushaf page", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        longPressMushafAyah()
        compose.onNodeWithContentDescription("More").assertExists()
    }

    private fun waitForStudy() {
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(studyPage).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * The chrome comes up on a tap on the paper, and the pill on a long press
     * of the ayah's own reference line, which the block owns.
     */
    private fun longPressStudyAyah() {
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.onAllNodesWithContentDescription(studyPage).onFirst()
            .tapThePaper()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("1:1", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("1:1", substring = true).onFirst()
            .performTouchInput { longClick() }
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription("Tafsir").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** The Mushaf's own ayah nodes carry the action the press raises. */
    private fun longPressMushafAyah() {
        // The page renders after the mode switch; the node exists only once
        // the rendered page is measured, so the press waits for it.
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithContentDescription("1:1.", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription("1:1.", substring = true).onFirst()
            .performTouchInput { longClick() }
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription("More").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
