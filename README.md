# Quran: The Noble Book

A free, open source Quran reader for Android. It opens on the page you left,
turns like paper, and keeps the Quran text itself at the center: no ads, no
trackers, no accounts, nothing collected, ever.

**Status:** 3.2 (versionCode 33) is submitted to Google Play for review. The
owner took the bundle from build run 100 (`quran-3.2-vc33.aab`,
148,302,873 bytes, SHA-256
`2a52889add53499e1b1691033afc04d6a20064994a2eff195e3fc69cf0c89b60`, `jar
verified`) and submitted it, and the hand-over copy was deleted the same
session, so the artifact stays in build run 100 and in Play. The store set
is not refreshed and that is on the record: the committed set is 3.0's,
because the 3.2 capture's 10 inch leg came back one frame short and a set is
never installed from a partial run (D-127). Two things are owed: the tour's
last step on the 10 inch profile, which now starts from a fact rather than a
theory, and a refreshed set for all three form factors. The release history
is [`docs/decisions.md`](docs/decisions.md) and the store's own face of it
is [`play-store/listing.md`](play-store/listing.md).

## What it does

- **Mushaf mode.** The page of the Madinah Mushaf, drawn glyph by glyph from
  the QPC V2 page fonts, with the printed page's own furniture: surah names,
  juz and hizb, and the page number in a gold medallion. A swipe turns the
  page with a light tick as it settles, a tap brings quiet chrome, and a
  long press asks about the ayah under your finger. Every ayah is a node a
  screen reader can read and act on.
- **A launch that lands on the page.** The page the reader left is kept as a
  picture and painted before the content database opens, so the app arrives
  already showing the Book instead of a splash.
- **Study mode.** One surah at a time, scrolling continuously to the end and
  offering the next. Arabic, then the translation, then footnotes behind
  their markers. An optional switch shows each word with its meaning beneath
  it, in the language of the translation above it.
- **A reading place you can go back to.** The app opens on the ayah you left.
  Browse keeps the last twenty places, newest first, each with the mode you
  were in and when you left it.
- **Browse.** Four tabs: Surahs, Juz, Last Read, and Saved. A surah row opens
  that surah's own ayah grid with your place marked, a juz row opens the
  ayah it names, and Saved is the one list of what you kept: every saved
  ayah, a note previewed under the ones that have it, ordered by the more
  recent of the two moments.
- **Two readings, one door.** The Mushaf and the study view are one door in
  the top bar: the icon shows the reading you are not in, and a tap takes
  you there. There is no bottom bar. The index, search, saved ayahs, and
  settings are one tap from the same edge, and listening is the play action
  on any ayah.
- **A language chosen once.** A first-launch screen offers English or
  Bangla, each named in its own script, and the choice sets the interface,
  the translation, the tafsir, and the word meanings together. It can be
  changed from Settings, and the whole app follows.
- **The ayah card.** Save, note, share, play from this ayah, the word
  meanings, and every tafsir you have installed. It offers to add what it
  does not have yet rather than showing an empty panel. Share shows the card
  as it will be sent, then sends the picture or the words, never both.
- **Search.** Arabic text, every enabled translation and tafsir, word
  meanings, surah names, and references like `2:255` or `baqara 255`, with a
  filter row under the field. Results are exact and instant, even over a
  forty megabyte tafsir, and they run from the verse outward: the reference
  you typed, the surah you named, the ayahs whose Arabic matched, then the
  translation, the word meanings, and the tafsir, each kind in the Book's
  own order.
- **Recitation.** Minshawi and Husary, one surah at a time, asked for once:
  one offer names the reciter, the surah, and the size, the reciter can be
  swapped in that offer, and the word being recited is washed as it is read.
  A tap in the word by word aid loops that one word. The page can follow
  the reciter, and an offer you do not want is one tap from gone. The pace
  and what happens at the end of the audio sit on the playing pill itself
  and in Settings as the default: the ayah again, the surah again, or the
  next surah on its own.
- **A library you choose.** The app ships the Quran text and its page layout
  and nothing else. Translations, tafsirs, word lists, and recitations are
  added when the reader wants them, from the project's own Releases, with
  the size shown first and the file verified by SHA-256. Installed packs can
  be read back and checked against what was published.
- **Readable in every room, on every setting.** Four themes (paper, sepia,
  night, black), an automatic night mode that follows the system when asked
  to, a text size of your own (0.65 through 1.2) for the Quran text, the
  translation, the tafsir, and the word meanings, a settings hub that shows
  the state of each choice on its own row, secondary text above 4.5:1
  contrast in all four themes, and every control a real touch target. More
  than one translation may be on at once, each in its own place under the
  ayah.

## The content

The Arabic text is the KFGQPC Hafs text used with the King Fahd Glorious
Quran Printing Complex fonts, audited word by word against the Tanzil
Project text. The translation is Saheeh International, issued by Noor
International Center and distributed by QuranEnc.com. Scripts, layouts,
tafsirs, metadata, and recitations come from the Quranic Universal Library
(QUL) by Tarteel. Every dataset carries its source, version, and credit, and
every license is honored: see [`docs/content-sources.md`](docs/content-sources.md).

| Language | Translation | Tafsir | Word meanings |
|---|---|---|---|
| English | Saheeh International | Ibn Kathir | yes |
| Arabic | the Quran itself | As-Sa'di | |
| Bangla | Taisirul Quran | Ibn Kathir | yes |

## How it is built

Thirteen Gradle modules with dependencies pointing one way, and a JVM
content pipeline. Every sentence the reader can see lives in a `strings.xml`
in the module that draws it, so the interface can be translated without
hunting through Kotlin. The module rules are in
[`docs/architecture.md`](docs/architecture.md), the design constitution in
[`docs/design.md`](docs/design.md), and the reasons behind every decision in
[`docs/decisions.md`](docs/decisions.md).

```
:core :data :content-assets :ui-kit
:feature-mushaf :feature-study :feature-search :feature-browse
:feature-playback :feature-settings
:app            the wiring: activity, view model, screen, manifest
:tools          the content pipeline (verify, audit, build, packs, audio)
:benchmark      the startup profile's generator, never shipped
```

## Building it

```bash
# A fresh clone needs the content packs, the database, and the fonts.
./gradlew :tools:run --args="fetch"          # downloads and verifies them

./gradlew :core:test :data:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:lintDebug :app:assembleDebug # a debug APK (carries every pack)
./gradlew :app:bundleRelease                 # the signed release bundle
```

The release bundle carries only the core pack, about ten megabytes of Quran
text and page layout; the 604 page fonts are the bulk of the app, because it
must render the Book with no network. The content pipeline (`verify`,
`audit`, `build`, `checkdb`, `search`, `fonts`, `packs`, `audio`) runs on the
maintainer's machine, where the raw sources live, and
[`AGENTS.md`](AGENTS.md) is the runbook: which gate needs which files, what
CI runs, and what a release owes.

## Privacy

The app collects nothing. The policy is
[online](https://muntasimulhaque.github.io/quran/privacy.html) and
[in this repo](docs/privacy.md). The network is used only for content from
the project's own Releases: a pack or a recitation package the reader asks
for, and a quiet refresh of an installed pack when a newer version ships, on
Wi-Fi only. Android's cloud backup and device transfer are refused
explicitly, so the reader's saved ayahs and notes stay on the device and move
nowhere.

## License

The code is MIT: see [LICENSE](LICENSE). The Quran text, translation, tafsir,
fonts, and recitations are not ours to license and keep their own terms and
credits, recorded in [`docs/content-sources.md`](docs/content-sources.md).
