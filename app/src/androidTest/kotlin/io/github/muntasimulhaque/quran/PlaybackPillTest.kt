package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
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
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
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
import org.junit.Assert.assertFalse
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
 * 4. The playing pill is two rows, and the offer is its sibling (owner
 *    decision, this session): one 48 dp line of words over one 48 dp
 *    transport row, 96 dp tall, in the playing voice. The offer is the same
 *    96 dp skeleton: a 48 dp line that is the door to the voice list over the
 *    48 dp pair. The voices that are not a recitation keep one row, 64 dp
 *    tall, or 74 with a download's line under it. The three stand at one
 *    width.
 * 5. The playing line is one block and one door: the voice and its pace, then
 *    the place and its key, with the chevron at the end. The one-row voices
 *    lead with the reciter's name beside the state's sentence. Nothing in the
 *    pill touches a neighbour or the capsule: the playing line keeps the
 *    shoulder's air at each end, the one-row voices keep it at their words'
 *    end, and the transport's targets stand edge to edge with their circles
 *    an [Space.Line] apart.
 * 6. The reciter is chosen on the playing pill, in the settings, and on the
 *    offer's own line, each with what every voice would still cost for the
 *    surah at hand, so the choice is made with the number in view (owner
 *    report). The offer's list is read for the offer's surah, not the playing
 *    pill's (owner decision, this session).
 * 7. The pill says who is reading, where, and how fast: the voice and the pace
 *    when it is not 1x, then the place and its key. The place, the key, and
 *    the pace never give way; the voice does, at the largest place with a
 *    non-1x pace (owner decision, this session). The pace is read back from
 *    the listening panel, where it is set. It does not say what happens at the
 *    end of the audio: the switch that carries the answer says its own state
 *    where the reader turned it (owner report).
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
 *     360 dp glass. The playing line and the transport are centered; the
 *     offer centers its door line and its pair; the transfer keeps the words
 *     and the X at its ends. The place's line is whole at the app's text
 *     sizes, and the ayah's key is whole at every one.
 * 11. The offer prints the surah's own name whole in the line it shares with
 *     the reciter and the size, in either language: the size may give way at
 *     the end of the line, the name never does (owner decision, this
 *     session).
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
 * 16. The offer is a two-row capsule, 96 dp tall: a 48 dp line that is the
 *     door to the voice list, then Not now and Download as one centered pair.
 *     The line's crown is 14 dp and the answers' foot 6. An offer with no
 *     voice to offer keeps the line as a notice and draws no chevron (owner
 *     decision, this session).
 * 17. Every door says its state: the pill's three doors speak Expanded and
 *     Collapsed the way the ayah's door and every disclosure row do, and
 *     their chevron turns up while the popup they opened is on the glass
 *     (owner decision, this session). The turn itself is arithmetic, pinned
 *     in `AppChevronTurnTest`; this suite reads the spoken half.
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
        reference: String? = "2:255",
        placeName: String? = "Al-Baqarah",
        /** The system text scale, for the legs that test the largest step. */
        fontScale: Float = 1f,
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
                        placeName = placeName,
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
                // popup's old room rule, which capped it at the ground above
                // the capsule, left the menu no room at all and the legs
                // failed on a ground the app never gives the pill.
                AtFontScale(fontScale) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (measureWidth == null) pill() else WideBox(measureWidth) { pill() }
                    }
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
                        placeName = state.value.placeName,
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
     * The pill at a system text scale, for the legs that test the largest step.
     * The density is the screen's; only the font scale is chosen.
     */
    @Composable
    private fun AtFontScale(fontScale: Float, content: @Composable () -> Unit) {
        if (fontScale == 1f) {
            content()
            return
        }
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, fontScale),
            content = content,
        )
    }

    /** The lines a text drew, read off the node the reader would meet. */
    private fun layoutOf(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val node = compose.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
        val action = node.config[SemanticsActions.GetTextLayoutResult].action
            ?: throw AssertionError("no laid-out text for $text")
        action(results)
        return results.first()
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
        compose.onNodeWithTag("playback-speed")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
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
     * The playing pill is two rows (owner decision, this session): one 48 dp
     * line of words over one 48 dp transport row, and the capsule is 96 dp.
     * The words and the transport are centered on the capsule's axis; the
     * one-row voices keep their own shape.
     */
    @Test
    fun thePlayingPillStacksTheTransportBelowTheWords() {
        showPill()
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val play = compose.onNodeWithContentDescription("Play or pause").getUnclippedBoundsInRoot()
        assertEquals(
            "the playing capsule stands 96 dp tall",
            96f,
            (bar.bottom - bar.top).value,
            1f,
        )
        assertEquals(
            "the words' line is the capsule's 48 dp crown",
            48f,
            (words.bottom - words.top).value,
            1f,
        )
        assertEquals(
            "the words stand at the capsule's top",
            bar.top.value,
            words.top.value,
            1f,
        )
        assertEquals(
            "the transport's target is the capsule's 48 dp foot",
            bar.bottom.value,
            play.bottom.value,
            1f,
        )
        assertEquals(
            "the words' row and the transport's row meet",
            0f,
            (play.top - words.bottom).value,
            1f,
        )
        assertEquals(
            "the words stand on the capsule's middle",
            (bar.left + bar.right).value / 2f,
            (words.left + words.right).value / 2f,
            1.5f,
        )
        assertEquals(
            "the transport stands on the capsule's middle",
            (bar.left + bar.right).value / 2f,
            (play.left + play.right).value / 2f,
            1.5f,
        )
    }

    /**
     * The playing line, in the offer's own order (owner decision, this
     * session): the voice and its pace, then the place and its key, all on
     * one 48 dp door. The voice leads, the place follows, and the key closes
     * the place.
     */
    @Test
    fun thePlayingPillLeadsWithTheVoiceThenThePlace() {
        showPill(speed = 0.75f)
        val voice = compose.onNodeWithText("Husary", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val pace = compose.onNodeWithText("0.75x", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val place = compose.onNodeWithText("Al-Baqarah", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val key = compose.onNodeWithText("2:255", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "the voice must lead the pace: $voice and $pace",
            voice.right.value <= pace.left.value + 0.5f,
        )
        assertTrue(
            "the pace must lead the place: $pace and $place",
            pace.right.value <= place.left.value + 0.5f,
        )
        assertTrue(
            "the place must lead its key: $place and $key",
            place.right.value <= key.left.value + 0.5f,
        )
        assertTrue(
            "the voice, the pace, the place, and the key share one line: " +
                "$voice, $pace, $place, $key",
            maxOf(voice.top.value, pace.top.value, place.top.value, key.top.value) <
                minOf(voice.bottom.value, pace.bottom.value, place.bottom.value, key.bottom.value),
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
     * 360 dp glass. The playing line and the transport are centered; the
     * offer centers its door and its pair; the transfer keeps its words and
     * its X at the two ends.
     */
    @Test
    fun theCapsuleIsOneMeasure() {
        showPill(pillWidth = Modifier.width(360.dp))
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val play = compose.onNodeWithContentDescription("Play or pause").getUnclippedBoundsInRoot()
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
            "the words stand on the capsule's own middle",
            (bar.left + bar.right).value / 2f,
            (words.left + words.right).value / 2f,
            1.5f,
        )
        assertEquals(
            "the transport stands on the capsule's own middle",
            (bar.left + bar.right).value / 2f,
            (play.left + play.right).value / 2f,
            1.5f,
        )
        assertTrue(
            "the words keep the shoulder at each end of the capsule: " +
                "words $words, bar $bar",
            words.left.value - bar.left.value >= Space.Block.value + Space.Tight.value - 0.5f &&
                bar.right.value - words.right.value >= Space.Block.value + Space.Tight.value - 0.5f,
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
        compose.onNodeWithTag("playback-speed")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
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
     * The place and the key are whole at the pill's capped type (owner
     * decision, this session). The pill's type follows the system scale only
     * up to the app's largest step, 1.2, and stops there. The line is the
     * capsule's width less the shoulder at each end, 288 dp; the place and
     * the key never give way, so they are whole at every system scale and the
     * voice is the part an ellipsis would take.
     */
    @Test
    fun thePlaceAndTheKeyStayWholeAtTheLargestStep() {
        showPill(
            pillWidth = Modifier.width(360.dp),
            reference = "60:13",
            placeName = "Al-Mumtahanah",
            fontScale = 1.3f,
        )
        val place = layoutOf("Al-Mumtahanah")
        assertFalse("the place must be whole at 1.3: $place", place.isLineEllipsized(0))
        val key = layoutOf("60:13")
        assertFalse("the ayah key must be whole at 1.3: $key", key.isLineEllipsized(0))
    }

    /**
     * The place is the pill's and the pane's, not a line the panel draws
     * (owner decision, this session). The compact pill says the surah and the
     * key, the popup speaks the whole place as its pane title, and the panel
     * opens on its first question: the same sentence in the same type, 10 dp
     * apart, was the app's only drawn popup title.
     */
    @Test
    fun thePlaceIsThePillsAndThePanes() {
        showPill(state = PlaybackUiState(ayahNumber = 255))
        compose.onNodeWithTag("playback-words").performClick()
        // The capsule under the panel still says the place and the key.
        compose.onNodeWithText("Al-Baqarah", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("2:255", useUnmergedTree = true).assertExists()
        // The panel still names the whole place to a screen reader, and draws
        // none of it.
        compose.onNode(
            hasTestTag("app-popup") and
                SemanticsMatcher("the panel's pane title") { node ->
                    node.config.getOrNull(SemanticsProperties.PaneTitle)
                        ?.contains("Al-Baqarah, Ayah 255") == true
                },
        ).assertExists()
        compose.onNodeWithText("Al-Baqarah, Ayah 255").assertDoesNotExist()
    }

    /**
     * The playing pill wears its own two-row shape (owner decision, this
     * session): one 96 dp capsule whose words row is 48 dp, whose transport
     * row is 48 dp, and whose visible air is the type's own inset and the
     * circles' inset. Nothing in it touches a neighbour or the capsule's edge.
     */
    @Test
    fun thePlayingPillWearsItsOwnShape() {
        showPill()
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val circles = compose.onAllNodesWithTag("glyph-press", useUnmergedTree = true)
        val first = circles.onFirst().getUnclippedBoundsInRoot()
        val play = compose.onNodeWithContentDescription("Play or pause").getUnclippedBoundsInRoot()
        val place = compose.onNodeWithText("Al-Baqarah", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(
            "the playing capsule stands 96 dp tall",
            96f,
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
            "the words stand at the capsule's top",
            bar.top.value,
            words.top.value,
            1f,
        )
        assertEquals(
            "the transport's target is the capsule's foot",
            bar.bottom.value,
            play.bottom.value,
            1f,
        )
        assertTrue(
            "the words keep the shoulder at each end: words $words, bar $bar",
            words.left.value - bar.left.value >= Space.Block.value + Space.Tight.value - 0.5f &&
                bar.right.value - words.right.value >=
                Space.Block.value + Space.Tight.value - 0.5f,
        )
        assertTrue(
            "the type and the first circle keep the app's row air: " +
                "place $place, circle $first",
            first.top.value - place.bottom.value >= Space.Row.value - 0.5f,
        )
        assertEquals(
            "the circle keeps its inset at the capsule's foot",
            (GlyphTarget.value - GlyphPress.value) / 2f,
            (bar.bottom - first.bottom).value,
            1f,
        )
        assertEquals(
            "the transport stands on the capsule's middle",
            (bar.left + bar.right).value / 2f,
            (play.left + play.right).value / 2f,
            1.5f,
        )
    }

    /**
     * Nothing in the pill touches, and nothing sits on a dead pixel either
     * (owner decision, this session): the words' row and the transport's row
     * meet at one edge, the three circles stand the app's line air apart, and
     * the targets under them stand edge to edge, so a finger that slides from
     * one control to the next never falls between them.
     *
     * The air is measured between the shapes the reader sees, never between
     * the invisible targets: each circle is 4 dp inside its own target, and
     * that inset is the whole of what lets three of them stand edge to edge
     * with their circles apart. The type and the first circle keep the app's
     * row air between the rows.
     */
    @Test
    fun theTouchedShapesKeepTheCapsulesAir() {
        showPill()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val place = compose.onNodeWithText("Al-Baqarah", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
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
        assertTrue(
            "the type and the first circle keep the app's row air: " +
                "place $place, circle $first",
            first.top.value - place.bottom.value >= Space.Row.value - 0.5f,
        )
        assertEquals(
            "the words' row and the first target meet at one edge",
            0f,
            (previousTarget.top - words.bottom).value,
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
     * The offer is a two-row capsule (owner decision, this session): one 48 dp
     * line that is the door to the voice list, then the two answers as one
     * centered pair. The capsule is 96 dp tall, the least that gives the door
     * its 48 dp target and the buttons theirs.
     */
    @Test
    fun theOfferIsTwoRowsAndCentered() {
        showPill(
            offer = ListenOffer(
                reciter = "husary",
                reciterName = "Husary",
                surah = 19,
                surahName = "Maryam",
                ayah = 1,
                bytes = 22L * 1024 * 1024,
                options = offerOptions(),
            ),
        )
        val bar = compose.onNodeWithTag("listen-offer-bar").getUnclippedBoundsInRoot()
        val words = compose.onNodeWithTag("offer-words").getUnclippedBoundsInRoot()
        val notNow = compose.onNodeWithTag("offer-not-now", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val answer = compose.onNodeWithTag("offer-answer", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val center = (bar.left + bar.right) / 2
        assertEquals(
            "the ask is the 96 dp capsule",
            96f,
            (bar.bottom - bar.top).value,
            1f,
        )
        assertEquals(
            "the offer stands at the pill's one measure",
            328f,
            (bar.right - bar.left).value,
            1f,
        )
        assertEquals(
            "the door stands at the capsule's top",
            bar.top.value,
            words.top.value,
            1f,
        )
        assertEquals(
            "the door is a 48 dp target",
            48f,
            (words.bottom - words.top).value,
            1f,
        )
        assertEquals(
            "the request's one line stands on the capsule's middle",
            center.value,
            ((words.left + words.right) / 2).value,
            1.5f,
        )
        assertEquals(
            "the answer row is the capsule's 48 dp foot",
            48f,
            (bar.bottom - words.bottom).value,
            1f,
        )
        assertEquals(
            "the two answers stand as one centered pair",
            center.value,
            ((notNow.left + answer.right) / 2).value,
            1.5f,
        )
        assertEquals(
            "the answers share one row under the words",
            answer.top.value,
            notNow.top.value,
            1f,
        )
        assertTrue(
            "the answers stand under the request: words $words, answer $answer",
            answer.top.value >= words.bottom.value,
        )
        assertTrue(
            "Not now leads and the answer trails",
            notNow.right.value < answer.left.value,
        )
        assertEquals(
            "the pair keeps the app's action gap between the pills",
            Space.Action.value,
            (answer.left - notNow.right).value,
            1.5f,
        )
    }

    /**
     * The offer's line is the door to its own voice list (owner decision, this
     * session): the same reciter rows the playing pill carries, with what each
     * voice would still need for the offer's surah. A chosen voice reaches the
     * pill's one setter, which re-asks the offer with its name and size.
     */
    @Test
    fun theOfferLineOpensTheVoiceListForItsOwnSurah() {
        var chosen: String? = null
        showPill(
            offer = ListenOffer(
                reciter = "husary",
                reciterName = "Husary",
                surah = 19,
                surahName = "Maryam",
                ayah = 1,
                bytes = 22L * 1024 * 1024,
                options = offerOptions(),
            ),
            onReciter = { chosen = it },
        )
        compose.onNodeWithTag("offer-words").performClick()
        compose.onNodeWithTag("app-popup").assertExists()
        compose.onNodeWithText("Minshawi").assertIsDisplayed()
        compose.onNodeWithText(
            formatBytes(177_000_000L, Locale.US, "KB", "MB"),
        ).assertIsDisplayed()
        compose.onNodeWithText("Minshawi").performClick()
        assertEquals("the offer's own list must reach the pill's setter", "minshawi", chosen)
        compose.onNodeWithTag("app-popup").assertDoesNotExist()
    }

    /** The two voices the offer's own chooser lists in these legs. */
    private fun offerOptions(): List<ListenOption> = listOf(
        ListenOption("husary", "Husary", 22L * 1024 * 1024),
        ListenOption("minshawi", "Minshawi", 177_000_000L),
    )

    /**
     * The offer prints the surah's own name whole in the line it shares with
     * the reciter and the size, in the reader's own language: the name is what
     * the reader reads, so it is the promise, and the size is what gives way
     * when the line is at its longest (owner decision, this session).
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
                options = offerOptions(),
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
                options = offerOptions(),
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
     * A door says its state on the mark that opened it (owner decision, this
     * session). The playing line's chevron turns up while the listening panel
     * is on the glass, and the line speaks the app's Expanded and Collapsed
     * words, so the reader who cannot see the turn is told the same thing.
     */
    @Test
    fun thePlayingDoorSaysWhenItsPanelIsOpen() {
        showPill()
        assertEquals(
            "the door starts closed",
            "Collapsed",
            doorState("playback-words"),
        )
        compose.onNodeWithTag("playback-words").performClick()
        compose.onNodeWithTag("app-popup").assertExists()
        assertEquals(
            "the door says its panel is open",
            "Expanded",
            doorState("playback-words"),
        )
        compose.onNodeWithText("Minshawi").performClick()
        compose.onNodeWithTag("app-popup").assertDoesNotExist()
        assertEquals(
            "the door is closed once its answer puts the panel away",
            "Collapsed",
            doorState("playback-words"),
        )
    }

    /**
     * The offer's line is a door too, so it says the same two words. It is the
     * first pill the reader meets on an undownloaded surah, which makes its
     * silent state the one a reader is most likely to meet first.
     */
    @Test
    fun theOffersDoorSaysWhenItsChooserIsOpen() {
        showPill(
            offer = ListenOffer(
                reciter = "husary",
                reciterName = "Husary",
                surah = 19,
                surahName = "Maryam",
                ayah = 1,
                bytes = 22L * 1024 * 1024,
                options = offerOptions(),
            ),
        )
        assertEquals(
            "the door starts closed",
            "Collapsed",
            doorState("offer-words"),
        )
        compose.onNodeWithTag("offer-words").performClick()
        compose.onNodeWithTag("app-popup").assertExists()
        assertEquals(
            "the door says its chooser is open",
            "Expanded",
            doorState("offer-words"),
        )
        compose.onNodeWithText("Minshawi").performClick()
        compose.onNodeWithTag("app-popup").assertDoesNotExist()
        assertEquals(
            "the door is closed once its choice puts the chooser away",
            "Collapsed",
            doorState("offer-words"),
        )
    }

    /**
     * The one-row voices keep the same door: the reciter's name opens the
     * voice list alone, and it speaks and turns with it (owner decision, this
     * session).
     */
    @Test
    fun theVoiceDoorSaysWhenTheVoiceListIsOpen() {
        showPill(state = PlaybackUiState(unavailable = true))
        assertEquals(
            "the door starts closed",
            "Collapsed",
            doorState("playback-reciter"),
        )
        compose.onNodeWithTag("playback-reciter").performClick()
        compose.onNodeWithTag("app-popup").assertExists()
        assertEquals(
            "the door says the voice list is open",
            "Expanded",
            doorState("playback-reciter"),
        )
        compose.onNodeWithText("Minshawi").performClick()
        compose.onNodeWithTag("app-popup").assertDoesNotExist()
        assertEquals(
            "the door is closed once its choice puts the list away",
            "Collapsed",
            doorState("playback-reciter"),
        )
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
        compose.onNodeWithText("Al-Baqarah").assertIsDisplayed()
        assertEquals(
            "the pill must not carry the end of the audio",
            0,
            compose.onAllNodesWithText("repeating the surah", substring = true)
                .fetchSemanticsNodes().size,
        )
    }

    @Test
    fun thePillReadsBackAPaceThatIsNotTheDefault() {
        // The panel is where the pace is set; the pill is where it is read
        // back after the panel closes. The line leads with the voice, then
        // the pace, then the place and its key (owner decision, this
        // session).
        showPill(speed = 0.75f)
        compose.onNodeWithText("Al-Baqarah").assertIsDisplayed()
        compose.onNodeWithText("0.75x", useUnmergedTree = true).assertExists()
    }

    @Test
    fun thePillSaysNothingAtTheDefaultPace() {
        // 1x is the pace the app starts at, so the line does not say it.
        showPill(speed = 1f)
        compose.onAllNodesWithText("1x", useUnmergedTree = true).assertCountEquals(0)
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

    /**
     * What a door says about itself, the way a screen reader hears it. The
     * chevron's turn is not in the semantics, so the turn itself is pinned as
     * arithmetic in `AppChevronTurnTest`; this reads the words beside it.
     */
    private fun doorState(tag: String): String? = compose
        .onNodeWithTag(tag)
        .fetchSemanticsNode()
        .config
        .getOrElseNullable(SemanticsProperties.StateDescription) { null }

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
