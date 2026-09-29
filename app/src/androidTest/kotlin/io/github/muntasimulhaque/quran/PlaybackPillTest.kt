package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.data.AppTheme
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
 * 2. The status line is the door to the pace and to what happens at the end
 *    of the audio, and all of them call the same setter the Listening page
 *    calls, so there is one value with two doors rather than two values to
 *    keep in step (D-087, widened by D-118).
 * 3. The automatic download says what it is fetching: the name and the size
 *    are on the pill even when no offer preceded it (owner decision, D-105).
 * 4. The words keep a measure or they take a line of their own: a phone in
 *    portrait cannot print the reference, the pace, and the repeat beside
 *    four 48 dp controls, and a name cut with an ellipsis is the defect
 *    D-119 exists to end. A wide pill can, and stays one row.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackPillTest {

    @get:Rule
    val compose = createComposeRule()

    private fun showPill(
        onSpeed: (Float) -> Unit = {},
        onEndOfAudio: (EndOfAudio) -> Unit = {},
        state: PlaybackUiState = PlaybackUiState(),
        reference: String? = "Al-Baqarah 2:255",
        pendingAudio: String? = null,
        pillWidth: Modifier = Modifier,
    ) {
        compose.setContent {
            QuranTheme(theme = AppTheme.Paper) {
                PlaybackBar(
                    state = state,
                    offer = null,
                    offerTitle = "",
                    reciterName = "Husary",
                    reference = reference,
                    pendingLabel = "Continue to Al-Baqarah \u00b7 177 MB",
                    pendingAudio = pendingAudio,
                    speed = 1f,
                    end = EndOfAudio.OFF,
                    onToggle = {},
                    onNext = {},
                    onPrevious = {},
                    onReciter = {},
                    onDownload = {},
                    onClose = {},
                    onSpeed = onSpeed,
                    onEndOfAudio = onEndOfAudio,
                    modifier = pillWidth,
                )
            }
        }
    }

    @Test
    fun theLongOfferNeverReachesTheScreenEdge() {
        showPill()
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
        showPill(onSpeed = { speed = it })
        // The line is the door; the menu it opens offers the five paces.
        compose.onNodeWithTag("playback-listening").performClick()
        compose.onAllNodesWithText("0.5x").onFirst().performClick()
        assertEquals(0.5f, speed)
    }

    @Test
    fun theListeningMenuCarriesTheSurahRepeat() {
        var end: EndOfAudio? = null
        showPill(onEndOfAudio = { end = it })
        compose.onNodeWithTag("playback-listening").performClick()
        compose.onNodeWithText("Repeat the surah").performClick()
        assertEquals(
            "the surah repeat reports its own answer and nothing else",
            EndOfAudio.REPEAT_SURAH,
            end,
        )
    }

    @Test
    fun theListeningMenuCarriesTheContinuation() {
        var end: EndOfAudio? = null
        showPill(onEndOfAudio = { end = it })
        compose.onNodeWithTag("playback-listening").performClick()
        compose.onNodeWithText("Continue to the next surah").performClick()
        assertEquals(EndOfAudio.CONTINUE, end)
    }

    @Test
    fun theWordsTakeTheirOwnLineOnAPhone() {
        // A phone in portrait: four controls and the words cannot share the
        // row at the measure the pill prints at (D-119).
        showPill(pillWidth = Modifier.width(393.dp))
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val play = compose.onNodeWithContentDescription("Play or pause").getUnclippedBoundsInRoot()
        assertTrue(
            "the transport must sit under the words, not beside them: ${play.top} of ${words.bottom}",
            play.top.value >= words.bottom.value - 0.5f,
        )
    }

    @Test
    fun theWordsShareTheRowOnAWidePill() {
        // A tablet, or a phone in landscape: the words keep their measure, so
        // the pill is the one row it has always been.
        showPill(pillWidth = Modifier.width(840.dp))
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val play = compose.onNodeWithContentDescription("Play or pause").getUnclippedBoundsInRoot()
        assertTrue(
            "the transport must share the words' row: ${play.top} of ${words.top}",
            play.top.value < words.top.value,
        )
    }

    @Test
    fun theAutomaticDownloadSaysWhatItIsFetching() {
        showPill(
            state = PlaybackUiState(
                pendingDownloadSurah = 2,
                pendingDownloadBytes = 185_000_000L,
                pendingIsContinuation = true,
                downloadProgress = 0.5f,
            ),
            reference = null,
            pendingAudio = "Al-Baqarah \u00b7 177 MB",
        )
        // The auto-continue path has no offer, so the name and the size must
        // be on the pill while the package downloads (owner decision, D-105).
        compose.onNodeWithText("Al-Baqarah \u00b7 177 MB \u00b7 50%").assertIsDisplayed()
    }
}
