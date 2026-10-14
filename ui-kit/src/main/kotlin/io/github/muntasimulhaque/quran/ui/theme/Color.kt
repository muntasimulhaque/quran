package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.graphics.Color

// Paper
val PaperBackground = Color(0xFFF1E9DA)
val PaperSurface = Color(0xFFFAF4E9)
/** The hairline and the tint of a control that floats over the reading. */
val PaperOutline = Color(0xFFE3D8C4)
val PaperInk = Color(0xFF2A241B)
val PaperMuted = Color(0xFF6B6355)
val PaperHairline = PaperOutline

/**
 * The night ground, and the family that gives up its blue. Night is the
 * page a reader opens before sleep, so its ground is warm: the blue of a
 * conventional dark theme is the short-wavelength light the evening eye is
 * best without, and the cost of leaving it is a warm charcoal that reads as
 * lamplight on old paper rather than as a screen (owner decision). The
 * text, the ink, and the gold were already warm; the grounds and the quiet
 * voice are a warm charcoal now, less brown than the first night page, so
 * the night stands beside the parchment day as the same room dimmed rather
 * than as a different app (owner decision, this session).
 *
 * The ground is a step lighter than the first night page (owner decision):
 * the reading ink was measured at 14.9:1 on a near black ground, the
 * pairing that blooms on an OLED panel. The lift and the softer page ink
 * below bring the reading to about 11.8:1, still past every contrast rule,
 * with less halation in a dark room. The sheet keeps its own step above the
 * page, as it was.
 */
val NightBackground = Color(0xFF1B1917)
val NightSurface = Color(0xFF211F1D)
val NightOutline = Color(0xFF35322E)
val NightText = Color(0xFFE6E4DF)
/** The reading's own ink on the night page: softer than the interface's word. */
val NightInk = Color(0xFFD6D4CE)
/** The small gray words. Lifted in the type audit, and lifted again with
 * the warm charcoal ground: the first tone sat under the perceptual floor
 * (APCA -57.6) for the small gray words every sheet carries, and this one
 * clears -60 on every ground a sheet stands on. */
val NightMuted = Color(0xFFBCB9B2)
val NightHairline = NightOutline
val NightGold = Color(0xFFC09A50)

// The one interactive accent, and the ornament gold that belongs to the
// Reading.
//
// Lapis carries every action; its two tones differ by ground because a lapis
// that reads as ink on paper reads as nothing at all on a night page.
val Lapis = Color(0xFF1B4D7A)
// The night accent is lifted so a small action label clears the same -60
// APCA floor the quiet voice holds (owner decision, this session): the old
// #7FB2E5 measured -57.5 on the night ground, a hair under the floor the
// app set for its own small words.
val LapisLight = Color(0xFF8FBCE9)

// Gold is the ornament tone, and it is read:
// juz, the surah names drawn into the page, and the ornamental name at the
// head of the reading. It therefore meets the same 4.5:1 rule every
// other text tone does, on each ground it is drawn on: 5.3:1 on paper and
// 6.7:1 on night. The older paper gold sat at 4.55:1 on the parchment,
// under the margin the app keeps, and was darkened without changing the
// hue (owner decision, this session).
val Gold = Color(0xFF7B5A10)

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
// of the two schemes and named here, because a control's colors are the
// theme's business and were being taken from two scheme roles that each mean
// something else: the off knob was `outline`, which is the hairline every
// divider in the app wears, and on the night ground it measured 1.03:1
// against its own track, so an off switch had no knob in it at all (owner
// report, forty-sixth session). See `Switch.kt` for the whole control.
val PaperControl = Color(0xFFFFFCF6)
val PaperOnPrimary = Color(0xFFFDFBF6)
val NightControl = Color(0xFF2B2927)
val NightOnPrimary = Color(0xFF08243D)
