package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.FloatingShadow

/**
 * The one lift a control wears when it floats over the reading: the playback
 * capsule, the offer bar, the popup they open, the ayah's own actions, the
 * pack's progress, and the long-press hint.
 *
 * The shadow is chosen here rather than taken from the platform, and it is
 * deliberately a whisper. The platform draws its own from a light model: a
 * nineteen percent spot and a four percent ambient whose angle changes with
 * the control's place on the page, so the same pill reads heavier at one
 * edge of the screen than the other and heavier below than above. This one
 * is a plain blur with the light straight overhead, the same on every
 * Android version the app runs on, so the lift is one look and not a
 * device's.
 *
 * It is also deliberately shallow. The floating tone is what says the
 * control stands above the page; the shadow only anchors it, so the Quran
 * line under the pill keeps its contrast and the page keeps its quiet. Ten
 * dp of blur with two dp of drop is the softest edge that still reads as a
 * lift at ten percent black, about a third of the black the platform's own
 * edge carried (owner decision, forty-ninth session).
 *
 * The blur stays inside the 8 dp a popup's own window sets aside for a
 * child's shadow (Compose's `PopupLayout.maxSupportedElevation`), so a menu
 * and the capsule that opened it wear the one silhouette.
 */
fun Modifier.floatingLift(shape: Shape): Modifier = dropShadow(
    shape = shape,
    shadow = Shadow(
        radius = 10.dp,
        spread = 0.dp,
        offset = DpOffset(x = 0.dp, y = 2.dp),
        color = FloatingShadow,
    ),
)
