package io.github.muntasimulhaque.quran

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.ui.QuranApp
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
        setContent { QuranApp(onPlaybackPermission = ::ensureNotificationPermission) }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

