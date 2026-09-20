package io.github.muntasimulhaque.quran.core

/**
 * The decision behind a bottom sheet's pull-to-close when its content
 * scrolls: whose gesture is it, the list's or the sheet's?
 *
 * A pull down and a scroll back up are the same finger movement, and a list
 * reports only what it could not scroll. By then it is already at its top, so
 * the leftover cannot tell the two apart. The beginning can: a gesture that
 * started with the list scrolled belongs to the list for its whole length,
 * and whatever the list leaves goes nowhere; a gesture that started with the
 * list at its top belongs to the sheet, and it closes as it always did. A
 * gesture that moves the list at any point belongs to the list too, so a
 * scroll that comes home does not turn into a pull on the way.
 *
 * One policy is one gesture at a time. [onPointerDown] opens a gesture,
 * [onUserInput] records where it found the list, and [leftoverScroll] and
 * [endGesture] answer with what the policy keeps in the list's stead. The
 * caller hands the results to the nested scroll system, so the sheet never
 * sees them.
 */
class SheetDragPolicy {

    private var startedAtTop = true
    private var dragging = false

    /**
     * A touch landed on the list: whatever record was still open is over, and
     * the next input is a new gesture. The nested scroll system has no call
     * for a finger landing, and a fling that a new touch interrupts never
     * reports its end, so the list's own pointer is the only place that can
     * say a gesture has begun.
     */
    fun onPointerDown() {
        dragging = false
        startedAtTop = true
    }

    /**
     * Called for user input; [atTop] is where the list stands now. A gesture
     * that begins with the list scrolled owns its leftovers, and so does a
     * gesture that finds the list scrolled later, because by then the finger
     * has moved the list and is scrolling, not pulling.
     */
    fun onUserInput(atTop: Boolean) {
        if (!dragging) {
            dragging = true
            startedAtTop = atTop
        } else if (!atTop) {
            startedAtTop = false
        }
    }

    /** What of the leftover scroll this policy keeps, in pixels. */
    fun leftoverScroll(available: Float): Float =
        if (dragging && !startedAtTop) available else 0f

    /**
     * Ends the gesture and answers with what of the leftover fling velocity
     * the policy keeps. The next gesture is judged from its own beginning.
     */
    fun endGesture(available: Float): Float {
        val kept = if (dragging && !startedAtTop) available else 0f
        dragging = false
        startedAtTop = true
        return kept
    }
}
