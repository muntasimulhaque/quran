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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
 * so the path is walked here end to end: the broadcast goes out exactly as
 * the alarm sends it, and the shade is read back afterwards from the phone's
 * own record of what it is holding.
 *
 * The reader has reported both halves of the promise before. The reminder
 * came minutes after the ten o'clock they had set, because Android 14
 * withholds the exact-alarm grant by default and the batched alarm sat in
 * Doze until the app was opened; the fix is a declaration, `USE_EXACT_ALARM`,
 * which makes the exact alarm an install-time grant on every phone. And an
 * alarm does not survive a reboot, so the second half is the boot receiver,
 * which arms the reminder again after a restart. Both are invisible, a
 * manifest line and a receiver that only runs at a boot, so both are pinned
 * here.
 *
 * The phone's record is read through `dumpsys` rather than through
 * `StatusBarNotification`, which is not part of the public SDK and so cannot
 * be named in a test at all. A receiver has seconds to live and a database
 * read is real work, so the waits are minutes rather than seconds: a slow
 * emulator is not a failing reminder.
 *
 * A windowed alarm was measured once and dropped: the platform has a floor of
 * ten minutes on Android 15, so the app's one-minute ask came back as ten, and
 * the windowed path would have been no better than the inexact alarm it
 * replaced. What is left is the exact alarm, and the armed-alarm test reads
 * the shape the platform is actually holding: an exact alarm and no window at
 * all on every phone by default, or the batched alarm for a reader who has
 * turned the phone's special access off, which the Daily page does not promise
 * the minute for.
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
    fun theArmedAlarmIsTheOneThisPhoneOffers() {
        // The moment is half an hour out, so the alarm is one the platform is
        // holding rather than one it is about to deliver, and the reader's
        // own minute is not the moment this test happens to run in. The
        // exact grant is given first, the way a reader gives it in the
        // phone's own screen, so this walks the path the app actually ships.
        grantExactAlarm()
        val alarm = context.getSystemService(android.app.AlarmManager::class.java)
        val minute = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 +
            Calendar.getInstance().get(Calendar.MINUTE) + 30) % (24 * 60)
        DailyAyahScheduler.apply(context, enabled = true, minuteOfDay = minute)
        try {
            val record = awaitAlarmRecord() ?: throw AssertionError(
                "the reminder armed no alarm. The phone is holding:\n" + alarmDump()
            )
            assertTrue(
                "a phone that grants the exact time must be given an exact alarm, " +
                    "so it arrives on a locked, idle phone (owner decision): $record",
                record.contains("window=0"),
            )
        } finally {
            // The alarm goes before the grant would: taking the grant back while this
            // process is alive makes the system kill it to drop its exact alarms,
            // and a killed process reads as a crash. The grant is left as the
            // reader gave it; the connected run uninstalls the app, which clears
            // it, so no test has to take it back.
            DailyAyahScheduler.apply(context, enabled = false, minuteOfDay = minute)
        }
    }

    /**
     * The declarations the whole fix rests on, read back from the merged
     * manifest. `SCHEDULE_EXACT_ALARM` is what an exact reminder needs, and
     * the phone asks for it once at each of the two acts that set the
     * reminder; `RECEIVE_BOOT_COMPLETED` and the boot receiver are what put
     * the alarm back after a restart. And `USE_EXACT_ALARM` must be absent:
     * Play accepts it for an alarm clock or a calendar, and rejected the 3.9
     * build over it, so it is pinned here as a negative because a future
     * edit that readds it costs a release.
     */
    @Test
    @Suppress("DEPRECATION")
    fun theExactAlarmAndTheBootReceiverAreDeclared() {
        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
            .toSet()
        assertTrue(
            "the exact alarm is the ordinary permission, asked once at the two acts that set the reminder",
            requested.contains("android.permission.SCHEDULE_EXACT_ALARM"),
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
     * The exact alarm is the reader's to give on Android 14 and later, and
     * the phone is asked in its own screen. The grant is read and cleared
     * here through the phone's own app-op, which is what the reader's screen
     * sets, so the whole shape is pinned: with the phone withholding it the
     * app is offered the exact alarm's screen, and with it granted the alarm
     * that goes out is exact.
     */
    @Test
    fun theGrantTurnsTheExactAlarmOn() {
        val minute = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 +
            Calendar.getInstance().get(Calendar.MINUTE) + 50) % (24 * 60)
        // The withheld state is read, not forced: taking the grant back while
        // this process is alive makes the system kill it, so a run that
        // inherited the grant from another test checks the granted half only,
        // and the half it does check is the one the reader meets.
        if (DailyAyahScheduler.exactGrantWithheld(context)) {
            assertNotNull(
                "a phone that withholds the exact time has a screen to ask on",
                DailyAyahScheduler.exactGrantRequest(context),
            )
        }
        grantExactAlarm()
        assertTrue(
            "the reader's answer is the phone's, and the app reads it",
            !DailyAyahScheduler.exactGrantWithheld(context),
        )
        assertNull(
            "and there is nothing left to ask for",
            DailyAyahScheduler.exactGrantRequest(context),
        )
        DailyAyahScheduler.apply(context, enabled = true, minuteOfDay = minute)
        try {
            val record = awaitAlarmRecord() ?: throw AssertionError(
                "the granted reminder armed no alarm. The phone is holding:\n" + alarmDump(),
            )
            assertTrue(
                "an exact alarm, window and all, so it arrives on a locked idle phone: $record",
                record.contains("window=0"),
            )
        } finally {
            DailyAyahScheduler.apply(context, enabled = false, minuteOfDay = minute)
        }
    }

    private fun grantExactAlarm() {
        shell("appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow")
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
                // record, so the record is read with its two neighbours.
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
