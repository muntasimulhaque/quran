# Store kit

Version: 15.5 (versionCode 157)

## Listing

Title (21 characters):
Quran: The Noble Book

Short description (under 80 characters):
The Quran, offline: translation, tafsir, search, recitation.

Full description (plain prose; Play strips markdown):

A Quran app built to be read. It opens where you left off and never asks
you to learn its interface first. The English and Bangla translations ship
with the app, so the first page is readable offline, with no account and no
waiting.

The Arabic is the QPC Hafs print, word by word, with the rules of recitation
drawn in the printed mushaf's seven color families and a Tajweed page that
explains every color in English and Bangla. The reading gives each ayah its
Saheeh International translation with the original footnotes; when word
meanings are turned on, a tap on any word opens its meaning under the verse;
and the ayah card opens Ibn Kathir in English, with the Quran quotations set
apart. Bangla readers
get the Al-Bayan Simple Translation, Ibn Kathir in Bangla, Bangla word
meanings, and the surah names in Bengali script, and the whole interface can
be read in English or Bangla.

Search reads Arabic without diacritics, in the letters a reader types
even where the print writes its small waw, small yeh, or dotless alef-maksura
base, and past the page signs the print glues to a word. English is read
without accents, so typing allah finds Allah and isa
finds Isa. Save any ayah, add your own
note to it, and find everything again under Browse: one list of the ayahs
you kept, with your note under the ayah it was written on, and a Last read
list that keeps the places you have been reading so you can return to one
you left.

Browse, Search, Aa, and Settings are one tap away in the reading's chrome,
and a quiet folio line at the foot always says the surah, the juz, and the
ayah. There is no bottom tab bar to learn. More than one translation may be on
at once, and each one draws in its own place under the ayah.

Recitation plays Minshawi or Husary, with the page following the reciter
and the word being recited marked. Audio is not bundled and not streamed:
when you tap Play on a surah, the app tells you its size and downloads
that one surah from the project's own release page. If you turn on Continue
to the next surah in the player's listening menu, the following surahs
download as the recitation reaches them, each with its own name and size on
the player. Everything you download works offline forever after.

There are no ads, no trackers, no analytics, and no account. The app is
free and its source code is public. It reaches the project's own release
page only for content: a pack or a surah's recitation you ask for, or a
quiet refresh of a pack you already have when a newer version ships, on
Wi-Fi only.

Credits: Quran text by the King Fahd Complex for the Printing of the Holy
Quran, audited against the QUL QPC text, with Tanzil as the independent
cross-reference. Translation by Saheeh International via QuranEnc. Tafsir
Ibn Kathir and the recitations via the Quranic Universal Library by Tarteel.
The app's About page names each source and records each dataset's license
from the content manifest. The source code is MIT.

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
  Releases: the tafsir of the language the reader chose, on an unmetered
  connection; a word list when word by word is turned on; a recitation
  package the reader asks for after seeing its size (or the next surah after
  Continue is turned on); and a quiet refresh of an installed pack when the
  app carries a newer version. The refresh runs on an unmetered connection
  only. Nothing is sent beyond the request. No analytics, no crash
  reporting, no advertising.
- Privacy policy URL:
  https://muntasimulhaque.github.io/quran/privacy.html

## Graphics

The icon and the feature graphic are made; the screenshots are captured by the
screenshot test on a real Android system, one folder per form factor Play asks
for.

- App icon: `icon-512.png`
- Feature graphic: `feature-graphic-1024x500.png`
- Phone screenshots: `screenshots/phone/` (1080 x 1920), eight of them
- 7 inch tablet screenshots: `screenshots/tablet7/` (1200 x 1920), eight of them
- 10 inch tablet screenshots: `screenshots/tablet10/` (2560 x 1800), eight of them

Play's current rule for preview assets is a JPEG or 24-bit PNG, 320 to 3840
pixels on each side, and a long side no more than twice the short side. Both
tablet sets stand inside it: the 7 inch set is 1200 x 1920, the 16:10 shape
turned upright, and the 10 inch set is 2560 x 1800, 1.42:1. The 16:9 and 9:16
shapes are Play's recommendation for large-screen placements, not a
submission requirement, so the tablet sets keep the shapes their real screens
give them.

The icon and the feature graphic are the app's own mark: an open book on its
rehal, the owner's own reference drawing, redrawn clean for this app as
uniform line art on the reference's own reading: the pages are sail arcs
diving into a spine that ends at the V where the two pages meet, each plank
folds onto the page's outer edge, runs down beside the book, crosses below
it, and its leg and the closing arc meet at one shared tip, so each foot is
one clean tapered face. The whole drawing is in the theme's ink `#2A2620` on
the theme's paper `#FBF9F4`, one line weight on every icon surface, the
color the reader already sees inside the app; the cover's burgundy belongs
to the cover and the ornament gold stays the feature graphic's frame and
rule. The launcher
carries the mark at five densities and in its adaptive
layers, the background the paper alone and the monochrome layer the book's
silhouette, sturdier than the mark so a themed home screen keeps the shape, and
`icon-512.png` is the same drawing at 512. The reminder's small icon carries
the silhouette with no ground, because the shade reads only its alpha channel
and paints the shape in the notification color. The share card carries the
drawing in the card's own ink beside the name, on the card's own paper, so the
card's foot and the home screen show one drawing. The feature graphic is the
book's own title page: the paper field, one gold hairline frame, the mark
centered on the axis as a seal, in the icon's own ink, and beneath it Quran in
Literata, a short gold rule, and The Noble Book in tracked small caps in
Inter, the gaps between them held tight so the caption keeps air above the
frame's foot. Every element stands inside the
safe area, well clear of the edges Play may crop or overlay, and the file is
1024 x 500, 24-bit PNG with no alpha.

