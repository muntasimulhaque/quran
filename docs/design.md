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

One sheet, grouped, nothing hidden:

* **Appearance**: Paper, Sepia, Night, Black, shown as swatches; text size
  for study mode; the reading page follows the theme too.
* **Reading**: follow the reciter, keep the screen awake, and an optional
  switch that shows footnotes under each ayah instead of behind their
  markers.
* **Recitation**: the reciter, and the downloaded surahs, with sizes.
* **Content**: enabled translation and tafsir packs, each with its own
  toggle, and a door to more packs when any exist.
* **About**: version, "no ads, no trackers", privacy policy, source code.

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
* The study text size follows the reader's setting; the Mushaf page never
  scales, because its lines are justified to the page, not to the screen.
* Line height is generous: 1.6x for Arabic, 1.55x for Latin reading text.
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
lapis accent is the only interactive color. Body text meets or exceeds
4.5:1 contrast on its ground in all four themes; large display text meets
3:1.

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
  alone without a content description.
* The Mushaf page is a picture of text, so it carries a semantics layer:
  TalkBack reads the page as its ayah text, and each ayah has its own node
  with its reference, so a screen reader can read, select, and act on it.
* The study list is real text: it scales with the system font setting inside
  a sane range, and every ayah is one accessibility node with a clear label.
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
done for a page the reader cannot see.

## 13. Content packs

The app ships one complete, verified Quran: the QPC V2 page script, KFGQPC
Hafs, Saheeh International, Ibn Kathir, As-Sa'di, Minshawi, and Husary. That
is the default library, and it is offline and complete.

Everything beyond it is a pack. A pack is a signed file with a manifest and
a payload, keyed by SHA-256, downloaded only on the reader's word, from the
project's own releases, and removable without touching the built-in
library. Translations, tafsirs, scripts, and recitations all use the same
shape, so the code learns the pack model once.

The seams exist from today, even with one pack of each kind:

* `pack` tables and pack columns in the content database, so a second
  translation is additive, never a schema change.
* A pack registry in the app, listing built-in and installed packs and the
  reader's choices.
* Search reads the enabled packs, so a new pack is searchable the day it is
  installed.

## 14. What this design removed

* The always-visible top bar with three text buttons.
* The bottom pill that recited page, juz, and hizb numbers at all times.
* The page-locked study view that cut ayahs in half at page boundaries.
* The row of eight action pills in the ayah card.
* Footnote number chips and boxed highlight chrome.
* Any spinner, any empty screen without a next step.
