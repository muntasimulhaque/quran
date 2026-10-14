package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Wash
import kotlin.math.abs

/**
 * One choice out of a scale, drawn as the app's one row of steps: the steps
 * stand in one pill on the quiet fill, every step owns the 48 dp target a
 * finger needs and draws its mark inside that cell, and the chosen step is
 * filled with the accent's own wash.
 *
 * The cell is the target. A scale is a row of small marks a reader aims at,
 * and a step that cannot be hit is a step a reader cannot take (owner
 * decision, the accessibility test). The container keeps the app's quiet
 * door fill, so the steps read as one control rather than a row of buttons.
 *
 * The settings text sizes and the pill's listening menu draw their steps
 * through this one component (owner decision, this session: the menu had
 * carried its own copy of the row, and the two had drifted, one filling the
 * whole 48 dp cell and the other hugging its text).
 */
@Composable
fun StepRow(
    steps: List<Float>,
    value: Float,
    onSelect: (Float) -> Unit,
    /** What one step draws, given its place in the scale and whether it is chosen. */
    cell: @Composable (index: Int, active: Boolean) -> Unit,
    /** What a step says to a screen reader, place and meaning together. */
    description: @Composable (step: Float) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(Radii.Pill)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Door))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        steps.forEachIndexed { index, step ->
            val active = abs(step - value) < 0.01f
            val said = description(step)
            Box(
                modifier = Modifier
                    // The 48 dp is the whole reason this control has a shape
                    // of its own: the mark inside is smaller, and the target
                    // a finger gets is not.
                    .size(48.dp)
                    .clip(Radii.Pill)
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.primary.copy(alpha = Wash.Control)
                        } else {
                            Color.Transparent
                        },
                    )
                    .selectable(selected = active, role = Role.RadioButton) { onSelect(step) }
                    .semantics { contentDescription = said },
                contentAlignment = Alignment.Center,
            ) {
                cell(index, active)
            }
        }
    }
}
