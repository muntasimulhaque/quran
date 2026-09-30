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
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AlignmentSpan
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.muntasimulhaque.quran.MainActivity
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.core.DailyReminder
import io.github.muntasimulhaque.quran.data.LAST_MINUTE_OF_DAY
import io.github.muntasimulhaque.quran.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The daily reminder: one quiet notification a day, carrying one ayah of the
 * Book and, when the reader reads with a translation, its first translation.
 *
 * The reminder is the only thing in the app that has a life outside it, so it
 * is built to be counted, not trusted. It schedules itself with the best
 * alarm the phone will give it: an exact one, when the reader has granted the
 * phone's own exact alarm access, so the reminder arrives at that minute even
 * on a phone that is locked and idle then, and the phone's own batched
 * alarm otherwise, which can still reach a sleeping phone. Nothing here
 * fetches anything: the ayah is read from the content database that already
 * ships on the device, and the translation from a pack the reader installed
 * themselves.
 *
 * An alarm is a one-shot, so the next one is armed in the same breath the
 * current one fires, and it is armed *before* the ayah is read: a receiver
 * has seconds to live, a database read is real work, and a process the system
 * takes back mid-read must not also be the reason there is no reminder
 * tomorrow. The alarm is armed only while the reader has the switch on, and is
 * re-armed from the app's own launch too, so a reboot or a clock change costs
 * at most the one morning before the app is opened again; that is the price of
 * not asking for a boot permission this app has never needed.
 */
object DailyAyahScheduler {

    /** Marks the intent as the reminder's own, for the receiver's own check. */
    const val ACTION_SHOW = "io.github.muntasimulhaque.quran.action.DAILY_AYAH"

    /** The channel the reminder uses, created by the app's first launch. */
    const val CHANNEL_ID = "daily_ayah"

