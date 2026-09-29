# Quran: The Noble Book

A free, open source Quran reader for Android. It opens on the page you left,
turns like paper, and keeps the Quran text itself at the center: no ads, no
trackers, no accounts, nothing collected, ever.

**In the tree, riding the next release.** The reading answers the reader:
word by word is a grid instead of a heap, the ayah card opens with its ayah,
the mushaf page is ruled and has an edge, the bar's scrim no longer dims the
page's own surah name, a word in the study reading can be heard again and
again, a footnote marker is a raised figure again, a sheet is a page rather
than a floating card, there is a heading for a screen reader to jump by, the
store's first frame is a full page, a startup profile is wired and its cost
measured, and Bangla is set in a face the app chose rather than in whatever
the phone happened to ship. D-122 has the reasoning; no `versionCode` moved
for any of it.

**Status:** 3.0 (versionCode 31) is submitted to Google Play for review
(D-118, D-119, D-120), carrying the thirty-eighth session's three reports:

- The end of the recitation is one answer, not three switches: repeat the
  ayah, repeat the surah, or carry on to the next surah. Turning one on turns
  the other two off, and a surah that repeats begins again at its first ayah
  (D-118).
- The playing pill's words keep a measure or they take a line of their own, so
  a surah name, its ayah, the pace, and the repeat are never cut with an
  ellipsis on a phone (D-119).
- A settings row ends with the mark it actually has: a row with a switch ends
  with the switch, a row without ends with its arrow, and the 64 dp of empty
  margin at the end of six rows is gone (D-120).

**2.9** is submitted to Google Play for review and carries the
thirty-seventh session's three reports (D-114, D-115, D-116):

- The daily ayah arrives at the minute the reader chose, where the phone gives
  the app the exact time it needs to say so even on a locked, sleeping phone,
  and the reminder keeps working where the reader does not (D-114).
- The app follows the phone's own day and night, at the moment it changes,
  with the switch on (D-115).
- The settings sheet reads as a list: no squeezed names, no rows that explain
  what their own switch already shows, three quiet groups, and every step of
  a text size or a pace a proper target to tap (D-116).

**2.8** carries (D-111, D-112, D-113):

- Settings is tidier: on every row that carries a switch and a door, the
  switch is now the last mark, so all switches run down one line and all
  chevrons another.
- The app has a new face: the icon and the feature graphic are the shamsa at
  the center of a mushaf cover, in flat champagne gold on ink navy.

**2.7** carries (D-108, D-109, D-110):

- Search leads with a surah name when a bare number can read as one, then a
  reference, so "2:255" still opens the verse and a surah name lands on the
  surah.
- The Bangla surface had a wording pass, and the daily reminder's text is
  centered.
- A pack the reader already has is refreshed quietly when the app carries a
  newer version, on an unmetered connection only and only after the first
  page is readable; the manual Check installed content repairs what it finds
  (D-109 amends the offline rule).

**2.6** carries (D-105, D-106):

- Settings reads tighter: the list rows keep their compact height again,
and the switch for a translation, a tafsir, or the daily reminder lives
on its hub row alone, so a page that lists them no longer repeats it.
- Continue to the next surah, off by default, plays the surah after the
one being heard on its own, downloading it with the reciter in use when
it is not on the device. The Continue offer stays for readers who leave
it off.
- The English Ibn Kathir tafsir carries QUL's correction of a typo at
2:238, and the content version is 1.0.1.

**2.5** carries (D-103, D-104):

- The shared text of an ayah reads left to right. A message carries one
direction, and it was the Arabic ayah that set it, so the English
translation ended at the right edge and its closing period was swept to
the front of the sentence. The text now opens with one invisible
left-to-right mark: the block sits on the left, punctuation stays where it
was written, and the Arabic still shapes right to left inside its line.
- The ayah grid's header names the surah in the Browse list's own type. The
name, the place, and the Arabic name are one pair of composables drawn by
both surfaces, so the same surah no longer changes size one tap after its
row.
- Every settings switch ends at one line and every chevron sits at one
place. A row that carries only a switch keeps the chevron's slot empty, so
the controls read down one column whatever a row carries.

