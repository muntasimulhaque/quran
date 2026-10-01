package io.github.muntasimulhaque.quran

import android.Manifest
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.daily.DailyAyahScheduler
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream
import java.util.Calendar

/**
 * The reminder's alarm, fired at the reader, really puts a notification in
 * the shade.
 *
 * The reader reported two things about the daily ayah: it came two minutes
 * after the ten o'clock they had set, and they did not find it in the shade
 * until they opened the app. The first was the alarm, and it is now the best
 * one the phone will give the app. This file is the second: everything
 * between an alarm and the shade is the receiver, a permission check, a read
 * of the day's ayah out of the content database, a build, and a post. Every
 * step of that can be true while the reader sees nothing, and nothing else in
 * the app would show it to them, so the path is walked here end to end: the
 * broadcast goes out exactly as the alarm sends it, and the shade is read back
 * afterwards from the phone's own record of what it is holding.
 *
 * The phone's record is read through `dumpsys` rather than through
 * `StatusBarNotification`, which is not part of the public SDK and so cannot
 * be named in a test at all. A receiver has seconds to live and a database
 * read is real work, so the waits are minutes rather than seconds: a slow
 * emulator is not a failing reminder.
 *
 * The second test is the alarm itself, and it is here because being wrong
 * about it is invisible otherwise. The platform holds a minimum window of its
 * own, ten minutes on Android 15, so the app's one minute ask for a windowed
 * alarm comes back as ten: a path that was written on the belief that a
 * windowed alarm lands inside the minute the reader chose, and that would have
 * been no better than the inexact alarm it replaced, was measured here and
 * dropped. The test now arms the reminder exactly as the app does and reads
 * the record the platform keeps: with the reader's grant, an exact alarm and
 * no window at all; without it, an alarm the platform holds and does not
 * promise the minute, which is what the Daily page tells the reader.
 */
@RunWith(AndroidJUnit4::class)
class DailyReminderTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        // The permission is asked for at the two acts of setting, and a
        // reader who has set the reminder has granted it. A receiver that
        // finds it ungranted returns in silence, which is the correct
        // behavior and useless as a test of the rest of the path.
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            context.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        PackStore(context).install("translation-saheeh-en")
        LanguagePreference(context).set("en")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage("en")
                setTranslationPacks(setOf("translation-saheeh-en"))
            }
        }
        DailyAyahScheduler.createChannel(context)
        clearShade()
    }

    @Test
    fun theAlarmBroadcastPutsTheAyahInTheShade() {
        val channel = context.getSystemService(android.app.NotificationManager::class.java)
            .getNotificationChannel(DailyAyahScheduler.CHANNEL_ID)
        assertTrue("the reminder speaks through a channel of its own", channel != null)

        context.sendBroadcast(
            Intent(DailyAyahScheduler.ACTION_SHOW)
                .setClass(context, DailyAyahScheduler.Receiver::class.java),
        )

        val record = awaitRecord() ?: throw AssertionError(
            "the reminder never reached the shade. The phone is holding:\n" + shadeDump()
        )
        assertTrue(
            "and it is this app's own record, on the reminder's own channel",
            record.contains("pkg=" + context.packageName),
        )
    }

    /**
     * The phone's own line for a notification it is holding, or null while
     * there is none. Only this app's own records are counted, so another app's
     * notification in the shade cannot pass for the reminder.
     */
    private fun awaitRecord(): String? {
        val deadline = System.currentTimeMillis() + 120_000L
        while (System.currentTimeMillis() < deadline) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            val records = shadeDump()
                .lineSequence()
                .filter { it.trimStart().startsWith("NotificationRecord(") }
                .filter { it.contains("pkg=" + context.packageName) }
                .toList()
            if (records.isNotEmpty()) return records.joinToString("\n")
            Thread.sleep(500)
        }
        return null
    }

    private fun clearShade() {
        runCatching {
            context.getSystemService(android.app.NotificationManager::class.java)
                .cancel(DailyAyahScheduler.NOTIFICATION_ID)
        }
    }

    @Test
    fun theArmedAlarmIsTheOneThisPhoneOffers() {
        // The moment is half an hour out, so the alarm is one the platform is
        // holding rather than one it is about to deliver, and the reader's
        // own minute is not the moment this test happens to run in.
        val alarm = context.getSystemService(android.app.AlarmManager::class.java)
        val exact = DailyAyahScheduler.canScheduleExact(alarm)
        val minute = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 +
            Calendar.getInstance().get(Calendar.MINUTE) + 30) % (24 * 60)
        DailyAyahScheduler.apply(context, enabled = true, minuteOfDay = minute)
        try {
            val record = awaitAlarmRecord() ?: throw AssertionError(
                "the reminder armed no alarm. The phone is holding:\n" + alarmDump()
            )
            if (exact) {
                assertTrue(
                    "a phone that grants the exact time must be given an exact alarm, " +
                        "so it arrives on a locked, idle phone (owner decision): $record",
                    record.contains("window=0"),
                )
            } else {
                // What the inexact path may not be is a promise of the minute,
                // and what it must be is an alarm at all: the platform's own
                // window is its own number, not this app's.
                assertTrue(
                    "a phone that refuses must still be holding an alarm for the " +
                        "reminder, and it must not be pretending to be exact: $record",
                    !record.contains("window=0") || !record.contains("exactAllowReason"),
                )
            }
        } finally {
            DailyAyahScheduler.apply(context, enabled = false, minuteOfDay = minute)
        }
    }

    /**
     * The phone's own line for our alarm, or null while there is none. The
     * tag is the alarm's own action, so nothing else in the phone's list can
     * pass for it.
     */
    private fun awaitAlarmRecord(): String? {
        val deadline = System.currentTimeMillis() + 30_000L
        var found: String? = null
        while (System.currentTimeMillis() < deadline) {
            found = alarmDump()
                .lineSequence()
                .filter { it.contains(ALARM_TAG) }
                .map { line ->
                    // The window and the reason sit on the lines under the
                    // record, so the record is read with its two neighbours.
                    alarmDump().lineSequence().dropWhile { it != line }.take(4).joinToString(" ")
                }
                .firstOrNull()
            if (found != null) return found
            Thread.sleep(500)
        }
        return found
    }

    private fun alarmDump(): String = shell("dumpsys alarm")

    private fun shadeDump(): String = shell("dumpsys notification --noredact")

    private fun shell(command: String): String = runCatching {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
            .use { stream ->
                FileInputStream(stream.fileDescriptor).bufferedReader().use { it.readText() }
            }
    }.getOrDefault("")

    private companion object {
        /** The alarm's own tag, which is the action its pending intent names. */
        const val ALARM_TAG = "io.github.muntasimulhaque.quran.action.DAILY_AYAH"
    }
}
