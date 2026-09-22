package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import io.github.muntasimulhaque.quran.core.SheetDragPolicy

/**
 * The connection between a scrolling list and the sheet that holds it.
 *
 * The sheet closes on a downward pull. When its content is a list, the pull
 * and a scroll back to the top are the same gesture, and the list's leftovers
 * can reach the sheet: a reader who scrolled down ten results and then dragged
 * back up would pull the sheet along as the list reached its top. The sheet's
 * own nested scroll handling cannot tell the two apart, because by the time a
 * delta is left over the list is already at its top.
 *
 * The decision itself lives in [SheetDragPolicy], which is pure and tested;
 * this class only tells it where the list stood when the gesture began, and
 * keeps its answers away from the sheet. Pulling down to close still works:
 * the reader only has to start the gesture at the top, which is where a pull
 * to close begins anyway.
 *
 * One instance belongs to one scrolling thing: a [LazyListState] or a
 * [ScrollState]. A list that can never scroll (Saved with nothing in it, for
 * example) is always at its top, so its pull passes through untouched.
 *
 * Browse and Search both keep their results in a sheet, and every sheet whose
 * content scrolls needs the same gate: the ayah card and the settings pages
 * scroll in a column rather than a list, and a scroll back to the top of a
 * long tafsir closed the card the same way it once closed Browse. The gate is
 * worn the same way everywhere, so one gesture behaves the same in all of
 * them.
 */
class SheetDragGate(
    private val atTop: () -> Boolean,
) : NestedScrollConnection {

    /** A lazy list: at its top when it can scroll back no further. */
    constructor(listState: LazyListState) : this({ !listState.canScrollBackward })

    /** A scrollable column: the same rule over its scroll position. */
    constructor(scrollState: ScrollState) : this({ !scrollState.canScrollBackward })

    private val policy = SheetDragPolicy()

    /** A touch landed on the list: the next input is a new gesture. */
    fun onPointerDown() {
        policy.onPointerDown()
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput) {
            policy.onUserInput(atTop = atTop())
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset =
        if (source == NestedScrollSource.UserInput) {
            Offset(0f, policy.leftoverScroll(available.y))
        } else {
            Offset.Zero
        }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        Velocity(0f, policy.endGesture(available.y))
}

/**
 * The modifier one list wears: the gate for the sheet, and the touch that
 * tells the gate a new gesture has begun. A press is watched, never consumed,
 * so the list scrolls exactly as it did without it.
 */
fun Modifier.sheetDragGate(gate: SheetDragGate): Modifier =
    nestedScroll(gate).pointerInput(gate) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press) gate.onPointerDown()
            }
        }
    }

/**
 * A column that scrolls inside a sheet, gate and scroll as one modifier. The
 * gate is listed before the scroll, which places it outside: what the scroll
 * leaves over reaches the gate first, and the sheet behind the gate only ever
 * sees a pull the reader meant for it.
 */
@Composable
fun Modifier.sheetVerticalScroll(state: ScrollState): Modifier =
    sheetDragGate(remember(state) { SheetDragGate(state) }).verticalScroll(state)
