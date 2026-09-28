package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.settings.PageRow
import io.github.muntasimulhaque.quran.ui.settings.ToggleRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The settings sheet's controls run down two columns, the chevrons' and the
 * switches', with the switch always last.
 *
 * A row can carry a switch, a chevron, or both. The owner asked for the
 * switch to be the row's last mark and for the two kinds of control to stand
 * at one place each, so the three shapes are composed here side by side and
 * the test reads the real bounds: every switch ends at one line at the
 * sheet's edge, every chevron sits one column before it, and the switch of a
 * row with a door comes after that row's chevron (owner report, D-111).
 *
 * The column is a width, not a height: the tail's 48 dp square stretched
 * every plain row where the chevron was not a control of its own, so the
 * page row's compact height is pinned here too (owner report, D-105).
 */
class SettingsRowAlignmentTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun switchesEndTheLineAndChevronsShareOneColumn() {
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
            "every switch must end at one line",
            alone.right.value,
            doorSwitch.right.value,
            0.5f,
        )

        val chevrons = compose.onAllNodesWithTag("row-chevron", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals("the page row and the door row draw one each", 2, chevrons.size)
        val pageChevron = chevrons.first().boundsInRoot
        val doorChevron = chevrons.last().boundsInRoot
        assertEquals("every chevron must sit at one left edge", pageChevron.left, doorChevron.left, 0.5f)
        assertEquals("every chevron must sit at one right edge", pageChevron.right, doorChevron.right, 0.5f)
        assertTrue(
            "the switch must be the row's last mark, after its chevron",
            doorChevron.right <= doorSwitch.left.value + 0.5f,
        )

        val page = compose.onNodeWithTag("page-row").getUnclippedBoundsInRoot()
        assertTrue(
            "a page row must stay a line and its padding, not a 48 dp slot taller",
            page.bottom - page.top <= 56.dp,
        )
    }
}
