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

    // Asked the first time the reader starts a recitation, never at launch.
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
        // switch is off: the setting is read, and no alarm is set.
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
     */
    private fun openNotificationSettings() {
        val page = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.parse("package:" + packageName))
        }
        try {
            startActivity(page.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (notFound: ActivityNotFoundException) {
            Toast.makeText(this, R.string.notification_settings_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        /**
         * The ayah a reminder's tap opens. An extra on a cold start's own
         * intent, never replayed onto a task that is already up: the tap
         * clears the task so what it asked for is always what is shown.
         */
        const val EXTRA_AYAH = "io.github.muntasimulhaque.quran.extra.AYAH"
    }
}

