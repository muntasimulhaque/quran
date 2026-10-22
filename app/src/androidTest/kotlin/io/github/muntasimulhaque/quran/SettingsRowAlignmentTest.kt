package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.ui.settings.AboutPage
import io.github.muntasimulhaque.quran.ui.settings.PageRow
import io.github.muntasimulhaque.quran.ui.settings.SizeRow
import io.github.muntasimulhaque.quran.ui.settings.SwitchSlot
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
        assertEquals(
            "the page row, the door row and the row with nothing to say draw one each",
            3,
            slots.fetchSemanticsNodes().size,
        )
        val pageSlot = slots[0].getUnclippedBoundsInRoot()
        val doorSlot = slots[1].getUnclippedBoundsInRoot()
        val bareSlot = slots[2].getUnclippedBoundsInRoot()
        assertEquals(
            "a row that says nothing about itself ends on the same line as one that does",
            pageSlot.right.value,
            bareSlot.right.value,
            0.5f,
        )
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
        // owner read as the row not knowing where it stops (owner report).
        // The mark is read from the unmerged tree because the slot is
        // tagged there, so its bounds come back in pixels and are brought to
        // dp here: this test once compared the two.
        val density = compose.density.density
        val marks = compose.onAllNodesWithTag("row-chevron", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals("the three door rows draw one chevron each", 3, marks.size)
        val pageMarkRight = marks[0].boundsInRoot.right / density
        assertEquals(
            "a row with no switch ends with its chevron on the line every switch ends on",
            alone.right.value,
            pageMarkRight,
            0.5f,
        )

        val page = compose.onNodeWithTag("page-row").getUnclippedBoundsInRoot()
        assertTrue(
            "a page row must stay its two lines and its padding, not a 48 dp slot taller",
            page.bottom - page.top <= 72.dp,
        )
        val bare = compose.onNodeWithTag("bare-row").getUnclippedBoundsInRoot()
        assertTrue(
            "a row with no value must stay a line and its padding, not a 48 dp slot taller",
            bare.bottom - bare.top <= 56.dp,
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
    @Test
    fun aSwitchRowKeepsTheListsHeightAndTheSwitchesTarget() {
        var checked = true
        var opened = false
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
                        checked = checked,
                        onChange = { checked = it },
                        onOpen = { opened = true },
                        openLabel = "Open translations",
                        switchTag = "switch-door",
                    )
                }
            }
        }
        val page = compose.onNodeWithTag("page-row").getUnclippedBoundsInRoot()
        val switchRow = compose
            .onNode(hasText("Show translation") and hasClickAction())
            .getUnclippedBoundsInRoot()
        assertEquals(
            "a switch row must keep the list's own height, not the switch's 48 dp",
            (page.bottom - page.top).value,
            (switchRow.bottom - switchRow.top).value,
            1f,
        )
        // The target the switch owes the finger is 48 dp around its own 32 dp
        // track, so its top edge stands 8 dp above the track. A finger two dp
        // inside that edge is on the switch, not on the row's door.
        val track = compose.onNodeWithTag("switch-door", useUnmergedTree = true)
        track.performTouchInput {
            click(Offset(center.x, center.y - 22.dp.toPx()))
        }
        compose.runOnIdle {
            assertTrue(
                "the switch's own target must answer at its edge, not only at its middle",
                !checked,
            )
            assertTrue("the door must not take the switch's own target", !opened)
        }
    }

    /**
     * A row keeps no width for a mark it does not draw.
     *
     * The chevron's column used to be reserved on every row, drawn or not, so
     * a row with no door carried 48 dp of empty sheet in front of its switch.
     * A switch stands at the row's own margin whatever is in front of it, so
     * the empty column never kept a switch on one line; what it bought was a
     * row that grows a door later keeping its switch where it was, which is a
     * convenience for whoever edits the file and not a thing a reader can see
     * (owner decision, forty-sixth session).
     *
     * This measures the two halves. A row that draws no chevron composes no
     * chevron slot, so there is no empty column left to be empty, and neither
     * switch moved to pay for the one that is gone.
     *
     * It does not measure whether a label fits on one line, because that is a
     * question about the words and the phone, not about the column.
     */
    @Test
    fun aRowKeepsNoWidthForAMarkItDoesNotDraw() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light) {
                // Two rows side by side in the app's own voice, one with a
                // door and one without, carrying the same name so the only
                // variable is the chevron column.
                Column {
                    ToggleRow(
                        title = "Show translation",
                        subtitle = null,
                        checked = true,
                        onChange = {},
                        switchTag = "long-alone",
                    )
                    ToggleRow(
                        title = "Show translation",
                        subtitle = null,
                        checked = true,
                        onChange = {},
                        onOpen = {},
                        openLabel = "Open translations",
                        switchTag = "short-door",
                    )
                }
            }
        }

        // The crispest half of the sentence: a row that draws no chevron
        // composes no chevron slot, so there is nothing there to be empty.
        // The slot's own tag is on a row that draws one, which is what makes
        // this a count of the rows that have a door.
        val slots = compose.onAllNodesWithTag("chevron-slot", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals(
            "only the row that draws a chevron composes one",
            1,
            slots.size,
        )

        val alone = compose.onNodeWithTag("long-alone", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val doorSwitch = compose.onNodeWithTag("short-door", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(
            "and releasing the chevron column moved no switch",
            alone.right.value,
            doorSwitch.right.value,
            0.5f,
        )
    }

    /**
     * A row's value is under its name, on every row that has one.
     *
     * The hub used to answer the same question twice: a row with no switch
     * printed its value in a column at the right, measured so that the name
     * kept its room, and a row with a switch printed the same grey
     * line under its name, so one list read as two grammars (owner decision).
     * This is the measurement that says the value is under the name
     * now: on the sheet's two longest values, the value begins below the name
     * and on the name's own left edge, and the name still holds a line of its
     * own rather than one letter at a time.
     *
     * The tail is what decided it, and that is measurable too: the marks after
     * the words sit at one left edge and one right edge whatever the value
     * says, which is what a value in that tail would have had to fight for.
     */
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
     * Nothing on the About page opens.
     *
     * The page is facts only: version, license, and credits, each read where
     * the reader already is (owner decision). Corrections and rights requests
     * travel through the store listing's contact and the project repository
     * rather than through the app, so the page holds no door and draws no
     * chevron anywhere.
     */
    @Test
    fun nothingOnTheAboutPageOpens() {
        compose.setContent {
            MaterialTheme {
                AboutPage(
                    packs = emptyList(),
                    version = "3.3 (34)",
                )
            }
        }

        val doors = compose.onAllNodes(hasClickAction(), useUnmergedTree = true)
        assertEquals("the page holds no door", 0, doors.fetchSemanticsNodes().size)
        val slots = compose.onAllNodesWithTag("chevron-slot", useUnmergedTree = true)
        assertEquals("and draws no chevron", 0, slots.fetchSemanticsNodes().size)
    }

    /**
     * Every step of every segmented row is a 48 dp target.
     *
     * The text sizes drew 38 dp cells, under the design document's own rule,
     * and the fix was structural: an outer 48 dp touch box with the mark
     * inside it. The name moved above the steps for it, because five 48 dp
     * boxes are 250 dp wide and a name beside them would break in the middle
     * on a small phone. The measurement is the rule's own number, read off the
     * real controls.
     */
    @Test
    fun everyStepIsAFortyEightDpTarget() {
        compose.setContent {
            MaterialTheme {
                Column {
                    SizeRow(TypeRole.Translation, 1f) {}
                }
            }
        }
        val steps = compose.onAllNodes(isSelectable(), useUnmergedTree = true)
        assertEquals(
            "the text size row's five steps",
            5,
            steps.fetchSemanticsNodes().size,
        )
        steps.onFirst().assertWidthIsAtLeast(48.dp)
        steps.onFirst().assertHeightIsAtLeast(48.dp)
        steps.onLast().assertWidthIsAtLeast(48.dp)
        steps.onLast().assertHeightIsAtLeast(48.dp)
    }
}
