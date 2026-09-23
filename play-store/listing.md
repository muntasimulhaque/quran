# Store kit

Version: 1.6 (versionCode 17)

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
English and As-Sa'di in Arabic, with the Quran quotations set apart. Bangla
readers get the Taisirul Quran translation, Ibn Kathir in Bangla, and Bangla
word meanings, and the whole interface can be read in English or Bangla.

Search reads Arabic without diacritics and English without accents, so
typing allah finds Allah and isa finds Isa. Save any ayah, add your own
note to it, and find everything again under Browse: one list for your saved
ayahs, one for the ayahs you wrote notes on, and a Last read list that keeps
the places you have been reading so you can return to one you left.

The two readings are one door at the top of the page, and it always
offers the other one: the printed page, or the study view. There is no
bottom bar to learn. More than one translation may be on at once, and
each one draws in its own place under the ayah.

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
- 7 inch tablet screenshots: `screenshots/tablet7/` (800 x 1280), eight of them
- 10 inch tablet screenshots: `screenshots/tablet10/` (2560 x 1800), eight of them

The set was refreshed for the 1.5 release from the `Capture store
testscreenshots` workflow (run 35771681366), for all three form factors, in
the eight frames the store lists. Every frame was compared with its artifact
by `cmp` before it replaced the committed set. It carries the two fixes:
the ayah card's word by word section now stands above the translation, and
surah introductions keep their headings on their own lines. The remaining
frame differences between runs are the status bar's own clock.

The workflow runs the capture test on three emulator profiles (phone, 7 inch,
10 inch), caches the AVD per profile so only the first run of each pays for
creating the emulator, waits for the emulated storage to mount before the test
starts, and uploads each set as its own artifact, so the store images always
match the shipped build. Run it from the Actions tab, or let it run when the UI
changes.

What each set shows, in order:

1. The Mushaf page
2. The chrome: the mode door, Browse, Search, and Settings
3. The study reading with its translation
4. The surah opening
5. Search with the matched word marked, and the filters under the field
6. The settings hub
7. Browse, Surahs
8. The ayah card, with its tafsir doors

## Release notes (1.6, 438 characters)

Saving and noting are now separate: a note no longer appears under Saved, and Remove sits beside every note in Browse. Surah numbers in Browse are whole again (100 was showing as 10) and read in the digits of your language. The Saved list shows just the surah and ayah, tapping a note highlights its ayah, About this surah wears the same highlight as a long-pressed ayah, and Forget reads মুছুন in Bangla. No ads, no trackers, no account.

## Release notes (1.5, 338 characters)

About this surah keeps its headings now: Name, Period of Revelation, and Theme each stand on their own line instead of running into the paragraph they head. Opened from the Mushaf, the ayah card now reads word by word first, then the translation, then the tafsir, in the order the study view already uses. No ads, no trackers, no account.

## Release notes (1.4, 432 characters)

Share now sends a picture of the ayah with its translation, its reference, and the app's mark, with the plain text along as the caption. Browse's surah and juz numbers line up so every name starts in the same place. The ayah card names its word by word section, scrolling back up no longer closes a sheet, the ayah actions read Play, Note, Save, Share, More, and the note sheet opens as Take a note. No ads, no trackers, no account.

## Release notes (1.3, 410 characters)

Changing the language now keeps you on the Language page, in the language you just chose. The ayah card reads under Translation and Tafsir names instead of lines. About this surah no longer cuts off when opened. Words that act, like Save and Remove, now look like buttons. Browse has a Notes tab listing every ayah you wrote a note on, and font sizes now run 0.65 through 1.2. No ads, no trackers, no account.

## Release notes (1.2, 397 characters)

Long-press an ayah and the pill now has a Note action, and the phone's back button closes it instead of the app. More shows only what the reading behind it does not: from the Mushaf the translation and word by word, from study the tafsir. Settings lists read alphabetically, and word meanings sits at the top of Translations. Fixed a crash when switching language. No ads, no trackers, no account.

## Release notes (1.1, submitted, 387 characters)

Fixed a crash when turning Mushaf pages quickly. The reciter chooser now matches the pill it opens from, and scrolling back to the top of search results no longer closes the sheet. Settings are tidier: word by word sits under Translations, following the reciter sits with the Reciters, and keeping the screen awake is in the hub. Bangla wording improved. No ads, no trackers, no account.

## Release notes (1.0, submitted, 439 characters)

Choose your language on the first screen: English or Bangla, and the app, the translation, the tafsir, and the word meanings all follow it. The top bar is one row again, with a single icon that takes you between the Mushaf and the study view. Word meanings are one switch in Settings, which fetches the word list your translation speaks. The reciter chooser now matches the pill in color and rounded shape. No ads, no trackers, no account.

## Release notes (0.10, submitted, 365 characters)

