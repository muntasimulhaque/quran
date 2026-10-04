package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * One switch in one ground: the track behind the knob, the knob, and the
 * sheet the whole control stands on.
 *
 * Material's switch takes its unchecked colors from the scheme, and the
 * scheme gives it two tones that each already mean something else: the knob
 * is `outline` and the track is `surfaceContainerHighest`. On a night ground
 * those two are the same paint. The night hairline is `#2A3644` and its
 * container tone is `#263443`, four levels away, so the knob measured 1.03:1
 * against the track it sits in and 1.01:1 on Black: an off switch was one
 * flat lump with no knob in it, and the ring that was supposed to draw its
 * edge was the same lump again (owner report, forty-sixth session). Paper
 * only read because its hairline happens to sit 24 levels off white, which
 * is luck and not a choice, and it still measured 1.30:1, under the 3:1 a
 * control boundary owes.
 *
 * So the switch is declared, per ground, in the app's own tones. The knob and
 * the ring take the ground's muted voice, which is the tone every quiet word
 * in the sheet is already set in, and the track keeps the tone it had. On
 * that pair the knob reads at 5.8:1 to 7.1:1 on the track and 6.6:1 to 7.6:1
 * on the sheet, and [SwitchContrastTest] is what holds it there.
 *
 * Nothing new is invented and no hairline moves: the change is in the switch,
 * not in the palette the rest of the app draws with.
 *
 * The pair a switch is on in is the scheme's own accent and its own ink on
 * that accent, declared here so that all four grounds answer for the whole
 * control in one place. It is the pair that was already there, and it already
 * measured 7.07:1 on Night and up on every ground.
 *
 * There is no off switch without a pair, and there is no disabled pair: no
 * switch in the app is ever disabled, and a color nothing can reach is a
 * color nobody chose.
 */
data class SwitchPalette(
    val offTrack: Color,
    val offKnob: Color,
    val onTrack: Color,
    val onKnob: Color,
    /** The sheet this switch stands on, kept beside it so a test can measure against it too. */
    val ground: Color,
)

internal val PaperSwitch = SwitchPalette(
    offTrack = PaperControl,
    offKnob = PaperMuted,
    onTrack = Lapis,
    onKnob = PaperOnPrimary,
    ground = PaperSurface,
)

internal val SepiaSwitch = SwitchPalette(
    offTrack = SepiaControl,
    offKnob = SepiaMuted,
    onTrack = Lapis,
    onKnob = SepiaOnPrimary,
    ground = SepiaSurface,
)

internal val NightSwitch = SwitchPalette(
    offTrack = NightControl,
    offKnob = NightMuted,
    onTrack = LapisLight,
    onKnob = NightOnPrimary,
    ground = NightSurface,
)

internal val BlackSwitch = SwitchPalette(
    offTrack = BlackControl,
    offKnob = BlackMuted,
    onTrack = LapisLight,
    onKnob = BlackOnPrimary,
    ground = BlackSurface,
)

internal val LocalSwitchPalette = staticCompositionLocalOf { PaperSwitch }

/** The switch as the ground the reading is drawn on says it should look. */
@Composable
fun appSwitchColors(): SwitchColors {
    val palette = LocalSwitchPalette.current
    return SwitchDefaults.colors(
        checkedThumbColor = palette.onKnob,
        checkedTrackColor = palette.onTrack,
        checkedBorderColor = palette.onTrack,
        uncheckedThumbColor = palette.offKnob,
        uncheckedTrackColor = palette.offTrack,
        uncheckedBorderColor = palette.offKnob,
    )
}
