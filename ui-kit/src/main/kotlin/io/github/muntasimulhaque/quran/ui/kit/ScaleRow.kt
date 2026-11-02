package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Wash
import kotlin.math.abs
import kotlin.math.roundToInt

/** The air the scale keeps between its own edge and its first and last stops. */
private val RailInset = 10.dp

/** The rail's one line: a hairline, not a rule, and never the accent. */
private val RailStroke = 1.dp

/** The stop a reader is not on: a quiet point on the line. */
private val RestingStop = 3.dp

/** The wash around the chosen stop, so the choice reads before the dot does. */
private val ChosenHalo = 12.dp

/** The chosen stop's own point. */
private val ChosenStop = 5.dp

/**
 * One choice out of a scale, drawn as a measure: the name at the leading end,
 * a hairline rail, one stop for each value, and the chosen stop filled in the
 * accent inside its own quiet halo. The two ends carry the smallest and the
 * largest mark, so the scale says which way is quieter and which is louder
 * without a row of five cells.
 *
 * **One control, not five.** The whole rail is one 48 dp target and one
 * TalkBack node: a reader aims anywhere on the line, the nearest stop is
 * chosen, and a screen reader adjusts it with its own increase and decrease.
 * The five cell row before this was also five 48 dp targets, which is why it
 * needed 250 dp of width and a line of its own above it; the rail is one
 * target that fits in the row beside its name, and the size settings come
 * down by a line each (owner decision, this session). The rule that every
 * choice is reachable by a finger still holds: the rail is one finger-sized
 * strip, and the stops are placed inside it.
 *
 * [steps] are the values, in the order the reader reads them. [value] is the
 * one chosen; a value between two stops lands on the nearer one, so a stored
 * value from an older ladder still draws a choice. [startMark] and [endMark]
 * are the two ends' drawings, cleared from the semantics because they say
 * what the rail already says. [label] is the control's spoken name, and
 * [valueText] is the chosen value said beside it where the value is a number
 * the reader needs to hear (a pace), never where the place is the state (a
 * size).
 *
 * A tap chooses the stop under the finger; a horizontal drag carries the
 * choice with it. A vertical drag is left to the surface, so a rail inside a
 * scrolling page does not take the page's scroll. Both pointer blocks are
 * keyed on [steps] and read the latest [onSelect] through a state, so a drag
 * that changes the value cannot restart its own gesture under the finger.
 *
 * The rail itself carries the `scale-rail` tag. The rail is what the reader
 * reads as one measure, so a test measures the node the eye sees, not the row
 * around the label and the two marks (owner decision, this session: the four
 * size rails are one length).
 */
@Composable
fun ScaleRow(
    label: String,
    steps: List<Float>,
    value: Float,
    onSelect: (Float) -> Unit,
    startMark: @Composable () -> Unit,
    endMark: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    valueText: (@Composable (Float) -> String)? = null,
) {
    if (steps.isEmpty()) return
    val chosen = steps.indices.minByOrNull { abs(steps[it] - value) } ?: 0
    val spokenValue = valueText?.invoke(steps[chosen])
    val select = rememberUpdatedState(onSelect)
    val focusSource = remember { MutableInteractionSource() }
    val inset = with(LocalDensity.current) { RailInset.toPx() }
    val rail = MaterialTheme.colorScheme.outline
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    fun choose(x: Float, width: Int) {
        if (steps.size == 1) {
            select.value(steps.first())
            return
        }
        val usable = (width - 2 * inset).coerceAtLeast(1f)
        val fraction = ((x - inset) / usable).coerceIn(0f, 1f)
        val index = (fraction * steps.lastIndex).roundToInt().coerceIn(0, steps.lastIndex)
        select.value(steps[index])
    }

    Row(
        modifier = modifier
            .height(48.dp)
            .focusable(interactionSource = focusSource)
            .focusRing(focusSource, Radii.Pill)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val direction = when (event.key) {
                    Key.DirectionLeft -> -1
                    Key.DirectionRight -> 1
                    else -> return@onKeyEvent false
                }
                val next = (chosen + direction).coerceIn(0, steps.lastIndex)
                if (next != chosen) select.value(steps[next])
                true
            }
            .semantics {
                contentDescription = label
                if (spokenValue != null) stateDescription = spokenValue
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = chosen.toFloat(),
                    range = 0f..steps.lastIndex.toFloat(),
                    steps = (steps.size - 2).coerceAtLeast(0),
                )
                setProgress { target ->
                    val next = target.roundToInt().coerceIn(0, steps.lastIndex)
                    select.value(steps[next])
                    true
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clearAndSetSemantics { }) { startMark() }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("scale-rail")
                .pointerInput(steps) {
                    detectTapGestures { at -> choose(at.x, size.width) }
                }
                .pointerInput(steps) {
                    detectHorizontalDragGestures(
                        onDragStart = { at -> choose(at.x, size.width) },
                    ) { change, _ ->
                        choose(change.position.x, size.width)
                        change.consume()
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val start = RailInset.toPx()
                val end = size.width - RailInset.toPx()
                val middle = size.height / 2f
                drawLine(
                    color = rail,
                    start = Offset(start, middle),
                    end = Offset(end, middle),
                    strokeWidth = RailStroke.toPx(),
                )
                val span = (end - start).coerceAtLeast(1f)
                steps.indices.forEach { index ->
                    val x = start + span * index / steps.lastIndex.coerceAtLeast(1)
                    if (index == chosen) {
                        drawCircle(
                            color = accent.copy(alpha = Wash.Control),
                            radius = ChosenHalo.toPx(),
                            center = Offset(x, middle),
                        )
                        drawCircle(
                            color = accent,
                            radius = ChosenStop.toPx(),
                            center = Offset(x, middle),
                        )
                    } else {
                        drawCircle(
                            color = quiet,
                            radius = RestingStop.toPx(),
                            center = Offset(x, middle),
                        )
                    }
                }
            }
        }
        Box(Modifier.clearAndSetSemantics { }) { endMark() }
    }
}
