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
 * is a block inside the page, the tile is a card that stands on a page, the
 * sheet is a page over the reading, and the popup is the one surface that
 * hangs from another control. A title that answers a tap is a control, so it
 * wears the pill and never a shape of its own (owner decision, this session).
 *
 * The sheet's top corners are 28 dp, Material's large surface, and only its
 * top corners are round: it rises from the foot of the glass. The 18 dp shape
 * was called a sheet but was used for a card on a page, so the name moved to
 * [Tile] and the sheet's real shape is now the one the app draws (owner
 * decision, this session).
 */
object Radii {

    /** A capsule: every chip, button, and floating control the app repeats. */
    val Pill = RoundedCornerShape(50)

    /** The reading's own blocks: an ayah, a door, an add row. */
    val Card = RoundedCornerShape(14.dp)

    /** A card that stands on a page: a share card, a welcome tile. */
    val Tile = RoundedCornerShape(18.dp)

    /** The page a sheet raises: round at the head, open at the foot. */
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** The pill's popup. */
    val Popup = RoundedCornerShape(20.dp)
}
