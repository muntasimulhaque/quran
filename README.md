# Quran: The Noble Book

A free, open source Quran reader for Android. It opens on the page you left,
turns like paper, and keeps the Quran text itself at the center: no ads, no
trackers, no accounts, nothing collected, ever.

**Status:** 0.8 (versionCode 8) is submitted to Google Play for review. This
session answered the reader's fourth report: Mushaf pages now turn the way a
printed Mushaf turns, with the next page to the left and a swipe to the right
going forward; the long-press ayah bar floats as a rounded pill; the top bar reads a
little smaller; search filters carry a check when they are on and say what an
empty choice means; the phone back button returns from a settings page to
Settings; the reciter list's downloaded surahs sit closer under one label with
a turning arrow; and a scroll back up in Browse can no longer pull the sheet
closed, with the gesture judged from the finger's own landing. The bundle
carries only the core pack, the Quran text and its page layout, with everything
else added from the project's own Releases when a reader asks for it. The
listing, icon, feature graphic, and screenshots per form factor are in
[`play-store/`](play-store/), refreshed from CI.

## What it does

- **Mushaf mode.** The page of the Madinah Mushaf, drawn glyph by glyph from
  the QPC V2 page fonts, with the printed page's own furniture: surah names,
  juz and hizb, and the page number in a gold medallion. A swipe turns the
  page, with a light tick as the turn settles. A
  tap brings quiet chrome; a long press asks about the ayah under your finger.
  Every ayah is a node a screen reader can read and act on.
- **A launch that lands on the page.** The page the reader left is kept as a
  picture and painted before the content database opens, so the app arrives
  already showing the Book instead of a splash.
- **Study mode.** One surah at a time, scrolls continuously to the end of the
  surah and offers the next. Arabic, then the translation, then footnotes
  behind their markers. An optional word by word aid puts each word's meaning
  beneath it, in the language of the translation above it.
- **A reading place you can go back to.** The app opens on the ayah you left.
  Browse keeps the last twenty places you read, newest first, each with the
  mode you were in and when you left it, so a surah you visited last week is
  one tap away again.
- **Two readings, one switch.** The Mushaf and the study view are one icon in
  the top bar that always offers the other one. There is no bottom bar: the
  index, search, saved ayahs, and settings are all one tap from the same edge,
  and listening is the play action on any ayah.
- **The ayah card.** Save, note, share, play from this ayah, word by word, and
  every tafsir you have installed. It offers to add what it does not have yet
  rather than showing an empty panel.
- **Search.** Arabic text, every enabled translation and tafsir, word
  meanings, surah names, and references like `2:255`, with a filter row under
  the field for narrowing the sources when a query returns too much. Results
  are exact and instant, even over a forty megabyte tafsir.
- **Recitation.** Minshawi and Husary, one surah at a time, asked for once: one
  offer names the reciter, the surah, and the size, the reciter can be swapped
  in that offer, and the word being recited is washed as it is read. The page
  follows the reciter if you ask it to. An offer you do not want is one tap
  away from gone.
- **A library you choose.** The app ships the Quran text and its page layout
  and nothing else. Translations, tafsirs, word lists, and recitations are
  added when the reader wants them, from the project's own Releases, with the
  size shown first and the file verified by SHA-256. The app can also read
  every installed pack back and tell you if one no longer matches what was
  published.
- **Readable in every room, on every setting.** Four themes, an automatic night
  mode that follows the system when the reader asks it to, a size of your own
  for the Quran text, the translation, the tafsir, and the word by word aid, a
  settings hub with the state of each choice on its row, secondary text above
  4.5:1 contrast in all four themes, and every control a real 48 dp target.
  More than one translation may be on at once; each draws in its own place
  under the ayah.

## The content

The Arabic text is the KFGQPC Hafs text used with the King Fahd Glorious
Quran Printing Complex fonts, audited word by word against the Tanzil
Project text. The translation is Saheeh International, issued by Noor
International Center and distributed by QuranEnc.com. Scripts, layouts,
tafsirs, metadata, and recitations come from the Quranic Universal Library
(QUL) by Tarteel. Every dataset carries its source, version, and credit, and
every license is honored: see [`docs/content-sources.md`](docs/content-sources.md).

The library today, language by language:

| Language | Translation | Tafsir | Word by word |
|---|---|---|---|
| English | Saheeh International | Ibn Kathir | yes |
| Arabic | the Quran itself | As-Sa'di | |
| Bangla | Taisirul Quran | Ibn Kathir | yes |

## How it is built

The app is ten small Gradle modules with one way dependencies, and a JVM
content pipeline. Every sentence the reader can see lives in a `strings.xml`
in the module that draws it, so the interface can be translated without
hunting through Kotlin. The rules are in [`docs/architecture.md`](docs/architecture.md);
the design constitution is in [`docs/design.md`](docs/design.md); the reasons
behind every decision are in [`docs/decisions.md`](docs/decisions.md).

```
:core :data :content-assets :ui-kit
:feature-mushaf :feature-study :feature-search :feature-browse
:feature-playback :feature-settings
:app            the wiring: activity, view model, screen, manifest
:tools          the content pipeline (verify, audit, build, packs, audio)
```

## Building it

```bash
# A fresh clone needs the content packs, the database, and the fonts.
./gradlew :tools:run --args="fetch"          # downloads and verifies them

./gradlew :core:test                          # the pure Kotlin tests
./gradlew :data:testDebugUnitTest             # pack ids and the saved store's shape
./gradlew :app:assembleDebug                  # a debug APK (carries every pack)
./gradlew :app:bundleRelease                  # the signed release bundle
```

The release bundle carries only the core pack, ten megabytes of Quran text and
page layout; the 604 page fonts are the bulk of the app, because it must
render the Book with no network.

The content pipeline, run on the maintainer's machine where the raw sources
live:

```bash
./gradlew :tools:run --args="verify"   # every source hash and structure
./gradlew :tools:run --args="audit"    # letter by letter against Tanzil
./gradlew :tools:run --args="build"    # the database and the packs
./gradlew :tools:run --args="checkdb"  # the committed database and catalog
./gradlew :tools:run --args="search"   # Arabic, Bangla, and Latin search
./gradlew :tools:run --args="packs"    # pack files and the catalog
./gradlew :tools:run --args="audio"    # recitation packages
```

## Privacy

The app collects nothing. The policy is [online](https://muntasimulhaque.github.io/quran/privacy.html)
and [in this repo](docs/privacy.html). The one network use is a content pack
or a recitation package the reader asks for, from the project's own Releases.
Android's cloud backup and device transfer are refused explicitly, so the
reader's saved ayahs and notes stay on the device and move nowhere.

## License

The code is MIT: see [LICENSE](LICENSE). The Quran text, translation, tafsir,
fonts, and recitations are not ours to license and keep their own terms and
credits, recorded in [`docs/content-sources.md`](docs/content-sources.md).
