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

/**
 * The night ground, and the family that gives up its blue. Night is the
 * page a reader opens before sleep, so its ground is warm: the blue of a
 * conventional dark theme is the short-wavelength light the evening eye is
 * best without, and the cost of leaving it is a brown-gray that reads as
 * lamplight on old paper rather than as a screen (owner decision, this
 * session). The text, the ink, and the gold were already warm; only the
 * grounds and the quiet voice moved. The accent stays lapis: one
 * cool thing on the page is the action color, and it is the one thing a
 * reader looks for.
 *
 * The ground is a step lighter than the first night page (owner decision,
 * this session): the reading ink was measured at 14.9:1 on a near black
 * ground, the pairing that blooms on an OLED panel. The lift and the
 * softer page ink below bring the reading to 12.7:1, still past every
 * contrast rule, with less halation in a dark room. The sheet keeps its
 * own step above the page, as it was.
 */
val NightBackground = Color(0xFF17130E)
val NightSurface = Color(0xFF1D1813)
val NightOutline = Color(0xFF332B22)
val NightText = Color(0xFFE8E4DA)
/** The reading's own ink on the night page: softer than the interface's word. */
val NightInk = Color(0xFFD9D3C4)
/** The small gray words. Lifted in the type audit: the old #B0A693 read
 * 7.9:1 but sat under the perceptual floor (APCA -57.6) for the small
 * gray words every sheet carries; this tone clears -60 on every ground a
 * sheet stands on. */
val NightMuted = Color(0xFFBAB09D)
val NightHairline = NightOutline
val NightGold = Color(0xFFC09A50)

// The one interactive accent, and the ornament gold that belongs to the
// Reading.
//
// Lapis carries every action; its two tones differ by ground because a lapis
// that reads as ink on paper reads as nothing at all on a night page.
val Lapis = Color(0xFF1B4D7A)
val LapisLight = Color(0xFF7FB2E5)

// Gold is the ornament tone, and it is read:
// juz, the surah names drawn into the page, and the ornamental name at the
// head of the reading. It therefore meets the same 4.5:1 rule every
// other text tone does, on each ground it is drawn on: 5.0:1 on paper and
// 7.0:1 on night. The older paper gold sat at 3.2:1, under the rule, and
// was darkened without changing the hue; the night gold was lifted in the
// type audit so the ornament reads as gold rather than olive and clears
// the perceptual floor for large text.
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
// of the two schemes and named here, because a control's colors are the
// theme's business and were being taken from two scheme roles that each mean
// something else: the off knob was `outline`, which is the hairline every
// divider in the app wears, and on the night ground it measured 1.03:1
// against its own track, so an off switch had no knob in it at all (owner
// report, forty-sixth session). See `Switch.kt` for the whole control.
val PaperControl = Color(0xFFFFFFFF)
val PaperOnPrimary = Color(0xFFFDFBF6)
val NightControl = Color(0xFF2C251D)
val NightOnPrimary = Color(0xFF08243D)
