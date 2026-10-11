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
 * The one wash that is not here is the reciter's: it follows a word being
 * heard rather than a thing being chosen, and it turns over with the theme,
 * so it lives in [PagePalette.highlight], louder than [Match].
 */
object Wash {

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
