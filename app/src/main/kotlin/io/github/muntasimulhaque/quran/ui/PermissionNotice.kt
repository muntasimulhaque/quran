package io.github.muntasimulhaque.quran.ui

/**
 * One thing the phone withheld, and the door the reader can open, carried
 * from the Activity's permission callbacks into the reading's Snackbar.
 *
 * The Activity owns the system dialogs and the reading owns the one Snackbar
 * host, so the notice travels between them as a value. [kind] chooses the
 * door: the app's notification settings, or the phone's exact alarm screen.
 * The text and the action are resource ids so the notice speaks the reader's
 * language where it is drawn (owner decision, this session).
 */
data class PermissionNotice(
    val text: Int,
    val action: Int,
    val kind: Kind,
) {
    enum class Kind { Notifications, ExactAlarm }
}
