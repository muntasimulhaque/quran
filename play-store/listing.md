# Store kit

Version: 6.7 (versionCode 69)

## Listing

Title (21 characters):
Quran: The Noble Book

Short description (under 80 characters):
The Quran, offline: translation, tafsir, search, recitation.

Full description (plain prose; Play strips markdown):

A Quran app built to be read. It opens where you left off and never asks
you to learn its interface first.

The reading gives each ayah its Saheeh International translation with the
original footnotes, word by word meanings, and Ibn Kathir in English,
with the Quran quotations set apart. Bangla
readers get the Taisirul Quran translation, Ibn Kathir in Bangla, and Bangla
word meanings, and the whole interface can be read in English or Bangla.

Search reads Arabic without diacritics and English without accents, so
typing allah finds Allah and isa finds Isa. Save any ayah, add your own
note to it, and find everything again under Browse: one list of the ayahs
you kept, with your note under the ayah it was written on, and a Last read
list that keeps the places you have been reading so you can return to one
you left.

Browse, Search, and Settings are doors at the top of the reading. There
is no bottom bar to learn. More than one translation may be on at once, and
each one draws in its own place under the ayah.

Recitation plays Minshawi or Husary, with the page following the reciter
and the word being recited marked. Audio is not bundled and not streamed:
when you tap Play on a surah, the app tells you its size and downloads
that one surah from the project's own release page. Surahs you have not
asked for are never downloaded. Everything you do download works offline
forever after.

There are no ads, no trackers, no analytics, and no account. The app is
free and its source code is public. It reaches the project's own release
page only for content: a pack or a surah's recitation you ask for, or a
quiet refresh of a pack you already have when a newer version ships, on
Wi-Fi only.

Credits: Quran text by the King Fahd Complex for the Printing of the Holy
Quran, audited against the Tanzil Uthmani reference. Translation by
Saheeh International via QuranEnc. Tafsir Ibn Kathir and the recitations
via the Quranic Universal Library by Tarteel. Full credits and licenses
are in the app and in the repository.

## Classification

- Category: Books & Reference
- Tags: Quran, Islam, Tafsir, Recitation
- Content rating: Everyone
- Contains ads: No
- In-app purchases: No
- Free: Yes, no account, no sign-in

## Data safety

- Data collected: none.
- Data shared: none.
- Data security: no account, no identifiers, nothing stored off the
  device.
- The app declares `INTERNET` for content from the project's own GitHub
  Releases: the reading of the language the reader chose (its translation,
  and its tafsir on an unmetered connection), a word list when word by word
  is turned on, a recitation package the reader asks for after seeing its
  size, and a quiet refresh of an installed pack when the app carries a
  newer version, on an unmetered connection only. Nothing is sent beyond the
  request. No analytics, no crash reporting, no advertising.
- Privacy policy URL:
  https://muntasimulhaque.github.io/quran/privacy.html

## Graphics

The icon and the feature graphic are made; the screenshots are captured by the
screenshot test on a real Android system, one folder per form factor Play asks
for.

- App icon: `icon-512.png`
- Feature graphic: `feature-graphic-1024x500.png`
- Phone screenshots: `screenshots/phone/` (1080 x 1920), eight of them
- 7 inch tablet screenshots: `screenshots/tablet7/` (800 x 1280), eight of them
- 10 inch tablet screenshots: `screenshots/tablet10/` (2560 x 1800), eight of them

The icon and the feature graphic are the app's own mark: an open book on its
rehal, the owner's own reference drawing, redrawn clean for this app as
uniform line art on the reference's own reading: the pages are sail arcs
diving into a spine that ends at the V where the two pages meet, each plank
folds onto the page's outer edge, runs down beside the book, crosses below
it, and its leg and the closing arc meet at one shared tip, so each foot is
one clean tapered face. The book is in the
theme's ink `#1C1B18` on the theme's paper
`#F8F5EF`, the stand in the ornament gold `#856411`. These are the
colors the reader already sees inside the app; the cover's burgundy belongs to
the cover. The launcher carries the mark at five densities and in its adaptive
layers, the background the paper alone and the monochrome layer the book's
silhouette, sturdier than the mark so a themed home screen keeps the shape, and
`icon-512.png` is the same drawing at 512. The reminder's small icon carries
the silhouette with no ground, because the shade reads only its alpha channel
and paints the shape in the notification color. The share card carries the
drawing in its own colors beside the name, on the card's own paper, so the
card's foot and the home screen show one drawing. The feature graphic is the
book's own title page: the paper field, a double gold hairline (a fine line at
the margin, the page's own frame inside it), the mark centered on the axis as
a seal, and beneath it Quran in Literata, a short gold rule, and The Noble
Book in tracked small caps in Inter, the gaps between them held tight so the
caption keeps air above the frame's foot. Every element stands inside the
safe area, well clear of the edges Play may crop or overlay, and the file is
1024 x 500, 24-bit PNG with no alpha.

