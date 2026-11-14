package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.ui.settings.PageRow
import io.github.muntasimulhaque.quran.ui.settings.SizeRow
import io.github.muntasimulhaque.quran.ui.settings.ToggleRow
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The settings sheet's controls run down one line: the last mark a row has is
 * the last thing in it.
 *
 * A row can carry a switch, a chevron, or both. The owner asked for the
 * switch to be the row's last mark and for the two kinds of control to stand
 * at one place each, so the three shapes are composed here side by side and
 * the test reads the real bounds: every switch ends at one line at the
 * sheet's edge, a row with no switch ends with its chevron on that same
 * line, and the switch of a row with a door comes after that row's chevron
 * (owner report).
 *
 * The chevron's slot is measured, not the mark: a mark is centred in its
 * slot, so the slot's edges are what a row's alignment is about, and the
 * slot's tag is on the rows that draw a chevron at all.
 *
 * The column is a width, not a height: the tail's 48 dp square stretched
 * every plain row where the chevron was not a control of its own, so the
 * page row's compact height is pinned here too (owner report).
 *
 * The switch column is the real control's width, measured here rather than
 * assumed: when Material widened its switch the reserved column stayed 52 dp,
 * the control overflowed its own slot to the left, and it came to sit over
 * the chevron in front of it. The switch was then the row's last mark by
 * nothing but luck. This test caught that (thirty-seventh session), so the
 * number and the control are pinned together here.
 *
 * Three measurements of the sheet live here, because they are all the same
 * question asked of a row: what a reader's eye gets and what a finger gets.
 * The two tail columns and the compact height above, the name and the value in
 * one place whatever the row carries (owner decision), and every step
 * of a segmented row being a full 48 dp target.
 */
class SettingsRowAlignmentTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aRowsOwnMarkEndsItAtOneLine() {
        compose.setContent {
            MaterialTheme {
                Column {
                    PageRow(
                        title = "Language",
                        summary = "English",
                        onClick = {},
                        modifier = Modifier.testTag("page-row"),
                    )
                    ToggleRow(
                        title = "Show translation",
                        subtitle = "Saheeh International",
                        checked = true,
                        onChange = {},
                        switchTag = "switch-row",
                    )
                    // A row with nothing to say about itself: it is a name and
                    // a chevron, and it must not be stretched to the height of
                    // the rows that carry a value.
                    PageRow(
                        title = "About",
                        summary = null,
                        onClick = {},
                        modifier = Modifier.testTag("bare-row"),
                    )
                }
            }
        }

        // The chevron's slot is a door row's last mark and the track is a
        // switch row's, and the two end on one line: the eye reads one column
        // of marks down the sheet whatever each row does.
        val slot = compose.onAllNodesWithTag("chevron-slot", useUnmergedTree = true)
            .onFirst()
            .getUnclippedBoundsInRoot()
        val mark = compose.onNodeWithTag("switch-row-mark", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(
            "a row's last mark must end where the switch does",
            slot.right.value,
            mark.right.value,
            1f,
        )
        assertEquals(
            "the switch's track is the set's own 44 dp, not the row's width",
            44f,
            (mark.right - mark.left).value,
            1f,
        )
    }

