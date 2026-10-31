package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp

/**
 * The one handle every sheet in the app wears: the bar the eye reads, inside
 * a 48 dp-wide and 48 dp-tall target.
 *
 * Material's own handle is a 32 dp-wide target, narrower than a finger and
 * narrower than the rule. This one keeps the 32 dp bar inside a 48 dp drag
 * target.
 *
 * It is a drawing, not a control. The app's sheets open whole, so there is no
 * half height to toggle, and the owner's own rule is that a tap on the bar
 * does not take the sheet away; the scrim, Back, and a pull down are the ways
 * out. A screen reader therefore hears the sheet's pane title and its first
 * row, not a "Drag handle" that answers nothing. The drawing is cleared from
 * the accessibility tree, while the touch target still drags because
 * Material's draggable belongs to the whole sheet (owner decision, this
 * session).
 */
@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .sheetHandleGuard()
            .width(48.dp)
            .height(48.dp)
            // The tag is the test's handle, not a spoken name: the node is
            // empty to a screen reader and finds the same box to a test.
            .clearAndSetSemantics { testTag = "sheet-handle" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(32.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}
