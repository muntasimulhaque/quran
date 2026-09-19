# Design: the page is the interface

This document is the constitution of the app's look, feel, and behavior.
Code follows it; when code and this document disagree, one of them is a bug.

The Book is the message of the Creator to every human being. The app exists
to put that message in front of a reader with nothing between them and the
words. Everything else is a guest.

## 1. Principles

1. **The Quran text is the interface.** On a reading screen there is no
   header, no footer, no badge, no counter, no tab bar. When the reader is
   reading, the page fills the screen.
2. **Chrome is summoned, never resident.** A tap on the page's own paper
   (the margins, the head band, the foot band, the gaps between lines) fades
   the chrome in over the page edges; the chrome steps back on its own.
   Chrome never covers the text it serves: it sits on a soft gradient at the
   very top and bottom.
3. **Two taps to anything.** From the page: tap, then the control. From an
   ayah: tap the ayah, then the action. No feature hides behind three taps,
   long-press-only gestures, or a settings maze.
4. **A feature earns its place.** Every control must answer "what does the
   reader lose without this?" If the answer is "nothing most days", it lives
   in the settings sheet, not on the page.
5. **Nothing is a dead end.** No empty screens, no spinners, no unexplained
   disabled controls. If something cannot be shown, say why in one calm
   sentence and offer the way forward.
6. **Quiet is beautiful.** Hairlines instead of boxes, one accent instead of
   five colors, one animation per action, motion that follows the finger.
7. **The app is a book, not a dashboard.** Pages have edges and numbers.
   Reading has a direction. Nothing blinks for attention.

## 2. The reading surface (Mushaf mode)

* The page is a paper sheet that fills the screen, drawn from the QPC V2
  page font, exactly as the printed Madinah Mushaf lays it out.
* Page furniture belongs to the page, not to the app: the surah names come
  from the layout's own ornamental line, and the foot of the page carries a
  hairline rule with the juz at its left and the page number in a small gold
  medallion at its center. This is what a printed Mushaf has, so the page
  reads as a book.
* One page per screen, turned by a horizontal swipe. Adjacent pages are
  pre-rendered, so a turn is a texture draw, never a render.
* While the reader swipes, the moving page casts a soft shadow on the page
  under it, and a light haptic marks the settle.
* A **tap** anywhere belongs to the reading: it brings the chrome, or puts
  it away. No tap ever opens a panel by surprise.
* A **long press** on an ayah asks about that ayah: it washes the ayah, hums
  once, and shows one row of actions (save, play, copy, share, more).
* While a recitation plays, the current word carries a soft lapis wash and
  the current ayah a fainter one. Nothing else moves.

### The chrome

Revealed by a tap on the paper, faded out after seven seconds of no touch.

* Top edge: the surah name and juz at the left, then three icon buttons at
  the right: **Browse** (surah and juz lists), **Search**, **Settings**.
* Bottom edge: a centered two-state control **Mushaf | Study**, with a
  **Listen** button at its left and a **Saved** button at its right.
* The chrome floats on a vertical gradient of the paper color, so the text
  under it stays legible even at its edge.

## 3. Study mode

One surah at a time, scrolled continuously, ending where the surah ends. The
reader is never slid into the next surah without a word: the end of a surah
says so, and offers the next one. The surah is the unit the Quran itself
gives, and readers read it as one.

* A surah opens with an ornament: the Arabic name in gold Amiri, the simple
  name, and one quiet line of metadata (place of revelation, ayah count).
* Each ayah is Arabic first (KFGQPC Hafs, right aligned), then the
  translation (Literata). Footnote markers are quiet superscripts, and a tap
  on a marker opens that note in its own sheet, so the reading is never
  interrupted by a wall of notes it did not ask for.
* The ayah's reference sits under its translation in small muted type, the
  way a printed study Quran numbers verses.
* Tapping any ayah opens its card. The card is the only place features live.
* While a recitation plays, the playing ayah takes a soft lapis wash, and
  the view follows it only when the reader is not scrolling.

## 4. The ayah card

One sheet, three depths, no tabs to learn:

1. **Reference and actions.** The reference in small type, then three icon
   buttons at the right: save, copy, share.