The set is refreshed for 6.7 from capture run 38446275338: all three legs green with eight frames each, and every frame compared with its artifact by `cmp`. The surah-opening frames on all three legs carry neither the introduction preview nor the one-time long-press note: the opening is the surah's own name and basmalah with the About this surah door, and the door is the whole of what stands there until it is opened (owner decision, this session). The settings frames that show the About row read Version 6.7. Every other changed frame differs only in the emulator's clock, the battery, the soft keyboard's caret, and a pixel of the scrim behind the ayah card, so the set is installed whole.

The workflow runs the capture test on three emulator profiles (phone, 7 inch,
10 inch), caches the AVD per profile so only the first run of each pays for
creating the emulator, waits for the emulated storage to mount before the test
starts, and uploads each set as its own artifact, so the store images always
match the shipped build. Run it from the Actions tab, or let it run when the UI
changes.

What each set shows, in order:

1. The reading with its translation
2. The chrome: Browse, Search, and Settings
3. The surah opening
4. Search with the matched word marked, and the filters under the field
5. The settings hub
6. Browse, Surahs
7. The ayah card, with its tafsir doors
8. The tafsir open on the card

## Release notes (6.7, 395 characters)
Press and hold any ayah to open its actions: Play, Tafsir, Save, Note, and More. A tap now belongs to the page, so it brings or hides the reading controls and puts the actions away. About this surah stays folded behind its door until you open it. Tafsir opens at once: with one tafsir installed it opens with the ayah card, already read and ready. The Browse tabs press as their own shape again.

## Before each release

1. Raise `versionCode` by 1 and `versionName` by 0.1 in
   `app/build.gradle.kts` and update the version line at the top of this
   file, in the same commit that ends the session.
2. Run the owner-machine gates: `./gradlew :tools:run --args="verify"`,
   `audit`, `fonts`, `checkdb`, `search`, and the instrumented tests on an
   emulator.
3. Commit and push. The `build` workflow's `signed-bundle` job signs on every
   push to `main`, checks the content, and leaves the bundle in the run's own
   artifacts. Pull it down into `play-store/aab/`:

   ```bash
   gh run list --workflow=build --limit 1
   gh run download <run-id> -n quran-signed-aab -D play-store/aab
   ```

   The artifact is private and expires after two weeks, so nothing signed is
   ever public and nothing signed lingers.
4. Upload the bundle to the internal track, check the size report, then
   promote. Delete the copy from `play-store/aab/` once Play has it, so a
   stale bundle can never be uploaded twice.

The four secrets signing needs (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`) live in the repository's secrets, each value
from the upload key in the owner's vault. The workflow refuses to publish an
unsigned bundle: if the secrets are missing it fails instead of handing over
something Play would reject.

## Assets in this folder

| File | Size | Use |
|---|---|---|
| `icon-512.png` | 512 x 512 | store icon |
| `feature-graphic-1024x500.png` | 1024 x 500 | feature graphic |
| `screenshots/phone/*.png` | 1080 x 1920 | phone screenshots (8) |
| `screenshots/tablet7/*.png` | 800 x 1280 | 7 inch tablet screenshots (8) |
| `screenshots/tablet10/*.png` | 2560 x 1800 | 10 inch tablet screenshots (8) |



## What the app carries, and what a reader adds

The app ships the Quran text only: about eight
megabytes, a complete offline reading that needs no network and no account. Everything
else is added by the reader, from the project's own GitHub Releases, with the
size shown before a byte moves and a SHA-256 check before it is used:

| Language | Translation | Tafsir | Word by word |
|---|---|---|---|
| English | Saheeh International (2.2 MB) | Ibn Kathir (23 MB) | 4.6 MB |
| Bangla | Taisirul Quran (5.1 MB) | Ibn Kathir (47 MB) | 6.5 MB |

Recitations: Minshawi and Husary, one surah at a time (0.2 to 122 MB each),
plus 1.7 MB of timing data per reciter.

Content updates arrive on their own: when a pack you already have is
superseded, the app replaces it quietly on Wi-Fi, so the tafsir and
translation stay current without a tap.

## Store answers to have ready

* **Is the app free?** Yes, and open source (MIT).
* **Does it show ads?** No, and it has no analytics and no accounts.
* **Why does it need the internet permission?** To download a content pack
  or a surah's recitation the reader asks for, from the project's own
  releases, and to quietly refresh an installed content pack when the app
  carries a newer version, on Wi-Fi only. No other host is ever contacted.
* **Data safety**: no data collected, no data shared, nothing stored off the
  device. Notes and bookmarks stay in the app's private storage.