    /**
     * The one notification the reminder posts under. It is a single number for
     * the whole life of the app, so a fire that lands twice in one morning
     * (the reader opened the app at their own moment and the launch re-armed
     * one) updates the one line in the shade instead of leaving two.
     */
    const val NOTIFICATION_ID = 2

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
     * after every fire and after every change the reader makes, and it is what
     * a launch that finds no alarm due calls too ([reArmOnLaunch]).
     *
     * The shape of the alarm is the phone's answer and not the app's: an
     * exact alarm when the reader has given the app the phone's own exact
     * alarm access, which is the only kind that arrives at the chosen minute
     * on a phone that is locked and idle then, and the platform's own batched
     * alarm otherwise, which is what a reader who declines the grant gets.
     * Either way the moment is the reader's, and the choice itself is the pure
     * `plan` in `core`, which the JVM suite pins (owner decision, D-114).
     */
    fun apply(context: Context, enabled: Boolean, minuteOfDay: Int) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context)
        if (!enabled) {
            alarm.cancel(pending)
            return
        }
        val plan = DailyReminder.plan(
            canScheduleExact = canScheduleExact(alarm),
            minuteOfDay = minuteOfDay,
            now = System.currentTimeMillis(),
            lastMinuteOfDay = LAST_MINUTE_OF_DAY,
        )
        when (plan.kind) {
            DailyReminder.Kind.Exact ->
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC, plan.triggerAtMillis, pending)
            // The inexact fallback is the one that can reach a sleeping
            // phone. A windowed alarm cannot: the platform gives it a floor
            // of ten minutes on Android 15 and never delivers it from Doze, so
            // a window was measured and dropped (D-114).
            DailyReminder.Kind.Batched ->
                alarm.setAndAllowWhileIdle(AlarmManager.RTC, plan.triggerAtMillis, pending)
        }
    }

    /**
     * Whether this app may set an exact alarm on this phone.
     *
     * Android 12 and later gate it behind the reader's own grant, which is
     * not a runtime permission and is not given at install. The app never asks
     * for it and never says anything about it in the settings sheet (owner
     * report, D-130): the reader has one notification to answer for, not two,
     * and a second door into the phone's own pages is a thing a reader has to
     * understand before they can turn one thing on. So this answer is asked
     * where the alarm is armed and used there and nowhere else. The exact
     * path is still taken whenever the phone allows it, which is every
     * release before Android 12 and any phone where the reader has granted it
     * in the phone's settings of their own accord; otherwise the reminder is
     * armed with the phone's own batched alarm, which still reaches a
     * sleeping phone (D-114).
     */
    fun canScheduleExact(alarm: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()

    /**
     * The launch re-arm, and the one place it is allowed not to re-arm.
     *
     * Every launch re-arms the reminder, because the app hears nothing about a
     * reboot and a dead alarm is a reminder that never comes again. But an
     * arm replaces the pending one, and the reader's own moment is the minute
     * they are most likely to open the app in: a launch there threw away the
     * delivery the platform was still going to make, and the morning was
     * silent (owner report, 37th session). Inside the window after that
     * moment, the alarm is left exactly where the platform has it. Everywhere
     * else, the launch re-arms, and an alarm that really was lost is put back.
     */
    fun reArmOnLaunch(context: Context, enabled: Boolean, minuteOfDay: Int) {
        if (enabled &&
            DailyReminder.stillDueToday(minuteOfDay, System.currentTimeMillis(), LAST_MINUTE_OF_DAY)
        ) {
            return
        }
        apply(context, enabled = enabled, minuteOfDay = minuteOfDay)
    }

    /**
     * Arms the next day's reminder from wherever the current one fired. The
     * reader's own settings are read, so a switch turned off while the phone
     * slept is honored rather than overridden by the fire's own moment, and
     * the alarm is anchored on the moment again rather than on the fire, so a
     * late delivery never walks the hour forward day after day.
     */
    private suspend fun armNext(context: Context) {
        val settings = runCatching { SettingsStore(context).settings.first() }.getOrNull()
            ?: return
        apply(context, enabled = settings.dailyAyah, minuteOfDay = settings.dailyAyahMinute)
    }

    /**
     * The next moment the reminder should come, in the reader's own local
     * time. The hour and the minute are theirs, read off the one number the
     * picker writes, and the arithmetic lives in `core` so the JVM suite can
     * pin it without an alarm manager.
     */
    internal fun nextOccurrence(minuteOfDay: Int, now: Long = System.currentTimeMillis()): Long =
        DailyReminder.nextOccurrence(
            minuteOfDay = minuteOfDay,
            now = now,
            lastMinuteOfDay = LAST_MINUTE_OF_DAY,
        )

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
                    // The alarm that fired is spent, and tomorrow's is armed
                    // before anything else: a receiver's process may be taken
                    // back the moment it goes quiet, and a reader whose process
                    // died reading the ayah must still have a reminder
                    // tomorrow. Arming after the notification was the one order
                    // that lost a whole day (owner report, 37th session).
                    armNext(application)
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
                            .setContentText(centered(content.arabic))
                            .setStyle(
                                NotificationCompat.BigTextStyle().bigText(
                                    centered(
                                        buildString {
                                            append(content.arabic)
                                            content.translation?.takeIf { it.isNotBlank() }?.let {
                                                append("\n\n")
                                                append(it)
                                            }
                                        },
                                    ),
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
                } finally {
                    pending.finish()
                }
            }
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

/**
 * The reminder's text, centered. The reading draws Arabic centered on the
 * manuscript page, and a notification that left-aligned it read as English
 * wearing Arabic letters. Center is direction-neutral, so it needs no bidi
 * mark and cannot turn the translation's closing punctuation around. If the
 * system ignores paragraph spans, the text is exactly what it was before
 * this (owner decision, D-108).
 */
private fun centered(text: CharSequence): CharSequence {
    if (text.isEmpty()) return text
    return SpannableString(text).apply {
        setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0,
            length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE,
        )
    }
}
