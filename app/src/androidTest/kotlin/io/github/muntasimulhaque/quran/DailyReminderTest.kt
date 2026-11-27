package io.github.muntasimulhaque.quran

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.daily.DailyAyahBootReceiver
import io.github.muntasimulhaque.quran.daily.DailyAyahScheduler
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream
import java.util.Calendar

/**
 * The reminder's way out of the app: the alarm fires, the receiver reads the
 * day's ayah, and the shade holds it. Every step of that can be true while
 * the reader sees nothing, and nothing else in the app would show it to them,
 * so the path is walked here end to end.
 *
 * The reminder is the platform's batched alarm, at an hour the reader chose,
 * and the page says the ayah arrives around it (owner decision, 23.8). It
 * needs no special access and no exact permission, so what is pinned here is
 * that the armed alarm is inexact and still wakes a sleeping phone, and that
 * the boot receiver puts it back after a restart. The shade's record is read
 * through 'dumpsys', which is the only public way a test can see it.
 *
 * A receiver has seconds to live and a database read is real work, so the
 * waits are minutes rather than seconds: a slow emulator is not a failing
 * reminder.
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
        //
        // The reminder is turned on here too, and this is its own doing: the
        // receiver reads the reader's settings and a fire whose switch is off
        // stays silent, which is right and means a test that inherits an off
        // switch from an earlier class is testing the switch and not the
        // path.
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
                setDailyAyah(true)
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
    fun theArmedAlarmIsBatchedAndWakesThePhone() {
        // The moment is half an hour out, so the alarm is one the platform is
        // holding rather than one it is about to deliver, and the reader's
        // own minute is not the moment this test happens to run in.
        val minute = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 +
            Calendar.getInstance().get(Calendar.MINUTE) + 30) % (24 * 60)
        DailyAyahScheduler.apply(context, enabled = true, minuteOfDay = minute)
        try {
            val record = awaitAlarmRecord() ?: throw AssertionError(
                "the reminder armed no alarm. The phone is holding:\n" + alarmDump()
            )
            assertTrue(
                "the reminder is the platform's batched alarm, not an exact one: $record",
                !record.contains("window=0"),
            )
            assertTrue(
                "the reminder must wake a sleeping phone, not wait for the next touch: $record",
                record.contains("RTC_WAKEUP"),
            )
        } finally {
            DailyAyahScheduler.apply(context, enabled = false, minuteOfDay = minute)
        }
    }

    /**
     * The declarations the reminder rests on, read back from the merged
     * manifest. The boot receiver puts the alarm back after a restart, and no
     * exact alarm is declared: the daily ayah is an inexact nudge, and Play
     * accepts exact alarms for an alarm clock or a calendar only (owner
     * decision, 23.8).
     */
    @Test
    @Suppress("DEPRECATION")
    fun theBootReceiverIsDeclaredAndNoExactAlarmIs() {
        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
            .toSet()
        assertTrue(
            "the exact alarm must never be declared: the reminder is an inexact nudge",
            !requested.contains("android.permission.SCHEDULE_EXACT_ALARM"),
        )
        assertTrue(
            "USE_EXACT_ALARM must never be declared: Play accepts it for an alarm clock or a " +
                "calendar only, and rejected 3.9 for it (owner decision)",
            !requested.contains("android.permission.USE_EXACT_ALARM"),
        )
        assertEquals(
            "a reboot must not cost a morning",
            PackageManager.PERMISSION_GRANTED,
            context.packageManager.checkPermission(
                "android.permission.RECEIVE_BOOT_COMPLETED",
                context.packageName,
            ),
        )
        runCatching {
            context.packageManager.getReceiverInfo(
                ComponentName(context, DailyAyahBootReceiver::class.java),
                0,
            )
        }.onFailure {
            throw AssertionError("the boot receiver is missing from the merged manifest")
        }
    }

    /**
     * A reboot takes the alarm with it, and the boot receiver is what puts it
     * back. The broadcast itself cannot be sent from a test (the platform
     * takes BOOT_COMPLETED only from the system), so the receiver's own work
     * is called here directly, after clearing every alarm the app holds,
     * which is exactly what a restart leaves: none.
     */
    @Test
    fun theBootReArmPutsTheAlarmBack() {
        val minute = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 +
            Calendar.getInstance().get(Calendar.MINUTE) + 45) % (24 * 60)
        runBlocking {
            SettingsStore(context).apply {
                setDailyAyah(true)
                setDailyAyahTime(minute)
            }
        }
        // What a restart leaves behind: the app holds nothing at all.
        DailyAyahScheduler.apply(context, enabled = false, minuteOfDay = minute)
        assertTrue(
            "the app holds no alarm after the restart stand-in:\n" + alarmDump(),
            awaitNoAlarm(),
        )
        DailyAyahScheduler.rearm(context)
        val record = awaitAlarmRecord() ?: throw AssertionError(
            "the re-arm armed no alarm. The phone is holding:\n" + alarmDump(),
        )
        assertTrue(
            "and the re-armed alarm is the reminder's own: $record",
            record.contains(ALARM_TAG),
        )
        // Leave nothing behind for the next test or the next run.
        DailyAyahScheduler.apply(context, enabled = false, minuteOfDay = minute)
    }

    /** True once no alarm of this app's is held at all. */
    private fun awaitNoAlarm(): Boolean {
        val deadline = System.currentTimeMillis() + 30_000L
        while (System.currentTimeMillis() < deadline) {
            if (heldAlarmRecord() == null) return true
            Thread.sleep(500)
        }
        return false
    }

    /**
     * The phone's own line for our alarm, or null while there is none. The
     * tag is the alarm's own action, so nothing else in the phone's list can
     * pass for it; the search is over the pending alarm records only, because
     * a cancelled alarm leaves its tag behind in the phone's own history and
     * a history line is not a held alarm.
     */
    private fun awaitAlarmRecord(): String? {
        val deadline = System.currentTimeMillis() + 30_000L
        while (System.currentTimeMillis() < deadline) {
            heldAlarmRecord()?.let { return it }
            Thread.sleep(500)
        }
        return null
    }

    /**
     * The pending record the platform is holding right now, or null when the
     * app holds no alarm. A pending record's tag line is the one that starts
     * with `tag=`; the history's is a snapshot line that carries `type=`
     * before the same tag.
     */
    private fun heldAlarmRecord(): String? =
        alarmDump()
            .lineSequence()
            .filter { it.trimStart().startsWith("tag=") && it.contains(ALARM_TAG) }
            .map { line ->
                // The window and the reason sit on the lines under the
                // record, so the record is read with its two neighbors.
                alarmDump().lineSequence().dropWhile { it != line }.take(4).joinToString(" ")
            }
            .firstOrNull()

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
