package io.github.muntasimulhaque.quran.ui

/**
 * One thing the phone withheld, and the door the reader can open, carried
 * from the Activity's permission callbacks into the reading's sentence.
 *
 * The Activity owns the system dialogs and the reading owns the one sentence
 * host, so the notice travels between them as a value. [kind] chooses the
 * door: the app's notification settings, or the phone's exact alarm screen.
 * The text and the action are resource ids so the notice speaks the reader's
 * language where it is drawn (owner decision, this session).
 *
 * A notice may name no door, and then it is one sentence and nothing else:
 * that is the phone which answered the door with nothing at all, and the
 * reader is told so on the app's own paper instead of in a bubble the system
 * cannot let them put away (owner decision, this session). [action] and
 * [kind] therefore travel together, and both are absent together.
 */
data class PermissionNotice(
    val text: Int,
    /** The door the notice opens, when the phone has a screen for it. */
    val action: Int? = null,
    /** What that door is; the reader hears it as the answer's name. */
    val kind: Kind? = null,
) {
    enum class Kind { Notifications, ExactAlarm }
}
