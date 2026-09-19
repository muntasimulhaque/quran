# Store kit

Version: 0.2 (versionCode 2)

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
  https://muntasimulhaque.github.io/quran/privacy.html

## Graphics

The icon and the feature graphic are made; the screenshots are captured by the
screenshot test on a real Android system, one folder per form factor Play asks
for.

- App icon: `icon-512.png`
- Feature graphic: `feature-graphic-1024x500.png`
- Phone screenshots: `screenshots/phone/` (1080 x 1920), eight of them
- 7 inch tablet screenshots: `screenshots/tablet7/` (1200 x 1920)
- 10 inch tablet screenshots: `screenshots/tablet10/` (2560 x 1800)

The `.github/workflows/screenshots.yml` workflow runs the capture test on
three emulator profiles (phone, 7 inch, 10 inch) and uploads each set as its
own artifact, so the store images always match the shipped build. Run it from
the Actions tab, or let it run when the UI changes.
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

## Release notes (ready for the next release, 499 characters)

Opens on the exact ayah you left, in both reading modes, and paints that page before anything else loads. Settings is a hub: its own page for text sizes, reciters, translations, and tafsirs, each sized as you like. Listening asks once, names the reciter and the size, and lets you swap reciter in the offer. The modes are two drawn icons, and footnotes no longer collide at large sizes. Saved ayahs export and import, and Settings can check every pack on the device. No ads, no trackers, no account.

## Before each release

1. Raise `versionCode` by 1 and `versionName` by 0.1 in
   `app/build.gradle.kts` and update the version line at the top of this
   file. The release workflow refuses a tag that does not name the version
   the app carries, so this step comes first.
2. Run the owner-machine gates: `./gradlew :tools:run --args="verify"`,
   `audit`, `fonts`, and the instrumented tests on an emulator.
3. Tag it, and let the workflow do the rest:
   `git tag v<version> && git push origin v<version>`. The `release`
   workflow runs the tests and the content gates, builds the signed bundle
   from repository secrets, verifies the signature, and drafts a GitHub
   release with the bundle and its SHA-256 attached.
4. Download the draft's bundle, upload it to the internal track, check the
   size report, then promote. Delete the draft's assets once Play has it.

Signing is optional in the workflow: without the four repository secrets
(`UPLOAD_KEYSTORE_BASE64`, `UPLOAD_KEYSTORE_PASSWORD`, `UPLOAD_KEY_ALIAS`,
`UPLOAD_KEY_PASSWORD`) it still builds, and says the bundle is unsigned.

## Assets in this folder

| File | Size | Use |
|---|---|---|
| `icon-512.png` | 512 x 512 | store icon |
| `feature-graphic-1024x500.png` | 1024 x 500 | feature graphic |
| `screenshots/phone/*.png` | 1080 x 1920 | phone screenshots (8) |
| `screenshots/tablet7/*.png` | 1200 x 1920 | 7 inch tablet screenshots |
| `screenshots/tablet10/*.png` | 2560 x 1800 | 10 inch tablet screenshots |

Screenshots: the Mushaf page, the summoned chrome, the study reading, an
ayah's actions, the ayah card, search, the surah list, and settings. The
tablet sets are produced by the same test on the taller and wider profiles.

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
