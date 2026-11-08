package io.github.muntasimulhaque.quran.ui.theme

/**
 * The translucent fills the app paints, named once, in one order.
 *
 * A wash is a tint laid under words that keep the reading's own ink: it says
 * what a thing is, the chosen ayah or a matched word, without becoming a
 * color of its own. The tint is chosen at the call site, the accent for
 * something chosen or found and the ink for a quiet ground. The steps are
 * ordered by how loudly they speak, and the whole app reads one scale: a door
 * is the quietest, a selection is one step up, a button's own shape is a step
 * up again, and a match is the loudest, because a matched word must be found
 * at a glance (owner decision, the audio and type audit session).
 *
 * Before this, sixteen call sites carried sixteen alphas between 0.05 and
 * 0.25 doing five jobs, and the next session that painted a chosen thing had
 * no way to know which number a chosen thing wore.
 *
 * The one wash that is not here is the page's word mark: the reciter's word
 * and the word a search arrived on follow a place on the Quran's own line
 * rather than a thing being chosen, they turn over with the theme, and their
 * alpha is the ceiling the seven rule colors keep, so they live in
 * [PagePalette.highlight]. That mark is a ground under the tajweed colors,
 * not under the interface's ink, so it is a quieter number than [Match]; the
 * search result's own line keeps [Match] for its text.
 *
 * A control's fill has three levels, and the app keeps them apart (owner
 * decision, this session): the accent at [Control] is a chosen state inside a
 * set of controls (a search source turned on), carried by the accent's own
 * ink; the accent at [Button] is the reader's own action (Save, Add,
 * Download, Share image); and the ink at [Door] is a quiet action or an
 * unselected chip (Cancel, Remove, Share text). The reading's ayah door is the
 * one control that takes no fill: its head already stands on the block's own
 * wash while its menu is open, and its turn is what says so (owner decision,
 * this session). The solid level is the one place the reader is at (the tab
 * you are on, the ayah number you are at) and the one prominent answer on a
 * surface that asks a single question (Save, Turn it on, Download); it never
 * shares that surface with a selected tab, so the two meanings cannot be read
 * at once. A search source is a chosen control and not a place: seven of them
 * stood as seven solid pills over the results, which read as seven pressed
 * buttons, so the chosen chip wears the door's own tint and ink and the
 * solid level stays for the one place and the one answer (owner decision,
 * this session).
 */
object Wash {

    /** The ground of a control that cannot answer: under a door's own quiet. */
    const val Disabled = 0.04f

    /** The ground under a quiet door or row: the quietest step. */
    const val Door = 0.06f

    /** One step up: the chosen thing, an ayah or an open introduction. */
    const val Selected = 0.08f

    /** The shape of the button that carries the app's one action color. */
    const val Button = 0.10f

    /** A chosen step inside a control, and a switch's track on its way. */
    const val Control = 0.14f

    /** A matched word in a search result: the loudest of the quiet fills. */
    const val Match = 0.16f
}
