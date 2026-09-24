package io.github.muntasimulhaque.quran.ui.kit

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Whether the reader has asked the system to reduce motion.
 *
 * The design document promises it: when the system asks to reduce motion,
 * page-turn animations become crossfades and scroll animations become jumps.
 * The value is the animator duration scale, which is 0 when animations are
 * off; anything at or above 1 is the reader's normal setting, and a fractional
 * value is a slowed-down animation, which is still motion and is still wanted.
 *
 * It is read on a worker, once, and never on the main thread: a settings read
 * is a blocking provider call, and the app does not block the main thread for
 * anything. The state starts false, so the first frame animates as it always
 * did and a reader who reduces motion sees the change the moment the read
 * lands, which is within the first frame or two of a screen.
 */
@Composable
fun rememberReducedMotion(): State<Boolean> {
    val context = LocalContext.current
    return produceState(initialValue = false, context) {
        value = withContext(Dispatchers.IO) {
            val scale = runCatching {
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f,
                )
            }.getOrDefault(1f)
            scale == 0f
        }
    }
}
