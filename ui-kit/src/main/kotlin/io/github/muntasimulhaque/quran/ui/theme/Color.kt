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
// because the day page is only 2.8 L* under [PaperControl], so a 64 dp capsule
// in the track's tone had no edge to read at a glance.
//
// **The day's step is warmth, not shade** (owner decision, this session). The
// day page leaves no lightness to give: it stands at L* 98.0, two points under
// white, and the block a menu is always raised over, an ayah painted at 8
// percent of the accent (`Wash.Selected`), stands at L* 92.9. So the floating
// paper is the page's own paper carried 4.4 L* down and warmed to three times
// its own chroma: L* 93.6, C 8.11, h 91, which is three degrees of hue from the
// page itself. An ivory slip rather than a second, dimmer paper, and it stands
// a touch above the chosen block rather than under it, which is the direction
// a card stands over its ground.
//
// The three tones before it each asked the lightness to do the whole job.
// 17.2's `#E8E2D5` darkened by 8.0 L* and warmed to chroma 7.07 at once, and it
// read as a sepia laid over the page (owner report). 17.3's `#E4E2DD` kept the
// page's own chroma to a hundredth (2.68 against 2.64) at the same 8.0 L* down,
// and it read as a smudge (owner report): over a chosen block it stood 3.0 L*
// and dE76 4.2 away, at chroma 2.68 against that block's 0.71, which is two
// grays at one lightness, so the edge was left to the shadow and the shape
// became a veil. The number this tone is chosen by is chroma, and a ratio
// cannot see it: 1.02 to 1 against the block it is drawn on.
//
// The accent's own porcelain, a pale lapis, was drawn over the page in the same
// session and read as a second material on a surface whose subject is the text
// (owner decision), so the day's floating paper stays paper. What it must hold
// is arithmetic in `ContrastTest`: at least three times the page's chroma at
// the page's hue within thirty degrees, 3.0 to 6.5 L* under the page so the
// step can never become a shade, and dE76 6.0 clear of the chosen block. This
// tone reads 3.07 times, 3 degrees, 4.4 L* and 8.4.
//
// The pairs it lands in: 1.12:1 from the page (dE00 5.1) and 1.18:1 from the
// sheet, and on it the quiet word reads 5.92:1, the reading's ink 10.81:1 and
// lapis 7.47:1. The two stock container slots the scheme fills only so a stock
// Material control cannot paint purple (`#EDE8DD` at `secondaryContainer`,
// `#F2E7CE` at `tertiaryContainer`) now stand dE76 2.6 and 5.7 from it; nothing
// draws either of them, so neither moves.
//
// The night does not follow it. Its page (L* 9.4) and its sheet (L* 13.4) are
// close enough that [NightControl] at L* 17.7 is 8.3 L* above the page, a step
// the dark ground reads without any hue at all, and chroma 2.54 there is the
// room's own lamplight rather than a tint, which is why no night floating paper
// was ever reported.
val PaperControl = Color(0xFFF4F1EA)
val PaperFloating = Color(0xFFF3ECDD)
val PaperOnPrimary = Color(0xFFFFFFFF)
val NightControl = Color(0xFF2E2B28)
val NightOnPrimary = Color(0xFF08243D)
