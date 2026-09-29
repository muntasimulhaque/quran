package io.github.muntasimulhaque.quran.daily

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Whether the phone will show the daily reminder.
 *
 * The reminder has one dependency outside the app: the reader can turn
 * notifications off for the app, or for this one channel, in the system
 * settings, and no switch inside the app can say "on" honestly about that. The
 * setting is read from the phone rather than remembered from the last
 * permission dialog, because the app is not what changes it, and it is
 * refreshed on every return to the foreground, so a reader who goes to the
 * system settings and comes back is not told a thing that is no longer true.
 *
 * Both doors are read, and that is the whole of this file's reason to exist.
 * `areNotificationsEnabled()` answers for the app, and a channel the reader
 * turned off in the phone's own page answers nothing to it: the app-level
 * switch stays on while the one notification the app ever posts never appears.
 * A reader who did that was told nothing, and the reminder looked broken
 * (owner report, 37th session), so the channel's own importance is read too
 * and a channel that does not exist yet is not a block, since the app creates
 * it at its first launch.
 *
 * The answer is handed back as a [State] rather than a value, so a sheet can
 * read it where it is drawn and be the only thing that recomposes when the
 * reader comes back from the phone's own page.
 */
@Composable
fun rememberNotificationsBlocked(): State<Boolean> {
    val context = LocalContext.current
    val blocked = remember { mutableStateOf(notificationsBlocked(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) blocked.value = notificationsBlocked(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return blocked
}

/** True when the phone will not show the reminder, for either of its reasons. */
fun notificationsBlocked(context: Context): Boolean =
    !NotificationManagerCompat.from(context).areNotificationsEnabled() ||
        reminderChannelHidden(context)

/**
 * Whether the phone will keep an exact time for the reminder.
 *
 * This one is not a runtime permission: on Android 12 and later it is the
 * reader's own switch in the phone's settings, called Alarms and reminders,
 * and the app never gets it without a deliberate tap. So it is read, never
 * asked for in the middle of a flow, and the page that wants it says so in
 * one line and opens the phone's own page for it. On the releases before
 * Android 12 the platform never gated exact alarms and the answer is a plain
 * yes, with nothing for the reader to do.
 */
fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarm = context.getSystemService(AlarmManager::class.java) ?: return false
    return runCatching { alarm.canScheduleExactAlarms() }.getOrDefault(false)
}

/**
 * The same answer, kept live the way the notification answer is: the reader
 * leaves for the phone's own page, grants it, and comes back, and the line
 * under the switch must be gone by then. It is read on every return to the
 * foreground rather than remembered from the last time the page was drawn.
 */
@Composable
fun rememberExactAlarmsAllowed(): State<Boolean> {
    val context = LocalContext.current
    val allowed = remember { mutableStateOf(canScheduleExactAlarms(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed.value = canScheduleExactAlarms(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return allowed
}

/**
 * Whether this one channel has been turned off in the phone's own settings.
 * A channel the app has not created yet is not a block: the phone's page
 * creates nothing, and the app makes the channel at its first launch.
 *
 * It is read apart from [notificationsBlocked] so the way out can be the
 * reader's own channel and not the app's whole page, which is what a reader
 * who turned this one reminder off is looking for.
 */
fun reminderChannelHidden(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val manager = context.getSystemService(NotificationManager::class.java) ?: return false
    val channel = runCatching {
        manager.getNotificationChannel(DailyAyahScheduler.CHANNEL_ID)
    }.getOrNull() ?: return false
    return channel.importance == NotificationManager.IMPORTANCE_NONE
}
