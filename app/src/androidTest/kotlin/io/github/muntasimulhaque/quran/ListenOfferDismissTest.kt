package io.github.muntasimulhaque.quran

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
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
 * A page tap puts the download offer away.
 *
 * The app's raised controls answer a tap on the page: the chrome goes, the
 * ayah pill goes, and a paused transport pill stops. The download offer was
 * the one exception, so a reader who changed their mind had to find its own
 * close. It now answers the page tap the same way, while a package already
 * on its way keeps its own close as the deliberate cancel (owner decision,
 * this session).
 */
@RunWith(AndroidJUnit4::class)
class ListenOfferDismissTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * A reading with no translation, so 1:1 is on screen, and no recitation
     * timings for either reciter, so Play raises the offer rather than
     * starting a download the test did not ask for.
     */
    private val library = object : ExternalResource() {
        override fun before() {
            LanguagePreference(context).set("en")
            // Tapping Play asks for the notification once, on a fresh
            // install, and the phone's own dialog takes the window while the
            // test waits on the reading. Grant it up front so the reading
            // stays the window the test is looking at.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                InstrumentationRegistry.getInstrumentation().uiAutomation
                    .grantRuntimePermission(
                        context.packageName,
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
            }
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setTranslationPacks(emptySet())
                    setTafsirPacks(emptySet())
                    setShowTranslation(true)
                    setShowTafsir(true)
                    setWordByWord(false)
                    setRecitation("husary")
                    setLongPressHintShown()
                }
            }
            PackStore(context).apply {
                remove("reciter-husary")
                remove("reciter-minshawi")
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun aPageTapPutsTheDownloadOfferAway() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        // The first ayah's own actions, raised by the long press.
        compose.onAllNodesWithText("1:1", substring = true).onFirst()
            .performTouchInput { longClick() }
        val play = compose.activity.getString(R.string.action_play)
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithContentDescription(play).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(play).performClick()
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("listen-offer-bar", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        // The tap on the page is the dismissal every raised control answers
        // to, and the offer must not be the exception anymore.
        compose.onAllNodesWithContentDescription(readingPage).onFirst().tapThePaper()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithTag("listen-offer-bar", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }
}
