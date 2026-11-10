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
// (#F8F1E3) without their density. The page keeps the light it had, and the
// reading's own ink steps under the interface's word below, so the pair that
// carries an hour of reading is a 95 percent page and a 12:1 ink rather than
// the 14.3:1 a label needs (owner decision, the long read session).
val PaperBackground = Color(0xFFFBF9F4)
val PaperSurface = Color(0xFFFFFFFF)
/** The hairline and the tint of a control that floats over the reading. */
val PaperOutline = Color(0xFFE8E3D9)
val PaperInk = Color(0xFF2A2620)

/**
 * The reading's own ink on the day page, one step under the interface's word,
 * the way [NightInk] is under the night's.
 *
 * The Quran, the translation, the tafsir, and the word meanings are read for
 * an hour at a time, and the interface's near black is the tone a label and a
 * button want. On the night page the separation was made for bloom; on paper
 * it is made for glare, since the same 14.3:1 against a 95 percent ground is
 * the harshest pair the app owns. At 12.1:1 on the page, 12.7:1 on a sheet,
 * and 9.8:1 on the word mark, the page reads as ink rather than as a laser and
 * the rule colors keep every floor they owe (owner decision, the long read
 * session). A label, a value, a button, and a sheet's own word keep
 * [PaperInk].
 */
val PaperReadingInk = Color(0xFF37322A)
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

// Gold is the ornament tone, and it is read: juz, the surah names drawn into
// the page, and the ornamental name at the head of the reading. It is never a
// control's color: every control that answers a tap wears Lapis, including
// the ayah's number (owner decision, the consistency audit). Gold therefore
// meets the same 4.5:1 rule every other text tone does, on each ground it is
// drawn on: 6.0:1 on the day page and 8.4:1 on the night page. The night gold
// is the light honey this page needs: the first #C09A50 measured APCA -50 on
// the night ground, the one ornament tone under the small-text floor the rest
// of the night voice already held (owner decision, the theme and consistency
// session).
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
//
// [PaperControl] is the switch's track, and it is measured against the sheet
// the switch stands on: 4.8 L* and 1.13:1 from [PaperSurface]. It stays the
// switch's own tone. A track is a small groove on the white sheet, not a bar
// over the paper page.
//
// [PaperFloating] is the paper a control floats on over the reading: the
// playback pill, the offer, the pack row, the snackbar, and the menu that
// hangs from one. It is the highest paper on its ground, never a shade of the
// page. The day wears the sheet's own white; the night wears one step above
// its sheet, because the chosen ayah outranks the sheet on a dark page (owner
// decision, this session).
//
// **The day's floating paper is the sheet's white.** The page stands at
// L* 98.0, the sheet at 100.0, and the block a menu is always raised over at
// 92.9, so white is 2.0 L* over the page (1.05:1, dE76 3.3) and 7.1 L* over
// the block (dE76 7.2). That is the step every sheet already stands on, and a
// sheet wears no border: its scrim and its lift are its edge. The pill has no
// scrim, so its lift is that edge. The four tones before this one all went
// down and asked the fill to carry the step: 17.0's [PaperControl] at 2.8 L*
// under the page, 17.2's #E8E2D5 at 8.0, 17.3's #E4E2DD at 8.0 with no chroma,
// and 17.4's #F3ECDD at 4.4 with three times the page's chroma. Each read as a
// slip, a shade, a smudge, or a veneer, because a raised surface is the
// lightest thing near it: the reference frames put a white pill on a white
// page and draw a 1 px #DBDBDB edge, and our white sheet on a cream page is
// already a step. `ContrastTest` now pins the direction instead of the warmth:
// the floating paper is above the page and above the chosen block on both
// grounds.
//
// **The night cannot take the sheet's tone.** Its page is L* 9.4, its sheet
// 13.4, and its chosen block 15.6: the lapis wash lifts a dark page where it
// darkens a light one, so the block sits above the sheet. A floating paper at
// the sheet's tone would sink under the menu it is meant to raise.
// [NightFloating] is the highest paper on that ground: #322E2A at L* 19.2,
// 9.8 L* over the page (1.29:1), 5.8 over the sheet, and dE76 5.9 from the
// block. The shadow is invisible at night, so the fill is the only edge there;
// that is why the night's step is the larger one. The tone before it,
// [NightControl] at #2E2B28, stood dE76 4.4 from the block; the one-step lift
// is the owner's word this session, and the switch's track keeps
// [NightControl] because a track is a groove on the sheet, not a bar over the
// page.
//
// The pairs the day lands in: 1.05:1 from the page, dE76 7.2 from the block,
// and on it the quiet word reads 6.96:1, the reading's ink 12.7:1, and lapis
// 8.79:1. The night's floating paper carries the ink at 10.41:1, the quiet
// word at 7.12:1, and lapis at 7.06:1.
val PaperControl = Color(0xFFF4F1EA)
val PaperFloating = Color(0xFFFFFFFF)
val PaperOnPrimary = Color(0xFFFFFFFF)
val NightControl = Color(0xFF2E2B28)
val NightFloating = Color(0xFF322E2A)
val NightOnPrimary = Color(0xFF08243D)
