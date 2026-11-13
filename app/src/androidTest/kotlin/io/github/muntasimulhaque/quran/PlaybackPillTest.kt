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
 * 4. The playing pill is one row, and the offer is its sibling (owner
 *    decision, this session): a 64 dp capsule with the words' lockup at the
 *    leading end and one answer at the trailing one. The offer is the pill's
 *    own two-line block: the reciter's name over the surah and its size, with
 *    Download. The voices that are not a recitation keep the same 64 dp row,
 *    or 74 with a download's line under it. The four stand at one width.
 * 5. The playing words are one block, one door, and a lockup: the voice and
 *    its pace over the place and its key, with the chevron at the end of the
 *    place's line. The one-row voices lead with the reciter's name beside the
 *    state's sentence. Nothing in the pill touches a neighbour or the capsule:
 *    the playing words keep the shoulder's air at the leading end, the
 *    one-row voices keep it at their words' end, the words' shape and the
 *    answer's circle keep the app's shape air, and the circle keeps its inset
 *    inside its own target.
 * 6. The reciter is chosen on the playing pill, in the settings, and on the
 *    offer's own line, each with what every voice would still cost for the
 *    surah at hand, so the choice is made with the number in view (owner
 *    report). The offer's list is read for the offer's surah, not the playing
 *    pill's (owner decision, this session).
 * 7. The pill says who is reading, where, and how fast: the voice and the pace
 *    when it is not 1x over the place and its key. The place, the key, and the
 *    pace never give way; the voice does, and it is the only part of the
 *    lockup that can (owner decision, this session). The pace is read back from
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
 *     360 dp glass. The playing words lead and its transport closes; the
 *     offer centers its door line and its pair; the transfer keeps the words
 *     and the X at its ends. The place's line is whole at the app's text
 *     sizes, and the ayah's key is whole at every one.
 * 11. The offer prints the surah's own name whole on the place line, and the
 *     voice's size whole beside the voice, in either language: the size is
 *     the voice's value, so the voice is the only part that gives way (owner
 *     report, this session).
 * 12. The transfer's X keeps the capsule's air at its end, and the block and
 *     the transport stand the app's row air apart, measured between the shapes
 *     the reader sees (owner decision).
 * 13. The recitation's stop has no shape on the glass and is still reachable:
 *     it is the action TalkBack offers on Play or pause, so the close's own
 *     act is not lost with the close's mark (owner decision, this session).
 * 14. The one X the app keeps is the transfer's own abort, and it is the last
 *     thing in the row: a package on its way is the one thing a stray tap on
 *     the page must never end (owner decision, this session).
 * 15. The answer is the app's one round control: a 40 dp press circle inside
 *     its 48 dp target (owner decision, this session).
 * 16. The offer is one row, 64 dp tall: the whole two-line block is the door
 *     to the voice list, the reciter's name and its size over the surah, and
 *     Download is the only answer. The size stands with the voice it prices,
 *     so a change of voice explains a change of price, and the place is whole
 *     at both languages' longest. An offer with no voice to offer keeps the
 *     block as a notice and draws no chevron (owner report, this session).
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
        /** The two skips, which are actions on the answer now. */
        onPrevious: () -> Unit = {},
        onNext: () -> Unit = {},
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
         * The offer's own size, where a leg needs the real words in the
         * reader's language: the harness builds an English one by default,
         * and the test process's own strings are always English.
         */
        offerPrice: String? = null,
        /** The reader's language, which is the voice and the digits. */
        language: UiLanguage = UiLanguage.English,
    ) {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = language) {
                val pill: @Composable () -> Unit = {
                    PlaybackBar(
                        state = state,
                        offer = offer,
                        offerPrice = offerPrice ?: offer?.let { pending ->
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
                        onNext = onNext,
                        onPrevious = onPrevious,
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
                        offerPrice = "",
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
     * The playing pill is one row (owner decision, this session): one 64 dp
     * capsule whose words lead and whose answer closes it. It used to stand
     * 96 dp tall, the words over a transport row, and that second row was
     * three marks centered in the capsule's whole measure, which read as an
     * empty band under the words (owner report, this session). 64 is the
     * height the pill's one-row voices already wear: 8 dp, the finger's 48 dp,
     * 8 dp.
     */
    @Test
    fun thePlayingPillSharesOneRowWithTheAnswer() {
        showPill(pillWidth = Modifier.width(360.dp))
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val play = compose.onNodeWithContentDescription("Play or pause").getUnclippedBoundsInRoot()
        assertEquals(
            "the playing capsule stands 64 dp tall",
            64f,
            (bar.bottom - bar.top).value,
            1f,
        )
        assertEquals(
            "the words' door is the finger's 48 dp",
            48f,
            (words.bottom - words.top).value,
            1f,
        )
        assertEquals(
            "the words stand on the capsule's own middle",
            (bar.top + bar.bottom).value / 2f,
            (words.top + words.bottom).value / 2f,
            1f,
        )
        assertEquals(
            "the answer stands on the capsule's own middle",
            (bar.top + bar.bottom).value / 2f,
            (play.top + play.bottom).value / 2f,
            1f,
        )
        assertTrue(
            "the words lead the answer: words $words, play $play",
            words.right.value <= play.left.value,
        )
        assertTrue(
            "the answer closes the capsule: play $play, bar $bar",
            play.right.value < bar.right.value,
        )
    }

    /**
     * The playing words are a lockup in the offer's own order (owner decision,
     * this session): the voice and its pace over the place and its key, one
     * line each, with the place's key closing the second. One line cannot hold
     * the four facts beside the answer at the top step, and stacking them costs the
     * block nothing, because a lockup is as wide as its wider line and never
     * the sum of both.
     */
    @Test
    fun thePlayingPillLeadsWithTheVoiceAboveThePlace() {
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
            "the voice's line must stand above the place's: $voice and $place",
            voice.bottom.value <= place.top.value + 0.5f,
        )
        assertEquals(
            "the two lines share one leading edge: $voice and $place",
            voice.left.value,
            place.left.value,
            0.5f,
        )
        assertTrue(
            "the pace must follow the voice: $voice and $pace",
            voice.right.value <= pace.left.value + 0.5f,
        )
        assertEquals(
            "the pace shares the voice's line: $pace and $voice",
            (voice.top + voice.bottom).value / 2f,
            (pace.top + pace.bottom).value / 2f,
            1.5f,
        )
        assertTrue(
            "the place must lead its key: $place and $key",
            place.right.value <= key.left.value + 0.5f,
        )
        assertEquals(
            "the key shares the place's line: $place and $key",
            (place.top + place.bottom).value / 2f,
            (key.top + key.bottom).value / 2f,
            1.5f,
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
     * The two skips and the stop have no shape any more, and they are not lost
     * with it: a screen reader finds all three on Play or pause, under the words
     * they were announced with (owner decision, this session). The skips left
     * the glass when the pill became one row: three 48 dp targets cut the place
     * the pill never cuts (see `pillControls`), so they live where Stop already
     * lived, and the media notification keeps them for every reader.
     */
    @Test
    fun theSkipsAndTheStopAreOfferedToAScreenReaderOnTheAnswer() {
        var stopped = false
        var previous = false
        var next = false
        showPill(
            onStop = { stopped = true },
            onPrevious = { previous = true },
            onNext = { next = true },
        )
        val actions = compose.onNodeWithContentDescription("Play or pause")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(
            "one answer offers its three actions",
            listOf("Previous ayah", "Next ayah", "Stop"),
            actions.map { it.label },
        )
        compose.runOnUiThread { actions.single { it.label == "Previous ayah" }.action() }
        compose.runOnUiThread { actions.single { it.label == "Next ayah" }.action() }
        compose.runOnUiThread { actions.single { it.label == "Stop" }.action() }
        assertTrue("the previous action must reach the pill's own skip", previous)
        assertTrue("the next action must reach the pill's own skip", next)
        assertTrue("the stop action must reach the pill's own stop", stopped)
    }

    /**
     * The capsule is one measure in every voice (owner decision, this
     * session). It used to follow its words, from about 285 dp at the shortest
     * places to the page's own 328 at the longest while playing, and a share
     * of that while an offer or a transfer stood: one download was two widths,
     * and the transfer's edge moved as its percent did. Now the playing
     * capsule, the offer, and the transfer stand at the page's own 328 on a
     * 360 dp glass. The playing words lead and the transport closes; the
     * offer centers its door and its pair; the transfer keeps its words and
     * its X at the two ends.
     */
    @Test
    fun theCapsuleIsOneMeasure() {
        showPill(pillWidth = Modifier.width(360.dp))
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val play = compose.onNodeWithContentDescription("Play or pause")
            .getUnclippedBoundsInRoot()
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
            "the words keep the shoulder at the capsule's leading end: " +
                "words $words, bar $bar",
            Space.Block.value + Space.Tight.value,
            (words.left - bar.left).value,
            0.5f,
        )
        assertEquals(
            "the answer keeps the capsule's air at its trailing end: " +
                "play $play, bar $bar",
            Space.Line.value,
            (bar.right - play.right).value,
            1.5f,
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
     * up to the app's largest step, 1.2, and stops there. The place's line is
     * the room the answer's one 48 dp target leaves, less the shoulder and
     * the block's own inset; the place and the key never give way, so they are
     * whole at every system scale and the voice is the part an ellipsis would
     * take.
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
     * The same promise in the wider script, at the same step (owner decision,
     * this session). Bangla is where the line is longest: `আল-মুতাফফিফীন` is
     * the content's own longest surah name, and its 36 is the widest key on it.
     * This is the leg that chose the answer's one mark: beside three 48 dp
     * targets the line was 174 dp of text in 148, so the place was cut, and the
     * place is the one thing the pill never cuts.
     */
    @Test
    fun theLongestBanglaPlaceStaysWholeAtTheLargestStep() {
        showPill(
            pillWidth = Modifier.width(360.dp),
            language = UiLanguage.Bangla,
            reference = "৮৩:৩৬",
            placeName = "আল-মুতাফফিফীন",
            fontScale = 1.3f,
        )
        val place = layoutOf("আল-মুতাফফিফীন")
        assertFalse("the place must be whole at 1.3: $place", place.isLineEllipsized(0))
        val key = layoutOf("৮৩:৩৬")
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
     * The playing pill wears one row (owner decision, this session): a 64 dp
     * capsule whose words are a 48 dp door at the leading end and whose answer
     * closes it, with the circle 4 dp inside its target. Nothing in it touches
     * a neighbour or the capsule's edge.
     */
    @Test
    fun thePlayingPillWearsItsOwnShape() {
        showPill()
        val bar = compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val circles = compose.onAllNodesWithTag("glyph-press", useUnmergedTree = true)
        val first = circles.onFirst().getUnclippedBoundsInRoot()
        val place = compose.onNodeWithText("Al-Baqarah", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
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
            "the words stand on the capsule's middle",
            (bar.top + bar.bottom).value / 2f,
            (words.top + words.bottom).value / 2f,
            1f,
        )
        assertTrue(
            "the words keep the shoulder at the leading end: words $words, bar $bar",
            words.left.value - bar.left.value >= Space.Block.value + Space.Tight.value - 0.5f,
        )
        assertTrue(
            "the lockup's two lines stand inside the door: " +
                "place $place, words $words",
            place.top.value > words.top.value && place.bottom.value < words.bottom.value,
        )
        assertEquals(
            "the circle keeps the capsule's air and its own inset at the foot",
            Space.Line.value + (GlyphTarget.value - GlyphPress.value) / 2f,
            (bar.bottom - first.bottom).value,
            1f,
        )
    }

    /**
     * Nothing in the pill touches, and nothing sits on a dead pixel either
     * (owner decision, this session): the words' shape and the answer's circle
     * keep the app's shape air apart, and the circle keeps its inset inside the
     * target the finger gets.
     *
     * The air is measured between the shapes the reader sees, never between
     * the invisible targets: the circle is 4 dp inside its own target, which is
     * why [GlyphLead] gives that inset back on the words' side.
     */
    @Test
    fun theTouchedShapesKeepTheCapsulesAir() {
        showPill()
        val words = compose.onNodeWithTag("playback-words").getUnclippedBoundsInRoot()
        val circles = compose.onAllNodesWithTag("glyph-press", useUnmergedTree = true)
        circles.assertCountEquals(1)
        val play = circles.onFirst().getUnclippedBoundsInRoot()
        val playTarget = compose.onNodeWithContentDescription("Play or pause")
            .getUnclippedBoundsInRoot()
        // The words are content-sized at the leading end and the answer stands
        // at the trailing one, so the air between them is the capsule's own
        // ground: at least the app's shape air, and more whenever the place is
        // short. The floor is what the day it is long is promised.
        assertTrue(
            "the words' shape and the answer's circle keep at least the app's " +
                "shape air: words $words, circle $play",
            (play.left - words.right).value >= Space.Row.value - 0.5f,
        )
        assertEquals(
            "the circle stands its own inset inside its target",
            (GlyphTarget.value - GlyphPress.value) / 2f,
            (play.left - playTarget.left).value,
            1f,
        )
        assertEquals(
            "the target keeps the capsule's air at its trailing end",
            Space.Line.value,
            (compose.onNodeWithTag("playback-bar").getUnclippedBoundsInRoot().right.value -
                playTarget.right.value),
            1.5f,
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
     * The offer is one row (owner decision, this session): the words' lockup
     * at the leading end and the one answer at the trailing one. Its 64 dp is
     * the playing pill's own height, 8 + 48 + 8; the 96 dp of the two-row
     * capsule went with Not now, and the place line keeps the surah and its
     * size whole.
     */
    @Test
    fun theOfferIsOneRow() {
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
        val answer = compose.onNodeWithTag("offer-answer", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val centerY = (bar.top + bar.bottom) / 2
        assertEquals(
            "the ask is the 64 dp capsule",
            64f,
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
            "the words are the finger's 48 dp row",
            48f,
            (words.bottom - words.top).value,
            1f,
        )
        assertEquals(
            "the words keep the capsule's shoulder",
            Space.Block.value + Space.Tight.value,
            (words.left - bar.left).value,
            1f,
        )
        assertEquals(
            "the answer keeps the capsule's air at its end",
            Space.Line.value,
            (bar.right - answer.right).value,
            1f,
        )
        assertEquals(
            "the words and the answer keep the app's row air",
            Space.Row.value,
            (answer.left - words.right).value,
            1.5f,
        )
        assertEquals(
            "the words stand on the capsule's middle",
            centerY.value,
            ((words.top + words.bottom) / 2).value,
            1f,
        )
        assertEquals(
            "the answer stands on the capsule's middle",
            centerY.value,
            ((answer.top + answer.bottom) / 2).value,
            1f,
        )
        compose.onNodeWithTag("offer-not-now", useUnmergedTree = true).assertDoesNotExist()
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
     * The offer prints the surah's own name whole on its place line, and the
     * voice's own size whole beside the voice, in the reader's own language
     * (owner report, this session). The size is a fact about the voice: the
     * bytes are that reciter's timings plus that reciter's audio for the
     * surah, the list the block opens prices every voice, and choosing a voice
     * asks the offer again with that voice's size. The size used to share the
     * place line with the surah, where the longest name made it the first
     * thing an ellipsis took.
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
            offerPrice = "9.0 MB",
        )
        assertTheOfferPrintsThePlaceAndSizeWhole(
            place = "Al-Mumtahanah",
            size = "9.0 MB",
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
            offerPrice = "৫.৮ এমবি",
        )
        assertTheOfferPrintsThePlaceAndSizeWhole(
            place = "আল-মুতাফফিফীন",
            size = "৫.৮ এমবি",
        )
    }

    /**
     * The size is never the part the line cuts (owner report, this session).
     * The voice gives way, because the size stands after the name and before
     * it there is only the name: at a name longer than any the two shipped
     * reciters carry, the credit the app's own catalog prints,
     * `Muhammad Siddiq Al-Minshawi`, the size is still whole and the name is
     * the one an ellipsis takes.
     */
    @Test
    fun theOfferNeverCutsTheSize() {
        showPill(
            pillWidth = Modifier.width(360.dp),
            offer = ListenOffer(
                reciter = "minshawi-full",
                reciterName = "Muhammad Siddiq Al-Minshawi",
                surah = 2,
                surahName = "Al-Baqarah",
                ayah = 1,
                bytes = 177L * 1024 * 1024,
                options = offerOptions(),
            ),
            offerPrice = "177 MB",
        )
        assertFalse(
            "the size must be whole: ${layoutOf("177 MB")}",
            layoutOf("177 MB").isLineEllipsized(0),
        )
        assertTrue(
            "the voice must be the part that gives way",
            layoutOf("Muhammad Siddiq Al-Minshawi").isLineEllipsized(0),
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
     * The answer's press is the app's one round control: a 40 dp circle inside
     * the 48 dp target the finger gets, the same circle every icon button in
     * the app lights. It is not the 20 dp glyph, because an indication on the
     * glyph flashed a dot inside the button the finger hit (owner report, an
     * earlier session), and it is not the whole target, because the circle is
     * the shape the reader sees (owner decision, this session).
     */
    @Test
    fun theAnswerPressIsTheAppRoundControl() {
        showPill()
        // The shape is a child of the clickable, which merges its
        // descendants, so the tag is read from the unmerged tree.
        val shapes = compose.onAllNodesWithTag("glyph-press", useUnmergedTree = true)
        shapes.assertCountEquals(1)
        shapes.onFirst().assertWidthIsEqualTo(GlyphPress)
        shapes.onFirst().assertHeightIsEqualTo(GlyphPress)
        // The target under the circle is still the whole 48 dp a finger gets.
        compose.onNodeWithContentDescription("Play or pause").assertWidthIsEqualTo(GlyphTarget)
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
     * The offer must print the place and the size whole, and the size must
     * stand on the voice's line above the place (owner report, this session).
     * The claim is read from the text's own layout through the semantics
     * action Compose gives every `Text`: `isLineEllipsized` is true where the
     * line gave something up, and the bounds are what the eye sees.
     */
    private fun assertTheOfferPrintsThePlaceAndSizeWhole(place: String, size: String) {
        val placeLayout = layoutOf(place)
        assertFalse(
            "the offer must print the place whole: $placeLayout",
            placeLayout.isLineEllipsized(0),
        )
        val sizeLayout = layoutOf(size)
        assertFalse(
            "the offer must print the size whole: $sizeLayout",
            sizeLayout.isLineEllipsized(0),
        )
        val placeBounds = compose.onNodeWithText(place, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val sizeBounds = compose.onNodeWithText(size, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "the size must stand with the voice, above the place: size $sizeBounds, " +
                "place $placeBounds",
            (sizeBounds.bottom - placeBounds.top).value <= 1f,
        )
    }
}
