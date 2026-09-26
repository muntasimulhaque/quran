package io.github.muntasimulhaque.quran.daily

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.muntasimulhaque.quran.MainActivity
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.data.LAST_MINUTE_OF_DAY
import io.github.muntasimulhaque.quran.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * The daily reminder: one quiet notification a day, carrying one ayah of the
 * Book and, when the reader reads with a translation, its first translation.
 *
 * The reminder is the only thing in the app that has a life outside it, so it
 * is built to be counted, not trusted. It schedules itself with the system's
 * own inexact alarm: the platform may move it by minutes under Doze, and that
 * is exactly right for an invitation to read, where a wake-up at an exact
 * second is neither wanted nor worth a wake lock. Nothing here fetches
 * anything: the ayah is read from the content database that already ships on
 * the device, and the translation from a pack the reader installed
 * themselves.
 *
 * An alarm is a one-shot, so the next one is armed in the same breath the
 * current one fires: that is what makes the reminder daily without a
 * repeating alarm, and it re-anchors the moment every day instead of letting
 * the platform's deferrals walk it later and later. The alarm is armed only
 * while the reader has the switch on, and is re-armed from the app's own
 * launch too, so a reboot or a clock change costs at most the one morning
 * before the app is opened again; that is the price of not asking for a boot
 * permission this app has never needed.
 */
object DailyAyahScheduler {

    /** Marks the intent as the reminder's own, for the receiver's own check. */
    const val ACTION_SHOW = "io.github.muntasimulhaque.quran.action.DAILY_AYAH"

    /** The channel the reminder uses, created by the app's first launch. */
    const val CHANNEL_ID = "daily_ayah"

    private const val REQUEST_CODE = 41

    /**
     * The receiver's own worker, one per process. A receiver is finished the
     * moment its coroutine is launched, so the scope outlives it on purpose
     * and the `goAsync` pending result is what keeps the process alive.
     */
    private val workers = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Creates the reminder's channel at app start, so it is visible in the
     * system's own settings before it ever speaks, and refreshes its words when
     * the app's own wording has changed since the install was made.
     *
     * Silent on purpose: a reminder waits in the shade without making a sound,
     * and a reader who wants to hear it can raise the channel themselves.
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val name = context.getString(R.string.daily_channel_name)
        val words = context.getString(R.string.daily_channel_description)
        val existing = manager.notificationChannels?.firstOrNull { it.id == CHANNEL_ID }
        if (existing != null && existing.name.toString() == name && existing.description == words) {
            return
        }
        // Re-creating a channel that is already there is the platform's own
        // way to change its words: the system keeps whatever the reader chose
        // in its settings and takes only the name and the description from
        // what the app passes. That is what carries a renamed or reworded
        // channel to an install that has had it since before the words
        // changed (owner decision, 2.3), and it is why the call is made here
        // rather than once, ever.
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW).apply {
                description = words
            },
        )
    }

    /**
     * Arms the reminder for the next occurrence of [minuteOfDay], or clears it
     * when the reader has turned it off. Re-arming the same pending intent
     * moves the alarm rather than adding a second one, so this is safe to call
     * on every launch and after every fire.
     */
    fun apply(context: Context, enabled: Boolean, minuteOfDay: Int) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context)
        if (!enabled) {
            alarm.cancel(pending)
            return
        }
        val at = nextOccurrence(minuteOfDay)
        // Inexact on purpose: the system may move this by minutes to keep the
        // whole phone's radios and wake ups quieter, and a reminder that is
        // ten minutes late is a reminder.
        alarm.set(AlarmManager.RTC, at, pending)
    }

    /**
     * Arms the next day's reminder from wherever the current one fired. The
     * reader's own settings are read, so a switch turned off while the phone
     * slept is honored rather than overridden by the fire's own moment.
     */
    private suspend fun armNext(context: Context) {
        val settings = runCatching { SettingsStore(context).settings.first() }.getOrNull()
            ?: return
        apply(context, enabled = settings.dailyAyah, minuteOfDay = settings.dailyAyahMinute)
    }

    /**
     * The next moment the reminder should come, in the reader's own local
     * time. The hour and the minute are theirs, read off the one number the
     * picker writes.
     */
    internal fun nextOccurrence(minuteOfDay: Int, now: Long = System.currentTimeMillis()): Long {
        val minute = minuteOfDay.coerceIn(0, LAST_MINUTE_OF_DAY)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, minute / 60)
            set(Calendar.MINUTE, minute % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    /**
     * The reminder itself. The reader tapped nothing to get here, so the
     * notification carries the whole thought: the Arabic, the translation
     * when they read with one, and the place, with the tap opening that ayah
     * in the study reading.
     *
     * A receiver runs with seconds to live and no process, so the work is
     * small and every failure is a skipped morning rather than a crash: the
     * ayah is looked up, and if it cannot be, the reminder is simply not
     * posted.
     */
    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != ACTION_SHOW) return
            // The reader can revoke notifications at any moment; a reminder
            // must never throw at the system door.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }
            // Reading the ayah is a database read, and a receiver has seconds
            // to live on the main thread: the work moves to a worker through
            // goAsync, and the pending result is finished in every path so
            // the system is never left holding a receiver that has not
            // returned. A receiver's process may die the moment it returns,
            // which is why the reminder's next alarm is armed before the
            // notification is built: a skipped morning must never also mean
            // no further reminders.
            val pending = goAsync()
            val application = context.applicationContext
            workers.launch {
                try {
                    val content = runCatching { DailyAyahContent.load(application) }.getOrNull()
                        ?: return@launch
                    val notification = runCatching {
                        NotificationCompat.Builder(application, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_daily)
                            .setContentTitle(
                                application.getString(
                                    R.string.daily_notification_title,
                                    content.surahName,
                                    content.surah,
                                    content.ayah,
                                ),
                            )
                            .setContentText(content.arabic)
                            .setStyle(
                                NotificationCompat.BigTextStyle().bigText(
                                    buildString {
                                        append(content.arabic)
                                        content.translation?.takeIf { it.isNotBlank() }?.let {
                                            append("\n\n")
                                            append(it)
                                        }
                                    },
                                ),
                            )
                            .setContentIntent(content.pendingIntent(application))
                            .setAutoCancel(true)
                            // A reminder is not an alarm: no sound of its own
                            // beyond the channel, no ongoing flag.
                            .setCategory(NotificationCompat.CATEGORY_REMINDER)
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .build()
                    }.getOrNull() ?: return@launch
                    runCatching {
                        NotificationManagerCompat.from(application)
                            .notify(NOTIFICATION_ID, notification)
                    }
                    // The alarm that fired is spent; tomorrow's is armed now,
                    // from the setting the reader left, so the reminder keeps
                    // arriving and the hour never drifts forward.
                    armNext(application)
                } finally {
                    pending.finish()
                }
            }
        }

        private companion object {
            const val NOTIFICATION_ID = 2
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, Receiver::class.java).setAction(ACTION_SHOW)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