The top bar is two rows now: the reading modes stay at the center of the screen, and the surah and Juz sit centered beneath them, whole on every phone. The word being recited in the study view is marked with the Mushaf's rounded wash, About this surah answers in the same shape, and the end of a surah offers the next one as a card. No ads, no trackers, no account.

## Release notes (0.9, submitted, 496 characters)

The two reading modes are one switch at the center of the top bar, and the Play offer lets you pick the reciter, with the size shown. The Reciters page no longer downloads or explains the word timings; they come with the first surah you play, and removing a reciter selects another. The font size page previews a short ayah, Arabic inside a tafsir reads larger than the prose, the ayah card drops its repeated Save and Share, and downloaded surah rows sit closer. No ads, no trackers, no account.

## Release notes (0.8, submitted, 494 characters)

Pages now turn the way a printed Mushaf turns: the next page lies to the left, so a swipe to the right goes forward. The ayah actions bar floats as a rounded pill. The top bar reads a little smaller, and search filters show a check when they are on. Downloaded surahs sit closer to their reciter under one label with a turning arrow. The phone back button returns from a settings page to Settings, and scrolling back up in Browse does not pull the sheet closed. No ads, no trackers, no account.

## Release notes (0.7, submitted, 487 characters)

Every Mushaf page now reads right to left, as the printed page does. The Mushaf and study icons were redrawn, Browse lost its title, and a surah opened from Browse lands on your last place in it or at its top. Footnotes follow the size of the translation, About this surah closes with a tap anywhere, Last Read rows lost their Open button, reciters gained a clearer downloaded-surahs list, and a scroll back up in Browse no longer pulls the sheet closed. No ads, no trackers, no account.

## Release notes (0.6, submitted, 377 characters)

Footnotes in the ayah card open from their marker now, like the study reading. The word by word aid is larger, its Arabic centered over each meaning. Search filters wrap so none are hidden, settings have more room, and reciter rows say what the download is. A download request leaves with its surah, and a finished surah no longer stays marked. No ads, no trackers, no account.

## Release notes (0.5, shipped, 235 characters)

Search results no longer show raw markup where a tafsir was matched; every result now reads as plain prose, and a matched word stays highlighted. The surah introduction in study mode reads as prose too. No ads, no trackers, no account.

## Release notes (0.4, shipped, 423 characters)

Fixed a crash when adding a translation, tafsir, or word list. Tapping such a row now adds it and turns it on, and removing one downloaded surah works. The page no longer lifts with a shadow during a turn, the top bar hides while you scroll, and the Mushaf and study icons were redrawn to match. Appearance can follow your phone dark mode. Last Read, search filters, and alphabetical lists. No ads, no trackers, no account.

## Release notes (0.3, shipped, 496 characters)

The reading place no longer jumps back when you come from another surah. The mode switch is one icon in the top bar that offers the other view, and the bottom bar is gone: Saved sits in Browse beside the surahs and juz, and listening is the play action on any ayah. Browse has a Last read tab with your last twenty places. More than one translation may be on at once, text sizes gained a smaller step and lost the largest, and the download offer can be dismissed. No ads, no trackers, no account.

## Release notes (0.2, as shipped)

Opens on the exact ayah you left, in both reading modes, and paints that page before anything else loads. Settings is a hub: its own page for text sizes, reciters, translations, and tafsirs, each sized as you like. Listening asks once, names the reciter and the size, and lets you swap reciter in the offer. The modes are two drawn icons, and footnotes no longer collide at large sizes. Saved ayahs export and import, and Settings can check every pack on the device. No ads, no trackers, no account.

## Release notes (first release, as shipped)

First release. Mushaf and study modes, Saheeh International with
footnotes, word by word, Ibn Kathir and As-Sa'di, search, bookmarks and
notes, and recitations by Minshawi and Husary downloaded per surah on
request. No ads, no trackers, no account.

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
`KEY_ALIAS`, `KEY_PASSWORD`) are described in `RELEASE.md` step 3c, with
where each value comes from in the vault. The workflow refuses to publish an
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

Screenshots: the Mushaf page, the summoned chrome, the study reading, the
surah opening, search, the settings hub, Browse, and the ayah card. The tablet
sets are produced by the same test on the taller and wider profiles.

## What the app carries, and what a reader adds

The app ships the Quran text and its page layout only: about ten megabytes,
a complete offline Mushaf that needs no network and no account. Everything
else is added by the reader, from the project's own GitHub Releases, with the
size shown before a byte moves and a SHA-256 check before it is used:

| Language | Translation | Tafsir | Word by word |
|---|---|---|---|
| English | Saheeh International (2.2 MB) | Ibn Kathir (23 MB) | 4.6 MB |
| Arabic | the Quran itself | As-Sa'di (15 MB) | |
| Bangla | Taisirul Quran (5.1 MB) | Ibn Kathir (47 MB) | 6.5 MB |

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
