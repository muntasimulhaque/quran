package io.github.muntasimulhaque.quran.ui.theme

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
 * against the track it sits in: an off switch was one
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
 * that accent, declared here so that both grounds answer for the whole
 * control in one place. It is the pair that was already there, and it already
 * measured 7.07:1 on Night and up on every ground.
 *
 * One switch in the app can be out of reach: Show translation is written as
 * the reader's answer while the pack that answers it is still arriving, so
 * the control stands there with nothing to draw behind it. Material paints
 * that state from its own disabled roles, a set of alphas over `onSurface`
 * that the theme never chose, and the first version of this file said the app
 * had no disabled switch at all. It has one, so it has a pair, declared with
 * the others. The rule is the control's own: a switch out of reach keeps its
 * knob and gives up its track, because the knob is the state the reader wrote
 * and the track is the promise that it draws. A disabled control owes no
 * contrast rule, so the pair is held to the weaker promise that it still
 * reads as a control: the knob is visible on its track, and the two states
 * are still told apart.
 */
data class SwitchPalette(
    val offTrack: Color,
    val offKnob: Color,
    val onTrack: Color,
    val onKnob: Color,
    val offTrackOutOfReach: Color,
    val onTrackOutOfReach: Color,
    /** The sheet this switch stands on, kept beside it so a test can measure against it too. */
    val ground: Color,
)

/**
 * A tone moved toward the ground it stands on. The only blend the palette
 * does, and it is here rather than at the call site so the out of reach track
 * is a number the file declares and a test can read back.
 */
private fun Color.dimmedToward(ground: Color, keep: Float): Color = Color(
    red = red * keep + ground.red * (1f - keep),
    green = green * keep + ground.green * (1f - keep),
    blue = blue * keep + ground.blue * (1f - keep),
    alpha = 1f,
)

internal val LightSwitch = SwitchPalette(
    offTrack = PaperControl,
    offKnob = PaperMuted,
    onTrack = Lapis,
    onKnob = PaperOnPrimary,
    offTrackOutOfReach = PaperControl.dimmedToward(PaperSurface, 0.8f),
    onTrackOutOfReach = Lapis.dimmedToward(PaperSurface, 0.5f),
    ground = PaperSurface,
)

internal val DarkSwitch = SwitchPalette(
    offTrack = NightControl,
    offKnob = NightMuted,
    onTrack = LapisLight,
    onKnob = NightOnPrimary,
    offTrackOutOfReach = NightControl.dimmedToward(NightSurface, 0.8f),
    onTrackOutOfReach = LapisLight.dimmedToward(NightSurface, 0.5f),
    ground = NightSurface,
)

internal val LocalSwitchPalette = staticCompositionLocalOf { LightSwitch }
