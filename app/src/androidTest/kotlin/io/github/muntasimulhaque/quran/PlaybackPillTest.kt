package io.github.muntasimulhaque.quran

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.playback.PlaybackBar
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The pill's own promises, pinned without a device reading behind it:
 *
 * 1. It keeps a gutter from the glass whatever the label says, so a long
 *    offer cannot grow into the screen's edge (owner report, D-090).
 * 2. The status line is the door to the pace, the repeat, and the
 *    continuation, and all of them call the same setters the Listening page
 *    calls, so there is one value with two doors rather than two values to
 *    keep in step.
 * 3. The automatic download says what it is fetching: the name and the size
 *    are on the pill even when no offer preceded it (owner decision, D-105).
 */
@RunWith(AndroidJUnit4::class)
class PlaybackPillTest {

    @get:Rule
    val compose = createComposeRule()

    private fun showLongOffer(
        onSpeed: (Float) -> Unit = {},
        onContinue: (Boolean) -> Unit = {},
    ) {
        compose.setContent {
            QuranTheme(theme = io.github.muntasimulhaque.quran.data.AppTheme.Paper) {
                PlaybackBar(
                    state = PlaybackUiState(),
                    offer = null,
                    offerTitle = "",
                    reciterName = "Husary",
                    reference = "Al-Baqarah 2:255",
                    pendingLabel = "Continue to Al-Baqarah \u00b7 177 MB",
                    speed = 1f,
                    repeating = false,
                    onToggle = {},
                    onNext = {},
                    onPrevious = {},
                    onReciter = {},
                    onDownload = {},
                    onClose = {},
                    onSpeed = onSpeed,
                    onContinue = onContinue,
                )
            }
        }
    }

    @Test
    fun theLongOfferNeverReachesTheScreenEdge() {
        showLongOffer()
        val barBounds = compose.onNodeWithTag("playback-bar").fetchSemanticsNode().boundsInRoot
        // The gutter is 16 dp, scaled by the test device's own density, so
        // the assertion holds on any profile the tour runs on.
        val gutterPx = 16f * compose.density.density
        val rootWidth = compose.onRoot().fetchSemanticsNode().boundsInRoot.width
        org.junit.Assert.assertTrue(
            "the pill must keep a left gutter: ${barBounds.left} < $gutterPx",
            barBounds.left >= gutterPx - 1f,
        )
        org.junit.Assert.assertTrue(
            "the pill must keep a right gutter: right ${barBounds.right} of $rootWidth",
            rootWidth - barBounds.right >= gutterPx - 1f,
        )
    }

    @Test
    fun theStatusLineCarriesTheListeningDoor() {
        var speed: Float? = null
        showLongOffer(onSpeed = { speed = it })
        // The line is the door; the menu it opens offers the five paces.
        compose.onNodeWithTag("playback-listening").performClick()
        compose.onAllNodesWithText("0.5x").onFirst().performClick()
        assertEquals(0.5f, speed)
    }

    @Test
    fun theListeningMenuCarriesTheContinuation() {
        var continuation = false
        showLongOffer(onContinue = { continuation = it })
        compose.onNodeWithTag("playback-listening").performClick()
        compose.onNodeWithText("Continue to the next surah").performClick()
        assertTrue(continuation)
    }

    @Test
    fun theAutomaticDownloadSaysWhatItIsFetching() {
        compose.setContent {
            QuranTheme(theme = io.github.muntasimulhaque.quran.data.AppTheme.Paper) {
                PlaybackBar(
                    state = PlaybackUiState(
                        pendingDownloadSurah = 2,
                        pendingDownloadBytes = 185_000_000L,
                        pendingIsContinuation = true,
                        downloadProgress = 0.5f,
                    ),
                    offer = null,
                    offerTitle = "",
                    reciterName = "Husary",
                    reference = null,
                    pendingLabel = "Continue to Al-Baqarah \u00b7 177 MB",
                    pendingAudio = "Al-Baqarah \u00b7 177 MB",
                    onToggle = {},
                    onNext = {},
                    onPrevious = {},
                    onReciter = {},
                    onDownload = {},
                    onClose = {},
                )
            }
        }
        // The auto-continue path has no offer, so the name and the size must
        // be on the pill while the package downloads (owner decision, D-105).
        compose.onNodeWithText("Al-Baqarah \u00b7 177 MB \u00b7 50%").assertIsDisplayed()
    }
}
