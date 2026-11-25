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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import io.github.muntasimulhaque.quran.daily.DailyAyahScheduler
import io.github.muntasimulhaque.quran.daily.rememberExactGrantWithheld
import io.github.muntasimulhaque.quran.daily.rememberNotificationsBlocked
import io.github.muntasimulhaque.quran.daily.reminderChannelHidden
import androidx.lifecycle.lifecycleScope
import io.github.muntasimulhaque.quran.core.DailyAyah
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.ThemePreference
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.data.chosenLanguageTag
import io.github.muntasimulhaque.quran.data.isDark
import io.github.muntasimulhaque.quran.data.resolved
import io.github.muntasimulhaque.quran.ui.PermissionNotice
import io.github.muntasimulhaque.quran.ui.QuranApp
import io.github.muntasimulhaque.quran.ui.applicationLocaleTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    /**
     * The one thing the phone withheld, waiting for the reading to say it.
     * A permission answer can arrive from Settings before the reading is
     * composed, so the notice is held here until the reading's own Snackbar
     * host can draw it (owner decision, 15.5).
     */
    private var permissionNotice by mutableStateOf<PermissionNotice?>(null)

    /**
     * The two screens this phone answered with nothing, kept for the window's
     * life. Neither is a moment but a property of the phone, so it is said as
     * state where the switch that needs it is drawn, and once more on the
     * reading's own paper at the door that was tapped. It used to be a Toast,
     * which is the one thing on the glass a tap cannot put away, and the app
     * says nothing in the platform's bubble (owner decision, 19.3).
     */
    private var notificationScreenMissing by mutableStateOf(false)
    private var alarmScreenMissing by mutableStateOf(false)

    /**
     * The reader's language belongs to every window of the app. A dialog, a
     * sheet, or a popup takes the Activity's own resources rather than a
     * composition's, so the locale is applied here, before anything is
     * created, from the synchronous mirror the language choice writes. A
     * change of language writes that mirror and recreates the Activity.
     */
    /**
     * The language this window speaks, decided in `attachBaseContext` before
     * anything is created. It is handed to the reading so the packs the
     * reader's language reads follow the same answer, including a change made
     * in the phone's own per-app language screen (owner decision, 21.3).
     */
    private var resolvedLanguageTag: String = UiLanguage.English.tag

    /**
     * The reader's language belongs to every window of the app. A dialog, a
     * sheet, or a popup takes the Activity's own resources rather than a
     * composition's, so the locale is applied here, before anything is
     * created, from the synchronous mirror the language choice writes. A
     * change of language writes that mirror and recreates the Activity.
     *
     * There are two doors to that choice since Android 13 (owner decision,
     * 21.3): the app's Settings row and the platform's per-app language row.
     * The precedence between them is `chosenLanguageTag` in `:data`, and it is
     * resolved here, once per window, because every other surface inherits it:
     * the platform's own list first, because it is the door the reader touched
     * last when they used the system screen, and the mirror the Settings row
     * writes when the platform holds none.
     */
    override fun attachBaseContext(newBase: Context) {
        // A fresh install has no tag yet, so the phone's own language seeds
        // the first frame; an unsupported phone takes English, the fallback,
        // and therefore LTR. Without this the first session on an Arabic or
        // Hebrew phone came up as an English interface mirrored for the
        // phone's direction, and only the next launch was LTR (owner
        // decision, 19.7).
        val mirror = LanguagePreference(newBase)
        val phoneTag = runCatching {
            newBase.resources.configuration.locales[0]?.language
        }.getOrNull()
        val tag = chosenLanguageTag(
            applicationLocales = applicationLocaleTags(newBase),
            storedTag = mirror.tag(),
            phoneTag = phoneTag,
        )
        resolvedLanguageTag = tag
        // The mirror follows the door that won, so the next launch reads the
        // same answer even on a phone whose platform screen is where the
        // choice was last made.
        if (mirror.tag() != tag) mirror.set(tag)
        val locale = Locale.forLanguageTag(tag)
        // The platform controls that take a locale of their own (the daily
        // reminder's Material clock is the one that showed Latin digits in a
        // Bangla interface) read Locale.getDefault(), not this Activity's
        // resources, so the reader's one language choice is written there
        // too (owner decision, 8.2).
        Locale.setDefault(locale)
        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    // The notification permission, asked only from the acts that set the
    // reminder: the reminder cannot arrive without it, so it goes first, and
    // its answer leads on to the exact alarm's own screen. It is never asked
    // before the reader has read a page (owner decision, 15.5).
    private val askReminderNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                askExactAlarmIfNeeded()
            } else {
                permissionNotice = PermissionNotice(
                    text = R.string.permission_notifications_denied,
                    action = R.string.permission_notice_settings,
                    kind = PermissionNotice.Kind.Notifications,
                )
            }
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
        // The window opens on the reader's chosen ground and not the phone's:
        // the bars' icons are decided here, from the synchronous theme mirror
        // the first Compose frame already reads, so frame zero is right when
        // the two disagree instead of waiting one effect for the stored ground
        // to land (owner decision, 22.8).
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val dark = ThemePreference(this).theme().resolved(night).isDark()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        // The reminder's channel exists from the first launch, so it is
        // visible in the system's own settings before it ever speaks.
        DailyAyahScheduler.createChannel(this)
        // The alarm is armed here rather than at the moment the switch is
        // flipped, so a reboot, a timezone change, or a Doze deferral is
        // corrected on the next launch. Nothing is scheduled while the
        // switch is off: the setting is read, and no alarm is set. A launch
        // cannot throw away a delivery the platform is still holding, because
        // each day is armed under its own pending intent.
        //
        // The work runs on the window's own lifecycle scope, so a launch one
        // rotation long is cancelled with the window, and the application
        // context is what it carries: a coroutine that outlives the Activity
        // must not hold it (owner decision, 19.7).
        lifecycleScope.launch(Dispatchers.IO) {
            val settings = runCatching { SettingsStore(applicationContext).settings.first() }
                .getOrNull() ?: return@launch
            DailyAyahScheduler.apply(
                applicationContext,
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
                onReminderPermission = ::ensureReminderPermissions,
                permissionNotice = permissionNotice,
                onPermissionNoticeShown = { permissionNotice = null },
                notificationsBlocked = { notificationsBlocked.value },
                exactGrantWithheld = { exactGrantWithheld.value },
                notificationScreenMissing = { notificationScreenMissing },
                alarmScreenMissing = { alarmScreenMissing },
                onOpenNotificationSettings = ::openNotificationSettings,
                onAskExactAlarm = { askExactAlarmIfNeeded() },
                languageTag = resolvedLanguageTag,
            )
        }
    }

    /**
     * The phone's own page for this app's notifications, opened for the reader
     * who was told the reminder cannot arrive. The channel API arrived with
     * Android 8, and the releases before it have no notification settings to
     * name, so those get the app's page in the system settings, which holds
     * the same switch. A phone with no settings app at all is told so where
     * the door was tapped, and the Daily page keeps the sentence beside the
     * switch, rather than being left tapping a word that did nothing (owner
     * decision, 19.3).
     *
     * A reader who turned off this one reminder in the phone's own settings is
     * sent to that reminder's own page rather than to the app's, because the
     * app's page is a list they have already looked at and found on, and the
     * reminder is the one switch in it that is off (owner report, 2.9).
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
                    .setData("package:$packageName".toUri())
        }
        try {
            startActivity(page.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (notFound: ActivityNotFoundException) {
            // The phone has no such screen. The Daily page keeps the sentence
            // beside the switch that needs it, and the reading says it once
            // where a door was tapped from the reading itself.
            notificationScreenMissing = true
            permissionNotice = PermissionNotice(text = R.string.notification_settings_unavailable)
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
     *
     * The Daily page says when the phone still withholds the exact time, so
     * the answer is read where the switch is, not announced over the reading.
     * A phone with no such screen at all keeps the line and loses the door:
     * the fact is a property of the phone, so it is kept as state and not
     * said in a bubble (owner decision, 19.3).
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
            alarmScreenMissing = true
            permissionNotice = PermissionNotice(text = R.string.exact_alarm_settings_unavailable)
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
        val previous = this.intent
        super.onNewIntent(intent)
        // A second launch with the same request is already the window the
        // reader is on. Rebuilding it would only churn the activity and give
        // another app a way to make the reader's window flicker; a new
        // request still replaces the intent and rebuilds the page.
        if (sameRequest(previous, intent)) return
        setIntent(intent)
        recreate()
    }

    /** Whether two launches ask the window for the same ayah or door. */
    private fun sameRequest(previous: Intent?, next: Intent): Boolean =
        previous != null &&
            previous.action == next.action &&
            previous.data == next.data &&
            previous.getStringExtra(EXTRA_OPEN) == next.getStringExtra(EXTRA_OPEN) &&
            previous.getIntExtra(EXTRA_AYAH, 0) == next.getIntExtra(EXTRA_AYAH, 0)

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

