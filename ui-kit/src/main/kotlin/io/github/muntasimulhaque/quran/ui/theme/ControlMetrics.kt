package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The measurements a control shares with every other control of its kind.
 *
 * The theme already owns the room between things ([Space]) and the corners
 * ([Radii]). These are the numbers inside a control: the finger's floor, the
 * circle a round control lights, the room a pill's word keeps above and below
 * it, and the gap between a door's words and its chevron. They were typed at
 * each call site and had drifted: 34, 36, and 32 dp pills were all called
 * "the chip's measure" (owner decision, the consistency audit).
 *
 * A pill follows its own line, so [PillPad] is the inset that gives a 36 dp
 * capsule at the default type and grows with the system font scale. A height
 * is never fixed.
 */
object ControlMetrics {

    /** The floor a finger is owed, in either direction. */
    val Target = 48.dp

    /** The circle a round control's press lights inside [Target]. */
    val Press = 40.dp

    /** The room a pill's word keeps above and below its line: 8 + 20 + 8 = 36. */
    val PillPad = 8.dp

    /** The pill's floor at the default type. It grows with the line, never shrinks into a Material host. */
    val PillHeight = 36.dp

    /** The gap between a door's words and the chevron that turns. */
    val ChevronGap = 6.dp
}