2. **The ayah.** Arabic in Hafs, then the translation, then footnotes.
3. **Deeper.** Three quiet rows, each a door: *Word by word*, *Ibn Kathir*,
   *As-Sa'di*. Tapping one unfolds it in place; tapping again closes it.
4. **Note.** "Add a note" row; when a note exists it shows as its own
   paragraph in the reader's voice, with an edit affordance.
5. **Listen.** One outlined button at the foot of the card: play from this
   ayah.

## 5. Search

One field, no modes, no filters to set up. A tap on the field puts the
keyboard up; results appear as the reader types, with no visible wait.

Searched, in one pass:

| Source | How |
|---|---|
| Arabic ayah text | normalized column, diacritics ignored |
| Translation | every enabled pack, diacritic-folded, "allah" finds "Allah" |
| Tafsir | every enabled pack, folded, matched passages shown with their range |
| Word by word meanings | folded, so a single word finds its ayahs |
| Surah names | simple and Latin spellings |
| References | "2:255", "2 255", "surah 2" and "baqara 255" |

* Results stay in Mushaf order, capped at 200, with a one line summary of
  where the matches are: "18 in the text, 4 in the translation, 3 in Ibn
  Kathir".
* Matched Arabic words and matched English words are marked in the accent
  color, exactly, without disturbing the rest of the sentence.
* Tapping a result opens the ayah in the reader and, in the study card, its
  context.
* The indexes are built in the background after the first page appears, so
  the first search is as fast as the hundredth. Nothing is built on the
  reader's keystroke.

## 6. Browse

* **Surahs**: number, simple name, Arabic name, place and ayah count.
* **Juz**: the thirty parts, each with its first ayah reference.
* **Saved**: saved ayahs and notes, newest first, each opening at its ayah.

## 7. Settings

A hub, not a scroll: nine rows, each carrying where it stands right now, each
opening a page of its own. Back steps out of a page before it closes the
sheet, and the hub keeps its place while a page is open (D-046).

* **Appearance**: Paper, Sepia, Night, Black, as swatches that are the page
  each one paints. The theme is the whole app: the page, the sheets, the bars.
* **Text**: the Quran text, the translation, the tafsir, and the word by word
  aid, each with its own five steps, above a sample drawn from the reader's
  own ayah so a change is judged on the page it is about to change.
* **Reading**: keep the screen awake, follow the reciter, and show footnotes
  under each ayah instead of behind their markers.
* **Reciters**: one reciter is the reader's, a mark says which, each reciter
  carries its own downloaded surahs with their sizes, and adding a reciter
  selects it.
* **Translations**: one is read at a time, grouped by the language it speaks,
  with the size before a byte moves.
* **Tafsirs**: as many as the reader wants, grouped by language, each opening
  from its own door under the ayah.
* **Word by word**: the switch, and the lists by language. The meaning under
  an Arabic word is only useful in the language the reader is reading in, so
  the list follows the chosen translation rather than asking (D-046).
* **Your saved ayahs**: how many are saved, export to a file, import on
  another phone.
* **About**: the version, the credits and licenses, the corrections and
  rights contact, and a self check that reads every installed pack back and
  names anything damaged.

## 8. Typography

| Voice | Face | Use |
|---|---|---|
| The Book | QPC V2 page fonts | the Mushaf page, untouched by the app |
| The Quran in study | KFGQPC Uthmanic Hafs | ayah text, word by word |
| Reading | Literata | translations, tafsir, notes |
| Interface | Inter | labels, buttons, settings |
| Ornament | Amiri Quran | surah names, page furniture, dividers |

Rules:

* Arabic is right aligned and never letter-spaced or scaled horizontally.
* The study text sizes follow the reader's own four settings; the Mushaf page
  never scales, because its lines are justified to the page and the page is
  the printed page, not a reflowed screen.
* A paragraph takes the line height of the script actually in it: 1.9x for a
  line that carries Arabic, 1.6x for Latin alone, never the Arabic's room
  where there is no Arabic (D-049).
* No more than three sizes of type on one surface: a label, a reading size,
  and a display size.
* Numbers in references are Latin (2:255) so they can be searched and read
  aloud; numbers inside the Mushaf stay as the page prints them.

## 9. Color

Two families, four surfaces:

