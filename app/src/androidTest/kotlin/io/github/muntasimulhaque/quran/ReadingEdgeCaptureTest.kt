package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File

/**
 * The reading's top edge, photographed for review.
 *
 * The page keeps a band of its own color at the status bar's height, so the
 * clock, the signal, and the battery stand on paper while a scrolled line is
 * cut at the page's edge (owner report, this session). The band is invisible
 * wherever the reading stands at the top of a surah, where the page already
 * keeps its 64 dp of air, and the tour's frames are taken exactly there: no
 * store frame can show this state.
 *
 * The frame is taken from the display rather than from the app, because the
 * system's clock and icons are drawn in another window and the app's own
 * capture cannot carry them; the whole of what the reader sees is the point of
 * this picture. It is a review frame, not a store frame, and it hands the app
 * back where it found it: the place is read before the page moves and written
 * again in the rule's own `after`, after the activity is finished.
 */
@RunWith(AndroidJUnit4::class)
class ReadingEdgeCaptureTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        /** The place the app was left in, so the frame changes nothing. */
        private var previous = 1

        override fun before() {
            previous = runBlocking { SettingsStore(context).settings.first().ayah }
        }

        override fun after() {
            runBlocking { SettingsStore(context).setAyah(previous) }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun captureThePageUnderTheStatusBar() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        // The page is put back at its top first, so the frame does not depend
        // on what the class before it left on the glass: the first ayah of
        // whatever surah the reading opened in is the line the frame shows.
        repeat(4) {
            compose.onAllNodesWithContentDescription(readingPage).onFirst()
                .performTouchInput { swipeDown() }
            compose.waitForIdle()
        }
        // A slow drag of a known length, released after a pause so it carries
        // no velocity: the first ayah's line comes to stand on the band's edge
        // instead of racing past it on a fling. The bar goes with the drag, as
        // it does for the reader, so the band is read alone.
        val drag = 450f * compose.activity.resources.displayMetrics.density
        compose.onAllNodesWithContentDescription(readingPage).onFirst()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, -drag))
                advanceEventTime(500)
                up()
            }
        compose.waitForIdle()

        val screen = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        checkNotNull(screen) { "the display did not answer with a frame" }
        File(screenshotOutputDirectory(), "reading-status-band.png").outputStream().use { output ->
            screen.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
