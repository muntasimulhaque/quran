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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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
 * 2. The status line is the door to the pace and to what happens at the end
 *    of the audio, and all of them call the pace's one setter, so there is one
 *    value with two doors rather than two values to keep in step.
 * 3. The automatic download says what it is fetching: the name and the size
 *    are on the pill even when no offer preceded it (owner decision).
 * 4. The words keep a measure or they take a line of their own: a phone in
 *    portrait cannot print the reference, the pace, and the repeat beside
 *    four 48 dp controls, and a name cut with an ellipsis is the defect
 *    the wide pill exists to end. A wide pill can, and stays one row.
 * 5. On that phone the words and the transport are centred, and the words are
 *    one line: the reciter's name and the place beside each other, not the
 *    reciter's name with the place under it against the pill's left edge
 *    (owner report).
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
 * 9. Both menus wear the app's own choice row: one leading mark, one 16 dp
 *    gap, and one 48 dp target, the same the settings lists draw, so an
 *    answer cannot change its shape from one door to the other (owner
 *    decision, forty-eighth session).
 * 10. The capsule hugs its words (owner decision, this session). It carries
 *     no close on the playing pill, the offer, a failure, or an unavailable
 *     answer, so it is the wider of its words' line and its transport plus
 *     the app's air, and never the page's width for no reason. Its width is
 *     the words' own, which is also what stops the place being cut with an
 *     ellipsis on the phone the owner read.
 * 11. The offer prints the surah's own name whole in the line it shares with
 *     the size, in either language: the size may give way at the end of the
 *     line, the name never does (owner decision, this session).
 * 12. No press shape touches another or the capsule: the app's `Space.Line`
 *     stands between the transport's three 48 dp circles, between the two
 *     rows, and between any shape and the capsule's edge (owner decision,
 *     this session).
 * 13. The recitation's stop has no shape on the glass and is still reachable:
 *     it is the action TalkBack offers on Play or pause, so the close's own
 *     act is not lost with the close's mark (owner decision, this session).
 * 14. The one X the app keeps is the transfer's own abort, and it is the last
 *     thing in the row: a package on its way is the one thing a stray tap on
 *     the page must never end (owner decision, this session).
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
         * The measure to offer the pill instead of the screen's, for the shape
         * a wider glass wears. See [WideBox]: a modifier cannot do this.
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
    private fun showMutablePill(state: MutableState<PlaybackUiState>) {
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

    /**
     * The pace keeps the settings page's own place: the label is a bare line
     * at the menu's left inset, and the five paces stand centred under it
     * (owner decision, forty-seventh session). Against the menu's left edge
     * they were 10 dp from one side and 28 from the other.
     */
    @Test
    fun thePaceStandsCentredInTheListeningMenu() {
        showPill()
        compose.onNodeWithTag("playback-listening").performClick()
        compose.onNodeWithText("Playback speed").assertExists()
        val menu = compose.onNodeWithTag("pill-popup").fetchSemanticsNode().boundsInWindow
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
    fun theListeningMenuCarriesTheStopAsItsOwnRow() {
        var end: EndOfAudio? = null
        showPill(onEndOfAudio = { end = it })
        compose.onNodeWithTag("playback-listening").performClick()
        // The stop is a row of its own now: the reader finds it by its name,
        // not at the off end of the default switch.
        compose.onNodeWithText("Stop after this ayah").performClick()
        assertEquals(EndOfAudio.STOP_AFTER_AYAH, end)
    }

    /**
     * The listening menu wears the app's own choice row: the same leading
     * radio mark, the same 16 dp gap, and the same 48 dp target the Listening
     * page draws, so the same answer cannot change its shape from one door to
     * the other (owner decision, forty-eighth session; the gap is 16, the
     * value `ChoiceRow` has always drawn, pinned here by the theme and
     * consistency session).
     */
    @Test
    fun theListeningMenuWearsTheAppChoiceRow() {
        showPill()
        compose.onNodeWithTag("playback-listening").performClick()
        compose.onAllNodesWithTag("choice-mark", useUnmergedTree = true).assertCountEquals(5)
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
     * decision, forty-eighth session).
     */
    @Test
    fun theReciterChooserWearsTheAppChoiceRow() {
        showPill()
        compose.onNodeWithTag("playback-reciter").performClick()
        val chosen = compose.onNode(
            hasText("Husary") and hasAnyAncestor(hasTestTag("pill-popup")),
        )
        chosen.assertIsSelected()
        val mark = compose.onAllNodesWithTag("choice-mark", useUnmergedTree = true).onFirst()
            .getUnclippedBoundsInRoot()
        val name = compose.onNode(
            hasText("Husary") and hasAnyAncestor(hasTestTag("pill-popup")),
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        assertTrue(
            "the mark must lead the chosen reciter's name: mark $mark, name $name",
            mark.left.value < name.left.value,
        )
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
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val play = compose.onNodeWithContentDescription("Play or pause").getUnclippedBoundsInRoot()
        val next = compose.onNodeWithContentDescription("Next ayah").getUnclippedBoundsInRoot()
        // "Share the row" is overlap and company, not an order: what
        // distinguishes the one row from the two is that the transport stands
        // beside the words and overlaps them vertically (the phone's own test
        // pins the other half of the rule: there the transport is under the
        // words).
        assertTrue(
            "the transport must stand beside the words: play $play, words $words",
            play.left.value > words.left.value,
        )
        assertTrue(
            "the transport must share the words' row: play $play, words $words",
            play.top.value < words.bottom.value && play.bottom.value > words.top.value,
        )
        // The wide pill hugs too: the glass may be 840 wide, and the capsule
        // is the words, the gap, and the transport, with the app's air around
        // them (owner decision, this session).
        assertTrue(
            "the wide pill must hug its row, not fill the glass: bar $bar",
            (bar.right - bar.left).value < 840f - 2 * 16f,
        )
        assertEquals(
            "the words keep the capsule's air at the leading end",
            Space.Line.value,
            (words.left - bar.left).value,
            1.5f,
        )
        assertEquals(
            "the transport keeps the capsule's air at the trailing end",
            Space.Line.value,
            (bar.right - next.right).value,
            1.5f,
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
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val reciter = compose.onNodeWithTag("playback-reciter").getUnclippedBoundsInRoot()
        val listening = compose.onNodeWithTag("playback-listening").getUnclippedBoundsInRoot()
        val name = compose.onNodeWithText("Husary").getUnclippedBoundsInRoot()
        val place = compose.onNodeWithText("Al-Baqarah 2:255").getUnclippedBoundsInRoot()
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
        // The words carry no close's rail any more, so the line stands on the
        // capsule's own axis, the axis the transport under it stands on (owner
        // decision, this session: the rail was there to keep the close's flash
        // clear of the two doors, and with the close gone it was empty ground
        // inside a shape that could be the words' own size).
        assertEquals(
            "the words stand on the capsule's own axis",
            (bar.left + bar.right).value / 2f,
            (words.left + words.right).value / 2f,
            1f,
        )
        assertEquals(
            "the two doors stand the app's air apart, so neither flash meets the other",
            Space.Row.value,
            (listening.left - reciter.right).value,
            1f,
        )
        assertEquals(
            "the words' flash keeps the capsule's air above it",
            Space.Line.value,
            (reciter.top - bar.top).value,
            1f,
        )
    }

    /**
     * The phone pill's own line: the words and the transport stand on the
     * capsule's centre, and the transport carries no close at all (owner
     * decision, this session). The stop the close used to carry is offered to
     * a screen reader on Play or pause, which the leg below pins.
     */
    @Test
    fun thePhonePillCentresItsTransport() {
        showPill(pillWidth = Modifier.width(393.dp))
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val first = compose.onNodeWithContentDescription("Previous ayah").getUnclippedBoundsInRoot()
        val next = compose.onNodeWithContentDescription("Next ayah").getUnclippedBoundsInRoot()
        assertEquals(
            "the transport stands centred under the words, not against one end",
            (first.left - bar.left).value,
            (bar.right - next.right).value,
            1f,
        )
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
     * The capsule hugs the words it prints (owner decision, this session). A
     * phone's page is no longer spent on empty ground: the capsule is the
     * words' own measure, and the transport's row can never be squeezed
     * inside it.
     */
    @Test
    fun theCapsuleHugsItsWords() {
        showPill()
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        assertTrue(
            "the capsule must not fill the page: bar $bar, root $root",
            (bar.right - bar.left).value < (root.right - root.left).value - 2 * 16f - 1f,
        )
        // Three 48 dp controls, the app's air between them, and the capsule's
        // own air around them is the narrowest the shape can be.
        assertTrue(
            "the capsule must hold its transport whole: bar $bar",
            (bar.right - bar.left).value >= 160f + 16f + 2 * 16f,
        )
        assertEquals(
            "the capsule stands on the glass's own middle",
            (root.left + root.right).value / 2f,
            (bar.left + bar.right).value / 2f,
            1f,
        )
    }

    /**
     * One air, everywhere: the transport's three circles stand `Space.Line`
     * apart, the two rows' flashes stand it apart, and no flash meets the
     * capsule (owner decision, this session: they used to meet point to point,
     * and the words' flash used to meet the capsule's own top edge).
     */
    @Test
    fun theTouchedShapesKeepTheCapsulesAir() {
        showPill(pillWidth = Modifier.width(393.dp))
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val reciter = compose.onNodeWithTag("playback-reciter").getUnclippedBoundsInRoot()
        val play = compose.onNodeWithContentDescription("Play or pause").getUnclippedBoundsInRoot()
        val first = compose.onNodeWithContentDescription("Previous ayah").getUnclippedBoundsInRoot()
        val next = compose.onNodeWithContentDescription("Next ayah").getUnclippedBoundsInRoot()
        assertEquals(
            "the two rows' flashes stand the app's air apart",
            Space.Line.value,
            (play.top - reciter.bottom).value,
            2f,
        )
        assertEquals(
            "the transport's flash keeps the capsule's air below it",
            Space.Line.value,
            (bar.bottom - play.bottom).value,
            2f,
        )
        assertEquals(
            "the transport's first two circles stand the app's air apart",
            Space.Line.value,
            (play.left - first.right).value,
            1f,
        )
        assertEquals(
            "the transport's last two circles stand the app's air apart",
            Space.Line.value,
            (next.left - play.right).value,
            1f,
        )
    }

    /**
     * The one X the app keeps stands under the words on the capsule's own
     * axis, alone in its row, and the playing pill's stop draws nothing here
     * (owner decision, this session).
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
        val line = compose.onNodeWithTag("download-line").getUnclippedBoundsInRoot()
        // One answer shares the words' line: the capsule is one row tall plus
        // the line's own room, where the two-row shape stood 130 dp to hold a
        // single mark under its words (owner decision, this session).
        assertTrue(
            "the transfer's abort trails the words on their own line: " +
                "words $words, cancel $cancel",
            cancel.left.value >= words.right.value - 0.5f,
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
     * The offer carries one answer and no close: the name and the size lead,
     * the Download follows them, and the capsule keeps the app's air at both
     * ends. Its old X answered "not now" exactly as a tap on the page does,
     * so it had nothing of its own to say (owner decision, this session).
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
        val root = compose.onRoot().getUnclippedBoundsInRoot()
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
        assertTrue(
            "the offer hugs its words rather than filling the page: bar $bar, root $root",
            (bar.right - bar.left).value < (root.right - root.left).value - 2 * 16f - 1f,
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
     * The pill's two word doors owe the finger the app's 48 dp: the name is
     * the chooser and the status line is the listening menu, and a reader
     * tapping the thin strip of type must land on the control behind it, not
     * on the letter under their fingertip (owner decision, this session).
     */
    @Test
    fun theWordDoorsAreFullTargets() {
        showPill(pillWidth = Modifier.width(393.dp))
        compose.onNodeWithContentDescription("Husary").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("Al-Baqarah 2:255").assertHeightIsAtLeast(48.dp)
    }

    /**
     * The transport's press is the whole target, not the 20 dp glyph. An
     * indication left on the glyph flashed a dot inside the 48 dp node the
     * finger actually hit, which read as too small (owner report, this
     * session). Three shapes, because the transport is the three marks it
     * always had: the close that was the fourth has left the glass (owner
     * decision, this session).
     */
    @Test
    fun theTransportPressFillsItsOwnTarget() {
        showPill()
        // The shape is a child of the clickable, which merges its
        // descendants, so the tag is read from the unmerged tree.
        val shapes = compose.onAllNodesWithTag("transport-shape", useUnmergedTree = true)
        shapes.assertCountEquals(3)
        repeat(3) { index ->
            shapes[index].assertWidthIsEqualTo(48.dp)
            shapes[index].assertHeightIsEqualTo(48.dp)
        }
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
