package io.github.muntasimulhaque.quran.daily

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Whether the phone will show this app's notifications.
 *
 * The daily reminder has one dependency the app does not own: the reader can
 * turn notifications off for the app in the system settings, or the phone can
 * do it on its own, and no switch inside the app can say "on" honestly about
 * that. The setting is read from the phone rather than remembered from the
 * last permission dialog, because the app is not what changes it, and it is
 * refreshed on every return to the foreground, so a reader who goes to the
 * system settings and comes back is not told a thing that is no longer true.
 *
 * The answer is handed back as a [State] rather than a value, so a sheet can
 * read it where it is drawn and be the only thing that recomposes when the
 * reader comes back from the phone's own page.
 */
@Composable
fun rememberNotificationsBlocked(): State<Boolean> {
    val context = LocalContext.current
    val blocked = remember { mutableStateOf(notificationsBlocked(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) blocked.value = notificationsBlocked(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return blocked
}

private fun notificationsBlocked(context: Context): Boolean =
    !NotificationManagerCompat.from(context).areNotificationsEnabled()
