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
 * What the pill's own row spends before the words get a pixel of it: the
 * row's padding, the gap, the controls, and the two chevrons the words
 * wear. Four controls, the playing state, is 560 dp, which is a tablet and
 * a landscape phone and no phone in portrait.
 */
internal fun oneRowFloor(controls: Int): Dp =
    RowInsets + WordsGap + (TransportSize * controls) + ChevronRoom +
        ReciterChevronRoom + WordsMeasure
