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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.playback.PlaybackBar
import io.github.muntasimulhaque.quran.ui.kit.GlyphPress
import io.github.muntasimulhaque.quran.ui.kit.GlyphTarget
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import io.github.muntasimulhaque.quran.ui.theme.Space
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * The pill's own promises, pinned without a device reading behind it:
 *
 * 1. It keeps a gutter from the glass whatever the label says, so a long
 *    offer cannot grow into the screen's edge (owner report).
 * 2. The playing words are one block and one door, and the door opens the
 *    whole listening panel: the voices, the pace, and what happens at the end
 *    of the audio, and all of them call the pill's one setter, so there is one
 *    value with one door. Every answer in that panel puts it away: the voice
 *    list has always closed on its choice, and the pace and the ending follow
 *    it, so one question is answered by one tap (owner decision, this
 *    session).
 * 3. The automatic download says what it is fetching: the name and the size
 *    are on the pill even when no offer preceded it (owner decision).
 * 4. The playing pill is one row in every voice, the offer's shape: the words
 *    block at the leading end and one answer at the trailing end, 64 dp tall.
 *    The block gives way with an ellipsis before the answer does; a voice that
 *    is not a recitation keeps one line and the voice list alone, because the
 *    pace and the end answer a recitation that is not there (owner decision,
 *    this session).
 * 5. The block's two lines are the reciter's name over the place while
 *    playing, and the name over the state's sentence when there is no
 *    recitation. Nothing in the pill touches a neighbour or the capsule: the
 *    block keeps the app's air above and below, the answer keeps it at its
 *    end, and the words' own end keeps the shoulder's air, where an 8 dp pad
 *    read as a name touching the border (owner decision, this session). * 6. The reciter is chosen on the pill and not in the settings, and the
 *    chooser carries every voice with what it would still cost for the surah
 *    at hand, so the choice is made with the number in view (owner report).
 * 7. The pill says where the reader is, and the pace stands beside the
 *    reciter's name when it is not the ordinary one: a tail at the end of the
 *    place was the first thing the line's ellipsis cut, and at the longest
 *    names the place alone is wider than the line (owner decision, this
 *    session). It does not say what happens at the end of the audio: the
 *    sentence does not fit the line, and the switch that carries the answer
 *    says its own state where the reader turned it (owner report).
 * 8. The popup stands on the capsule's own centre, on the phone and on the
 *    wide glass alike. A menu is placed from the box it hangs on, and an
 *    anchor on the word the reader touched put the reciter chooser at one
 *    end of the pill and the listening menu at the other (owner report).
 * 9. The panel's rows wear the app's own choice row: one leading mark, one
 *    16 dp gap, and one 48 dp target, the same the settings lists draw, so an
 *    answer cannot change its shape from one door to the other (owner
 *    decision, forty-eighth session). Its three sections wear the app's one
 *    head over a list, which is small, accent, and a heading to a screen
 *    reader: a head at the size of the rows under it made the panel twelve
 *    lines of one voice, with no hierarchy for the air between them to lean on
 *    (owner decision, this session).
 * 10. The capsule is one measure in every voice (owner decision, this
 *     session). It used to follow its words: about 285 dp at the shortest
 *     places, the page's own 328 at the longest, about 230 for an offer, and
 *     about 210 for a transfer that widened again as its percent did. The
 *     playing capsule, the offer, and the transfer now stand at 328 dp on a
 *     360 dp glass, and the words and the answer keep its two ends while the
 *     ground between them takes what the words do not need. The place is one
 *     line and may end in an ellipsis at the longest names.
 * 11. The offer prints the surah's own name whole in the line it shares with
 *     the size, in either language: the size may give way at the end of the
 *     line, the name never does (owner decision, this session).
 * 12. The transfer's X keeps the capsule's air at its end, and the block and
 *     the transport stand the app's row air apart, measured between the shapes
 *     the reader sees (owner decision).
 * 13. The recitation's stop has no shape on the glass and is still reachable:
 *     it is the action TalkBack offers on Play or pause, so the close's own
 *     act is not lost with the close's mark (owner decision, this session).
 * 14. The one X the app keeps is the transfer's own abort, and it is the last
 *     thing in the row: a package on its way is the one thing a stray tap on
 *     the page must never end (owner decision, this session).
 * 15. The transport is the app's one round control: a 40 dp press circle
 *     inside each 48 dp target, which lets the three targets stand edge to
 *     edge while the circles keep the app's air, and gives the place 20 dp
 *     more of the capsule than a circle the size of its target allowed (owner
 *     decision, this session).
 */
@RunWith(AndroidJUnit4::class)
class PlaybackPillTest {

    @get:Rule
    val compose = createComposeRule()

