package io.github.muntasimulhaque.quran.daily

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * The reminder's re-arm after the things that take an alarm away: the phone
 * booting, the app updating, and the reader changing the phone's clock or
 * time zone. An alarm is a one-shot and it does not survive a reboot, so
 * without this a restart could cost a morning (owner report).
 *
 * The receiver holds its process with `goAsync` until
 * [DailyAyahScheduler.rearm] has armed the alarm, and it never throws: it
 * runs at a boot, where there is no one to show a crash to, and the next
 * launch of the app arms the alarm again anyway. A failure inside is a
 * skipped re-arm, never a crash the system records against the app.
 *
 * It is not exported. Every broadcast it answers is a protected system
 * broadcast, and the system is the only sender there can be.
 */
class DailyAyahBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in ACTIONS) return
        val pending = goAsync()
        DailyAyahScheduler.rearm(context) { pending.finish() }
    }

    private companion object {
        /**
         * `TIME_SET` is the platform's own name for a changed clock: the
         * constant is `Intent.ACTION_TIME_CHANGED`.
         */
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
