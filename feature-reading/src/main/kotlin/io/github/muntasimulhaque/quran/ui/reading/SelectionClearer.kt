package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.text.selection.SelectionState

/**
 * The selectable lines on one surface, so one touch can put the platform's
 * selection away.
 *
 * Compose keeps a selection until something releases it. The toolbar is a
 * window of its own, so a tap on the page never reaches it, and the reader is
 * left with a selected line and a menu that will not go. A surface holds one
 * clearer, each selectable line registers its state, and the surface's next
 * touch clears them all. The states stay registered while they are composed,
 * because the same line can be selected again.
 */
class SelectionClearer {

    private val states = mutableSetOf<SelectionState>()

    fun register(state: SelectionState) {
        states += state
    }

    fun unregister(state: SelectionState) {
        states -= state
    }

    /**
     * Releases every selection on the surface. The list is copied first: a
     * clear can recompose a line and dispose its state, and the set must not
     * change under the loop.
     */
    fun clear() {
        states.toList().forEach { it.clear() }
    }
}
