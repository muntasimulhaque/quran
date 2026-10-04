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
import java.util.Calendar
import java.util.TimeZone

/**
 * The daily reminder: one quiet notification a day, carrying one ayah of the
 * Book and, when the reader reads with a translation, its first translation.
 *
 * The reminder is the only thing in the app that has a life outside it, so it
 * is built to be counted, not trusted. It schedules itself with the exact
 * alarm on every phone: `SCHEDULE_EXACT_ALARM` is granted at install before
 * Android 14, and from Android 14 the app's own `USE_EXACT_ALARM` declaration
 * restores that grant at install, because a reminder at the reader's own
 * minute is the one time-critical promise this app makes. So the reminder
 * arrives at that minute even on a phone that is locked and idle then, and
 * the reader is never asked for anything to make it so. The phone's own
 * batched alarm is left for the one case where the exact grant is gone, a
 * reader who has turned the phone's special access off themselves; it can
 * still reach a sleeping phone and can be minutes late. Nothing here fetches
 * anything: the ayah is read from the content database that already ships on
 * the device, and the translation from a pack the reader installed
 * themselves.
 *
 * An alarm is a one-shot, so the next one is armed in the same breath the
 * current one fires, and it is armed *before* the ayah is read: a receiver
 * has seconds to live, a database read is real work, and a process the system
 * takes back mid-read must not also be the reason there is no reminder
 * tomorrow. The alarm is armed only while the reader has the switch on, and is
 * re-armed from the app's own launch too, and from the phone's boot, its own
 * clock or zone change, and the app's own update through
 * [DailyAyahBootReceiver], so a reboot no longer costs a morning. A reader
 * who force-stops the app loses its alarms until the next launch, which is
 * platform law and nothing here can change (owner decision).
 *
 * Every alarm is armed under its own day's request code, which is the whole
 * of the launch re-arm. A pending intent is identified by the code as well as
 * by the intent, so an alarm for a moment that has passed and an alarm for
 * tomorrow are two different alarms rather than one alarm moved: opening the
 * app in the afternoon can no longer throw away the eight o'clock the phone is
 * still holding, which when only the batched alarm is left is the difference
 * between a reminder that arrived late and a morning with nothing in it
 * (owner report). Re-arming today is still a re-arm, because the same day is
 * the same slot.
 */
object DailyAyahScheduler {

    /** Marks the intent as the reminder's own, for the receiver's own check. */
    const val ACTION_SHOW = "io.github.muntasimulhaque.quran.action.DAILY_AYAH"

    /** The channel the reminder uses, created by the app's first launch. */
    const val CHANNEL_ID = "daily_ayah"

