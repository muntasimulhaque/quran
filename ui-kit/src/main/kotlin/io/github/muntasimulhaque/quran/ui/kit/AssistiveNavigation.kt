package io.github.muntasimulhaque.quran.ui.kit

import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether an accessibility service is steering the phone.
 *
 * The reading's top bar steps back after seven seconds of quiet, which is the
 * right rhythm for a page that wants the reader's eyes. It is the wrong
 * rhythm for a reader who reaches the bar through touch exploration or a
 * switch: the controls can leave before they arrive. This value stays true
 * while any accessibility service is enabled, and the top bar then stays as
 * long as it is up. It is read once at composition and kept current by the
 * platform's own two change listeners, so turning TalkBack off restores the
 * quiet step-back without a restart.
 *
 * Any enabled service counts, not only the screen reader: Switch Access and
 * the OEM services that draw their own navigation are all services, and the
 * cost of keeping a quiet bar on the glass for one of them is less than the
 * cost of taking it away from a reader who needs it (owner decision, this
 * session).
 */
@Composable
fun rememberAssistiveNavigation(): State<Boolean> {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(AccessibilityManager::class.java)
    }
    val enabled = remember(manager) {
        mutableStateOf(
            manager?.isEnabled == true || manager?.isTouchExplorationEnabled == true,
        )
    }
    DisposableEffect(manager) {
        if (manager == null) return@DisposableEffect onDispose { }
        val stateListener = AccessibilityManager.AccessibilityStateChangeListener { on ->
            enabled.value = on || manager.isTouchExplorationEnabled
        }
        val touchListener = AccessibilityManager.TouchExplorationStateChangeListener { on ->
            enabled.value = on || manager.isEnabled
        }
        manager.addAccessibilityStateChangeListener(stateListener)
        manager.addTouchExplorationStateChangeListener(touchListener)
        onDispose {
            manager.removeAccessibilityStateChangeListener(stateListener)
            manager.removeTouchExplorationStateChangeListener(touchListener)
        }
    }
    return enabled
}
