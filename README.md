# Quran: The Noble Book

<img align="right" width="96" src="play-store/icon-512.png" alt="The app icon: an open book drawn by hand in dark ink on cream paper">

A free Quran reader for Android, built to be read. It opens on the ayah you
left, with the QPC Hafs text, color-coded tajweed, the Saheeh International and
Al-Bayan translations, word-by-word meanings with roots and grammar, Tafsir Ibn
Kathir, and recitation by Minshawi, Husary, Alafasy, and Abdul Basit. In
English and বাংলা.

There is one reading and no home screen. No ads, no trackers, no accounts, and
no network beyond the project's own release page.

- **Platform:** Android 7.0 and newer (API 24), phone and tablet, either orientation
- **Package:** `io.github.muntasimulhaque.quran`, app name `Quran`
- **Price:** free, no ads, no purchases, no account
- **License:** MIT for the code; each content dataset keeps its own, recorded in [`content/manifest.json`](content/manifest.json)
- **Privacy policy:** [online](https://muntasimulhaque.github.io/quran/privacy.html) · [in this repo](docs/privacy.html)

<p align="center">
  <img src="play-store/screenshots/phone/01-reading.png" width="180" alt="The reading: al-Kursi in the QPC Hafs print with tajweed colors and Saheeh International under it">
  <img src="play-store/screenshots/phone/02-tafsir.png" width="180" alt="The ayah card: Tafsir Ibn Kathir for 2:255">
  <img src="play-store/screenshots/phone/03-search.png" width="180" alt="Search: kahf 10 goes straight to 18:10">
  <img src="play-store/screenshots/phone/04-saved.png" width="180" alt="Saved ayahs with the notes written on them">
</p>
<p align="center">
  <img src="play-store/screenshots/phone/05-tajweed.png" width="180" alt="Tajweed colors: each rule family explained on a word from the reading">
  <img src="play-store/screenshots/phone/06-dark-reading.png" width="180" alt="The night page">
  <img src="play-store/screenshots/phone/07-share-card.png" width="180" alt="An ayah rendered as one shareable card">
  <img src="play-store/screenshots/phone/08-listen-offer.png" width="180" alt="The listening offer, naming the reciter, the surah, and its size">
</p>

The same reading on a 7 inch and a 10 inch tablet:

<p align="center">
  <img src="play-store/screenshots/tablet7/01-reading.png" width="230" alt="The reading on a 7 inch tablet">
  <img src="play-store/screenshots/tablet10/01-reading.png" width="330" alt="The reading on a 10 inch tablet">
</p>

Every frame here is a real capture from an Android system, taken by the
project's own screenshot test on a phone and on two tablets. Nothing is a
mockup.

## The reading

The app opens where you left off. The bar is the page's head: it carries the
surah and the juz, it steps aside as you read forward, and it comes back on your
first backward scroll or a tap. Browse, Search, and Settings are raised from it
as sheets, so nothing stands between you and the text.

| | |
|---|---|
| **The page** | The QPC Hafs print, word by word, set in the King Fahd Complex's own typeface. Every ayah's number is the door to its card. |
| **Translations** | Saheeh International in English and Al-Bayan in Bangla ship inside the app, so the first page is readable offline with no download and no language screen. More than one may be on at once, and each column says which pack it is. |
| **Tajweed** | The printed mushaf's seven color families, drawn on the words themselves, with a Tajweed page that explains every color and its rules in English and Bangla. |
| **Word by word** | Press and hold any word for its meaning, its root, its dictionary form, its grammar, and its own pronunciation. A root or a form counts every place it stands in the Book, and a tap on one moves the reading there. |
| **Tafsir** | Ibn Kathir opens in the ayah card, in English or Bangla, with the Quran quotations set apart from the commentary. |
| **Search** | Arabic is read without diacritics, and a spelling the print does not carry still finds its verse; English is read without accents. A reference like `2:255` or `kahf 10` goes straight to the ayah. Search in the Quran text, translations, word meanings, tafsirs, your notes, surah names, or references. |
| **Saved and notes** | Keep any ayah, write your own note on it, and find both again under Browse. Last Read keeps the places you have been reading, newest first. |
| **Recitation** | Minshawi, Husary, Alafasy, and Abdul Basit, with the page following the reciter and the word being recited washed as it sounds. Five speeds, repeat the ayah or the surah, stop after this ayah, or continue to the next surah. |
| **Share** | Send the verse as text, or as one image card in the app's own paper and type. |
| **Daily ayah** | One ayah a day at a time you choose, dealt by the day number from a reshuffled deck of all 6,236. A notification, no streaks and no guilt. |
| **Links** | A `quran://ayah/2:255` link opens that verse. |
| **Type and theme** | Light, dark, or the phone's own setting, with five sizes each for the Quran text, the translation, and the tafsir, and the system font scale honored on top of them. |

## Reader first

- **No screen before the text.** No wizard, no dialog, and no feature that
  asks for a choice before the reading. A cold start lands on readable text
  with no spinner, and the reading is a scroll through rows already measured.
- **The chrome steps aside.** There is no home screen, no dashboard, and no
  permanent tab bar. The bar carries the place and is the page's head, and it
  leaves as you read forward.
- **Built for TalkBack.** Every control is named, an ayah's card is spoken as
  one, and the reading works end to end without sight.
- **Measured, not eyeballed.** Contrast targets live in the theme and a test
  measures them. The seven tajweed families sit in one band of lightness, so
  no rule outshouts its neighbor over a long sitting.
- **Nothing borrowed.** Every icon is drawn for this app, the switch and the
  pill are its own shapes, and the typefaces are chosen rather than inherited
  from the platform.

## What ships inside, and what arrives on your word

The Book is here before the first tap. The app installs with the QPC Hafs text
and its tajweed, both translations, the surah names, the recitation timings for
every reciter, and Al-Fatiha's audio in every voice, so the first Play on 1:1
works with the phone in airplane mode.

Everything else is fetched from this project's GitHub Releases, only when you
ask for it:

| What | When | Size |
|---|---|---|
| Tafsir Ibn Kathir | When you open the ayah card and ask for it, in the language you read | 30 MB English, 56 MB Bangla |
| Word meanings | With the switch that turns word by word on | 11 MB English, 13 MB Bangla, plus 3 MB of roots |
| Word pronunciation | The surah being read, when you press a word's speaker or turn the feature on | 352 MB for the Book, one surah at a time |
| Recitation | One surah, after you see its size and tap Download (Al-Fatiha is already here) | Under 1 MB to 186 MB for Al-Baqarah |

A pack you already hold refreshes quietly when a newer version of the app ships,
on an unmetered connection only, and only after the first page is readable.
Nothing is ever streamed: a downloaded surah works offline forever after.

## Private by construction

- **No ads, no trackers, no analytics, no crash reporting, no accounts.** There
  is no third-party SDK. The reader's own work, the saved ayahs, the notes, and
  the reading place, never leaves the phone.
- **No backup, no export, no import.** The app asks the platform not to copy its
  data, on old and new Android alike.
- **Six permissions, each with one reason.** `INTERNET` for content from this
  project's releases, `POST_NOTIFICATIONS` for the daily ayah, the two
  foreground-service permissions for recitation, `SCHEDULE_EXACT_ALARM` for a
  reminder at the minute you chose, and `RECEIVE_BOOT_COMPLETED` to put that
  reminder back after a restart. `USE_EXACT_ALARM` is deliberately not declared.
  The app asks for the notification once, at the reminder card; it asks for the
  exact time only where the phone withholds it, at the two moments the reminder
  is set.
- **One host.** Every request goes to this project's own GitHub Releases page,
  and to the storage GitHub redirects it to, over HTTPS, carrying nothing but
  the request itself. See the [privacy policy](docs/privacy.html).
- **No WebView.** The reading is native Jetpack Compose end to end.

## Two languages, one Book

English is the default and the fallback, and the app follows the phone's
language silently: no language screen stands before the reading. English and
Bangla ship together, interface and content alike, including Tafsir Ibn Kathir
and the word meanings, with the surah names written in Bengali script. A reader
who wants the other language chooses it in Settings, where it also sets the
translation, the tafsir, and the word meanings. On Android 13 and later the
same choice stands in the phone's own per-app language settings, and either
row sets the other. Arabic is data, not a locale, so
right-to-left stays on.

## The Book is not ours to change

Not one byte of the Quran text, the translations, or the tafsir is edited by
hand. Every dataset travels through `tools/`, which verifies and rebuilds the
content database, and each one records its source, version, checksum, and
license in [`content/manifest.json`](content/manifest.json), the one place those
facts live. The app's About page reads that record and names every source.

| Dataset | Source | Terms, as the source gives them |
|---|---|---|
| Quran text and tajweed | King Fahd Glorious Quran Printing Complex, via the Quranic Universal Library | Free for Islamic use |
| Saheeh International | Noor International Center, via QuranEnc, version 1.1.2 | Republishing terms: unchanged, credited, version shown, footnotes kept |
| Al-Bayan Simple Translation | Al-Bayan Foundation Bangladesh, via QUL and IslamHouse | Free to copy and distribute unchanged, never for sale |
| Tafsir Ibn Kathir, English and Bangla | Quranic Universal Library by Tarteel | QUL lists no license; rights unverified |
| Word by word and word roots | Quranic Universal Library | QUL lists no license; rights unverified |
| Grammar and dictionary forms | Quranic Arabic Corpus by Dr. Kais Dukes, via Quranpedia | GNU GPL, attribution required |
| Recitations | Sheikh Muhammad Siddiq Al-Minshawi, Sheikh Mahmoud Khalil Al-Husary, Mishari Rashid al-Afasy, and Abdul Basit Abdul Samad, via QUL | QUL lists no license; rights unverified |
| Word pronunciation | Mishari Rashid al-Afasy, via QUL, with word timings by quran-align | Audio: rights unverified; timings: CC BY 4.0 |
| Uthmani reference text | Tanzil Project | CC BY 3.0, unmodified, credited |
| Bangla surah names | Compiled and reviewed for this app after Dr. Abu Bakr Muhammad Zakaria's index | No verse text is copied |

**A note on the footnotes.** Saheeh International's note 403 is missing from the
source of 7:143: both QuranEnc and the QUL copy carry 401 and 402 and no 403.
The app draws 403 as a plain superscript that opens nothing, and no note was
invented to fill the gap. A verified note from a Saheeh source would travel
through `tools/` like every other content change.

## Build it

`JAVA_HOME` must point at the Android Studio JBR; it is not on `PATH`.

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"

# the release gates: unit tests, lint, and a debug build
./gradlew :core:test :tools:test :data:testDebugUnitTest :ui-kit:testDebugUnitTest \
  :feature-browse:testDebugUnitTest :feature-playback:testDebugUnitTest \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

# the content pipeline, every step and its arguments
./gradlew :tools:run --args="help"
```

An Android SDK is needed, and `local.properties` must carry its `sdk.dir`. A
release build signs from a keystore outside the tree and degrades to unsigned
when that keystore is absent, so a fresh clone still builds. Some content gates
read `content/raw/`, the owner's own exports, which are not in the repository.

## How the code is laid out

| Module | What is there |
|---|---|
| `core/` | Pure JVM Kotlin, zero `android.*`: references, search normalization, rich text, the deck, the models |
| `data/` | Read-only content access, the downloads that fetch a pack from this project's Releases, the saved-ayah and last-read stores, preferences |
| `ui-kit/` | The theme and palettes, the hand-drawn icons, the shared rows and shapes, the rich text and tajweed views, the small formatters |
| `feature-reading/`, `feature-search/`, `feature-browse/`, `feature-playback/`, `feature-settings/` | One surface each: the reading, the search sheet, Surahs and Juz and Last Read and Saved, the pill and the player, the settings hub and its pages |
| `app/` | The shell: the one activity, the view model, the screen that composes the features, the daily ayah's scheduler and notification |
| `content-assets/` | The typefaces the app ships |
| `tools/` | The offline pipeline that builds every dataset, and `tools/icons/forge.py`, which draws every icon and the feature graphic |
| `benchmark/` | The startup profile's generator, development only, never in the bundle |

Every settled design decision lives in the KDoc beside the code it decided,
tagged `owner decision`, and a test measures the ones that can be measured, so
`git grep "owner decision"` is the record of why the app is the way it is.

## Status

**21.5 (versionCode 217).** The signed bundle comes from `build` run
44931558235, green on `main` for the commit `a4520eca`: the unit tests,
`checkdb`, `search`, lint, and `assembleDebug` passed, and the signed bundle
`quran-21.5-vc217.aab` has SHA-256
`8eb68658f288a41314a993ea29c1941c0611ba5345f78281d3a60db01b5de175`.
`jarsigner` reads `jar verified`, and the upload certificate is the same key
21.4 shipped under, SHA-256
`53:7D:09:D2:03:00:12:9E:97:3B:79:45:31:6B:FE:24:CF:AD:CF:BC:77:EE:C5:22:9C:BF:30:17:0D:9D:E5:21`.
The whole JVM suite, `checkdb`, `search`, lint, and the debug build were also
green on this machine before the push, and the four accessibility classes the
release rule now names ran on the phone after the moves: `AccessibilityTest`,
`LargeTextTest`, `ReadingSemanticsTest`, and `KeyboardFocusTest`, 11 tests and
no failures. The run after the version commit was red at dependency
verification, because the regenerated file held only this machine's AAPT2
binary: the Linux and macOS copies were added from Google's own Maven,
cross-checked against the SHA-1 that repository publishes, and proved locally
by resolving both classifiers through the verification gate, and the run that
built this bundle is the one after that fix. No visible surface changed, so
the three store sets stand as captured for 21.2, and no content, no string,
and no permission moved. The build moves to AGP 9.5.0, and every artifact it
adds is pinned by hash in
`gradle/verification-metadata.xml`, regenerated over the whole task graph
and proved artifact by artifact. The 21.5 bundle was submitted for Play
review, and the hand-off copy in `play-store/aab/` was deleted once Play had
it.

**What changed in 21.5.** The rules that had no gate now have one. The five
answers about a translation, a tafsir, and the word list; the line under the
words switch; the order of the word sheet's grammar; the clock's digits and
period marker; and the medallion's ink, its place on the glass, and its
touch target all moved into `:data`, `core`, and `ui-kit`, whose JVM suites
the release gates already run, and each carries a sentence-named test.
Nothing the reader sees moved, and no string changed modules.

The quiet refresh's decision is written beside it now (owner decision,
21.5): the reader's word was the pack's choosing, only packs already on the
device are touched, it never runs on a metered connection, and a newer
version is named by the catalog inside the app, so it can only arrive with a
new release of the app. Its dead `force` switch and the manual check its
note promised are gone, because the catalog cannot change between two
launches of one build and a button could never find anything. The
accessibility leg is a release rule now, triggered by a path rather than a
judgment.

**What changed in 21.4.** The compile platform is 37.1, declared as
`compileSdk = 37` with `compileSdkMinor = 1` in all ten modules;
`targetSdk` stays 37 and `minSdk` 24 for the reasons their own notes give.
The compose BOM moves to 2026.11.01, which carries Compose 1.13.0 and
material3 1.5.0 stable, and the hand pin on material3 is gone: the pin's own
comment waited for exactly this session, and the comment beside the version
now records where the pin went. Nothing the reader sees moved, and no
string, color, or frame changed.


## Corrections and contributions

Please say so; it is far more use as an issue than as a review. Open a
[content correction](https://github.com/muntasimulhaque/quran/issues/new) with
the reference (`2:255`), what the app shows, and what it should say. From inside
the app, About, then Send feedback, opens an email instead.

Pull requests are welcome. A change to the reading must keep the gates above
green and is checked against the decisions in the KDoc. Content is never edited
by hand: it changes through `tools/`, with its source, version, checksum, and
license recorded.

## License

**The code is MIT**: see [LICENSE](LICENSE). Take it, build on it, ship it.

**The content is not ours to license.** Every dataset in `content/` belongs to
the source that published it, under the terms that source states, and those
terms are recorded per dataset in [`content/manifest.json`](content/manifest.json)
and shown in the app's About page. Nothing in the MIT grant covers the Quran
text, the translations, the tafsir, the recitations, or the word lists.

**The typefaces** are the SIL Open Font License 1.1 for six of them, and the
King Fahd Complex's own terms for the Hafs face, which ships unmodified and is
never sold. See [licenses/](licenses/).
