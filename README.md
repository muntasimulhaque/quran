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

Every screen inside every card is a real capture from an Android system, taken
by the project's own screenshot test on a phone and on two tablets. The card
around it, its caption, its gold rule, and its device outline are drawn by the
project's own tools from the app's own fonts and the theme's own colors. No
screen is a mockup.

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
every reciter, and Al-Fatiha's audio in the default voice, so the first Play
on 1:1 works with the phone in airplane mode.

Everything else is fetched from this project's GitHub Releases, only when you
ask for it:

| What | When | Size |
|---|---|---|
| Tafsir Ibn Kathir | When you open the ayah card and ask for it, in the language you read | 30 MB English, 56 MB Bangla |
| Word meanings | With the switch that turns word by word on | 11 MB English, 13 MB Bangla, plus 3 MB of roots |
| Word pronunciation | The surah being read, when you press a word's speaker or turn the feature on | 352 MB for the Book, one surah at a time |
| Recitation | One surah, after you see its size and tap Download (Al-Fatiha is already here in Husary's voice) | Under 1 MB to 186 MB for Al-Baqarah |

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

# the release gates: the JVM suite, the content gates, the store cards, lint
./gradlew :core:test :tools:test :data:testDebugUnitTest :ui-kit:testDebugUnitTest \
  :feature-browse:testDebugUnitTest :feature-playback:testDebugUnitTest \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :tools:run --args="checkdb"
./gradlew :tools:run --args="search"
./gradlew :tools:run --args="storecards --check"

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

**22.4 (versionCode 226).** The whole JVM suite, `checkdb`, `search`,
`storecards --check`, lint, and `assembleDebug` were green on this machine
before the push. The diff touches `tools/`, `play-store/`, the two records,
and one comment in the capture workflow, so no accessibility class was owed
and none ran. The three store sets were captured on this session's legs, all
green: `quran_phone` at 1080 x 1920, `quran_tablet7` at 1200 x 1920 and 320
dpi, and `quran_tablet10` at 2560 x 1800. The app itself is 22.3: no file
under `app/`, `core/`, `data/`, `ui-kit/`, or a `feature-*` changed, and
`content/quran.db` still hashes
`9f808fdf4ea6e114a492a507c05b5ad712d626f1a381eee4c27ce7c904ff224f`.
The signed bundle follows from the newest green `build` run on `main`.

**What changed in 22.4.** The store set is the app's own face (owner
decision, 22.4). The eight screenshots Play shows are cards now: each is a
real capture from the screenshot test, whole and unedited, laid on the app's
own paper inside an outline drawn in its ink, under one line set in the app's
Literata with the app's Inter beneath it. The card's gold rule is the feature
graphic's, so the icon, the graphic, and the cards read as one face.
`./gradlew :tools:run --args="storecards"` composes all three sets from the
captures committed under `play-store/frames/`, and `storecards --check` is a
release gate: it compares the pictures the command would draw with the ones
the repository holds, at a tolerance wide enough for a platform's own font
rasterizer and narrow enough that one changed word fails. The app itself is
unchanged, and the accessibility classes were not owed: the diff touches
`tools/`, `play-store/`, and the records, and no path the trigger names.

**What changed in 22.3.** Search recovers where it used to stop (owner
decision, 22.3). A query that finds nothing is offered the one word the
reader's own library says is nearest to what they typed, in their own
sentence, and tapping it rewrites that word and runs the search again. A
reference past its surah's last ayah answers with the surah's own count, and
a surah past the hundred and fourteenth with the Book's count, in place of
"No matches". Words wrapped in double quotes must sit together, and a quoted
reference is a phrase and never a place. `juz 30`, `para 30`, `পারা ৩০`, and
`جزء 30` land on the ayah a juz opens, and the row names the juz in the
reader's own word. The vocabulary is the Book's words, the enabled
translations, and the word list, warmed when the sheet opens, so the
suggestion lands in a frame; nothing leaves the phone. The text, both
translations, both tafsirs, and the content database are unchanged.

**What changed in 22.2.** The page opens on Light (owner decision, 22.2,
superseding 13.5). A fresh install no longer follows the phone's own day and
night: the Book opens as its own warm page, the page the printed Book reads
on, and System stays one row away for a reader who wants the phone to
choose. An install that has opened before and never chose a page keeps the
System it reads under, written once before the new default can move it, so
no update changes a reader's page for a reason they cannot see. A language
change now swaps a tafsir the reader has chosen and never introduces one:
until now, moving from English to Bangla started a 56 MB tafsir download
behind a reader who had never asked for a tafsir, which the first open
deliberately leaves off. Al-Fatiha's recitation rides in Husary's voice
alone, the default recitation; a reader who has chosen another voice meets
the same download offer every other surah makes, 0.7 MB for the surah. All
four voices' word timings still ship, so every reciter is ready offline.
The Quran text, both translations, both tafsirs, and the content database
are unchanged.

**What changed in 22.1.** The one action color returns to lapis, measured
into the ultramarine the illuminated Qurans were painted in (owner decision,
22.1). 22.0 had turned the accent to emerald on the category's word; the
green's claim proved to be the 1837 dome and the flags rather than the text,
while the blue the app wore before it is the hue the platform reads as the
way things work and the one hue red-green color weakness still sees as
itself. The day tone #1F3D76 reads 10.0 to 1 on the page and the night tone
#A8C2EF reads 9.6 to 1, both past every floor the tests enforce, and wider
from the verse's own blue families than the emerald ever sat from its green.
The night's chosen block, the wash behind the current surah and juz in Browse
and behind the word the word sheet names, is lifted to 18.4 L*, where the
15.4 it carried since 21.0 stood 2 L* over its sheet and read as nothing at
all. The paper, the ink, the gold ornament, the tajweed palette, the icon,
and every content byte are unchanged.

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
