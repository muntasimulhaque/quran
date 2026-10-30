package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.kit.GlyphPress
import io.github.muntasimulhaque.quran.ui.kit.GlyphTarget
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The pill's own geometry, in one place, because the bar that holds the
 * words and the words themselves measure against the same numbers: a
 * number that drifted between them would put the words where the row is
 * not, and the row's own measure is the answer to the phone question.
 */

/**
 * The gutter a floating control keeps from the glass in the least: a pill
 * that reaches the screen's own edge stops reading as a control over the
 * page and starts reading as a sheet the app forgot to inset (owner report).
 *
 * It is the floor, not the measure. [PillMeasure] is the width the pill and
 * the offer stand at, and a glass narrower than that takes this much on each
 * side and no more, while a glass wider than it keeps the capsule on the
 * page's own middle with the page's ground beside it.
 */
internal val BarGutter = 16.dp

/**
 * The width the pill stands at in every voice, and the width of the offer bar
 * that asks before it: one measure, chosen once, never a function of the words
 * it holds.
 *
 * The capsule used to hug its content, so it was about 285 dp at the shortest
 * places, the page's own 328 at the longest, about 230 for an offer, and about
 * 210 for a transfer that widened again as the percent did. A control whose
 * edge moves while a download advances is a shape the reader watches instead
 * of the reading, and the progress line at its foot changed length with it
 * (owner report, this session: the transfer was narrower at its first percent
 * than at its last, and an offer and the transfer that followed it were two
 * widths for one download).
 *
 * 328 is the page's own width on the app's reference 360 dp phone, 360 less
 * the two [BarGutter]s, which is the widest the capsule ever stood while
 * hugging. So nothing that fits stops fitting: the words' block keeps 148 dp,
 * and the place line its 140, where 108 of the 114 surahs' longest places fit
 * whole in English and 102 in Bangla. The transfer's status gets that room at
 * every percent instead of the room its digits happen to need, and the offer's
 * name and size get it before the download starts.
 *
 * The pill's other numbers are fixed rather than derived from the glass (64 dp
 * tall, the 48 dp targets, the 40 dp press, the 288 dp menu), so its width is
 * too: a tablet does not get a taller pill, and it does not get a wider one.
 * The same control stands at the same measure on all three store profiles. A
 * glass narrower than the measure takes the glass less the gutters instead,
 * where the 288 dp menu still stands on the capsule's middle.
 */
internal val PillMeasure = 328.dp

/**
 * The air between the shapes the reader sees: between two controls, the
 * words' block and the transport, and the transfer's X and the words.
 *
 * It is the app's `Space.Line`, the gap a cluster of same-kind small controls
 * keeps, and the transport is that cluster. The pill carries no close any
 * more, so its shape is free to shrink to its words, and this is the one
 * number that shape is measured against (owner decision).
 *
 * Every shape in the pill but the transport's circles fills its target: the
 * words' block, the Download, and the X's word answers. The circles do not,
 * because a 48 dp circle cannot stand beside another without touching it, so
 * a circle is [TransportInset] inside its target and two of them keep this
 * number between them with no target air at all (owner decision, this
 * session). Everything else measures this air from its target, and everything
 * measures this air from its shape.
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
 *
 * It is measured between the shapes the reader sees, never between the
 * invisible targets, which is the app's own rule for a side-by-side gap. A
 * glyph answer draws [TransportShape] inside a [TransportSize] target, so the
 * row gives that inset back and hands the answer [GlyphLead] of target air for
 * [WordsGap] of shape air. A word answer's shape ends at its own edge, so its
 * lead is this number whole (owner decision, this session).
 */
internal val WordsGap = 12.dp

/**
 * One control's own target, which is the app's [GlyphTarget] and what
 * `minimumInteractiveComponentSize` gives it. A Material release that widens
 * either number has to widen the other, or the row is measured against a
 * control that no longer exists.
 */
internal val TransportSize = GlyphTarget

/**
 * The circle a transport control's press lights, inside its target: the app's
 * one round control, [GlyphPress].
 *
 * The glyph inside the circle is the control's own mark, 24 dp, so the press
 * is not the mark and the mark is not the press: a mark that filled its target
 * would flash a dot inside the button the finger hit (owner report, an earlier
 * session), and a circle the size of the target cannot stand beside another
 * circle without touching it. 40 dp inside 48 dp is the app's answer to both,
 * and it is Material's own icon button (owner decision, this session).
 */
internal val TransportShape = GlyphPress

/** The room a transport target keeps around its own circle, on each side. */
internal val TransportInset = (TransportSize - TransportShape) / 2

/**
 * The air two transport targets keep: none.
 *
 * The circles the reader sees keep [PillAir], the app's one air between a
 * cluster's shapes, and a circle is [TransportInset] inside its target on each
 * side, so the targets themselves stand [PillAir] less two insets apart: edge
 * to edge. That is the whole of the compression the transport took: the two
 * 8 dp gaps between the targets became 0, the circles kept the 8 they always
 * had, and the words gained those 16 dp of the capsule's width (owner
 * decision, this session). Two
 * targets that overlapped would be a 40 dp target wearing a 48 dp name, so
 * edge to edge is the floor.
 */
internal val TransportAir = PillAir - (TransportSize - TransportShape)

/**
 * The lead a glyph answer's target takes so its circle keeps [WordsGap].
 *
 * The answer at the pill's own end is a round control on every voice that has
 * one: the transport, and the transfer's X. Its circle is [TransportInset]
 * inside its target on each side, and air measured to the target would read as
 * 16 dp where the app's rule asks for 12.
 */
internal val GlyphLead = WordsGap - TransportInset

/**
 * The room a download's line and the foot under it take beneath the pill's
 * own row: the 2 dp line and the 8 dp of ground below it.
 *
 * It is added to the capsule rather than drawn over the row, so the line
 * never shares a pixel with a control's press shape (owner decision).
 */
internal val ProgressRoom = 10.dp
