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
import io.github.muntasimulhaque.quran.daily.rememberExactGrantWithheld
import io.github.muntasimulhaque.quran.daily.rememberNotificationsBlocked
import io.github.muntasimulhaque.quran.daily.reminderChannelHidden
import io.github.muntasimulhaque.quran.core.DailyAyah
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
        // The platform controls that take a locale of their own (the daily
        // reminder's Material clock is the one that showed Latin digits in a
        // Bangla interface) read Locale.getDefault(), not this Activity's
        // resources, so the reader's one language choice is written there
        // too (owner decision, this session).
        Locale.setDefault(locale)
        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    // Asked once, at the reminder card that ends the one screen before the
    // reading, because the reminder is the only thing this app does with the
    // app closed. The reader has one notification to answer for here, so this
    // is the whole of what is put in front of them: one system dialog, on the
    // card they have already answered, and then the phone's own screen for
    // the exact time when it withholds it, and then the reading. The exact
    // alarm is not asked for at this screen when the notification was
    // refused: an exact alarm with nothing to show is a promise the phone
    // cannot keep.
    private val askOnFirstScreen =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) askExactAlarmIfNeeded()
        }

    // The same permission, asked again at the moment a reader starts a
    // recitation: that is when the app needs a notification of its own for the
    // playback controls, and it is the second chance for a reader who did not
    // grant it on the first screen.
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // The notification permission, asked from the two acts that set the
    // reminder: the reminder cannot arrive without it, so it goes first, and
    // its answer leads on to the exact alarm's own screen.
    private val askReminderNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            askExactAlarmIfNeeded()
        }

    // The exact alarm, asked once in the phone's own screen, at each of those
    // two acts, on the phones that withhold it from Android 14 on (owner
    // decision, after Play rejected USE_EXACT_ALARM). Whatever the answer
    // was, the alarm is armed again from the reader's own settings, so an
    // exact one replaces the batched one within the minute.
    private val askExactAlarm =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            DailyAyahScheduler.rearm(this)
        }

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
            // What this launch was asked for, read once: the reminder's ayah,
            // a daily-ayah shortcut, a search shortcut, or a link to one
            // verse. A plain launch asks for none of them and leaves the
            // reader where they were.
            val launch = intent
            val open = launch?.getStringExtra(EXTRA_OPEN)
            val sharedAyah = launch?.getIntExtra(EXTRA_AYAH, 0)?.takeIf { it > 0 }
            val reference = referenceOf(launch?.data)
            // Whether the phone will show this app's notifications is the one
            // thing about the daily reminder the app does not own, so it is
            // read from the phone and refreshed every time the app comes back
            // to the foreground: the reader can change it in the system
            // settings and return without a restart.
            val notificationsBlocked = rememberNotificationsBlocked()
            // And the second thing the phone can withhold, read the same way:
            // the Daily page says whether the exact time is still being held
            // back, and the answer changes in the phone's own screen, not here.
            val exactGrantWithheld = rememberExactGrantWithheld()
            QuranApp(
                initialAyah = sharedAyah
                    ?: if (open == OPEN_DAILY) {
                        // The shortcut asks for the same verse the reminder
                        // would have shown, dealt from the same deck.
                        DailyAyah.numberFor(System.currentTimeMillis())
                    } else {
                        // No extra, no jump. 0 is the sentinel, and it must
                        // never become ayah 1: a plain launch has to leave the
                        // reader where they were, which is the app's promise.
                        null
                    },
                initialReference = reference,
                openSearch = open == OPEN_SEARCH,
                onPlaybackPermission = ::ensureNotificationPermission,
                onReminderPermission = ::ensureReminderPermissions,
                afterFirstScreen = { recreate() },
                onFirstScreenDone = ::finishFirstScreen,
                notificationsBlocked = { notificationsBlocked.value },
                exactGrantWithheld = { exactGrantWithheld.value },
                onOpenNotificationSettings = ::openNotificationSettings,
                onAskExactAlarm = ::askExactAlarmIfNeeded,
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
     * The end of the reminder card.
     *
     * When the reader turned the daily ayah on, the two grants it needs are
     * asked here: the notification first, and then, if it was granted and the
     * phone withholds it, the exact alarm in the phone's own screen. When the
     * reader said Not now, nothing is asked and the reminder is already off.
     * The locale was brought up by the recreation at the language step, so
     * this is the whole of what follows the card.
     */
    private fun finishFirstScreen(reminderOn: Boolean) {
        if (!reminderOn) return
        if (needsNotificationPermission()) {
            askOnFirstScreen.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            askExactAlarmIfNeeded()
        }
    }

    private fun ensureNotificationPermission() {
        if (needsNotificationPermission()) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * What the two acts that set the reminder ask for: the notification
     * first, since nothing can arrive without it, and then the phone's own
     * screen for the exact time. Neither is asked twice: each is asked only
     * while it is missing, so a reader who has already granted both hears
     * nothing at all.
     */
    private fun ensureReminderPermissions() {
        if (needsNotificationPermission()) {
            askReminderNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            askExactAlarmIfNeeded()
        }
    }

    /**
     * The phone's own screen for the exact alarm, on the phones that are
     * withholding it. A phone with no such screen keeps the batched alarm,
     * and the Daily page still says the exact time is being held back, which
     * is the truth rather than a promise the phone cannot keep.
     */
    private fun askExactAlarmIfNeeded() {
        // Without the notification granted, an exact alarm fires into a shade
        // that shows nothing, so the exact-time screen waits for a reader who
        // will actually hear the reminder.
        if (needsNotificationPermission()) return
        val request = DailyAyahScheduler.exactGrantRequest(this) ?: return
        try {
            askExactAlarm.launch(request)
        } catch (notFound: ActivityNotFoundException) {
            Toast.makeText(this, R.string.exact_alarm_settings_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    /**
     * A second launch while the reading is already up, from a shortcut or a
     * link: the new intent replaces the one the window was built from, and
     * the window is rebuilt so the composition reads it. The view model
     * survives the recreation, so the reader's place and playback are kept.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    /**
     * The verse a `quran://ayah/2:255` link names, or null when the link is
     * not one. Both the colon form and the two-segment form are accepted,
     * because a link typed by hand may take either, and a link that names
     * nothing is ignored rather than guessed at.
     */
    private fun referenceOf(link: android.net.Uri?): Pair<Int, Int>? {
        if (link == null || link.scheme != "quran" || link.host != "ayah") return null
        val segments = link.pathSegments.filter { !it.isNullOrBlank() }
        val pair = when {
            segments.size >= 2 -> segments[0].toIntOrNull() to segments[1].toIntOrNull()
            segments.size == 1 -> segments[0].split(':')
                .takeIf { it.size == 2 }
                ?.let { it[0].toIntOrNull() to it[1].toIntOrNull() }
            else -> null
        } ?: return null
        val surah = pair.first ?: return null
        val ayah = pair.second ?: return null
        if (surah !in 1..114 || ayah < 1) return null
        return surah to ayah
    }

    companion object {
        /**
         * The ayah a reminder's tap opens. An extra on a cold start's own
         * intent, never replayed onto a task that is already up: the tap
         * clears the task so what it asked for is always what is shown.
         */
        const val EXTRA_AYAH = "io.github.muntasimulhaque.quran.extra.AYAH"

        /** What a shortcut asks to be opened, beside the reading it lands on. */
        const val EXTRA_OPEN = "io.github.muntasimulhaque.quran.extra.OPEN"
        const val OPEN_SEARCH = "search"
        const val OPEN_DAILY = "daily"
    }
}

