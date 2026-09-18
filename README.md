# Quran: The Noble Book

A free, open source Quran reader for Android. It opens on the page you left,
turns like paper, and keeps the Quran text itself at the center: no ads, no
trackers, no accounts, nothing collected, ever.

**Status:** submitted to Google Play for review; the seventh session's audit
is in the tree, unbuilt and unpublished, waiting for the owner's word. The
listing, icon, feature graphic, and screenshots are in
[`play-store/`](play-store/).

## What it does

- **Mushaf mode.** The page of the Madinah Mushaf, drawn glyph by glyph from
  the QPC V2 page fonts, with the printed page's own furniture: surah names,
  juz and hizb, and the page number in a gold medallion. A swipe turns the
  page, with the shadow of a sheet of paper and a light tick as it settles. A
  tap brings quiet chrome; a long press asks about the ayah under your finger.
  Every ayah is a node a screen reader can read and act on.
- **A launch that lands on the page.** The page the reader left is kept as a
  picture and painted before the content database opens, so the app arrives
  already showing the Book instead of a splash.
- **Study mode.** One surah at a time, scrolls continuously to the end of the
  surah and offers the next. Arabic, then the translation, then footnotes
  behind their markers. An optional word by word aid puts each word's meaning
  beneath it, in the language of the translation above it.
- **The ayah card.** Save, note, copy, share, play from this ayah, word by
  word, and every tafsir you have installed.
- **Search.** Arabic text, every enabled translation and tafsir, word
  meanings, surah names, and references like `2:255`. Results are exact and
  instant, even over a forty megabyte tafsir.
- **Recitation.** Minshawi and Husary, one surah at a time. The page follows
  the reciter and the word being recited is washed as it is read.
- **Your own work, yours to move.** Saved ayahs and notes export to a file and
  import back on another phone, through the system's own file picker.
- **A library you choose.** The app ships the Quran text and its page layout
  and nothing else. Translations, tafsirs, word lists, and recitations are
  added when the reader wants them, from the project's own Releases, with the
  size shown first and the file verified by SHA-256. The app can also read
  every installed pack back and tell you if one no longer matches what was
  published.
- **Readable in every room, on every setting.** Four themes, a screen dimmer,
  a text size scale, secondary text above 4.5:1 contrast in all four themes,
  and every control a real 48 dp target.

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
| Bengali | Taisirul Quran | Ibn Kathir | yes |

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
./gradlew :tools:run --args="search"   # Arabic, Bengali, and Latin search
./gradlew :tools:run --args="packs"    # pack files and the catalog
./gradlew :tools:run --args="audio"    # recitation packages
```

## Privacy

The app collects nothing. The policy is [online](https://muntasimulhaque.github.io/quran/privacy.html)
and [in this repo](docs/privacy.html). The one network use is a content pack
or a recitation package the reader asks for, from the project's own Releases.
Android's cloud backup and device transfer are refused explicitly, so the
reader's saved ayahs and notes stay on the device until they export them.

## License

The code is MIT: see [LICENSE](LICENSE). The Quran text, translation, tafsir,
fonts, and recitations are not ours to license and keep their own terms and
credits, recorded in [`docs/content-sources.md`](docs/content-sources.md).
