package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.SettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * The Mushaf's pages turn the way a physical Book turns: the next page lies to
 * the left of this one, so a swipe to the right goes forward and a swipe to
 * the left goes back. A still frame cannot show which way the pages move, so
 * the direction is pinned here by the ayahs the reading exposes: Al-Fatihah
 * is page 1, and a page becomes the reading (its ayahs are spoken) only while
 * it is the page on screen.
 *
 * The library is prepared before the activity starts, the order a reader
 * creates it in, and the page order is the only thing the test touches.
 */
@RunWith(AndroidJUnit4::class)
class MushafTurnTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    private val library = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val settings = SettingsStore(context)
            runBlocking {
                settings.setUiLanguage("en")
                settings.setAyah(1)
                settings.setMode(ReadingMode.Mushaf)
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    /** Waits for an ayah to be the one on screen; only the current page says its ayahs. */
    private fun waitForTheAyahOnScreen(reference: String) {
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodes(hasContentDescription("$reference.", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun page(pageNumber: Int) =
        compose.onAllNodes(hasContentDescription("Mushaf page $pageNumber", substring = true)).onFirst()

    @Test
    fun theNextPageLiesToTheLeft() {
        waitForTheAyahOnScreen("1:1")
        // The Book opens on the right, so its pages run leftward: a swipe to
        // the right turns forward, to Al-Baqarah's first page.
        page(1).performTouchInput { swipeRight() }
        waitForTheAyahOnScreen("2:1")
        // And back again with a swipe to the left.
        page(2).performTouchInput { swipeLeft() }
        waitForTheAyahOnScreen("1:1")
    }
}
