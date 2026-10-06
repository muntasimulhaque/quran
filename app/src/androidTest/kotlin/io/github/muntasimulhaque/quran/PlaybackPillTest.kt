package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Layout
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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.playback.PlaybackBar
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
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
 *    offer cannot grow into the screen's edge (owner report).
 * 2. The status line is the door to the pace and to what happens at the end
 *    of the audio, and all of them call the same setter the Listening page
 *    calls, so there is one value with two doors rather than two values to
 *    keep in step.
 * 3. The automatic download says what it is fetching: the name and the size
 *    are on the pill even when no offer preceded it (owner decision).
 * 4. The words keep a measure or they take a line of their own: a phone in
 *    portrait cannot print the reference, the pace, and the repeat beside
 *    four 48 dp controls, and a name cut with an ellipsis is the defect
 *    the wide pill exists to end. A wide pill can, and stays one row.
 * 5. On that phone the two lines are centred, and the words are one line: the
 *    reciter's name and the place beside each other, not the reciter's name
 *    with the place under it against the pill's left edge (owner report).
 * 6. The reciter's name is the chooser and not a hop to the settings, and
 *    the chooser carries every reciter with what it would still cost for
 *    the surah at hand, so the choice is made with the number in view
 *    (owner report).
 * 7. The pill says where the reader is and, when it is not the ordinary
 *    one, the pace. It does not say what happens at the end of the audio:
 *    the sentence does not fit the line, and the switch that carries the
 *    answer says its own state where the reader turned it (owner report).
 * 8. Both popups stand on the capsule's own centre. A menu is placed from the
 *    box it hangs on, and an anchor on the word the reader touched put the
 *    reciter chooser at one end of the pill and the listening menu at the
 *    other: two popups in two places on one control (owner report).
 */
@RunWith(AndroidJUnit4::class)
class PlaybackPillTest {

    @get:Rule
    val compose = createComposeRule()