    /**
     * The one notification the reminder posts under. It is a single number for
     * the whole life of the app, so a fire that lands twice in one morning
     * (a reader who changed the time with one already held, say) updates the
     * one line in the shade instead of leaving two.
     */
    const val NOTIFICATION_ID = 2

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
     * when the reader has turned it off. This is also the launch re-arm, and
     * it is safe to call at any moment: an alarm for a moment still ahead is
     * the same day's slot and is replaced by an identical one, and an alarm
     * for a moment already past belongs to a day that is over, so arming
     * tomorrow leaves it where the platform is holding it.
     *
     * The shape of the alarm is the phone's answer and not the app's: the
     * exact alarm, which every phone in the app's own support range grants at
     * install, and which is the only kind that arrives at the chosen minute on
     * a phone that is locked and idle then; the phone's own batched alarm
     * only when the exact grant is gone, which is a reader who has turned the
     * phone's special access off themselves. Either way the moment is the
     * reader's, and the choice itself is the pure `plan` in `core`, which the
     * JVM suite pins (owner decision).
     */
    fun apply(context: Context, enabled: Boolean, minuteOfDay: Int) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val now = System.currentTimeMillis()
        if (!enabled) {
            // Every alarm this app ever arms belongs to today or to tomorrow,
            // because an arm always asks for the next occurrence of the
            // reader's own minute. Those two slots are the whole of what a
            // switch being turned off has to clear.
            alarm.cancel(pendingIntent(context, localDay(now)))
            alarm.cancel(pendingIntent(context, localDay(tomorrowFrom(now))))
            return
        }
        val plan = DailyReminder.plan(
            canScheduleExact = canScheduleExact(alarm),
            minuteOfDay = minuteOfDay,
            now = now,
            lastMinuteOfDay = LAST_MINUTE_OF_DAY,
        )
        val pending = pendingIntent(context, localDay(plan.triggerAtMillis))
        when (plan.kind) {
            DailyReminder.Kind.Exact ->
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC, plan.triggerAtMillis, pending)
            // The batched fallback is the one that can reach a sleeping
            // phone, late. The exact path is the default on every phone now,
            // so this branch is left for a reader who has turned the phone's
            // own special access off. A windowed alarm is never used: the
            // platform gives it a floor of ten minutes on Android 15 and
            // never delivers it from Doze, so a window was measured and
            // dropped.
            DailyReminder.Kind.Batched ->
                alarm.setAndAllowWhileIdle(AlarmManager.RTC, plan.triggerAtMillis, pending)
        }
    }

    /**
     * The reminder's re-arm after the phone's own events: a boot, an app
     * update, a clock change, or a time zone change. An alarm is a one-shot
     * and a reboot takes it with it, so without this a restart could cost a
     * morning (owner report); the reader's settings are read fresh and the
     * alarm is put back exactly as the app's own launch puts it back.
     *
     * A receiver has seconds to live, so the work runs on the reminder's own
     * worker scope and [onFinished] is called when it is done, which is what
     * lets the caller hold its process with `goAsync` until the alarm is
     * really armed. Every failure is silence: a boot is not a place to show
     * anyone a crash, and the next launch of the app arms the alarm again
     * anyway.
     */
    internal fun rearm(context: Context, onFinished: () -> Unit = {}) {
        val application = context.applicationContext
        workers.launch {
            try {
                val settings = runCatching { SettingsStore(application).settings.first() }
                    .getOrNull() ?: return@launch
                runCatching {
                    apply(
                        context = application,
                        enabled = settings.dailyAyah,
                        minuteOfDay = settings.dailyAyahMinute,
                    )
                }
            } finally {
                onFinished()
            }
        }
    }

    /**
     * Whether this app may set an exact alarm on this phone.
     *
     * Android 12 and later gate it behind a special access that is not a
     * runtime permission. Before Android 14 the install grants it; from
     * Android 14 it starts out denied for apps this app targets, and the
     * manifest's own `USE_EXACT_ALARM` declaration restores the grant at
     * install, because a reminder at the reader's own minute is the one
     * time-critical promise this app makes (owner decision). The app never
     * asks for the grant, never names it as missing, and has no row or button
     * anywhere for it: the reader has one notification to answer for, not
     * two, and the moment the reminder is on is the moment the reader is told
     * about the one permission there is. So this answer is asked where the
     * alarm is armed and used there and nowhere else. The exact path is
     * therefore taken on every phone by default, and the phone's own batched
     * alarm is armed only when a reader has turned the special access off
     * themselves, because it still reaches a sleeping phone, late.
     */
    fun canScheduleExact(alarm: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()

    /** The reader's own day number, the one the alarm's request code is. */
    private fun localDay(millis: Long): Int =
        Math.floorDiv(
            millis + TimeZone.getDefault().getOffset(millis),
            DAY_MILLIS,
        ).toInt()

    /**
     * Tomorrow by the reader's own calendar rather than by 86,400,000
     * milliseconds, so the day number a cleared alarm is looked up under is
     * the right one on a morning the clocks change.
     */
    private fun tomorrowFrom(now: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

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
     * posted. A switch the reader turned off while the phone slept is honored
     * here too: a held alarm from a day the reminder was still on must not
     * speak for a reader who has since turned it off.
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
                    // that lost a whole day (owner report, 37th session). The
                    // reader's own settings are read here rather than taken
                    // from the fire, so a switch turned off while the phone
                    // slept is honored and the alarm is anchored on the
                    // moment again rather than on the fire, so a late delivery
                    // never walks the hour forward day after day.
                    val settings = runCatching { SettingsStore(application).settings.first() }
                        .getOrNull() ?: return@launch
                    apply(
                        context = application,
                        enabled = settings.dailyAyah,
                        minuteOfDay = settings.dailyAyahMinute,
                    )
                    if (!settings.dailyAyah) return@launch
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

    /**
     * The reminder's own pending intent, one per day.
     *
     * The request code is the reader's day number, so an alarm for a moment
     * that has passed and an alarm for tomorrow are two alarms rather than one
     * alarm moved, and a launch, a time change, or a fire touches only the
     * day it means to. The day number is a small positive number for the
     * life of any phone that is running, so the codes never collide.
     */
    private fun pendingIntent(context: Context, day: Int): PendingIntent {
        val intent = Intent(context, Receiver::class.java).setAction(ACTION_SHOW)
        return PendingIntent.getBroadcast(
            context,
            day,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val DAY_MILLIS = 86_400_000L
}

/**
 * The reminder's text, centered. The reading draws Arabic centered on the
 * manuscript page, and a notification that left-aligned it read as English
 * wearing Arabic letters. Center is direction-neutral, so it needs no bidi
 * mark and cannot turn the translation's closing punctuation around. If the
 * system ignores paragraph spans, the text is exactly what it was before
 * this (owner decision).
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
