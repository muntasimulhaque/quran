package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.ui.settings.PageRow
import io.github.muntasimulhaque.quran.ui.settings.SizeRow
import io.github.muntasimulhaque.quran.ui.settings.SpeedRow
import io.github.muntasimulhaque.quran.ui.settings.SwitchSlot
import io.github.muntasimulhaque.quran.ui.settings.ToggleRow
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
 * (owner report, D-111, and D-120 for the row with no switch).
 *
 * The chevron's slot is measured, not the mark: a mark is centred in its
 * slot, so the slot's edges are what a row's alignment is about, and the
 * slot's tag is on the rows that draw a chevron at all.
 *
 * The column is a width, not a height: the tail's 48 dp square stretched
 * every plain row where the chevron was not a control of its own, so the
 * page row's compact height is pinned here too (owner report, D-105).
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
 * The two tail columns and the compact height above, the name and the value
 * sharing one line whatever the value says (owner report, 37th session), and
 * every step of a segmented row being a full 48 dp target (D-087).
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
                        title = "Keep the screen awake",
                        subtitle = "The page stays lit while you read",
                        checked = true,
                        onChange = {},
                        switchTag = "switch-alone",
                    )
                    ToggleRow(
                        title = "Show translation",
                        subtitle = "Saheeh International",
                        checked = true,
                        onChange = {},
                        onOpen = {},
                        openLabel = "Open translations",
                        switchTag = "switch-door",
                    )
                }
            }
        }

        val alone = compose.onNodeWithTag("switch-alone", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val doorSwitch = compose.onNodeWithTag("switch-door", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(
            "the reserved column is the real control's own width, so a Material release that widens the track fails here",
            SwitchSlot.value,
            (doorSwitch.right - doorSwitch.left).value,
            0.5f,
        )
        assertEquals(
            "every switch must end at one line",
            alone.right.value,
            doorSwitch.right.value,
            0.5f,
        )

        // Every bound here is read the same way, in dp. `boundsInRoot` is in
        // pixels and `getUnclippedBoundsInRoot` is in dp, and this test once
        // compared the two: the switch's left edge in dp against the chevron's
        // right edge in pixels, which fails on every device whose density is
        // not one and passes on none of them. Nothing about the rows was
        // wrong; the measurement was (thirty-seventh session).
        val slots = compose.onAllNodesWithTag("chevron-slot", useUnmergedTree = true)
        assertEquals("the page row and the door row draw one each", 2, slots.fetchSemanticsNodes().size)
        val pageSlot = slots[0].getUnclippedBoundsInRoot()
        val doorSlot = slots[1].getUnclippedBoundsInRoot()
        assertEquals(
            "a row with no switch ends with its chevron on the line every switch ends on",
            doorSwitch.right.value,
            pageSlot.right.value,
            0.5f,
        )
        assertEquals(
            "the switch is the last mark of the row that has one, after its chevron column",
            doorSwitch.left.value,
            doorSlot.right.value,
            0.5f,
        )
        assertTrue(
            "a page row's chevron has moved to the end, so it stands right of the door row's",
            pageSlot.left.value > doorSlot.left.value,
        )

        // The mark, not only its column: a page row's chevron is the row's
        // last mark, and a mark centred in its own column stood a whole
        // chevron short of the line every switch ends on, which is the gap the
        // owner read as the row not knowing where it stops (owner report,
        // D-130). The mark is read from the unmerged tree because the slot is
        // tagged there, so its bounds come back in pixels and are brought to
        // dp here: this test once compared the two.
        val density = compose.density.density
        val marks = compose.onAllNodesWithTag("row-chevron", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals("the page row and the door row draw one chevron each", 2, marks.size)
        val pageMarkRight = marks[0].boundsInRoot.right / density
        assertEquals(
            "a row with no switch ends with its chevron on the line every switch ends on",
            alone.right.value,
            pageMarkRight,
            0.5f,
        )

        val page = compose.onNodeWithTag("page-row").getUnclippedBoundsInRoot()
        assertTrue(
            "a page row must stay a line and its padding, not a 48 dp slot taller",
            page.bottom - page.top <= 56.dp,
        )
    }

    /**
     * The name holds its room, and the value gets one line while it can.
     *
     * The owner's own sheet: the theme row's value was long enough to leave the
     * name "Theme" a column so narrow that the word broke one letter to a line
     * and the row became four lines tall, and "Font size" two. The value is
     * measured inside its own share of the row now, and the name's share is
     * the larger one, so a long value costs the value a second line at most
     * (D-116, D-120).
     *
     * Both of the values the sheet really prints are longer than that share,
     * so both of them broke to a second line under a name with room to give
     * (owner report, D-130). The name's column is now its own width and a
     * gap, capped at the share, so these two rows are one line each, and this
     * is the measurement that says so: the row is a line and its padding, not
     * a line of name above a line of value.
     */
    @Test
    fun aLongValueNeverSqueezesTheName() {
        compose.setContent {
            MaterialTheme {
                Column {
                    PageRow(
                        title = "Theme",
                        summary = "Night · day page Paper",
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

        val long = compose.onNodeWithTag("long-value").getUnclippedBoundsInRoot()
        assertTrue(
            "a long value may take a second line of its own, never a ladder of the name's",
            long.bottom - long.top <= 76.dp,
        )
        assertTrue(
            "a value that fits the room the name left must not break to a second line: " +
                "${long.bottom - long.top}",
            long.bottom - long.top <= 56.dp,
        )
        val short = compose.onNodeWithTag("short-value").getUnclippedBoundsInRoot()
        assertTrue(
            "every value the sheet prints must stay on the row's one line",
            short.bottom - short.top <= 56.dp,
        )
        val name = compose.onNodeWithText("Theme").getUnclippedBoundsInRoot()
        assertTrue(
            "the name must keep a line to itself, not one letter at a time",
            name.right - name.left > (name.bottom - name.top) * 2f,
        )

        // Both halves fill their share, so the marks after them do not move.
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
     * Every step of every segmented row is a 48 dp target.
     *
     * The text sizes drew 38 dp cells and the pace drew 46, under the design
     * document's own rule, and the fix named in D-087 was structural: an outer
     * 48 dp touch box with the mark inside it. The name moved above the steps
     * for it, because five 48 dp boxes are 250 dp wide and a name beside them
     * would break in the middle on a small phone. The measurement is the
     * rule's own number, read off the real controls.
     */
    @Test
    fun everyStepIsAFortyEightDpTarget() {
        compose.setContent {
            MaterialTheme {
                Column {
                    SizeRow(TypeRole.Translation, 1f) {}
                    SpeedRow(1f) {}
                }
            }
        }
        val steps = compose.onAllNodes(isSelectable(), useUnmergedTree = true)
        assertEquals(
            "the text size row and the pace row, five steps each",
            10,
            steps.fetchSemanticsNodes().size,
        )
        steps.onFirst().assertWidthIsAtLeast(48.dp)
        steps.onFirst().assertHeightIsAtLeast(48.dp)
        steps.onLast().assertWidthIsAtLeast(48.dp)
        steps.onLast().assertHeightIsAtLeast(48.dp)
    }
}