**2.4** carries the simplification report (D-101, D-102):

- A surah row is the door to its own ayahs. Browse has four tabs now
  (Surahs, Juz, Last Read, Saved), and a tap on a surah opens its 48 dp
  number grid with the reader's own place marked and in view. The separate
  Go to Ayah tab is gone, and the grid marks the place the reader left in
  that surah even when they are standing in another one.
- A note is written on a kept ayah. Saved is the one list, with the note
  previewed under the places that have one, ordered by the most recent of
  the two moments. The pill keeps its Note action; writing a note saves the
  ayah with it, and removing a save that carries a note asks first in a
  small sheet that shows the note. saved.db is version 5, and note-only
  rows are brought into Saved by the migration so nothing is lost.
- The search word meanings stay, with the matched-word refinement from 2.3,
  because they reach ayahs the translation alone does not (D-099's numbers,
  restated in D-101).
- The settings chevron sits after the switch on the three combined rows, so
  the name and its control stay together and the door is the row's last
  mark.

**2.3** answers the reader's tenth report (D-099):
- A switch is a switch and the row around it is a door: only the switch
toggles, and the rest of the row opens the page, on Show translation, Show
tafsir, and the daily reminder alike. The list pages carry their own switch
at the head, so a page behind a switch is never a dead end.
- The reading builds in the order the reader puts it together: word meanings,
  then translation, then tafsir.
- The daily reminder now comes on with the app, its moment is chosen on its
own page on the platform's clock (typed input included, so every minute of
the day is reachable), and the page says so plainly when the phone has
turned notifications off, with the way out named.
- Go to Ayah's card carries the surah's Arabic name and its length, its ayah
  cells are 48 dp, and its surah step opens on the reader's own surah, marked
  with the same shape the grid uses for their own ayah.
- Search keeps its word meanings and stops printing Arabic that matched
  nothing: the Arabic is drawn when it is the match, when the row has no
  other evidence, and otherwise the row points at the word that earned it.
- The Bangla interface reads প ্ৰত িদ িন ের আয ় for the daily reminder,
  including on the notification channel already on a reader's phone.

**2.2** answers eight things from the owner:
- The Go to Ayah picker centers the reader's own ayah, so it is unmistakable
  instead of sitting at the viewport's top edge.
- Search results no longer repeat a translation match as a separate word
  meaning block; the meaning is drawn only when it is the row's only
  evidence of the match.
- Show translation and Show tafsir each carry the chevron that opens their
  own list, so one decision is one row.
- A long ayah's share card is captured whole on every device by stitching
  software slices, and the share sheet's preview scrolls it instead of
  clamping it.
- The theme swatch shows the page actually drawing when automatic night mode
  is on, with the day choice named beside it.
