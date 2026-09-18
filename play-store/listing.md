# Store kit

Version: 0.1 (versionCode 1)

## Listing

Title (21 characters):
Quran: The Noble Book

Short description (under 80 characters):
The Quran, offline: Mushaf pages, translation, tafsir, search, recitation.

Full description (plain prose; Play strips markdown):

A Quran app built to be read. It opens where you left off, turns pages the
way a printed Mushaf does, and never asks you to learn its interface first.

Mushaf mode renders the 604 pages of the Madinah Mushaf exactly as the
King Fahd Complex typeset them, with the QPC V2 page fonts, so every page
matches the printed copy line for line.

Study mode gives each ayah its Saheeh International translation with the
original footnotes, word by word meanings, and two tafsirs: Ibn Kathir in
English and As-Sa'di in Arabic, with the Quran quotations set apart.

Search reads Arabic without diacritics and English without accents, so
typing allah finds Allah and isa finds Isa. Save any ayah, add your own
note to it, and find everything again under Browse.

Recitation plays Minshawi or Husary, with the page following the reciter
and the word being recited marked. Audio is not bundled and not streamed:
when you tap Play on a surah, the app tells you its size and downloads
that one surah from the project's own release page. Surahs you have not
asked for are never downloaded. Everything you do download works offline
forever after.

There are no ads, no trackers, no analytics, and no account. The app is
free and its source code is public. The only connection it ever makes is
the recitation download you ask for.

Credits: Quran text by the King Fahd Complex for the Printing of the Holy
Quran, audited against the Tanzil Uthmani reference. Translation by
Saheeh International via QuranEnc. Tafsir Ibn Kathir and the recitations
via the Quranic Universal Library by Tarteel. Tafsir As-Sa'di via
QuranEnc. Full credits and licenses are in the app and in the repository.

## Classification

- Category: Books & Reference
- Tags: Quran, Islam, Mushaf, Tafsir, Recitation
- Content rating: Everyone
- Contains ads: No
- In-app purchases: No
- Free: Yes, no account, no sign-in

## Data safety

- Data collected: none.
- Data shared: none.
- Data security: no account, no identifiers, nothing stored off the
  device.
- The app declares `INTERNET` for one purpose: downloading a recitation
  package for one surah, from the project's own GitHub Releases, only
  after the reader taps Play and approves the shown size. Nothing is sent
  beyond that request. No analytics, no crash reporting, no advertising.
- Privacy policy URL:
  https://github.com/muntasimulhaque/quran/blob/main/docs/privacy.md

## Graphics needed before publishing

- App icon 512 x 512 PNG. Not made yet; the app currently uses the
  default Android icon. This is the next design task.
- Feature graphic 1024 x 500 PNG. Not made yet.
- Phone screenshots, 8 of them, 1080 x 1920 or larger:
  1. Mushaf page (Al-Fatihah, paper theme)
  2. Study page with translation and footnotes
  3. Study card with the action pills
  4. Ibn Kathir tafsir
  5. As-Sa'di tafsir
  6. Search results with the matched word marked
  7. Browse sheet, Saved tab with a note
  8. Recitation playing with the word marked
- Tablet screenshots: required by Play if the listing targets tablets.
  The layout is responsive Compose; captures still needed.
- Night and Sepia theme captures are a nice extra.

## Release notes (first release)

First release. Mushaf and study modes, Saheeh International with
footnotes, word by word, Ibn Kathir and As-Sa'di, search, bookmarks and
notes, and recitations by Minshawi and Husary downloaded per surah on
request. No ads, no trackers, no account.

## Before each release

1. Raise `versionCode` by 1 and `versionName` by 0.1 in
   `app/build.gradle.kts` and update the version line at the top of this
   file.
2. Run the owner-machine gates: `./gradlew :tools:run --args="verify"`,
   `audit`, `fonts`, and the instrumented tests on an emulator.
3. Build the bundle: `./gradlew :app:bundleRelease` (signed from the
   shared upload keystore when it is present).
4. Upload to the internal track, check the size report, then promote.

## Assets in this folder

| File | Size | Use |
|---|---|---|
| `icon-512.png` | 512 x 512 | store icon |
| `feature-graphic-1024x500.png` | 1024 x 500 | feature graphic |
| `screenshots/*.png` | 1080 x 1920 | phone screenshots, captured by the screenshot test on a real Android system |

Screenshots: the Mushaf page, the summoned chrome, the study reading, an
ayah's actions, the ayah card, search, the surah list, and settings.

## What the app carries, and what a reader adds

The app ships the Quran text and its page layout only: about ten megabytes,
a complete offline Mushaf that needs no network and no account. Everything
else is added by the reader, from the project's own GitHub Releases, with the
size shown before a byte moves and a SHA-256 check before it is used:

| Language | Translation | Tafsir | Word by word |
|---|---|---|---|
| English | Saheeh International (2.2 MB) | Ibn Kathir (23 MB) | 4.6 MB |
| Arabic | the Quran itself | As-Sa'di (15 MB) | |
| Bengali | Taisirul Quran (5.1 MB) | Ibn Kathir (47 MB) | 6.5 MB |

Recitations: Minshawi and Husary, one surah at a time (0.2 to 122 MB each),
plus 1.7 MB of timing data per reciter.

## Store answers to have ready

* **Is the app free?** Yes, and open source (MIT).
* **Does it show ads?** No, and it has no analytics and no accounts.
* **Why does it need the internet permission?** To download a content pack
  or a surah's recitation that the reader asks for, from the project's own
  releases. Nothing is fetched at launch, nothing automatically, and there is
  no other host.
* **Data safety**: no data collected, no data shared, nothing stored off the
  device. Notes and bookmarks stay in the app's private storage.
