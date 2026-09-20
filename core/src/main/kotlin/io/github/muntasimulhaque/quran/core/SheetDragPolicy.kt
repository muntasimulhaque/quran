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
 * list at its top belongs to the sheet, and it closes as it always did.
 *
 * One policy is one gesture at a time. [onUserInput] starts a gesture and
 * keeps its beginning; [leftoverScroll] and [endGesture] answer with what the
 * policy keeps in the list's stead. The caller hands the results to the
 * nested scroll system, so the sheet never sees them.
 */
class SheetDragPolicy {

    private var startedAtTop = true
    private var dragging = false

    /**
     * Called for user input; [atTop] is where the list stands now. Only the
     * first call of a gesture is kept, so a list that reaches its top half
     * way through a scroll does not turn the rest of that scroll into a pull.
     */
    fun onUserInput(atTop: Boolean) {
        if (dragging) return
        dragging = true
        startedAtTop = atTop
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
