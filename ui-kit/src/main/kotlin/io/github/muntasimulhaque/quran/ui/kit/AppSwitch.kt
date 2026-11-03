package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.LocalSwitchPalette
import io.github.muntasimulhaque.quran.ui.theme.Radii

/**
 * The app's own switch: one track, one knob, two stops, drawn on the set's own
 * numbers.
 *
 * Material's switch was the last borrowed shape in the interface. It is 52 by
 * 32 with a 24 dp knob and a ring drawn at its own weight, and it stood
 * beside rows that were drawn by hand. This one is 44 by 24 with a 16 knob,
 * inside the same 48 dp target every control owes the finger, so a switch row
 * and a door row are one height and one rhythm.
 *
 * The drawing only. A switch is never a control of its own here: the row it
 * stands in is the control, so the whole row answers a finger and a screen
 * reader hears one stop instead of a name, a switch, and a row. The composable
 * takes the state and the ink and nothing else, which is also why a switch
 * cannot end up with a tap that does something other than its row.
 *
 * The knob moves; it does not bounce. A switch is a state and not an
 * occasion, and a reader who has asked the system to reduce motion gets the
 * new state at once.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalSwitchPalette.current
    val reduced = rememberReducedMotion().value
    val travel = (TrackWidth - Knob - Inset * 2).value
    val spec: AnimationSpec<Float> = if (reduced) snap() else tween(durationMillis = 140)
    val knobAt by animateFloatAsState(
        targetValue = if (checked) travel else 0f,
        animationSpec = spec,
        label = "switch knob",
    )
    // The knob is the reader's answer, so it keeps its tone when the control
    // is out of reach; the track is the promise that the answer draws, and it
    // is what gives way (see Switch.kt).
    val track = when {
        !enabled && checked -> palette.onTrackOutOfReach
        !enabled -> palette.offTrackOutOfReach
        checked -> palette.onTrack
        else -> palette.offTrack
    }
    val knob = if (checked) palette.onKnob else palette.offKnob
    Box(
        modifier
            .size(TrackWidth, TrackHeight)
            .clip(Radii.Pill)
            .background(track)
            // An off switch keeps a ring, because its track is the sheet's
            // own quiet tone and a tone on itself has no edge. The ring is
            // the knob's ink, so the control reads as a knob inside a rim
            // and not as two separate shapes.
            .then(
                if (checked) {
                    Modifier
                } else {
                    Modifier.border(Ring, palette.offKnob, Radii.Pill)
                },
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = Inset + knobAt.dp)
                .size(Knob)
                .clip(CircleShape)
                .background(knob),
        )
    }
}

private val TrackWidth = 44.dp
private val TrackHeight = 24.dp
private val Knob = 16.dp
private val Inset = 4.dp
private val Ring = 2.dp
