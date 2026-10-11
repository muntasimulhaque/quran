package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.graphics.Color

// Paper
val PaperBackground = Color(0xFFF8F5EF)
val PaperSurface = Color(0xFFFFFDF8)
/** The hairline and the tint of a control that floats over the reading. */
val PaperOutline = Color(0xFFE7E1D4)
val PaperInk = Color(0xFF1C1B18)
val PaperMuted = Color(0xFF5F5B53)
val PaperHairline = PaperOutline

// Sepia
val SepiaBackground = Color(0xFFF3E9D7)
val SepiaSurface = Color(0xFFFAF1E2)
val SepiaOutline = Color(0xFFDFD2BA)
val SepiaInk = Color(0xFF3A2F1F)
val SepiaMuted = Color(0xFF61543C)
val SepiaHairline = SepiaOutline
val SepiaGold = Color(0xFF7E5C1C)

/**
 * The night ground, and the family that gives up its blue. Night is the
 * page a reader opens before sleep, so its ground is warm: the blue of a
 * conventional dark theme is the short-wavelength light the evening eye is
 * best without, and the cost of leaving it is a brown-gray that reads as
 * lamplight on old paper rather than as a screen (owner decision, this
 * session). The text, the ink, and the gold were already warm; only the
 * grounds and the quiet voice moved. The family keeps the old ground's
 * lightness step for step, so every contrast pair the theme already held
 * still holds, and the accent stays lapis: one
 * cool thing on the page is the action color, and it is the one thing a
 * reader looks for.
 */
val NightBackground = Color(0xFF14100C)
val NightSurface = Color(0xFF1D1813)
val NightOutline = Color(0xFF332B22)
val NightText = Color(0xFFE8E4DA)
val NightInk = Color(0xFFD9D3C4)
val NightMuted = Color(0xFFB0A693)
val NightHairline = NightOutline
val NightGold = Color(0xFFB08E45)

// The one interactive accent, and the ornament gold that belongs to the
// Reading.
//
// Lapis carries every action; its two tones differ by ground because a lapis
// that reads as ink on paper reads as nothing at all on a night page.
val Lapis = Color(0xFF1B4D7A)
val LapisLight = Color(0xFF7FB2E5)

// Gold is the ornament tone, and it is read:
// juz, the surah names drawn into the page, and the ornamental name at the
// head of the study reading. It therefore meets the same 4.5:1 rule every
// other text tone does, on each ground it is drawn on: 5.0:1 on paper,
// 5.1:1 on sepia, 6.1:1 on night. The older paper and sepia
// gold sat at 3.2:1 and 4.1:1, under the rule, and were darkened without
// changing the hue.
val Gold = Color(0xFF856411)

// The one shadow the app casts, black at ten percent, where the platform's
// own cast from a theme is a nineteen percent spot and a four percent
// ambient. The floating tone is the lift and the shadow only anchors it, so
// a Quran line under a control keeps its contrast and no edge of the screen
// wears a darker cast than another (owner decision, forty-ninth session).
val FloatingShadow = Color(0x1A000000)

// The switch's own two tones that are not already a word's tone: the ground a
// switch's track is filled with while it is off, and the ink its knob wears
// while it is on. The knob of an off switch and the ring around that track are
// the ground's muted voice, which is already a named tone above, and the track
// of an on switch is the accent.
//
// These are Material's `surfaceContainerHighest` and `onPrimary`, lifted out
// of the three schemes and named here, because a control's colors are the
// theme's business and were being taken from two scheme roles that each mean
// something else: the off knob was `outline`, which is the hairline every
// divider in the app wears, and on the night ground it measured 1.03:1
// against its own track, so an off switch had no knob in it at all (owner
// report, forty-sixth session). See `Switch.kt` for the whole control.
val PaperControl = Color(0xFFFFFFFF)
val PaperOnPrimary = Color(0xFFFDFBF6)
val SepiaControl = Color(0xFFFEF9F0)
val SepiaOnPrimary = Color(0xFFFDF8ED)
val NightControl = Color(0xFF2C251D)
val NightOnPrimary = Color(0xFF08243D)
