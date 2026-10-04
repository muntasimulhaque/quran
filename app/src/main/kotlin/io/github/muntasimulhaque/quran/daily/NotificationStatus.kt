package io.github.muntasimulhaque.quran.daily

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
 * Whether the phone will show the daily reminder, and whether it is
 * withholding the exact time.
 *
 * The reminder has two dependencies outside the app: the reader can turn
 * notifications off for the app, or for this one channel, in the system
 * settings, and Android 14 and later withhold the exact alarm from the start.
 * Neither is an in-app switch, and no switch inside the app can say "on"
 * honestly about either, so both are read from the phone rather than
 * remembered from the last dialog. The exact alarm is asked for once at each
 * of the two acts that set the reminder, in the phone's own screen; this
 * file only carries whether it is still being withheld, which the Daily page
 * draws as state (owner decision). Neither answer is refreshed only at
 * launch: both are read again on every return to the foreground, so a reader
 * who went to the system settings and came back is not told a thing that is
 * no longer true.
 *
 * The notification answers are read apart: `areNotificationsEnabled()`
 * answers for the app, and a channel the reader turned off in the phone's own
 * page answers nothing to it: the app-level switch stays on while the one
 * notification the app ever posts never appears. A reader who did that was
 * told nothing, and the reminder looked broken (owner report, 37th
 * session), so the channel's own importance is read too and a channel that
 * does not exist yet is not a block, since the app creates it at its first
 * launch.
 *
 * Each answer is handed back as a [State] rather than a value, so a sheet can
 * read it where it is drawn and be the only thing that recomposes when the
 * reader comes back from the phone's own page.
 */
@Composable
fun rememberNotificationsBlocked(): State<Boolean> = rememberPhoneAnswer(::notificationsBlocked)

/**
 * Whether the phone is withholding the exact alarm, read and refreshed the
 * same way as the notification answer: the reader can turn it on in the
 * phone's own screen and come straight back, and the page must not still be
 * waiting on the answer it had before.
 */
@Composable
fun rememberExactGrantWithheld(): State<Boolean> =
    rememberPhoneAnswer { context -> DailyAyahScheduler.exactGrantWithheld(context) }

/** A question for the phone, read where it is drawn and kept fresh. */
@Composable
private fun <T> rememberPhoneAnswer(read: (Context) -> T): State<T> {
    val context = LocalContext.current
    val answer = remember { mutableStateOf(read(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) answer.value = read(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return answer
}

/** True when the phone will not show the reminder, for either of its reasons. */
fun notificationsBlocked(context: Context): Boolean =
    !NotificationManagerCompat.from(context).areNotificationsEnabled() ||
        reminderChannelHidden(context)

/**
 * Whether this one channel has been turned off in the phone's own settings.
 * A channel the app has not created yet is not a block: the phone's page
 * creates nothing, and the app makes the channel at its first launch.
 *
 * It is read apart from [notificationsBlocked] so the way out can be the
 * reader's own channel and not the app's whole page, which is what a reader
 * who turned this one reminder off is looking for.
 *
 * The exact alarm is the other question of this file: what the phone allows
 * is read by [DailyAyahScheduler.canScheduleExact], the ask is made by
 * [DailyAyahScheduler.exactGrantRequest] at the two acts that set the
 * reminder, and the arming reads it where the alarm is armed.
 */
fun reminderChannelHidden(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val manager = context.getSystemService(NotificationManager::class.java) ?: return false
    val channel = runCatching {
        manager.getNotificationChannel(DailyAyahScheduler.CHANNEL_ID)
    }.getOrNull() ?: return false
    return channel.importance == NotificationManager.IMPORTANCE_NONE
}