    private fun showPill(
        onSpeed: (Float) -> Unit = {},
        onEndOfAudio: (EndOfAudio) -> Unit = {},
        onReciter: (String) -> Unit = {},
        state: PlaybackUiState = PlaybackUiState(),
        reference: String? = "Al-Baqarah 2:255",
        pendingAudio: String? = null,
        pillWidth: Modifier = Modifier,
        /**
         * The measure to offer the pill instead of the screen's, for the shape
         * a wider glass wears. See [WideBox]: a modifier cannot do this.
         */
        measureWidth: Dp? = null,
        speed: Float = 1f,
        end: EndOfAudio = EndOfAudio.CONTINUE_AYAH,
    ) {
        compose.setContent {
            QuranTheme(theme = AppTheme.Paper) {
                val pill: @Composable () -> Unit = {
                    PlaybackBar(
                        state = state,
                        offer = null,
                        offerTitle = "",
                        reciterName = "Husary",
                        reciterId = "husary",
                        reciterOptions = listOf(
                            ListenOption("husary", "Husary", 0L),
                            ListenOption("minshawi", "Minshawi", 177_000_000L),
                        ),
                        reference = reference,
                        pendingLabel = "Continue to Al-Baqarah \u00b7 177 MB",
                        pendingAudio = pendingAudio,
                        speed = speed,
                        end = end,
                        onToggle = {},
                        onNext = {},
                        onPrevious = {},
                        onReciter = onReciter,
                        onDownload = {},
                        onClose = {},
                        onSpeed = onSpeed,
                        onEndOfAudio = onEndOfAudio,
                        modifier = pillWidth,
                    )
                }
                // The app keeps the pill at the foot of the reading, so the
                // popups have the window above them to stand in. The harness
                // used to stand the pill at the window's own top, where the
                // popup's room rule, which caps it at the ground above the
                // capsule, left the menu no room at all and the legs failed
                // on a ground the app never gives the pill.
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (measureWidth == null) pill() else WideBox(measureWidth) { pill() }
                }
            }
        }
    }

    /**
     * The same pill, with a state the test can change under it, so a menu can
     * be watched as the pill becomes another pill.
     */
    private fun showMutablePill(state: MutableState<PlaybackUiState>) {
        compose.setContent {
            QuranTheme(theme = AppTheme.Paper) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    PlaybackBar(
                        state = state.value,
                        offer = null,
                        offerTitle = "",
                        reciterName = "Husary",
                        reciterId = "husary",
                        reciterOptions = listOf(ListenOption("husary", "Husary", 0L)),
                        reference = state.value.reference,
                        pendingLabel = "Continue to Al-Baqarah \u00b7 177 MB",
                        onToggle = {},
                        onNext = {},
                        onPrevious = {},
                        onReciter = {},
                        onDownload = {},
                        onClose = {},
                    )
                }
            }
        }
    }

    /**
     * A composable measured at a width the screen does not have.
     *
     * The wide pill (a tablet, or a phone in landscape, keeps the words
     * beside the controls) cannot be asked for with a modifier. `requiredWidth`
     * sets a child's minimum and the root still caps the constraint the child
     * is measured with, so the pill's own `maxWidth` was the phone's 393 dp
     * and the pill took the phone's two rows: the test measured the phone's
     * shape and called it the tablet's. This was not seen for a long time
     * because the class is not among the six the capture runs, and it
     * is why a measure a test needs is given by a layout that offers it.
     */
    @Composable
    private fun WideBox(width: Dp, content: @Composable () -> Unit) {
        Layout(content = content) { measurables, _ ->
            val offered = width.roundToPx()
            val placeable = measurables.firstOrNull()?.measure(
                Constraints(minWidth = 0, maxWidth = offered),
            )
            if (placeable == null) {
                layout(offered, 0) {}
            } else {
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
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
        assertEquals(EndOfAudio.CONTINUE_SURAH, end)
    }

    @Test
    fun theListeningMenuCarriesTheStopOnTheDefaultSwitch() {
        var end: EndOfAudio? = null
        showPill(onEndOfAudio = { end = it })
        compose.onNodeWithTag("playback-listening").performClick()
        // The default row is the one switch whose off is the stop, so this is
        // the reader's one-tap door to a reading that stops after the ayah.
        compose.onNodeWithText("Continue to the next ayah").performClick()
        assertEquals(EndOfAudio.STOP_AFTER_AYAH, end)
    }

    @Test
    fun theWordsTakeTheirOwnLineOnAPhone() {
        // A phone in portrait: four controls and the words cannot share the
        // row at the measure the pill prints at.
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
        //
        // The measure is offered by a layout rather than asked for with
        // `requiredWidth`: the root still caps the constraint a child is
        // measured with, so the pill read the phone's own width and took the
        // phone's two rows, and this measured the phone's shape and called it
        // the tablet's. The class is not among the six the capture runs, which
        // is how that survived.
        showPill(measureWidth = 840.dp)
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val play = compose.onNodeWithContentDescription("Play or pause").getUnclippedBoundsInRoot()
        // "Share the row" is overlap and company, not an order: both are
        // centered in the row, and the shorter one starts lower, so asking
        // for the transport to start above the words was asking for a shape
        // no centered row has. What distinguishes the one row from the two is
        // that the transport stands beside the words and overlaps them
        // vertically (the phone's own test pins the other half of the rule:
        // there the transport is under the words).
        assertTrue(
            "the transport must stand beside the words: play $play, words $words",
            play.left.value > words.left.value,
        )
        assertTrue(
            "the transport must share the words' row: play $play, words $words",
            play.top.value < words.bottom.value && play.bottom.value > words.top.value,
        )
    }

    @Test
    fun thePhonePillSaysBothOnOneCentredLine() {
        // The two-row pill the owner read on their own phone: the reciter's
        // name and the place on one line, centred, with the controls centred
        // under them. Before this the words were two lines against the pill's
        // own left edge, so the pill's first line began in a corner.
        showPill(pillWidth = Modifier.width(393.dp))
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val name = compose.onNodeWithText("Husary").getUnclippedBoundsInRoot()
        val place = compose.onNodeWithText("Al-Baqarah 2:255").getUnclippedBoundsInRoot()
        val inset = 20.dp
        // the two share a line when they stand over one another: the overlap
        // of their heights is most of the taller one, which a stacked pair
        // (the name above the place) could never be
        val overlap = kotlin.math.min(name.bottom.value, place.bottom.value) -
            kotlin.math.max(name.top.value, place.top.value)
        assertTrue(
            "the reciter and the place must share one line, not one above the other: " +
                "$name and $place",
            overlap > (name.bottom - name.top).value / 2f,
        )
        assertEquals(
            "the words' line is centred on the pill, so both ends stand off it equally",
            (name.left - bar.left).value,
            (bar.right - place.right).value,
            1f,
        )
        assertTrue(
            "the words' line keeps the pill's own inset from its curve",
            name.left - bar.left >= inset - 1.dp,
        )
    }

    @Test
    fun thePhonePillCentresItsControls() {
        showPill(pillWidth = Modifier.width(393.dp))
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val first = compose.onNodeWithContentDescription("Previous ayah").getUnclippedBoundsInRoot()
        val last = compose.onNodeWithContentDescription("Stop").getUnclippedBoundsInRoot()
        assertEquals(
            "the controls stand centred under the words, not against one end",
            (first.left - bar.left).value,
            (bar.right - last.right).value,
            1f,
        )
    }

    @Test
    fun theMenuClosesWhenThePillBecomesTheContinuationOffer() {
        val state = mutableStateOf(
            PlaybackUiState(
                connected = true,
                ayahNumber = 2,
                surah = 1,
                reference = "Al-Fatihah 1:2",
            ),
        )
        showMutablePill(state)
        compose.onNodeWithTag("playback-listening").performClick()
        compose.onNodeWithText("Continue to the next surah").assertExists()
        // The surah ended and the next package is offered: the pill underneath
        // is another pill, so the menu it belonged to goes with the old one.
        compose.runOnUiThread {
            state.value = PlaybackUiState(
                connected = true,
                surah = 1,
                pendingDownloadSurah = 2,
                pendingDownloadBytes = 185_000_000L,
                pendingIsContinuation = true,
            )
        }
        compose.onNodeWithText("Continue to the next surah").assertDoesNotExist()
    }

    @Test
    fun theMenuClosesWhenTheReadingMovesToAnotherAyah() {
        val state = mutableStateOf(
            PlaybackUiState(
                connected = true,
                ayahNumber = 2,
                surah = 1,
                reference = "Al-Fatihah 1:2",
            ),
        )
        showMutablePill(state)
        compose.onNodeWithTag("playback-listening").performClick()
        compose.onNodeWithText("Continue to the next surah").assertExists()
        compose.runOnUiThread {
            state.value = state.value.copy(ayahNumber = 3, reference = "Al-Fatihah 1:3")
        }
        compose.onNodeWithText("Continue to the next surah").assertDoesNotExist()
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
            pendingAudio = "Al-Baqarah \u00b7 177 MB \u00b7 50%",
        )
        // The auto-continue path has no offer, so the name and the size must
        // be on the pill while the package downloads (owner decision).
        // The status line names the pending package and the percent, and the
        // string adds the percent to what it is given, so the line reads the
        // name once and the percent twice over. It read as one number once, and
        // this leg had been red for nobody.
        val line = "Al-Baqarah \u00b7 177 MB \u00b7 50% \u00b7 50%"
        compose.onNodeWithText(line).assertIsDisplayed()
    }

    @Test
    fun theReciterNameIsTheChooserAndNotTheSettings() {
        var chosen: String? = null
        showPill(onReciter = { chosen = it })
        compose.onNodeWithTag("playback-reciter").performClick()
        // Every reciter, and what it would still cost for this surah, so the
        // choice is made with the number in view. The number is 177,000,000
        // bytes, which is the megabyte the app's own formatter prints, and the
        // two were written as if they were the same figure, which is why this
        // leg had been red for nobody (owner report).
        compose.onNodeWithText("Minshawi").assertIsDisplayed()
        compose.onNodeWithText(formatBytes(177_000_000L)).assertIsDisplayed()
        compose.onNodeWithText("Ready to play").assertIsDisplayed()
        compose.onNodeWithText("Minshawi").performClick()
        assertEquals("minshawi", chosen)
    }

    @Test
    fun thePillDoesNotCarryTheEndOfTheAudio() {
        // The repeat is a switch the reader turned on, and that switch says so
        // where it is turned. The pill is a control over the page, and the
        // sentence does not fit the line it has (owner report).
        showPill(end = EndOfAudio.REPEAT_SURAH)
        compose.onNodeWithText("Al-Baqarah 2:255").assertIsDisplayed()
        assertEquals(
            "the pill must not carry the end of the audio",
            0,
            compose.onAllNodesWithText("repeating the surah", substring = true)
                .fetchSemanticsNodes().size,
        )
    }

    @Test
    fun thePillStillCarriesThePace() {
        // A recitation at 0.75x is a mark and not a sentence, and it is the
        // one thing about the audio a reader notices without being told.
        showPill(speed = 0.75f)
        compose.onNodeWithText("Al-Baqarah 2:255 \u00b7 0.75x").assertIsDisplayed()
    }

    /**
     * Both of the pill's popups hang from the capsule's own centre, and this
     * pins the capsule's half of that: the anchor they share is the pill's
     * measure wide and stands on the pill's middle, on the phone's two rows
     * and the wide pill's one row alike.
     *
     * The popup's own place on the glass is not measured here, because it
     * cannot be: a popup draws in a window of its own, and every node inside
     * it reports coordinates measured from that window's corner, so a compose
     * test can see how big a popup is and never where it hangs.
     * `PillMenuPositionTest` pins what puts it there, in arithmetic that runs
     * anywhere.
     */
    @Test
    fun thePopupsHangFromThePillsOwnCentre() {
        showPill(pillWidth = Modifier.width(393.dp))
        assertTheAnchorStandsOnThePillCentre()
    }

    @Test
    fun thePopupsHangFromTheWidePillsCentreToo() {
        showPill(measureWidth = 840.dp)
        assertTheAnchorStandsOnThePillCentre()
    }

    private fun assertTheAnchorStandsOnThePillCentre() {
        val pill = compose.onNodeWithTag("playback-bar").fetchSemanticsNode().boundsInWindow
        val anchor = compose.onNodeWithTag("pill-menu-anchor").fetchSemanticsNode().boundsInWindow
        assertTrue(
            "the popup's anchor must be the pill's own measure, so the popup can stand on the " +
                "pill's centre: pill $pill, anchor $anchor",
            anchor.width > 0,
        )
        assertEquals(
            "the anchor's middle is the capsule's middle",
            (pill.left + pill.right) / 2,
            (anchor.left + anchor.right) / 2,
        )
    }
}
