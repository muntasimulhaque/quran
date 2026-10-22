package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * The juz label in the top bar follows the reader as they scroll.
 *
 * The reader's place is the first visible ayah, and the place is written
 * when a drag of their own ends. The label used to be a value of its own,
 * written only by a jump or by the recitation's follow, so scrolling from
 * juz 1 into juz 2 left it reading "Juz 1" until something else moved the
 * place (owner report). The reader stands at 2:141, the last ayah of juz 1,
 * swipes into 2:142 and the ayahs after it, and reads the label the way a
 * reader does: a tap on the paper brings the chrome.
 */
@RunWith(AndroidJUnit4::class)
class JuzLabelScrollTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    /** Al-Fatihah is 7 ayahs, so Al-Baqarah 2:141 is the Quran's number 148. */
    private val place = 7 + 141

    private val library = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(place)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theJuzLabelFollowsAScrollAcrossItsBoundary() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }

        // The reader stands at 2:141, the last ayah of juz 1, and the chrome
        // says so.
        compose.onAllNodesWithContentDescription(readingPage).onFirst().tapThePaper()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Juz 1", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

        // A drag of their own, which is what writes the place. Three screens
        // of scrolling land inside juz 2, which begins at 2:142 and runs 111
        // ayahs, so the swipe count cannot carry past it.
        repeat(3) {
            compose.onAllNodesWithContentDescription(readingPage).onFirst()
                .performTouchInput { swipeUp() }
            compose.waitForIdle()
        }

        // The chrome comes back on a tap, and the label has followed.
        compose.onAllNodesWithContentDescription(readingPage).onFirst().tapThePaper()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Juz 2", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