- The Bangla interface keeps one spelling of Tafsir (তাফসীর, the QUL
  corpus's own) and reads অডিও on the Listening page.
- A new daily reminder sends one ayah a day at the reader's own hour, with
  the translation when they read with one and a tap that opens that ayah in
  the study reading. The reminder is offline, adds no permission and no
  dependency, and is off until the reader asks for it.

**1.9**, submitted before it, carries the twenty-seventh session's
craftsmanship pass (D-086 through
D-088): every quiet text tone was measured against the design document's own
4.5:1 rule and raised, the ornament gold now meets it too, the color scheme
names every role Material draws from so nothing falls through to a default,
the study reading and the settings sheet hold a readable measure on a wide
screen instead of running the width of the glass, a floating control wears a
floating tone, the study surah opening is drawn to the printed page's model,
the study block answers TalkBack's action the way the Mushaf does, and
Settings gained a Listening page with a remembered recitation pace and ayah
repeat. The screenshot workflow's own bug, which had silently destroyed every
red leg's frames, is fixed and its failure modes are catalogued in
[`docs/screenshot-failures.md`](docs/screenshot-failures.md). 1.8, submitted
for review before it, moved Go to ayah into Browse's tab row and named the
deep door by what is behind it. All five owner content gates are green on the
restored sources. The version name follows the runbook: 1.0, 1.1 through
1.9. The bundle carries only the core pack, with everything else added from
the project's own Releases when a reader asks for it. The listing, icon,
feature graphic, and screenshots per form factor are in
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
  behind their markers. An optional switch shows each word with its meaning
  beneath it, in the language of the translation above it.
- **A reading place you can go back to.** The app opens on the ayah you left.
  Browse keeps the last twenty places you read, newest first, each with the
  mode you were in and when you left it, so a surah you visited last week is
  one tap away again.
- **Notes you can find again.** Write a note on any ayah from the pill a long
  press raises. Browse's Notes tab lists every ayah you wrote one on, newest
  first, and a tap opens the note over its ayah.
- **Any ayah, without scrolling.** Browse carries a Go to Ayah door in its
  tab row: your own surah is already chosen, another is one tap through the
  same list, and a tap on a number opens that ayah.
- **Two readings, one door.** The Mushaf and the study view are one door in
  the top bar: the icon shows the reading the reader is not in, and a tap
  takes them there. There is no bottom bar: the index, search, saved ayahs,
  and settings are all one tap from the same edge, and listening is the play
  action on any ayah.
- **A language chosen once.** A first-launch screen offers English or Bangla,
  each named in its own script, and the choice sets the interface, the
  translation, the tafsir, and the word meanings together. It can be changed
  at any time from Settings, and the whole app follows without a restart.
- **The ayah card.** Save, note, share, play from this ayah, the word
  meanings, and every tafsir you have installed. It offers to add what it
  does not have yet rather than showing an empty panel. Share shows the card
  as it will be sent, then sends the picture or the words, never both at
  once.
- **Search.** Arabic text, every enabled translation and tafsir, word
  meanings, surah names, and references like `2:255` or `baqara 255`, with a
  filter row under the field for narrowing the sources when a query returns
  too much. Results are exact and instant, even over a forty megabyte tafsir,
  and they run from the verse outward: the reference you typed, the surah you
  named, the ayahs whose Arabic matched, then the translation, the word
  meanings, and the tafsir, each kind in the Book's own order.
- **Recitation.** Minshawi and Husary, one surah at a time, asked for once: one
  offer names the reciter, the surah, and the size, the reciter can be swapped
  in that offer, and the word being recited is washed as it is read. The page
  follows the reciter if you ask it to. An offer you do not want is one tap
  away from gone. The pace and what happens at the end of the audio sit on the
  playing pill itself while a recitation plays, and in Settings as the
  default for the next one: the ayah again, the surah again, or the next
  surah on its own.
- **A library you choose.** The app ships the Quran text and its page layout
  and nothing else. Translations, tafsirs, word lists, and recitations are
  added when the reader wants them, from the project's own Releases, with the
  size shown first and the file verified by SHA-256. The app can also read
  every installed pack back and tell you if one no longer matches what was
  published.
- **Readable in every room, on every setting.** Four themes, an automatic night
  mode that follows the system when the reader asks it to, a size of your own
  (0.65 through 1.2) for the Quran text, the translation, the tafsir, and the
  word meanings, a
  settings hub with the state of each choice on its row, secondary text above
  4.5:1 contrast in all four themes, and every control a real touch target.
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

| Language | Translation | Tafsir | Word meanings |
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
and [in this repo](docs/privacy.html). The network is used only for content
from the project's own Releases: a pack or a recitation package the reader
asks for, and a quiet refresh of an installed pack when a newer version
ships, on Wi-Fi only. Android's cloud backup and device transfer are refused
explicitly, so the reader's saved ayahs and notes stay on the device and move
nowhere.

## License

The code is MIT: see [LICENSE](LICENSE). The Quran text, translation, tafsir,
fonts, and recitations are not ours to license and keep their own terms and
credits, recorded in [`docs/content-sources.md`](docs/content-sources.md).