The set is from capture run 41690301971 on 15.2: all three legs green on the
first pass, eight frames each, every frame read. The eight are a reader's path
through the app rather than the app's own map, and they are the first set to
show the card the app hands out and the page that explains the colors on the
Quran's own line. No frame carries the version line any more, because the
settings hub is not one of the eight.

The workflow runs the capture test on three emulator profiles (phone, 7 inch,
10 inch), caches the AVD per profile so only the first run of each pays for
creating the emulator, waits for the emulated storage to mount before the test
starts, and uploads each set as its own artifact, so the store images always
match the shipped build. Run it from the Actions tab, or let it run when the UI
changes.

What each set shows, in order:

1. The reading, with its translation, its tajweed colors, and the folio line that names the surah and juz
2. The ayah's own actions, opened from the number at its head
3. The share card the app hands out
4. The tafsir open on its passage, with the source line above it
5. Search reading a reference ("kahf 10"), with the places it reads behind the
   "Search in" door
6. The Tajweed page: seven color families, each with its word and its rules
7. The reading at night
8. The recitation's own door: the pill's offer, naming the surah, the reciter,
   and the size before a tap

The last frame is last on purpose. The offer stays on the reading until the
reader answers it (one answer and no close), so every frame that wants a clean
page is taken before it.

## Release notes (15.5, 407 characters)
15.5 opens on the Quran, in your phone's language, with both translations already on the device. The reading keeps a quiet line with your surah and juz, Aa opens the type page, a tap on a word shows its meaning, and a tafsir you do not have is offered where you ask for it. Search shows the verse for a reference result. The audio offer has a clear Not now. Text, translations, tafsirs, and packs unchanged.

## Release notes (15.4, 375 characters)
15.4 makes the Font size sample a type specimen. The page shows only the ayah and its translation, without the translation's name above it, and it shows the translation even when the reading's Show translation switch is off, because the sample is there to judge the text size. The reading still names each translation column. Text, translations, tafsirs, and packs unchanged.

## Release notes (15.3, 402 characters)
15.3 puts your language first. With two translations on, the reading, the daily reminder, and the share card lead with the one in your language, and a column in the other language always says which pack it is. The Font size sample shows that same line and its name, and follows Show translation. The Translations page lists your language's packs first. Text, translations, tafsirs, and packs unchanged.

## Release notes (15.2, 431 characters)
15.2 makes Settings one list. Every row says where a setting stands and opens the page that changes it: the switches moved to the pages they govern, so no row is half a switch and half a door, and the switch is drawn in the app's own hand. Word meanings gain the page they were missing. A failed download, a failed save, and a full phone each say one sentence now instead of three. Text, translations, tafsirs, and packs unchanged.

## Release notes (15.1, 431 characters)
15.1 quiets the search sheet and the tafsir card. Recent searches lose Clear all: every word keeps its own visible X, and the caption sits with its chips. The chips now grow with the system text size instead of clipping, and the filter chips keep their row gap at large text. A tafsir passage now ends on the card's block break, so the next language no longer reads as its caption. Text, translations, tafsirs, and packs unchanged.

## Before each release

1. Raise `versionCode` by 1 and `versionName` by 0.1 in
   `app/build.gradle.kts` and update the version line at the top of this
   file, in the same commit that ends the session.
2. Run the owner-machine gates: `./gradlew :tools:run --args="verify"`,
   `build`, `checkdb`, `audit`, `search`, `fonts`, and `packs`, then the
   instrumented tests on an emulator. They all run from a clean
   `content/work`, and the content tags are published before the app build
   downloads them.
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
| `screenshots/tablet7/*.png` | 1200 x 1920 | 7 inch tablet screenshots (8) |
| `screenshots/tablet10/*.png` | 2560 x 1800 | 10 inch tablet screenshots (8) |



## What the app carries, and what a reader adds

The app ships the Quran text and both translations: a complete offline
reading that needs no network and no account. The tafsir, the word lists,
and the recitations are added by the reader, from the project's own GitHub
Releases, with the size named before a tap or on the player while a Continue
package downloads, and a SHA-256 check before it is used:

| Language | Translation | Tafsir | Word by word |
|---|---|---|---|
| English | Saheeh International (ships) | Ibn Kathir (29.5 MB) | 6.3 MB |
| Bangla | Al-Bayan Simple Translation (ships) | Ibn Kathir (56.0 MB) | 8.4 MB |

Recitations: Minshawi and Husary, one surah at a time (about 0.2 to 185 MB
each), plus 1.7 MB of timing data per reciter.

Content updates arrive on their own: when a pack you already have is
superseded, the app replaces it quietly on Wi-Fi, so the tafsir and the
word lists stay current without a tap. The Quran text and the translations
update with the app itself.

## Store answers to have ready

* **Is the app free?** Yes, and open source (MIT).
* **Does it show ads?** No, and it has no analytics and no accounts.
* **Why does it need the internet permission?** To download a content pack
  or a surah's recitation the reader asks for, from the project's own
  releases, to quietly refresh an installed content pack when the app
  carries a newer version, and to repair a damaged local pack, on Wi-Fi
  only. The app asks no other host; GitHub's download service may redirect
  the request to its own release storage.
* **Data safety**: no data collected, no data shared, nothing stored off the
  device. Notes and bookmarks stay in the app's private storage.
