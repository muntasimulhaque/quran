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
 * The settings sheet's controls run down one column.
 *
 * A row can carry a switch, a chevron, or both, and before this the two
 * kinds of row put their controls at different edges: a switch-only row put
 * its switch at the sheet's edge, while a row with a door pushed its switch
 * a chevron's width inward, and a chevron-only row sat closer to the edge
 * than either. The three shapes are composed here side by side, and the test
 * reads the real bounds of the switches and the chevrons: every switch ends
 * at one line and every chevron sits at one place (owner report, D-103).
 *
 * The column is a width, not a height: the tail's 48 dp square stretched
 * every plain row where the chevron was not a control of its own, so the
 * page row's compact height is pinned here too (owner report, D-105).
 */
class SettingsRowAlignmentTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun switchesAndChevronsShareOneColumn() {
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
        val door = compose.onNodeWithTag("switch-door", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(
            "every switch must end at one line",
            alone.right.value,
            door.right.value,
            0.5f,
        )

        val chevrons = compose.onAllNodesWithTag("row-chevron", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals("the page row and the door row draw one each", 2, chevrons.size)
        val first = chevrons.first().boundsInRoot
        val second = chevrons.last().boundsInRoot
        assertEquals("every chevron must sit at one left edge", first.left, second.left, 0.5f)
        assertEquals("every chevron must sit at one right edge", first.right, second.right, 0.5f)

        val page = compose.onNodeWithTag("page-row").getUnclippedBoundsInRoot()
        assertTrue(
            "a page row must stay a line and its padding, not a 48 dp slot taller",
            page.bottom - page.top <= 56.dp,
        )
    }
}