    private fun showPill(
        onSpeed: (Float) -> Unit = {},
        onEndOfAudio: (EndOfAudio) -> Unit = {},
        onReciter: (String) -> Unit = {},
        /** The close's own act, which has no shape any more. */
        onStop: () -> Unit = {},
        onCancelDownload: () -> Unit = {},
        state: PlaybackUiState = PlaybackUiState(),
        reference: String? = "Al-Baqarah 2:255",
        pendingAudio: String? = null,
        pillWidth: Modifier = Modifier,
        /**
         * The measure to offer the pill instead of the screen's, for a wide
         * glass. See [WideBox]: a modifier cannot do this.
         */
        measureWidth: Dp? = null,
        speed: Float = 1f,
        end: EndOfAudio = EndOfAudio.CONTINUE_AYAH,
        /** The offer state, when the leg is about the offer bar. */
        offer: ListenOffer? = null,
        /**
         * The offer's own title, where a leg needs the real words in the
         * reader's language: the harness builds an English one by default,
         * and the test process's own strings are always English.
         */
        offerTitle: String? = null,
        /** The reader's language, which is the voice and the digits. */
        language: UiLanguage = UiLanguage.English,
    ) {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = language) {
                val pill: @Composable () -> Unit = {
                    PlaybackBar(
                        state = state,
                        offer = offer,
                        offerTitle = offerTitle ?: offer?.let { pending ->
                            "${pending.surahName} \u00b7 " +
                                formatBytes(pending.bytes, Locale.US, "KB", "MB")
                        }.orEmpty(),
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
                        onStop = onStop,
                        onCancelDownload = onCancelDownload,
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
    private fun showMutablePill(
        state: MutableState<PlaybackUiState>,
        pillWidth: Modifier = Modifier,
    ) {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light) {
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
                        onStop = {},
                        onCancelDownload = {},
                        modifier = pillWidth,
                    )
                }
            }
        }
    }

    /**
     * A composable measured at a width the screen does not have.
     *
     * The root caps the constraint a child is measured with, so a modifier
     * cannot offer the pill a tablet's width: the pill's own constraints were
     * the phone's. This box offers the width as a measurement, so a wide glass
     * is measured as one.
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

    /**
     * The playing words are one door, and it opens the whole listening panel:
     * the voices, the pace, and the end of the audio, in one popup (owner
     * decision, this session: the block is one door, and the three questions
     * are one subject). The pace is reached from the panel's own row.
     */
    @Test
    fun theWordsBlockCarriesTheListeningPanel() {
        var speed: Float? = null
        showPill(onSpeed = { speed = it })
        compose.onNodeWithTag("playback-words").performClick()
        compose.onNodeWithText("Reciter").assertExists()
        compose.onNodeWithText("Playback speed").assertExists()
        compose.onNodeWithText("When the audio ends").assertExists()
        compose.onAllNodesWithText("0.5x").onFirst().performClick()
        assertEquals(0.5f, speed)
    }

    /**
     * The panel opens quiet: a focusable popup hands the focus to its first
     * focusable child, and Material draws that focus as a state layer over the
     * first voice, which read as the chosen one even when another was (owner
     * report, this session). The panel clears its focus once it has settled;
     * the rows keep their focus for Tab and for a screen reader, which reaches
     * them through the semantics rather than through this focus.
     */
    @Test
    fun theListeningPanelOpensWithNoRowFocused() {
        showPill()
        compose.onNodeWithTag("playback-words").performClick()
        compose.onNode(
            hasText("Husary") and hasAnyAncestor(hasTestTag("app-popup")),
        ).assertIsNotFocused()
    }

    /**
     * The pace keeps the settings page's own place: the label is a bare line
     * at the menu's left inset, and the five paces stand centred under it
     * (owner decision, forty-seventh session). Against the menu's left edge
     * they were 10 dp from one side and 28 from the other.
     */
    @Test
    fun thePaceStandsCentredInTheListeningMenu() {
        showPill()
        compose.onNodeWithTag("playback-words").performClick()
        compose.onNodeWithText("Playback speed").assertExists()
        val menu = compose.onNodeWithTag("app-popup").fetchSemanticsNode().boundsInWindow
        val pace = compose.onNodeWithTag("playback-speed").fetchSemanticsNode().boundsInWindow
        assertEquals(
            "the pace's own middle is the menu's middle",
            (menu.left + menu.right) / 2f,
            (pace.left + pace.right) / 2f,
            1f,
        )
    }

    @Test
    fun theListeningMenuCarriesTheSurahRepeat() {
        var end: EndOfAudio? = null
        showPill(onEndOfAudio = { end = it })
        compose.onNodeWithTag("playback-words").performClick()
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
        compose.onNodeWithTag("playback-words").performClick()
        compose.onNodeWithText("Continue to the next surah").performClick()
        assertEquals(EndOfAudio.CONTINUE_SURAH, end)
    }

    @Test
    fun theListeningMenuCarriesTheStopAsItsOwnRow() {
        var end: EndOfAudio? = null
        showPill(onEndOfAudio = { end = it })
        compose.onNodeWithTag("playback-words").performClick()
        // The stop is a row of its own now: the reader finds it by its name,
        // not at the off end of the default switch.
        compose.onNodeWithText("Stop after this ayah").performClick()
        assertEquals(EndOfAudio.STOP_AFTER_AYAH, end)
    }

    /**
     * The panel wears the app's own choice row: the two voices and the five
     * answers, seven rows, each with the same leading radio mark, the same
     * 16 dp gap, and the same 48 dp target the settings lists draw, so the same
     * answer cannot change its shape from one door to the other (owner
     * decision, forty-eighth session; the gap is 16, the value `ChoiceRow` has
     * always drawn, pinned here by the theme and consistency session).
     */
    @Test
    fun theListeningMenuWearsTheAppChoiceRow() {
        showPill()
        compose.onNodeWithTag("playback-words").performClick()
        compose.onAllNodesWithTag("choice-mark", useUnmergedTree = true).assertCountEquals(7)
        val mark = compose.onAllNodesWithTag("choice-mark", useUnmergedTree = true).onFirst()
            .getUnclippedBoundsInRoot()
        val name = compose.onNodeWithText("Continue to the next ayah", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "the mark must lead the name in the pill too: mark $mark, name $name",
            mark.left.value < name.left.value,
        )
        compose.onNodeWithText("Continue to the next ayah").assertHeightIsAtLeast(48.dp)
    }

    /**
     * The reciter chooser is a choice, not a button: the chosen reciter wears
     * the app's leading mark and its row is the one a screen reader reports as
     * selected, where the check beside a Role.Button said neither (owner
     * decision, forty-eighth session). It leads the merged panel, the block's
     * first section.
     */
    @Test
    fun theReciterChooserWearsTheAppChoiceRow() {
        showPill()
        compose.onNodeWithTag("playback-words").performClick()
        val chosen = compose.onNode(
            hasText("Husary") and hasAnyAncestor(hasTestTag("app-popup")),
        )
        chosen.assertIsSelected()
        val mark = compose.onAllNodesWithTag("choice-mark", useUnmergedTree = true).onFirst()
            .getUnclippedBoundsInRoot()
        val name = compose.onNode(
            hasText("Husary") and hasAnyAncestor(hasTestTag("app-popup")),
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        assertTrue(
            "the mark must lead the chosen reciter's name: mark $mark, name $name",
            mark.left.value < name.left.value,
        )
    }

    /**
     * The playing pill is one row (owner decision, this session): the words
     * block at the leading end and the transport at the trailing end, on one
     * line, in the offer's shape. It used to take two rows on a phone, with
     * the words on a line of their own above the transport.
     */
    @Test
    fun thePlayingPillIsOneRow() {
        showPill()
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val play = compose.onNodeWithContentDescription("Play or pause").getUnclippedBoundsInRoot()
        assertTrue(
            "the transport must stand beside the words: play $play, words $words",
            play.left.value > words.right.value,
        )
        assertTrue(
            "the transport must share the words' row: play $play, words $words",
            play.top.value < words.bottom.value && play.bottom.value > words.top.value,
        )
        assertEquals(
            "the phone's row stands 64 dp tall",
            64f,
            (bar.bottom - bar.top).value,
            1f,
        )
    }

    /**
     * The block's two lines: the reciter's name over the place, both inside
     * one door, and the block's own shape keeps the capsule's air above and
     * below it, and the words' own air at its end.
     */
    @Test
    fun thePlayingPillStacksTheNameOverThePlace() {
        showPill()
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val name = compose.onNodeWithText("Husary", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val place = compose.onNodeWithText("Al-Baqarah 2:255", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "the reciter's name must stand above the place: $name and $place",
            name.bottom.value <= place.top.value + 0.5f,
        )
        assertEquals(
            "the block keeps the app's air above it",
            Space.Line.value,
            (words.top - bar.top).value,
            1f,
        )
        assertEquals(
            "the block keeps the app's air under it",
            Space.Line.value,
            (bar.bottom - words.bottom).value,
            1f,
        )
        assertEquals(
            "the block's end keeps the shoulder's air",
            Space.Block.value + Space.Tight.value,
            (words.left - bar.left).value,
            1f,
        )
    }

    /**
     * The phone pill carries no close at all (owner decision, an earlier
     * session). The stop it used to carry is the action TalkBack offers on
     * Play or pause, and the pill draws no Stop mark anywhere.
     */
    @Test
    fun thePlayingPillCarriesNoClose() {
        showPill()
        compose.onNodeWithContentDescription("Stop").assertDoesNotExist()
    }

    /**
     * The stop the close carried has no shape any more, and it is not lost
     * with it: a screen reader finds it on Play or pause, under the same word
     * the close was announced with (owner decision, this session).
     */
    @Test
    fun theStopIsOfferedToAScreenReaderWhereTheCloseUsedToStand() {
        var stopped = false
        showPill(onStop = { stopped = true })
        val actions = compose.onNodeWithContentDescription("Play or pause")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(
            "the stop keeps the word TalkBack heard from the close",
            "Stop",
            actions.single().label,
        )
        compose.runOnUiThread { actions.single().action() }
        assertTrue("the stop's action must reach the pill's own stop", stopped)
    }

    /**
     * The capsule is one measure in every voice (owner decision, this
     * session). It used to follow its words, from about 285 dp at the shortest
     * places to the page's own 328 at the longest while playing, and a share
     * of that while an offer or a transfer stood: one download was two widths,
     * and the transfer's edge moved as its percent did. Now the playing
     * capsule, the offer, and the transfer stand at the page's own 328 on a
     * 360 dp glass, and the words and the answer keep its two ends.
     */
    @Test
    fun theCapsuleIsOneMeasure() {
        showPill(pillWidth = Modifier.width(360.dp))
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val next = compose.onNodeWithContentDescription("Next ayah").getUnclippedBoundsInRoot()
        assertEquals(
            "the capsule stands at the page's own 328",
            328f,
            (bar.right - bar.left).value,
            1f,
        )
        assertEquals(
            "the capsule stands on the glass's own middle",
            (root.left + root.right).value / 2f,
            (bar.left + bar.right).value / 2f,
            1f,
        )
        assertEquals(
            "the words' end keeps the shoulder's air from the capsule",
            Space.Block.value + Space.Tight.value,
            (words.left - bar.left).value,
            1f,
        )
        assertEquals(
            "the transport keeps the app's air at its end",
            Space.Line.value,
            (bar.right - next.right).value,
            1f,
        )
    }

    /**
     * A percent never moves the capsule's edge (owner report, this session):
     * the transfer used to widen again as the status's digits grew, so its
     * shape reported the download rather than the reading. One measure and one
     * line, from the first percent to the last.
     */
    @Test
    fun theTransferKeepsOneWidthAsItsPercentGrows() {
        val state = mutableStateOf(
            PlaybackUiState(
                pendingDownloadSurah = 19,
                pendingDownloadBytes = 22L * 1024 * 1024,
                downloadProgress = 0.01f,
            ),
        )
        showMutablePill(state, pillWidth = Modifier.width(360.dp))
        val start = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        compose.runOnUiThread { state.value = state.value.copy(downloadProgress = 1f) }
        val end = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        assertEquals(
            "the transfer stands at the pill's one measure",
            328f,
            (start.right - start.left).value,
            1f,
        )
        assertEquals(
            "a percent must never move the capsule's leading edge",
            start.left.value,
            end.left.value,
            0.5f,
        )
        assertEquals(
            "a percent must never move the capsule's trailing edge",
            start.right.value,
            end.right.value,
            0.5f,
        )
        val line = compose.onNodeWithTag("download-line").getUnclippedBoundsInRoot()
        assertEquals(
            "the line keeps the capsule's own measure less its gutters",
            328f - 2 * 16f,
            (line.right - line.left).value,
            1f,
        )
    }

    /**
     * Every answer puts the panel away (owner decision, this session): the
     * voice list has always closed on its choice, and the pace and the ending
     * follow it. One panel asked one question, and the tap answered it, so the
     * reader is back on the reading rather than in a panel to dismiss.
     */
    @Test
    fun everyAnswerPutsThePanelAway() {
        var speed: Float? = null
        var end: EndOfAudio? = null
        var reciter: String? = null
        showPill(
            onSpeed = { speed = it },
            onEndOfAudio = { end = it },
            onReciter = { reciter = it },
        )
        fun openThePanel() {
            compose.onNodeWithTag("playback-words").performClick()
            compose.onNodeWithTag("app-popup").assertExists()
        }
        openThePanel()
        compose.onAllNodesWithText("0.5x").onFirst().performClick()
        assertEquals("the pace must reach the pill's setter", 0.5f, speed)
        compose.onNodeWithTag("app-popup").assertDoesNotExist()

        openThePanel()
        compose.onNodeWithText("Repeat the surah").performClick()
        assertEquals("the ending must reach the pill's setter", EndOfAudio.REPEAT_SURAH, end)
        compose.onNodeWithTag("app-popup").assertDoesNotExist()

        openThePanel()
        compose.onNodeWithText("Minshawi").performClick()
        assertEquals("the voice must reach the pill's setter", "minshawi", reciter)
        compose.onNodeWithTag("app-popup").assertDoesNotExist()
    }

    /**
     * The place line takes the row the transport gave back (owner decision,
     * this session). At the tightest glass the app draws the pill on, a 360 dp
     * phone, the capsule is the page's 328 and the transport takes 144 of it:
     * what is left for the place is 140 dp, where three circles the size of
     * their targets left 120. A place longer than that still ends in an
     * ellipsis, because the reader keeps the word Ayah on the line.
     */
    @Test
    fun thePlaceLineKeepsTheRowTheTransportGaveIt() {
        showPill(
            pillWidth = Modifier.width(360.dp),
            reference = "Al-Mumtahanah, Ayah 13",
        )
        val place = compose.onNodeWithText("Al-Mumtahanah, Ayah 13", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(
            "the place line keeps the row the transport gave it: place $place",
            140f,
            (place.right - place.left).value,
            1f,
        )
    }

    /**
     * The playing pill wears the offer's shape (owner decision): one 64 dp
     * capsule whose words block is 48 dp tall, whose transport circles stand
     * inside their own 48 dp targets, and whose airs are the app's own above,
     * below, between, and at the end. Nothing in it touches a neighbour or the
     * capsule's edge.
     */
    @Test
    fun thePlayingPillWearsTheOfferShape() {
        showPill()
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val circles = compose.onAllNodesWithTag("glyph-press", useUnmergedTree = true)
        val next = compose.onNodeWithContentDescription("Next ayah").getUnclippedBoundsInRoot()
        assertEquals(
            "the playing capsule stands 64 dp tall",
            64f,
            (bar.bottom - bar.top).value,
            1f,
        )
        assertEquals(
            "the words' block stands 48 dp tall",
            48f,
            (words.bottom - words.top).value,
            1f,
        )
        assertEquals(
            "the block keeps the app's air above it",
            Space.Line.value,
            (words.top - bar.top).value,
            1f,
        )
        assertEquals(
            "the block keeps the app's air under it",
            Space.Line.value,
            (bar.bottom - words.bottom).value,
            1f,
        )
        assertEquals(
            "the block's end keeps the shoulder's air",
            Space.Block.value + Space.Tight.value,
            (words.left - bar.left).value,
            1f,
        )
        assertEquals(
            "the block and the transport stand the app's row air apart, between the shapes",
            Space.Row.value,
            (circles.onFirst().getUnclippedBoundsInRoot().left - words.right).value,
            1f,
        )
        assertEquals(
            "the transport keeps the app's air at its end",
            Space.Line.value,
            (bar.right - next.right).value,
            1f,
        )
    }

    /**
     * Nothing in the pill touches, and nothing sits on a dead pixel either
     * (owner decision, this session): the block and the first circle stand the
     * app's row air apart, the three circles stand the app's line air apart,
     * and the targets under them stand edge to edge, so a finger that slides
     * from one control to the next never falls between them.
     *
     * The air is measured between the shapes the reader sees, never between
     * the invisible targets: each circle is 4 dp inside its own target, and
     * that inset is the whole of what lets three of them stand edge to edge
     * with their circles apart.
     */
    @Test
    fun theTouchedShapesKeepTheCapsulesAir() {
        showPill()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val circles = compose.onAllNodesWithTag("glyph-press", useUnmergedTree = true)
        circles.assertCountEquals(3)
        val first = circles[0].getUnclippedBoundsInRoot()
        val play = circles[1].getUnclippedBoundsInRoot()
        val next = circles[2].getUnclippedBoundsInRoot()
        val previousTarget = compose.onNodeWithContentDescription("Previous ayah")
            .getUnclippedBoundsInRoot()
        val playTarget = compose.onNodeWithContentDescription("Play or pause")
            .getUnclippedBoundsInRoot()
        val nextTarget = compose.onNodeWithContentDescription("Next ayah")
            .getUnclippedBoundsInRoot()
        assertEquals(
            "the block and the transport stand the app's row air apart, between the shapes",
            Space.Row.value,
            (first.left - words.right).value,
            1f,
        )
        assertEquals(
            "the transport's first two circles stand the app's line air apart",
            Space.Line.value,
            (play.left - first.right).value,
            1f,
        )
        assertEquals(
            "the transport's last two circles stand the app's line air apart",
            Space.Line.value,
            (next.left - play.right).value,
            1f,
        )
        assertEquals(
            "the transport's first two targets stand edge to edge",
            0f,
            (playTarget.left - previousTarget.right).value,
            1f,
        )
        assertEquals(
            "the transport's last two targets stand edge to edge",
            0f,
            (nextTarget.left - playTarget.right).value,
            1f,
        )
    }

    /**
     * The one X the app keeps trails the words on the capsule's own line,
     * alone in its row, and it is the last shape in the pill (owner decision,
     * this session).
     */
    @Test
    fun theTransfersOwnAbortIsTheOnlyMarkInItsRow() {
        showPill(
            state = PlaybackUiState(
                pendingDownloadSurah = 19,
                pendingDownloadBytes = 22L * 1024 * 1024,
                downloadProgress = 0.5f,
            ),
            reference = null,
        )
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val cancel = compose
            .onNodeWithContentDescription("Cancel download")
            .getUnclippedBoundsInRoot()
        val circle = compose.onAllNodesWithTag("glyph-press", useUnmergedTree = true)
            .onFirst()
            .getUnclippedBoundsInRoot()
        val line = compose.onNodeWithTag("download-line").getUnclippedBoundsInRoot()
        // One answer shares the words' line: the capsule is one row tall plus
        // the line's own room, where the two-row shape stood 130 dp to hold a
        // single mark under its words (owner decision, this session).
        assertEquals(
            "the transfer's abort stands the app's row air from the words, between the shapes",
            Space.Row.value,
            (circle.left - words.right).value,
            1f,
        )
        assertEquals(
            "the transfer's words keep the shoulder's air at the capsule's end",
            Space.Block.value + Space.Tight.value,
            (words.left - bar.left).value,
            1f,
        )
        assertEquals(
            "the transfer's abort keeps the capsule's air at the trailing end",
            Space.Line.value,
            (bar.right - cancel.right).value,
            1.5f,
        )
        // The line takes the room the capsule keeps under its row, so the row
        // above it is never crossed and the two never touch.
        assertTrue(
            "the transfer's abort keeps the app's air above the progress line: " +
                "cancel $cancel, line $line",
            (line.top - cancel.bottom).value >= Space.Line.value - 0.5f,
        )
        compose.onNodeWithContentDescription("Stop").assertDoesNotExist()
    }

    /**
     * The offer's words keep the shoulder's air at the capsule's end, where a
     * capsule's sphere has curved in a few dp by the name's line (owner
     * decision, this session). Without it the Bangla name, whose matra reaches
     * the top of its line, read as cut: the ink sat 5 dp inside the border.
     */
    @Test
    fun theOfferLeadsWithTheWordsAir() {
        showPill(
            offer = ListenOffer(
                reciter = "husary",
                reciterName = "Husary",
                surah = 19,
                surahName = "Maryam",
                ayah = 1,
                bytes = 22L * 1024 * 1024,
                options = listOf(ListenOption("husary", "Husary", 0L)),
            ),
        )
        val bar = compose.onNodeWithTag("listen-offer-bar").getUnclippedBoundsInRoot()
        val words = compose.onNodeWithTag("offer-words").getUnclippedBoundsInRoot()
        assertEquals(
            "the offer's words keep the shoulder's air at the capsule's end",
            Space.Block.value + Space.Tight.value,
            (words.left - bar.left).value,
            1f,
        )
    }

    /**
     * The offer carries one answer and no close: the name and the size lead,
     * the Download follows them, and the capsule keeps the words' own air at
     * the leading end and the app's air at the answer's. Its old X answered
     * "not now" exactly as a tap on the page does, so it had nothing of its
     * own to say (owner decision, this session).
     */
    @Test
    fun theOfferCarriesOneAnswerAndNoClose() {
        showPill(
            offer = ListenOffer(
                reciter = "husary",
                reciterName = "Husary",
                surah = 19,
                surahName = "Maryam",
                ayah = 1,
                bytes = 22L * 1024 * 1024,
                options = listOf(ListenOption("husary", "Husary", 0L)),
            ),
        )
        val bar = compose.onNodeWithTag("listen-offer-bar").getUnclippedBoundsInRoot()
        val title = compose.onNodeWithText("Maryam \u00b7 22 MB").getUnclippedBoundsInRoot()
        val download = compose.onNodeWithText("Download").getUnclippedBoundsInRoot()
        assertTrue(
            "the words must lead and the answer trail: title $title, download $download",
            title.left.value < download.left.value,
        )
        compose.onNodeWithContentDescription("Not now").assertDoesNotExist()
        assertEquals(
            "the offer's answer keeps the capsule's air at the trailing end",
            Space.Line.value,
            (bar.right - download.right).value,
            1.5f,
        )
        assertEquals(
            "the offer stands at the pill's one measure",
            328f,
            (bar.right - bar.left).value,
            1f,
        )
    }

    /**
     * The offer prints the surah's own name whole in the line it shares with
     * the size, in the reader's own language: the name is what the reader
     * reads, so it is the promise, and the size is what gives way when the
     * line is at its longest (owner decision, this session).
     *
     * The two names are the content's own longest in each language, and the
     * sizes are the packages' real bytes: `Al-Mumtahanah` at 9.0 MB, and
     * `আল-মুতাফফিফীন` at ৫.৮ এমবি. The pill is measured at the 360 dp
     * phone's own width, the tightest glass the app draws an offer on.
     */
    @Test
    fun theOfferPrintsTheLongestEnglishNameWhole() {
        showPill(
            pillWidth = Modifier.width(360.dp),
            offer = ListenOffer(
                reciter = "husary",
                reciterName = "Husary",
                surah = 60,
                surahName = "Al-Mumtahanah",
                ayah = 1,
                bytes = 9_395_684L,
                options = listOf(ListenOption("husary", "Husary", 0L)),
            ),
            offerTitle = "Al-Mumtahanah \u00b7 9.0 MB",
        )
        assertTheOfferPrintsTheNameWhole(
            title = "Al-Mumtahanah \u00b7 9.0 MB",
            name = "Al-Mumtahanah",
        )
    }

    @Test
    fun theOfferPrintsTheLongestBanglaNameWhole() {
        showPill(
            pillWidth = Modifier.width(360.dp),
            language = UiLanguage.Bangla,
            offer = ListenOffer(
                reciter = "husary",
                reciterName = "Husary",
                surah = 83,
                surahName = "আল-মুতাফফিফীন",
                ayah = 1,
                bytes = 6_077_998L,
                options = listOf(ListenOption("husary", "Husary", 0L)),
            ),
            offerTitle = "আল-মুতাফফিফীন \u00b7 ৫.৮ এমবি",
        )
        assertTheOfferPrintsTheNameWhole(
            title = "আল-মুতাফফিফীন \u00b7 ৫.৮ এমবি",
            name = "আল-মুতাফফিফীন",
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
        compose.onNodeWithTag("playback-words").performClick()
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
        compose.onNodeWithTag("playback-words").performClick()
        compose.onNodeWithText("Continue to the next surah").assertExists()
        compose.runOnUiThread {
            state.value = state.value.copy(ayahNumber = 3, reference = "Al-Fatihah 1:3")
        }
        compose.onNodeWithText("Continue to the next surah").assertDoesNotExist()
    }

    @Test
    fun theDownloadSaysWhatItIsFetching() {
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
        // Every download names the surah it is fetching, whichever door asked
        // for it: the auto-continue path has no offer to carry the name, and a
        // reader's own download has no offer left by the time the progress
        // line stands (owner report, this session: a progress line with no
        // name does not say what the reader is paying for). The status line
        // names the pending package and the percent, and the string adds the
        // percent to what it is given, so the line reads the name once and the
        // percent twice over. It read as one number once, and this leg had
        // been red for nobody.
        val line = "Al-Baqarah \u00b7 177 MB \u00b7 50% \u00b7 50%"
        compose.onNodeWithText(line).assertIsDisplayed()
    }

    /**
     * The words owe the finger the app's 48 dp: the playing block is one door,
     * and a reader tapping the thin strips of type must land on the control
     * behind them, not on the letter under their fingertip (owner decision,
     * this session).
     */
    @Test
    fun theWordDoorsAreFullTargets() {
        showPill()
        compose.onNodeWithTag("playback-words").assertHeightIsAtLeast(48.dp)
    }

    /**
     * The voice's own door, on the line a voice that is not a recitation
     * wears: the name is still a full 48 dp target, and the sentence beside it
     * is a notice (owner decision, this session).
     */
    @Test
    fun theVoiceNameIsAFullTarget() {
        showPill(state = PlaybackUiState(error = true))
        compose.onNodeWithContentDescription("Husary").assertHeightIsAtLeast(48.dp)
    }

    /**
     * The transport's press is the app's one round control: a 40 dp circle
     * inside the 48 dp target the finger gets, the same circle every icon
     * button in the app lights. It is not the 20 dp glyph, because an
     * indication on the glyph flashed a dot inside the button the finger hit
     * (owner report, an earlier session), and it is not the whole target,
     * because a 48 dp circle cannot stand beside another without touching it
     * (owner decision, this session).
     */
    @Test
    fun theTransportPressIsTheAppRoundControl() {
        showPill()
        // The shape is a child of the clickable, which merges its
        // descendants, so the tag is read from the unmerged tree.
        val shapes = compose.onAllNodesWithTag("glyph-press", useUnmergedTree = true)
        shapes.assertCountEquals(3)
        repeat(3) { index ->
            shapes[index].assertWidthIsEqualTo(GlyphPress)
            shapes[index].assertHeightIsEqualTo(GlyphPress)
        }
        // The target under each circle is still the whole 48 dp a finger gets.
        compose.onNodeWithContentDescription("Previous ayah").assertWidthIsEqualTo(GlyphTarget)
        compose.onNodeWithContentDescription("Play or pause").assertWidthIsEqualTo(GlyphTarget)
        compose.onNodeWithContentDescription("Next ayah").assertWidthIsEqualTo(GlyphTarget)
    }

    @Test
    fun thePaceIsSilentAtTheOrdinaryOne() {
        showPill()
        compose.onNodeWithText("1x", useUnmergedTree = true).assertDoesNotExist()
    }

    /**
     * The panel's three sections are heads over lists, not rows of their own
     * (owner decision, this session): the app's one head, which a screen reader
     * is told is a heading, so the voices, the pace, and the ending can be
     * skipped to instead of heard row by row.
     */
    @Test
    fun theListeningPanelsSectionsAreHeads() {
        showPill()
        compose.onNodeWithTag("playback-words").performClick()
        listOf("Reciter", "Playback speed", "When the audio ends").forEach { head ->
            compose.onNodeWithText(head).assert(isHeading())
        }
    }

    /**
     * The reciter is chosen on the pill and not in the settings, and the panel
     * that chooses carries every voice with what it would still cost for the
     * surah at hand (owner report).
     */
    @Test
    fun thePillChoosesTheReciterAndNotTheSettings() {
        var chosen: String? = null
        showPill(onReciter = { chosen = it })
        compose.onNodeWithTag("playback-words").performClick()
        // Every reciter, and what it would still cost for this surah, so the
        // choice is made with the number in view. The number is 177,000,000
        // bytes, which is the megabyte the app's own formatter prints, and the
        // two were written as if they were the same figure, which is why this
        // leg had been red for nobody (owner report).
        compose.onNodeWithText("Minshawi").assertIsDisplayed()
        compose.onNodeWithText(formatBytes(177_000_000L, Locale.US, "KB", "MB"))
            .assertIsDisplayed()
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
        // one thing about the audio a reader notices without being told. It
        // stands on the voice's own line now, over the place, and the place
        // says the place alone (owner decision, this session).
        showPill(speed = 0.75f)
        val pace = compose.onNodeWithText("0.75x", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val place = compose.onNodeWithText("Al-Baqarah 2:255", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "the pace must stand over the place, on the name's line: pace $pace, place $place",
            pace.bottom.value <= place.top.value,
        )
    }

    /**
     * Both of the pill's popups hang from the capsule's own centre, and this
     * pins the capsule's half of that: the anchor they share is the pill's
     * measure wide and stands on the pill's middle, on the phone and on the
     * wide glass alike.
     *
     * The popup's own place on the glass is not measured here, because it
     * cannot be: a popup draws in a window of its own, and every node inside
     * it reports coordinates measured from that window's corner, so a compose
     * test can see how big a popup is and never where it hangs.
     * `AppPopupPositionTest` pins what puts it there, in arithmetic that runs
     * anywhere.
     */
    @Test
    fun thePopupsHangFromThePillsOwnCentre() {
        showPill()
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
            // One pixel, which is the rounding the layout can do and not a
            // second centre: the anchor is 288 dp centred in a capsule whose
            // own width can be an odd number of pixels, so the two middles
            // can land a pixel apart on a profile the ATD phone never drew
            // (owner report, forty-seventh session). The promise is one
            // centre, not one integer.
            1f,
        )
    }

    /**
     * The offer must show the whole name in the line it shares with the size.
     * The text's own layout is read back through the semantics action Compose
     * gives every `Text`: `getLineEnd(0, visibleEnd = true)` is where the
     * visible line stops, so an offset at least the name's own length means
     * the ellipsis, if it came, fell on the size alone (owner decision, this
     * session).
     */
    private fun assertTheOfferPrintsTheNameWhole(title: String, name: String) {
        val text = compose.onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        assertTrue(
            "the offer must report the title's own layout: $title",
            text.config.contains(SemanticsActions.GetTextLayoutResult),
        )
        val reported = text.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertTrue(
            "the title must hand its own layout to the test: $title",
            reported == true && layouts.isNotEmpty(),
        )
        val visibleEnd = layouts.first().getLineEnd(0, visibleEnd = true)
        assertTrue(
            "the offer must print the name whole: $name at offset ${name.length} " +
                "of $title, which stops at $visibleEnd",
            visibleEnd >= name.length,
        )
    }
}
