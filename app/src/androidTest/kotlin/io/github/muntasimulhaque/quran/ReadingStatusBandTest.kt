package io.github.muntasimulhaque.quran

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The reading's page keeps a band of its own color across the status bar, at
 * the height the phone reports: the system's icons stand on paper, and a
 * scrolled line is cut at the page's edge rather than standing under the
 * clock (owner report, this session).
 *
 * The band's place is arithmetic, so this class reads it off the glass: the
 * top of the window, the whole width, and exactly the status bar's inset. Its
 * color is the page's own, and that is read the only way a color can be read
 * here, by comparing the pixel the band fills with the paper just below it.
 * The capture is the app's own, so the system's clock and icons are in
 * another window and the band's middle is pure fill.
 */
@RunWith(AndroidJUnit4::class)
class ReadingStatusBandTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun thePageKeepsItsOwnBandUnderTheSystemBar() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        val density = compose.activity.resources.displayMetrics.density
        val insets = ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
        val inset = insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        assertTrue("the phone must report a status bar before this band has a height", inset > 0)

        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val band = compose.onNodeWithTag("status-band", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        assertEquals("the band starts at the top of the glass", 0f, band.top.value, 1f)
        assertEquals(
            "the band spans the glass",
            (root.right - root.left).value,
            (band.right - band.left).value,
            1f,
        )
        assertEquals(
            "the band is the status bar's own height",
            inset / density,
            (band.bottom - band.top).value,
            1f,
        )

        val bandPixels = compose.onNodeWithTag("status-band", useUnmergedTree = true)
            .captureToImage()
            .toPixelMap()
        val fill = bandPixels[bandPixels.width / 2, bandPixels.height / 2]

        // The page's own paper, read under the band at the far left, where
        // neither the reading's words nor its wash reach.
        val page = compose.onRoot().captureToImage().toPixelMap()
        val paper = page[
            (4 * density).toInt(),
            ((band.bottom.value + 8f) * density).toInt(),
        ]
        assertTrue(
            "the band must be the page's own ground: band $fill, the page's paper $paper",
            abs(fill.alpha - paper.alpha) < 0.01f &&
                abs(fill.red - paper.red) < 0.01f &&
                abs(fill.green - paper.green) < 0.01f &&
                abs(fill.blue - paper.blue) < 0.01f,
        )
    }
}
