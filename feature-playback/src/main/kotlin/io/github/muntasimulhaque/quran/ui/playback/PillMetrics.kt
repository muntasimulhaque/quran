package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The pill's own geometry, in one place, because the bar that holds the
 * words and the words themselves measure against the same numbers: a
 * number that drifted between them would put the words where the row is
 * not, and the row's own measure is the answer to the phone question.
 */

/**
 * The gutter a floating control keeps from the glass. A pill that reaches
 * the screen's own edge stops reading as a control over the page and starts
 * reading as a sheet the app forgot to inset (owner report).
 */
internal val BarGutter = 16.dp

/** The row's own padding: 16 at the start, 6 at the end. */
internal val RowInsets = 22.dp

/** The gap between the words and the controls, in both shapes. */
internal val WordsGap = 12.dp

/**
 * The measure the pill's words keep, or the words take a line of their own.
 *
 * 300 dp is the whole line the pill can print at its longest: the longest
 * surah name and its ayah, and the slowest pace. Below that the words share
 * the row with four 48 dp controls and get about ninety dp, which is
 * fourteen characters at the size the status line is set: a surah name and
 * its ayah break across lines, the pace is cut with an ellipsis, and the
 * reader is left with a name they cannot read (owner report).
 *
 * So the pill is two rows wherever the words cannot keep that measure, and
 * one row where they can: a tablet, a landscape phone, and nowhere else. A
 * phone in portrait always takes the two rows, and pays for it in height.
 */
internal val WordsMeasure = 300.dp

/**
 * One control's own target, which is the app's floor and what
 * `minimumInteractiveComponentSize` gives it. A Material release that widens
 * it has to widen this number with it, or the one-row shape is measured
 * against a row that no longer exists.
 */
internal val TransportSize = 48.dp

/** The status line's own chevron: 12 for the mark, 4 before it, 2 after the text. */
internal val ChevronRoom = 18.dp

/**
 * The reciter's chevron beside the reciter's name: the mark, the gap before
 * it, and the pill's own inset at the end of the row.
 */
internal val ReciterChevronRoom = 16.dp

/**
 * The measure of a popup the pill opens, and of the anchor it hangs from.
 *
 * Both numbers are the same on purpose: a menu takes its own left edge from
 * the anchor it was given, so an anchor of another measure would stand the
 * menu beside the pill's centre rather than on it. The anchor is this wide
 * and centred, and the menu is this wide, so the two land on one centre
 * whatever the pill's own width is (owner report).
 *
 * It is the reciter menu's old maximum and the listening menu's own need: the
 * five paces at their 48 dp target with the padding around them, which is the
 * measure the listener has to get, and the widest the two ever were.
 */
internal val PillMenuMeasure = 288.dp

/**
 * The anchor hairline: a menu is placed from the bounds of the box it hangs
 * on, and a box with no height has no bounds to read, so a zero-height
 * anchor stands every popup at the window edge instead of the pill centre.
 * One dp is the thinnest box that has a place on the page, and it draws
 * nothing.
 */
internal val AnchorLine = 1.dp

/**
 * The ground the pill's popup keeps between its own foot and the capsule's
 * top edge.
 *
 * The popup wears the pill's own cloth, carries no border, and since the
 * owner read the two together it wears the pill's own lift as well (owner
 * decision, forty-seventh session). The ground is still what keeps the two
 * silhouettes apart: with the popup's foot on the capsule's own top edge
 * (owner report) the flat edges met along the middle, where no seam can be
 * seen, and the popup's foot corners and the capsule's shoulders kissed at
 * each end, so the two read as one shape with a waist. Ten dp keeps the
 * capsule's own lift from reaching the popup's foot and sits well
 * inside the popup's 20 dp corner, so the two silhouettes never meet, and
 * the popup still hangs from the capsule's middle.
 */
internal val PopupGap = 10.dp

/**
 * What the pill's own row spends before the words get a pixel of it: the
 * row's padding, the gap, the controls, and the two chevrons the words
 * wear. Four controls, the playing state, is 560 dp, which is a tablet and
 * a landscape phone and no phone in portrait.
 */
internal fun oneRowFloor(controls: Int): Dp =
    RowInsets + WordsGap + (TransportSize * controls) + ChevronRoom +
        ReciterChevronRoom + WordsMeasure

/**
 * The room a popup the pill opens may take, in pixels: the ground between
 * the window's own top and the line the popup keeps above the capsule.
 *
 * The popup's own scroll takes the content past this measure rather than
 * over the capsule. With the popup free to grow, a window too short for it
 * made the position rule clamp to the window's top, push the popup's foot
 * down, and take the ground away again, so the one place the two shapes met
 * was the place the screen was tightest (owner decision). A popup no taller
 * than this measure leaves the position rule unclamped, and the ground holds
 * on the shortest window as it does on the tallest.
 */
internal fun popupRoom(anchorTop: Int, gap: Int): Int =
    (anchorTop - gap).coerceAtLeast(0)
