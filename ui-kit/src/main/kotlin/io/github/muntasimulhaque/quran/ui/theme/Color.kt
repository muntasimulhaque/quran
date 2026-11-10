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
// [PaperFloating] is the tone a control floats on over the reading: the
// playback pill, the offer, the pack row, the snackbar, and the menu that
// hangs from one (owner decision, this session). It needed a tone of its own
// because the day page is only 2.8 L* under [PaperControl], so a 64 dp
// capsule in the track's tone had no edge to read at a glance. [PaperFloating]
// stands at L* 90.0, 1.23:1 from [PaperBackground] and 1.28:1 from the sheet,
// which is the same step the night page already holds with [NightControl]
// (1.23:1).
//
// The step is the whole of it, and the tone keeps the page's own warmth and no
// more of it (owner decision, this session). The first floating paper, the
// 17.2 `#E8E2D5`, reached the step by darkening *and* by tinting, at chroma
// 7.07 where the page carries 2.64, which is 2.7 times the paper's own
// warmth: a flat 64 dp capsule and a 215 dp menu in that tone read as a sepia
// laid over the page rather than as paper one step in shade (owner report).
// The tone it landed on was the hairline's own, since [PaperOutline] is
// 1.009:1 from it, and a hairline is chosen to disappear at one dp and cannot
// serve as a surface. A menu is also never raised over the plain page: an ayah
// whose menu is open paints its own block at 8 percent of the accent
// (`Wash.Selected`), which lands at L* 92.9, so the old tone stood 2.9 L* and
// 1.08:1 under its own ground on the opposite hue, and two chromatic
// neighbours at one lightness is the pair the eye reads as a flicker rather
// than as an edge.
//
// This tone holds the page's hue (94) and the page's own chroma (2.68 against
// 2.64), so the step is one of lightness and not a tint, and it is the same
// 1.23:1 step the night already wears. The pill was pure white by day before
// 17.0, the sheet's own tone, so it read as a colder shape laid on the paper
// rather than as a control; one step under the page lets the tone and the
// shadow together say that it floats, and the sheet stays the only pure white
// on the ground. The night ground needs one warm charcoal for both jobs,
// because its page (L* 9.4) and its sheet (L* 13.4) are close enough that
// [NightControl] at L* 17.7 is a step from both, and chroma 2.54 on that
// ground is the room's own lamplight rather than a tint, which is why the
// night never read as one.
val PaperControl = Color(0xFFF4F1EA)
val PaperFloating = Color(0xFFE4E2DD)
val PaperOnPrimary = Color(0xFFFFFFFF)
val NightControl = Color(0xFF2E2B28)
val NightOnPrimary = Color(0xFF08243D)
