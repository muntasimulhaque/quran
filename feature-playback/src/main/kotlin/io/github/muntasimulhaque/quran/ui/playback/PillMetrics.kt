package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 * The air between the shapes the reader sees: between two controls, the
 * words' block and the transport, and the transfer's X and the words.
 *
 * Every shape in the pill fills its target: the words' block, the transport's
 * circles, the Download, and the X. So this is the air between the targets
 * and to the capsule's edge, and there is no second number to learn. The pill
 * carries no close any more, so its shape is free to shrink to its words, and
 * this is the one number that shape is measured against (owner decision). It
 * is the app's `Space.Line`, the gap a cluster of same-kind small controls
 * keeps, and the transport is that cluster.
 */
internal val PillAir = Space.Line

/**
 * The air the words' own end keeps from the capsule on every pill the app
 * draws: the offer, the transfer, and the playing capsule alike.
 *
 * The capsule's end is a sphere, half its height, and the words do not stand
 * on the capsule's middle: the name's line starts about 15 dp below the
 * capsule's top, where the edge has already curved in 4.5 dp. An [PillAir]
 * measured from the capsule's widest point therefore reads as almost nothing
 * at the words' own height, and on the phone the reciter's name sat 5 to 8 dp
 * inside the border, with its press shape meeting the curve. `Space.Block + Space.Tight` is the app's 8 dp air plus that shoulder,
 * which is what makes the name read as held rather than crowded (owner
 * decision: the name looked cut in Bangla, whose matra reaches the top of its
 * line).
 *
 * It is the words' side only, and it is worn at whichever end the words stand
 * alone at: their leading end when an answer follows, and both their ends on
 * the unavailable pill, which is words alone. The transport, the Download,
 * and the X keep [PillAir] at their end, because they stand at the capsule's
 * widest height or wear a round shape of their own, where the air reads true.
 */
internal val PillEndAir = Space.Block + Space.Tight

/**
 * The gap between the words and what answers them: the transport, the
 * Download, the Retry, or the X.
 *
 * They are two standalone shapes rather than one cluster, so this is the
 * app's `Space.Row` (owner decision, the theme and consistency session).
 */
internal val WordsGap = 12.dp

/**
 * One control's own target, which is the app's floor and what
 * `minimumInteractiveComponentSize` gives it. A Material release that widens
 * it has to widen this number with it, or the row is measured against a
 * control that no longer exists.
 */
internal val TransportSize = 48.dp

/**
 * The room a download's line and the foot under it take beneath the pill's
 * own row: the 2 dp line and the 8 dp of ground below it.
 *
 * It is added to the capsule rather than drawn over the row, so the line
 * never shares a pixel with a control's press shape (owner decision).
 */
internal val ProgressRoom = 10.dp

/**
 * The room the pill's popup keeps from its own edge at the top and the foot.
 *
 * The popup's corner is 20 dp, so content left at its edge sat hard against
 * the curve and the first and last highlights read as touching the menu they
 * hang in (owner report). Ten dp keeps the first and last rows off the curve
 * above and below them; the rows keep their own 22 dp gutter at the sides.
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
