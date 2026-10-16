package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.ui.graphics.Color

// Paper
//
// The day page is a warm white, not a parchment: #FBF9F4 against the sheet's
// pure white. The first paper page (#F1E9DA) was a darker, more saturated
// parchment and every surface carried the tint, so the whole app read as
// sepia rather than as paper. The warmth stays, but it lives in the ink, the
// gold, and the hairlines now, and the page itself sits 95 percent bright, in
// line with Instapaper's sepia (#F8F2E3) and Apple Books' day page
// (#F8F1E3) without their density. The reading ink is a warm near black at
// 13.2:1 and up, soft enough not to glare on a bright ground (owner decision,
// the theme and consistency session).
val PaperBackground = Color(0xFFFBF9F4)
val PaperSurface = Color(0xFFFFFFFF)
/** The hairline and the tint of a control that floats over the reading. */
val PaperOutline = Color(0xFFE8E3D9)
val PaperInk = Color(0xFF2A2620)
val PaperMuted = Color(0xFF5E594F)
val PaperHairline = PaperOutline

/**
 * The night ground, and the family that gives up its blue. Night is the
 * page a reader opens before sleep, so its ground is warm: the blue of a
 * conventional dark theme is the short-wavelength light the evening eye is
 * best without, and the cost of leaving it is a warm charcoal that reads as
 * lamplight on old paper rather than as a screen (owner decision). The
 * text, the ink, and the gold were already warm; the ground is a warm
 * charcoal, less brown than the first night page, so the night stands beside
 * the warm white day as the same room dimmed rather than as a different app.
 *
 * The ground is lifted off pure black (owner decision): the reading ink was
 * measured at 14.9:1 on a near black ground, the pairing that blooms on an
 * OLED panel. This ground and the softer page ink below hold the reading at
 * about 10:1 and up, still past every contrast rule, with less halation in a
 * dark room. The sheet keeps its own step above the page, as it was.
 */
val NightBackground = Color(0xFF1C1A18)
val NightSurface = Color(0xFF242220)
val NightOutline = Color(0xFF37332E)
val NightText = Color(0xFFE5E2DB)
/** The reading's own ink on the night page: softer than the interface's word. */
val NightInk = Color(0xFFDAD7D0)
/** The small gray words. Lifted in the type audit, and lifted again with the
 * warm charcoal ground: the first tone sat under the perceptual floor
 * (APCA -57.6) for the small gray words every sheet carries, and this one
 * clears the -60 floor on every ground a sheet stands on. */
val NightMuted = Color(0xFFC1BCB2)
val NightHairline = NightOutline
val NightGold = Color(0xFFDCB870)

// The one interactive accent, and the ornament gold that belongs to the
// Reading.
//
// Lapis carries every action; its two tones differ by ground because a lapis
// that reads as ink on paper reads as nothing at all on a night page.
val Lapis = Color(0xFF1B4D7A)
// The night accent is lifted so a small action label clears the same -60
// APCA floor the quiet voice holds: it measures -65 on the night page and
// 9.1:1, where a lapis that reads as ink on the day page would read as
// nothing at all here.
val LapisLight = Color(0xFF95C0EA)

// Gold is the ornament tone, and it is read:
// juz, the surah names drawn into the page, and the ornamental name at the
// head of the reading. It therefore meets the same 4.5:1 rule every
// other text tone does, on each ground it is drawn on: 6.0:1 on the day page
// and 8.4:1 on the night page. The night gold is the light honey this page
// needs: the first #C09A50 measured APCA -50 on the night ground, the one
// ornament tone under the small-text floor the rest of the night voice
// already held (owner decision, the theme and consistency session).
val Gold = Color(0xFF7A5A12)

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
val PaperControl = Color(0xFFF4F1EA)
val PaperOnPrimary = Color(0xFFFFFFFF)
val NightControl = Color(0xFF2E2B28)
val NightOnPrimary = Color(0xFF08243D)
