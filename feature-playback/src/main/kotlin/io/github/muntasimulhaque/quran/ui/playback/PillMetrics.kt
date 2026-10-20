package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.reader.GlyphSize
import io.github.muntasimulhaque.quran.ui.theme.Space

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

/**
 * The air the capsule keeps around a press shape: between two controls, and
 * between a control and the capsule's own edge.
 *
 * The pill carries no close any more, so its shape is free to shrink to its
 * words, and this is the one number that shape is measured against (owner
 * decision, this session). A control's target and the shape its press draws
 * are the same 48 dp here, so the air is what keeps one flash from touching
 * the next and the end of a row from touching the capsule: at nothing, the
 * three transport circles met point to point, the words' two doors' flashes
 * met the capsule's own top edge, and the words' flash and the transport's
 * met on one line. It is the app's `Space.Line`, the gap a cluster of
 * same-kind small controls keeps, because that is what the transport row is.
 */
internal val PillAir = Space.Line

/** The gap between the pill's two word doors, which are a cluster of two. */
internal val WordsGap = 12.dp

/**
 * The measure the pill's words keep, or the words take a line of their own.
 *
 * 300 dp is the whole line the pill can print at its longest: the longest
 * surah name and its ayah, and the slowest pace. Below that the words get
 * the room left over beside three 48 dp controls, which is fourteen
 * characters at the size the status line is set: a surah name and its ayah
 * break across lines, the pace is cut with an ellipsis, and the reader is
 * left with a name they cannot read (owner report).
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

/** The status line's own chevron: the 14 dp mark and 4 dp before it. */
internal val ChevronRoom = GlyphSize.ChevronInline + 4.dp

/**
 * The reciter's chevron beside the reciter's name: the same 14 dp mark and
 * the same 4 dp before it. It was 16 dp, two short of the drawing it stood
 * for, so the one-row threshold counted a row that was never there (owner
 * decision, the theme and consistency session).
 */
internal val ReciterChevronRoom = GlyphSize.ChevronInline + 4.dp

/**
 * The room a download's line and the foot under it take beneath the pill's
 * own row: the 2 dp line and the 8 dp of ground below it.
 *
 * It is added to the capsule rather than drawn over the row, so the line
 * never shares a pixel with a control's press shape (owner decision, this
 * session).
 */
internal val ProgressRoom = 10.dp

/**
 * The room the pill's popup keeps from its own edge at the top and the foot.
 *
 * The popup's corner is 20 dp, so content left at its edge sat hard against
 * the curve and the first and last highlights read as touching the menu they
 * hang in (owner report, this session). Ten dp keeps the first and last rows
 * off the curve above and below them; the rows keep their own 22 dp gutter
 * at the sides.
 */
internal val PillMenuGutter = 10.dp

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
 * measure the listener has to get, and the widest the two ever were. The
 * capsule itself may stand narrower than this, since it hugs its words: the
 * anchor keeps its place on the capsule's middle and the menu hangs from
 * there, wider than the control it answers to.
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
 * A row of [controls] at their own target, the app's air between them and
 * none at the ends: what the state's own answers take before the capsule's
 * own [PillAir] is counted.
 */
internal fun transportRun(controls: Int): Dp =
    if (controls <= 0) 0.dp else TransportSize * controls + PillAir * (controls - 1)

/**
 * What the pill's own row spends before the words get a pixel of it: the
 * capsule's air at both ends, the gap, the controls, and the two chevrons the
 * words wear. Three controls, the playing state, is 464 dp, which is a tablet
 * and a landscape phone and no phone in portrait.
 */
internal fun oneRowFloor(controls: Int): Dp =
    PillAir + WordsGap + transportRun(controls) + ChevronRoom +
        ReciterChevronRoom + WordsMeasure + PillAir

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