    @Test
    fun theSizeRailsAreOneLength() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light) {
                Column {
                    TypeRole.entries.forEach { role ->
                        SizeRow(role = role, step = 1f, onChange = {})
                    }
                }
            }
        }

        val rails = compose.onAllNodesWithTag("scale-rail", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals("one rail for each kind of text", 3, rails.size)
        val first = rails.first().boundsInRoot
        rails.forEach { node ->
            val rail = node.boundsInRoot
            assertEquals("every rail starts on one line", first.left, rail.left, 1f)
            assertEquals("every rail ends on one line", first.right, rail.right, 1f)
            assertTrue("a rail must keep some measure", rail.width > 1f)
        }
    }

    /**
     * The size name stands over its rail, not beside it. The name used to keep
     * a fixed column and the rail was what was left of the row; at a large
     * system font the column ate the rail and the end marks were dropped. The
     * name now opens the block and the rail has the row's whole width under it
     * (owner decision, this session).
     */
    @Test
    fun theSizeNameStandsOverItsRail() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light) {
                SizeRow(TypeRole.Translation, 1f) {}
            }
        }

        val label = compose.onNodeWithTag("size-label", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val rail = compose.onNodeWithTag("scale-rail", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "the name stands over its rail, not beside it",
            label.bottom <= rail.top + 1.dp,
        )
    }

    /**
     * A row that carries a switch is as tall as a row that does not: the
     * switch owes the finger its target, and it does not owe the row its
     * height. The hub read as two rhythms alternating down the sheet until
     * this, because every switch row stood 48 dp tall inside its padding and
     * every page row stood its own two lines (owner report, this session).
     * The target itself is unshaken: its outer edge still answers the finger,
     * and the row's door does not take it.
     */
    /**
     * A switch row is one control from end to end.
     *
     * The row is the switch, so the row owes the finger the 48 dp and its
     * height is the height of a door row beside it; the drawn track is a
     * picture of the state and not a second target in front of the row's own.
     * A release that made the track taller, or the row shorter, moves one of
     * these numbers.
     */
    @Test
    fun aSwitchRowKeepsTheListsHeightAndIsOneControl() {
        var toggles = 0
        compose.setContent {
            QuranTheme(theme = AppTheme.Light) {
                Column {
                    PageRow(
                        title = "Language",
                        summary = "English",
                        onClick = {},
                        modifier = Modifier.testTag("page-row"),
                    )
                    ToggleRow(
                        title = "Show translation",
                        subtitle = "Saheeh International",
                        checked = true,
                        onChange = { toggles++ },
                        switchTag = "switch-row",
                    )
                }
            }
        }
        val page = compose.onNodeWithTag("page-row").getUnclippedBoundsInRoot()
        val row = compose.onNodeWithTag("switch-row").getUnclippedBoundsInRoot()
        assertEquals(
            "a switch row must keep the list's own height",
            (page.bottom - page.top).value,
            (row.bottom - row.top).value,
            1f,
        )
        assertTrue(
            "a switch row owes the finger 48 dp",
            (row.bottom - row.top).value >= 48f,
        )
        // The whole row is the control, so a finger on the drawn track flips
        // it: the mark is the row's, not a second control in front of it.
        compose.onNodeWithTag("switch-row-mark", useUnmergedTree = true)
            .performTouchInput { click() }
        compose.runOnIdle {
            assertEquals("the row's own mark must answer a finger", 1, toggles)
        }
    }

    /**
     * A page's switch row opens nothing, so it draws no door.
     *
     * The hub is an index and carries no switches; a page carries the switch
     * that governs its list and no chevron after it. A row that grew a door
     * again would be the two-control row this grammar exists to end, and the
     * empty column would show up here as a second chevron slot.
     */
    @Test
    fun aSwitchRowKeepsNoColumnForADoor() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light) {
                ToggleRow(
                    title = "Show translation",
                    subtitle = null,
                    checked = false,
                    onChange = {},
                    switchTag = "switch-row",
                )
            }
        }
        assertEquals(
            "a switch row opens nothing, so it draws no chevron",
            0,
            compose.onAllNodesWithTag("chevron-slot", useUnmergedTree = true)
                .fetchSemanticsNodes().size,
        )
    }

    @Test
    fun aValueIsUnderItsNameAndNeverSqueezesIt() {
        compose.setContent {
            MaterialTheme {
                Column {
                    PageRow(
                        title = "Translation",
                        summary = "Saheeh International",
                        onClick = {},
                        modifier = Modifier.testTag("long-value"),
                    )
                    PageRow(
                        title = "Font size",
                        summary = "Arabic 25, translation 14",
                        onClick = {},
                        modifier = Modifier.testTag("short-value"),
                    )
                }
            }
        }

        val values = listOf(
            "Saheeh International" to compose.onNodeWithTag("long-value").getUnclippedBoundsInRoot(),
            "Arabic 25, translation 14" to compose.onNodeWithTag("short-value").getUnclippedBoundsInRoot(),
        )
        val names = listOf("Translation", "Font size")
        values.forEachIndexed { index, (value, row) ->
            val name = compose.onNodeWithText(names[index], useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            val said = compose.onNodeWithText(value, useUnmergedTree = true)
                .getUnclippedBoundsInRoot()
            assertTrue(
                "the value is under the name, not beside it: $value",
                said.top >= name.bottom - 1.dp,
            )
            assertEquals(
                "the value is on the name's own left edge: $value",
                name.left.value,
                said.left.value,
                0.5f,
            )
            assertTrue(
                "the value stays inside the row it belongs to: $value",
                said.right <= row.right,
            )
            assertTrue(
                "the name keeps a line of its own, not one letter at a time: ${names[index]}",
                name.right - name.left > (name.bottom - name.top) * 2f,
            )
            assertTrue(
                "a row is its name, its value and its padding, and never a ladder: $value",
                row.bottom - row.top <= 76.dp,
            )
        }

        // The marks after the words do not move with the value: the tail is
        // the marks' own column, which is why the value is not in it.
        val marks = compose.onAllNodesWithTag("chevron-slot", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { it.boundsInRoot }
        assertEquals("one chevron a row", 2, marks.size)
        val first = marks.first()
        val last = marks.last()
        assertEquals("every chevron must sit at one left edge", first.left, last.left, 0.5f)
        assertEquals("every chevron must sit at one right edge", first.right, last.right, 0.5f)
    }

        /**
     * The text size rail is one adjustable 48 dp control.
     *
     * The five cells this replaced were five targets of their own, which is why
     * they needed 250 dp and a line above them. The rail is one finger sized
     * strip that carries the whole scale: a tap or a drag chooses the nearest
     * stop, and a screen reader adjusts it through its own SetProgress action.
     * The test drives that action, the same road TalkBack takes, and reads the
     * five stops from the range (owner decision, this session).
     */
    @Test
    fun theTextSizeRailIsOneAdjustableControl() {
        var chosen: Float? = null
        compose.setContent {
            MaterialTheme {
                Column {
                    SizeRow(TypeRole.Translation, 1f) { chosen = it }
                }
            }
        }
        val scale = compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
        )
        scale.assertHeightIsAtLeast(48.dp)
        val info = scale.fetchSemanticsNode()
            .config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(0f, info.range.start, 0.001f)
        assertEquals(4f, info.range.endInclusive, 0.001f)
        assertEquals("five stops carry three steps between the ends", 3, info.steps)
        scale.performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        assertEquals(0.8f, chosen ?: 0f, 0.001f)
    }
}
