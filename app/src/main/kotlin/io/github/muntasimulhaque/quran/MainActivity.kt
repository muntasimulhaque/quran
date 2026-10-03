package io.github.muntasimulhaque.quran

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import io.github.muntasimulhaque.quran.daily.DailyAyahScheduler
import io.github.muntasimulhaque.quran.daily.rememberNotificationsBlocked
import io.github.muntasimulhaque.quran.daily.reminderChannelHidden
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.ui.QuranApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    /**
     * The reader's language belongs to every window of the app. A dialog, a
     * sheet, or a popup takes the Activity's own resources rather than a
     * composition's, so the locale is applied here, before anything is
     * created, from the synchronous mirror the language choice writes. A
     * change of language writes that mirror and recreates the Activity.
     */
    override fun attachBaseContext(newBase: Context) {
        val locale = LanguagePreference(newBase).tag()
            ?.let { Locale.forLanguageTag(it) }
        if (locale == null) {
            super.attachBaseContext(newBase)
            return
        }
        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    // Asked once, at the one screen before the reading, because the reminder
    // is the only thing this app does with the app closed and a permission
    // asked for at the first recitation leaves the first mornings silent
    // (owner decision). The reader has one notification to answer for and the
    // exact alarm grant is never asked for at all, so this is the whole of
    // what is put in front of them: one system dialog, on the screen they
    // have already answered, and then the reading.
    private val askOnFirstScreen =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { recreate() }

    // The same permission, asked again at the moment a reader starts a
    // recitation: that is when the app needs a notification of its own for the
    // playback controls, and it is the second chance for a reader who did not
    // grant it on the first screen.
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The reminder's channel exists from the first launch, so it is
        // visible in the system's own settings before it ever speaks.
        DailyAyahScheduler.createChannel(this)
        // The alarm is armed here rather than at the moment the switch is
        // flipped, so a reboot, a timezone change, or a Doze deferral is
        // corrected on the next launch. Nothing is scheduled while the
        // switch is off: the setting is read, and no alarm is set. A launch
        // cannot throw away a delivery the platform is still holding, because
        // each day is armed under its own pending intent.
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            val settings = runCatching { SettingsStore(this@MainActivity).settings.first() }
                .getOrNull() ?: return@launch
            DailyAyahScheduler.apply(
                this@MainActivity,
                enabled = settings.dailyAyah,
                minuteOfDay = settings.dailyAyahMinute,
            )
        }
        setContent {
            // Whether the phone will show this app's notifications is the one
            // thing about the daily reminder the app does not own, so it is
            // read from the phone and refreshed every time the app comes back
            // to the foreground: the reader can change it in the system
            // settings and return without a restart.
            val notificationsBlocked = rememberNotificationsBlocked()
            QuranApp(
                initialAyah = intent?.let { incoming ->
                    // No extra, no jump. 0 is the sentinel, and it must never
                    // become ayah 1: a plain launch has to leave the reader
                    // where they were, which is the app's whole promise.
                    incoming.getIntExtra(EXTRA_AYAH, 0).takeIf { it > 0 }
                },
                onPlaybackPermission = ::ensureNotificationPermission,
                afterFirstScreen = ::leaveFirstScreen,
                notificationsBlocked = { notificationsBlocked.value },
                onOpenNotificationSettings = ::openNotificationSettings,
            )
        }
    }

    /**
     * The phone's own page for this app's notifications, opened for the reader
     * who was told the reminder cannot arrive. The channel API arrived with
     * Android 8, and the releases before it have no notification settings to
     * name, so those get the app's page in the system settings, which holds
     * the same switch. A phone with no settings app at all is told so in one
     * sentence rather than left tapping a word that did nothing.
     *
     * A reader who turned off this one reminder in the phone's own settings is
     * sent to that reminder's own page rather than to the app's, because the
     * app's page is a list they have already looked at and found on, and the
     * reminder is the one switch in it that is off (owner report, 37th
     * session).
     */
    private fun openNotificationSettings() {
        val page = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && reminderChannelHidden(this) ->
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, DailyAyahScheduler.CHANNEL_ID)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            else ->
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.parse("package:" + packageName))
        }
        try {
            startActivity(page.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (notFound: ActivityNotFoundException) {
            Toast.makeText(this, R.string.notification_settings_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * The end of the one screen before the reading.
     *
     * The notification permission is asked here and nowhere else on this path:
     * the reminder arrives whether or not the app is open, and a reader who
     * is asked for it at the first recitation has already had every morning
     * before that one silently. It is asked once, it is the only permission
     * this app ever asks for, and the exact alarm grant is not one of them.
     * The Activity is recreated when the answer lands, which is what brings
     * every window up in the language that was just chosen.
     */
    private fun leaveFirstScreen() {
        if (needsNotificationPermission()) {
            askOnFirstScreen.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            recreate()
        }
    }

    private fun ensureNotificationPermission() {
        if (needsNotificationPermission()) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    companion object {
        /**
         * The ayah a reminder's tap opens. An extra on a cold start's own
         * intent, never replayed onto a task that is already up: the tap
         * clears the task so what it asked for is always what is shown.
         */
        const val EXTRA_AYAH = "io.github.muntasimulhaque.quran.extra.AYAH"
    }
}

