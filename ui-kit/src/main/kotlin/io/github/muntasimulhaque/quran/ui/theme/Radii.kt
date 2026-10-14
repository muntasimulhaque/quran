package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * The corners the app draws, named once. The values were already chosen; they
 * live here so the next surface takes one of them instead of a new number, and
 * a pill stays a capsule at any height, which is why it is a share of the
 * height and not a dp.
 *
 * The steps are the app's one shape scale: the pill is the control, the card
 * is a block inside the page, the sheet is a page over the reading, and the
 * popup is the one surface that hangs from another control. A title that
 * answers a tap is a control, so it wears the pill and never a shape of its
 * own (owner decision, this session).
 */
object Radii {

    /** A capsule: every chip, button, and floating control the app repeats. */
    val Pill = RoundedCornerShape(50)

    /** The reading's own blocks: an ayah, a door, an add row. */
    val Card = RoundedCornerShape(14.dp)

    /** A sheet's card, a welcome tile. */
    val Sheet = RoundedCornerShape(18.dp)

    /** The pill's popup. */
    val Popup = RoundedCornerShape(20.dp)
}