* **Paper and sepia** (day): warm grounds, near-black ink, lapis accent.
* **Night and black** (night): deep blue-black grounds, warm light ink,
  lighter lapis accent. The Mushaf page itself turns over: the same glyphs
  in warm ink on a dark ground, never a bright page in a dark room.

Gold is the only ornament color and never carries meaning by itself. The
lapis accent is the only interactive color, and its two washes belong to the
theme: a lapis that reads as ink on paper reads as nothing at all on a night
ground, so each theme sets its own, and the wash under a recited word is
stronger than the wash under a chosen ayah. Body text meets or exceeds 4.5:1
contrast on its ground in all four themes; large display text meets 3:1.
Secondary text takes its tone from the theme rather than from an alpha that
happens to look quiet, so the contrast survives every theme and every
surface.

## 10. Motion

* One idea per animation, 150 to 250 ms, standard easing, no bounce for the
  sake of bounce.
* Page turns follow the finger and settle with a shadow and a light haptic.
* Chrome fades and slides 6 dp, nothing more.
* Sheets slide, cards unfold in place with a fade and a 4 dp slide.
* The playing ayah and word change color with a 250 ms crossfade.
* When the system asks to reduce motion, page turns become crossfades and
  scroll animations become jumps.

## 11. Accessibility

* Every control has a 48 dp target and a spoken label; icons never stand
  alone without a content description. The text size steps name their place
  in the scale, and a switch row is one labelled node rather than an
  unlabelled switch beside a label.
* The Mushaf page is a picture of text, so it carries a semantics layer: one
  node per ayah, in page order, each with its reference and its text and an
  action that opens that ayah's row of actions. A screen reader can read the
  page, ayah by ayah, and act on any of them.
* The study list is real text: it scales with the system font setting, and
  every ayah is one accessibility node with a clear label.
* The reading direction is right to left for Arabic runs and left to right
  for the interface; nothing is mirrored by hand.
* Color is never the only signal; the played word is also marked in
  semantics, the selected theme also says "selected".
* Muted text keeps 4.5:1 contrast; hairlines are decorative and carry no
  information.

## 12. Performance budget

| Moment | Budget |
|---|---|
| Cold start to first painted page | under 400 ms on a 2019 mid range phone |
| Page turn | no dropped frames at 60 Hz, page draw only |
| First search keystroke to results | under 150 ms, warm |
| Ayah card open | under 100 ms, data from cache |
| Memory, reading a 604 page Mushaf | under 120 MB, pages cached by theme |

Rules: no disk or database work on the main thread, ever. Every index is
built off the main thread after first paint. Every bitmap render happens on
a worker and lands in a cache keyed by page, width, and theme. No work is
done for a page the reader cannot see. The page the reader left is kept as a
picture on disk and is the first thing painted on the next launch, so a
launch is the Book rather than a wait. The measured numbers, from a minified
release build on a software rendered emulator (the slowest Android this app
will ever run on), are in D-037.

## 13. Content packs

The app ships the Quran text and its page layout and nothing else: the QPC V2
page script and glyph system, the KFGQPC Hafs study font, and the navigation
data. Ten megabytes, complete, offline, and enough to read the Book end to
end. The page fonts ship in the base app rather than an asset pack (D-018).

Everything else is a pack. A pack is a file with a manifest and a payload,
keyed by SHA-256, downloaded only on the reader's word, from the project's
own releases, and removable without touching the built-in library.
Translations, tafsirs, scripts, word lists, and recitations all use the same
shape, so the code learns the pack model once.

The seams exist from today, even with one pack of each kind:

* `pack` tables and pack columns in the content database, so a second
  translation is additive, never a schema change.
* A pack catalog in the app, listing built-in and installed packs, the
  reader's choices, and the license of every dataset a pack is built from.
* Search reads the enabled packs, so a new pack is searchable the day it is
  installed.
* The app can read every installed pack back and compare it with the
  fingerprint the catalog recorded, and it names anything that no longer
  matches.

## 14. What this design removed

* The always-visible top bar with three text buttons.
* The bottom pill that recited page, juz, and hizb numbers at all times.
* The page-locked study view that cut ayahs in half at page boundaries.
* The row of eight action pills in the ayah card.
* Footnote number chips and boxed highlight chrome.
* Any spinner, any empty screen without a next step.
