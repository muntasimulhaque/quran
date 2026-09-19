# Decisions

Why this, not the alternatives. History, not rules: rules live in
AGENTS.md, behavior lives in the code. Add an entry when a choice is made
that a future session could not recover from the code.

## D-001: The name is Quran: The Noble Book

Date: the first session.

The owner required the name to contain Quran, first, second, or after a
colon. Exact single-word names are all taken on Play. The colon pattern
turned out to be the only clean space: "Quran: The Noble Book" had zero
results on Google Play, the App Store, and exact-phrase web search, and
"The Noble Book" is the English of Al-Quran al-Karim (56:77), one of the
Quran's own names.

The three strings, which differ and are never merged:

- store title: `Quran: The Noble Book` (21 characters, inside Play's 30)
- app title: `Quran`
- launcher name: `Quran`

Package and repo: `io.github.muntasimulhaque.quran`,
`github.com/muntasimulhaque/quran`.

Rejected, all checked: Wird (the owner did not know the word and judged
it obscure to others), Just Quran ("Quran alone" carries a theological
connotation the app must not imply), Quran at Hand, Quran Moments,
Beacon Quran, Quran Daily, Quran Reader, Quran Every Day, Timeless Quran,
Living Quran, One Quran, Quran Path, Quran Compass, and every single-word
Arabic name tested (Furqan, Bayan, Dhikr, Nur, Huda, Suhuf, Sakinah, Iqra,
Tibyan, Mubin, Tanzil, Kitab).

## D-002: The engineering law is the family law, adapted

Date: the first session.

The owner adopted, from the existing apps, the product purity rules (no
network, no ads, no trackers, no accounts, no third-party SDKs beyond the
chosen AndroidX set, no crash, no AI slop, accessibility as a rule,
English UI), the architecture rules (pure JVM `core`, host separated from
domain, composables take state and callbacks and never a ViewModel, small
files, code as the source of truth, thin AGENTS.md, ADRs here, one source
of truth for shared constants), and the verification rules (tests as
guards, content invariant tests, lint that fails on new issues, CI-built
store screenshots, version discipline, plain commits, no AI attribution).

Nothing visual carries over from any other app. The design language is
created fresh for the Quran app and recorded in D-010.

## D-003: Content comes from QUL and QuranEnc, and existing licenses are honored

Date: the first session.

The owner decided not to seek separate permissions. The content spine is
the Quranic Universal Library (QUL, by Tarteel), used as designed: its
downloadable, proofread datasets are meant to be packaged with apps. The
Saheeh International translation comes from QuranEnc, whose published
terms already grant republishing. All licenses that do exist are honored
because it costs nothing: Tanzil attribution and link, QuranEnc credit and
version and footnotes, KFGQPC font notices, QUL and Tarteel credit, and
reciter names on the audio screens.

Two safeguards make this decision safe to live with:

1. Every dataset is swappable. The content database is built by `tools/`
   from `content/manifest.json`, so replacing one translation, tafsir,
   recitation, or script is a data change, never an app change.
2. About carries a corrections and rights contact row. If any rights
   holder ever objects to a bundled dataset, it is removed in the next
   update.

## D-004: Saheeh International is the only translation

Date: the first session.

Shipped from QuranEnc as `english_saheeh`, version 1.1.2, issued by Noor
International Center. Verified against the known Saheeh text at 1:1, 1:2,
2:255, 18:10, 112:1, and 114:6. The translation has footnotes and they
ship with it, rendered per ayah, because QuranEnc's terms require keeping
the transcript information and the version.

The About screen credits: "Saheeh International. Issued by Noor
International Center. Distributed by QuranEnc.com. Version 1.1.2."

Rejected: shipping several translations at launch (the owner chose one,
done well), and using QUL's untagged Saheeh copy when a versioned,
explicitly republishable edition exists.

## D-005: The tafsirs are Ibn Kathir in English and As-Sa'di in Arabic

Date: the first session.

Ibn Kathir (English, QUL resource 35) is ayah-linked and group-aware. Its
export leaves 4334 ayah rows without their own text, but every one of them
resolves through `group_ayah_key` to one of the 1902 passages that carry
text, so the English tafsir covers all 6236 ayahs through groups and the
verify step proves it.

The Arabic tafsir is As-Sa'di from QuranEnc (`arabic_saadi`, version
1.0.0, downloaded from the range API), not from the QUL export. The QUL
export leaves 59 ayahs with no passage to resolve to, a dead end the app
must never show; the QuranEnc edition covers all 6236 ayahs through 6514
passage ranges. This also keeps both tafsir sources and the translation
under the same QuranEnc republishing terms.

The QUL As-Sa'di export stays in the raw folder as a cross-check only.

An English translation of As-Sa'di does exist: "Tafseer as-Sa'di", 10
volumes, translated by Nasiruddin al-Khattab, edited by Huda Khattab,
English Edition 1, 2018, International Islamic Publishing House, and its
copyright page reserves all rights. There is no licensed digital edition;
the only digital copies are unofficial scans, and OCR of a scan would
introduce exactly the errors this app promises never to have. Rejected.
If the owner ever wants it, the path is a written request to
editorial@iiph.com and a digital text supplied by the publisher.

## D-006: The Arabic text runs on the KFGQPC spine, audited against Tanzil

Date: the first session.

The canonical working text is the KFGQPC Hafs script in word-by-word form
(QUL resource 312), because it is matched to the KFGQPC Uthmanic Hafs
font used in study mode and carries the word positions that page layouts,
word-by-word meanings, and audio segments all join on.

Tanzil Uthmani 1.1 is the independent audit reference, never shipped as
the display text. The build compares the two at word level, lists every
difference, and either resolves or documents each one. Attribution to
Tanzil and a link to tanzil.net appear in About and the README, as their
license requires.

## D-007: Recitation is Minshawi and Husary, bundled, with background playback

Date: the first session.

Reciters: Muhammad Siddiq Al-Minshawi (murattal) and Mahmoud Khalil
Al-Husary (murattal and the Muallim teaching set), with Husary mujawwad
as an optional later set. Source: QUL ayah-by-ayah recitation exports
(resources 108, 110, 112, optionally 111), which carry segment timings for
word and ayah highlighting. The audio files are downloaded once from the
URLs recorded in the manifest, checksummed, and bundled.

Background playback is approved, so exactly three permissions are added:
`POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, and
`FOREGROUND_SERVICE_MEDIA_PLAYBACK`. `INTERNET` remains absent forever.

Delivery: install-time Play asset packs, one per reciter, so every
recitation is offline from first launch with no Play Core library and no
in-app download flow. High bitrates from the source are re-encoded for
pack size only if the owner approves the encoding decision later.

## D-008: Reader first, two modes, no tab bar

Date: the first session.

The app opens where the reader left off. There is no home screen, no
dashboard, and no permanent tab bar. Index, search, library, and settings
are sheets raised from a slim bar.

Two modes share one position, one set of bookmarks, one audio queue:

- **Mushaf mode**: the 15-line page, glyph-rendered, swiped, zoomable.
- **Study mode**: ayah-by-ayah scroll with translation, word-by-word, and
  tafsir, honoring the system font scale.

Rejected: a three-tab layout, a card dashboard, and a home screen that
asks the reader to choose before reading.

## D-009: Daily reading, never engagement

Date: the first session.

A daily portion (pages per day or a finish-by date), a quiet reminder at
a chosen time, a progress view, and a widget. No streaks, no badges, no
scores, no social features, and no language of guilt when a reader has
been away. Returning after a month shows the page they left, nothing
more.

## D-010: The design language is "manuscript, not dashboard"

Date: the first session.

The app should feel like a finely printed book. Typography is the hero;
chrome gets out of the way.

- Quran in study mode: KFGQPC Uthmanic Hafs, generous line height,
  right aligned, no letter spacing, no faux bold or italic.
- Mushaf: the QPC V2 page fonts at page width, pinch zoom.
- Latin reading (translation, tafsir): Literata (OFL).
- UI: Inter (OFL).
- Palette "lapis and parchment": warm paper background, ink text, lapis
  as the single interactive accent, and gold reserved for Mushaf
  ornaments only. Four themes: Paper, Sepia, Night, Black.
- Motion: horizontal page turns in Mushaf mode, plain scroll in study
  mode, no parallax, standard sheet springs, and respect for the system's
  remove-animations setting.
- Icons: a custom line set drawn for this app, no Material defaults.

## D-011: Mushaf rendering is text-native QPC V2 glyphs, with a page bitmap cache

Date: the first session. Confirmed by prototype.

The prototype rendered pages 1, 2, 3, 42, 293, 400, 500 and 604 from the
QPC V2 glyph data on an API 35 Pixel 4 emulator, in a throwaway Compose
app outside the repository. What it proved:

The page fonts are pre-justified. A full page's lines each sum to about
39,000 font units at 2,500 units per em, which is 15.6 em per line, so no
justification engine is needed: concatenating the word glyphs at one font
size fills the line exactly. The word text is a sequence of one or more
Arabic presentation form codepoints, and the same codepoints map to
different glyphs on every page, so the page font is the coordinate that
makes them meaningful. Pages 1 and 2 use 8 lines and are centered; every
other page uses 15 justified lines. A page renders to a bitmap in 9 to
120 ms on the software emulator, font load is 2 to 21 ms per page, and
the app sits near 50 MB PSS.

The decision: Mushaf mode renders the glyph text natively and caches each
page as a bitmap, rendered off the main thread, with neighbor pages
pre-warmed. The page image fallback is dropped. Word-level styling spans
are safe because a word is a complete glyph or glyph sequence; sub-word
spans are still forbidden.

Tajweed stays out of launch scope, unchanged: the V4 tajweed fonts remain
disabled upstream pending proofreading (D-005 of the QUL record), and any
future tajweed mode would use the font's own embedded color, never
sub-word spans.

## D-012: English UI, Arabic content, RTL stays on

Date: the first session.

The interface is English. Arabic is the sacred content, not a localization
target, and is never translated in the UI chrome. RTL layout support
stays enabled because the content's direction is intrinsic, even though
the UI itself is left to right.

## D-013: Play target audience is all ages, not Families

Date: the first session.

The app targets all ages with a standard content rating and is not
enrolled in the Play Families program, because it is a general reading
app with no child-directed design, no ads, and no data collection. The
Data safety declaration is "no data collected or shared".

## D-014: The text audit gate

Date: the first session.

The content pipeline audits the canonical KFGQPC word text against the
independent Tanzil Uthmani 1.1 edition, ayah by ayah, at letter level:
diacritics, Quranic signs, tatweel and the alef, waw, yeh and heh forms
are folded away by `core.Arabic.skeleton`, so only the letter body is
compared. The audit fails the build on any unexplained difference.

Result of the first full run: 6236 ayahs compared, 6235 letter-identical,
zero unexplained differences. Two notes, both recorded in
`content/audit-report.md`:

- 2:72 is an accepted orthographic variant: the hamza of
  fa-iddarra'tum is a standalone letter in the KFGQPC edition and a
  combining mark in the Tanzil edition. Both are attested Uthmani
  orthography. It is allowlisted by ayah reference with a written reason,
  and the allowlist must stay this short.
- 37:130 is a segmentation note only: the letters agree, the two editions
  break the ayah into words differently. Word breaks never affect the
  canonical text; they affect word-by-word pairing, which is built from
  the KFGQPC side.

Word totals also agree: 77432 KFGQPC words against 77433 Tanzil tokens
after removing the 6236 ayah number markers from one side and the pause
mark tokens from the other; the one-word gap is the 37:130 break.

## D-015: The content database and its build

Date: the first session.

`./gradlew :tools:run --args=build` writes `content/build/quran.db` from
the verified sources. Tables: `surah`, `ayah`, `word`, `page_line`,
`translation`, `tafsir_passage`, `tafsir_ayah`, `surah_info`,
`recitation`, `recitation_ayah`, `meta`.

Properties proven by the first full run:

- Deterministic: two consecutive builds produced byte-identical files,
  43,933,696 bytes, sha256
  `00f9afa5f844e77138fd13a1929dc8e5300331f3b2c1f64e4d5af44a0e347d57`.
- Search columns are precomputed with `core.Arabic.normalizeForSearch` on
  the ayah text, the word text, and the translation, so a query typed at
  runtime meets an index built with the same rules.
- Tafsir resolution: Ibn Kathir's 1902 passages map all 6236 ayahs through
  their group keys; As-Sa'di's 6514 overlapping passages resolve to the
  shortest passage containing each ayah, which is the most specific one.
- Recitations: four reciters with 6236 rows each, segments included, and
  audio paths relative to the pack root.
- Fonts: 628,169 study codepoints covered by Uthmanic Hafs; 88,186 glyph
  codepoints across 604 pages covered by their page fonts.

Three data-shape traps surfaced during this work and are now guarded:

1. The QUL recitation export's `ayah_number` column is a global 1..6236
   counter, not a per-surah ayah number. The real coordinates are the
   three-digit surah and ayah in the audio file name (002001.mp3). Verify
   checks both the file name against the surah column and the counter
   against the global index, and the build derives the join from the
   file name.
2. As-Sa'di's range API returns overlapping passages, so a naive mapping
   violates the primary key and can attach the wrong commentary. The
   shortest passage containing the ayah wins.
3. The KFGQPC source text is not NFC-normalized, and it must never be:
   normalization happens only on derived search columns, never on the
   text that is displayed.

The built database is a build artifact under `content/build/`, currently
gitignored. Whether the repository commits the built database, the raw
sources, or both is decided when the app module lands.

## D-016: The database is committed, the page fonts live in a Release

Date: the first session.

The built content database ships in the repository at `content/quran.db`
(43,933,696 bytes, sha256 `00f9afa5...`). A fresh clone can build the app
immediately, and the exact shipped content is guaranteed for everyone.

`content/build-report.json` records the database's size, hash, and row
counts, and `tools checkdb` verifies the committed database against it
without needing any raw source, so drift is detectable in a fresh clone
and in CI.

The 604 page fonts (136 MB zipped, 208 MB unpacked) ship as a GitHub
Release asset on the `qpc-v2-fonts` release, pinned by SHA-256 in
`content/manifest.json`. `tools fetch` downloads any dataset that carries
an `assetUrl`, verifies the pinned hash, and does nothing when the file is
already present and correct. The first build on a new machine fetches the
pack once, about a minute; every build after that is offline. Copying the
zip by hand into `content/raw/qul/` is an equally valid route, because
fetch verifies before it uses.

The release asset must never be deleted or replaced: the pinned hash is
the only guard between a fresh clone and silent drift.

Rejected: committing the 208 MB of fonts (heavy clones and heavy history),
Git LFS (extra setup and storage quota for every contributor), and
downloading the fonts from the original QUL page on every build (needs an
account and is not reproducible).

## D-018: The 604 page fonts ship in the base app

Date: the first session.

Three deliveries were measured against the real bundle:

- Fonts in the base: base module 144.7 MB compressed (the database is
  13.3 MB of it), one install, no extra library, no added permission (the
  only entry is the app private DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
  from androidx core), and a release APK is a complete, sideloadable
  Quran. Built release: 151,980,003 bytes, 947 ms cold start on the API
  35 emulator, no crash.
- Install-time asset pack: the same total install size as the base, one
  more module, no user benefit.
- Fast-follow asset pack: a 14.9 MB base listing and a 129.8 MB pack
  after install. It requires Play Core, which pulls WorkManager and Room
  and merges five permissions (FOREGROUND_SERVICE,
  FOREGROUND_SERVICE_DATA_SYNC, WAKE_LOCK, ACCESS_NETWORK_STATE,
  RECEIVE_BOOT_COMPLETED), and the first R8 release crashed inside
  WorkManager until keep rules were added. Fixable, but it buys a smaller
  store listing with library weight, permissions, a preparing state, and
  a release APK that is incomplete outside Play.

Decision: the fonts ship in the base. The app stays one self-contained
install of about 145 MB, dependency free beyond AndroidX, permission
clean, side loadable, and offline. Audio packs are a separate decision
when the recitations land, because their size and the wish to choose
reciters actually change the question.

## D-017: Signing uses the shared upload keystore

Date: the first session.

The owner confirmed the app signs with the shared upload keystore in the
vault (the `Google Play Signing Key` folder under `My Apps` on D:, the
same upload key the family's other apps use). The build probes each drive
and each layout the folder has worn, and resolves the store file against
the properties file's own directory when the recorded drive letter is
wrong. The keystore and its properties never enter the repository; CI
gets them from secrets. An absent keystore means an unsigned release
build, never a failed one. Play App Signing holds the app signing key, so
the upload key can be reset with Google if it is ever lost.

Verified on the first release build: the APK is signed, certificate
SHA-256 `537d09d20300129e973b7945316bfe24cfadcfbc77eec5229cbf30170d9de521`.

## D-019: The study card is how one ayah opens

Date: the second session.

Tapping an ayah in study mode opens one bottom sheet built from four
pieces, in this order: the reference, the five actions, the Arabic ayah,
and the selected panel. The actions are pills: Copy, Share, Words, Ibn
Kathir, As-Sa'di. The three reading pills behave as tabs and swap the
panel under the ayah; tapping the active one returns to the translation.
Copy and Share write the Arabic, the translation without its footnote
markers, and the reference; a dangling `[7]` in a shared message would
confuse the reader on the other end. The sheet opens expanded and wraps
its content height, so a short ayah gives a short card.

Text rendering rules this decision fixes:

- Translation footnote markers become superscripts in the primary color,
  and every ayah with footnotes shows them underneath with a hanging
  indent. The reader never sees brackets in the running text.
- The tafsir HTML is parsed by `core/RichText.kt` (paragraphs, headings,
  line breaks, bold, italic, Arabic quotes) into runs; the gate checks
  every run against the font it will be drawn with.
- Arabic outside the Mushaf is Amiri Quran. A codepoint Amiri cannot draw
  falls back per codepoint to the bundled Hafs font. The tafsir source's
  pre-shaped presentation forms are unfolded at display time with NFKC,
  except the Prophet's ligature, which is kept as one glyph.
- The study page lives in the same pager as the Mushaf page, so swiping
  moves both modes and they share the reader's position.

Verified on the emulator: 1:1 and 33:21-22 render their translation,
footnotes, word-by-word meanings, Ibn Kathir (including its Arabic hadith
quotes and the Urdu blessing), and As-Sa'di Arabic with its Quran quotes
set apart; copy and share produce the expected text; the study page
swipes to the next page; `tools fonts` checks 12,865,369 reading
codepoints across translation, both tafsirs, word meanings, and surah
names, and fails if any codepoint has no bundled font.

## D-020: Search is one field, two languages, no tafsir yet

Date: the second session.

Search lives in a full-height bottom sheet with one field on top. The
query language is detected, not chosen: any Arabic letter means the query
is Arabic, everything else is English. Arabic terms run through the same
`Arabic.normalizeForSearch` that built the ayah column, so marks and
letter forms never stand between the reader and the text. English terms
run through `Search.normalizeEnglish` over a folded copy of the whole
translation held in memory for the session, because the translation
writes Allāh, ʿĪsā and Mūsā while readers type allah, isa and musa.
Surah names match the same way, and a Latin query can return surahs above
the ayah results.

Results stay in Mushaf order, cap at 200, and mark matched words as whole
words: Arabic through the word table's positions, English through folded
word tokens. Nothing is ever styled inside a word. Tapping a result jumps
to the page and opens the study card for that ayah, so search is a way
into the text, not a list beside it.

Tafsir search is deliberately not here yet: the tafsir tables carry no
normalized column, and adding one means rebuilding the content database
and its pinned hash. That is a separate decision when the owner wants it.

Verified: `tools search` passes 63 Arabic round trips, audits all 16
distinct non-ASCII codepoints of the translation into clean ASCII folds,
and proves plain words (allah, mercy, moses, paradise, pharaoh) match. On
the emulator: mercy returns 144 matches, allah 200+, allah mercy 55 with
both words highlighted, kahf returns Surah Al-Kahf, and a result tap on
2:64 lands on its page with the study card open.

## D-021: Bookmarks and notes are one saved row per ayah

Date: the second session.

Saving and noting are one act with two depths. The study card gains a
Save pill that toggles, and a Note pill that opens an editor inside the
card; saving a note saves the ayah with it. There is no separate
notebook, no folders, and no second list: the row is the ayah, its
optional note, and when it was saved. A blank note clears the note and
keeps the ayah; removing the row removes both.

The store is hand-rolled SQLite in `data/SavedStore.kt`: one table, one
schema version, no code generation, no KSP, and no dependency on the
content database, so a content rebuild can never disturb the reader's own
work. It exposes a `StateFlow` so the card and the list are always in
step, and its ordering is newest first with the ayah number as the
tiebreaker.

The surah index grew into the browse sheet to hold them: one sheet, two
tabs, Surahs and Saved. The top bar action was renamed from Index to
Browse. Saved rows show the reference, the Arabic ayah, and the note;
tapping one jumps to the page and opens its card; Remove drops it.

The behavior is pinned by six instrumented tests
(`data/src/androidTest/.../SavedStoreTest.kt`, run with
`./gradlew :data:connectedDebugAndroidTest`): toggle, note kept with its
ayah, blank note clears without dropping the ayah, newest first,
surviving reopening, and removal. Verified on the emulator: save and
unsave from the card, a note written and shown under the ayah in the
Saved tab, the row opening its card and showing the saved state, the note
surviving a force-stop and relaunch, and Remove returning to the empty
state.

## D-022: Recitation is a Media3 service over per-ayah files

Date: the second session.

The canonical text has word timings for four recitations (Minshawi
murattal, Husary murattal, Husary Muallim, Husary mujawwad) that QUL
publishes as per-ayah MP3s on `audio-cdn.tarteel.ai`. The app plays those
files through a Media3 `MediaSessionService`: one notification, one
foreground service with type mediaPlayback, and the three permissions the
owner approved in D-007. The app itself never touches the network.

Playback builds a playlist one surah at a time from the files actually on
the device, appends the next surah as the reader reaches the end, and
follows the recitation: the study page moves to the playing ayah, the
ayah is tinted, and the word being recited is marked in lapis from the
segments. The playback pill carries the reciter, the reference, and
previous, play/pause, next, and stop; tapping the reciter opens the
picker, which names the recitations and marks the ones missing from the
device. The chosen reciter is remembered with the reading position.

Development runs on a sample: `tools audio` downloads 24 spread ayahs for
every recitation (17 MB) into `content/work/audio-dev`, and only debug
builds bundle it. Release builds carry no audio yet, so the playback pill
says "Not on this device"; that is deliberate until the delivery
decision below.

Measured with a sampled HEAD sweep of the real CDN (312 files per
reciter, no failures):

| Recitation | Hours | Estimated size |
| --- | --- | --- |
| Minshawi murattal | 28.6 | ~1.58 GB |
| Husary murattal | 42.2 | ~2.36 GB |
| Husary Muallim | 47.1 | ~2.62 GB |
| Husary mujawwad | 61.2 | ~3.32 GB |

The delivery decision is the owner's, and is the only open item in this
decision. The options on the table: an install-time Play asset pack (one
large install, no extra permissions, no library), or a pack published as
a GitHub Release asset that the reader imports once (small Play download,
no INTERNET permission, one manual step). The import path reads a ZIP
through the storage picker and writes the files into app storage; no
network code exists in either option.

Verified on the emulator: playback starts from the study card and the
card closes so the reader follows; the notification appears with the
reference and the reciter; the playlist crosses from Al-Fatihah into
Al-Baqarah unattended; the page follows, the ayah is tinted, and the
recited word is marked; the reciter picker lists all four with the
playing one marked and switching restarts at the same ayah; and the
release build starts in 1.5 s with no crashes. The instrumented search
test (`app/src/androidTest`, 3 of 3) and the saved-store test
(`:data:connectedDebugAndroidTest`, 6 of 6) are green.

## D-023: Recitation is downloaded per surah, on the reader's word

Date: the second session. Approved by the owner in these words: nothing
ships by default; playing an ayah downloads only that surah, for the
chosen reciter; other surahs wait until the reader asks for them; and
`INTERNET` is approved for exactly this. This decision resolves the
delivery question D-018 left open.

What the reader sees: tapping Play on a surah that is not on the device
shows the surah's name and size in the playback pill with a Download
button. Tapping it downloads that one package, shows progress, verifies
its SHA-256 against `content/recitation-manifest.json`, unpacks it into
the app's private storage, and starts playing where the reader asked.
Nothing is downloaded at launch, nothing automatically, and never more
than one surah at a time. The Recitations sheet lists the reciters with
their downloaded counts and sizes, and removing a surah deletes only its
files for that reciter.

What the reader pays: a surah is about 13 MB on average at the published
bitrate. Al-Fatihah is 0.7 MB, Al-Mulk 6 MB, Ya-Sin 14 MB, Al-Kahf 31 MB,
Yusuf 40 MB, and Al-Baqarah 122 MB, which is the only giant. The two
published recitations total 1.53 GB (Minshawi) and 2.19 GB (Husary).

How it is built: `tools audio packs` downloads the verified per-ayah MP3s
from QUL's CDN, builds one ZIP per reciter per surah, and rewrites the
manifest with each package's byte size and SHA-256. `tools audio publish`
uploads them to the `recitation-minshawi` and `recitation-husary` GitHub
Releases, 114 assets each, resumable in batches. The app knows one URL
pattern and one host, sends no identifiers, and has no analytics.

Verified end to end on the emulator: Play on Ya-Sin 36:2 showed
"Ya-Sin · 14 MB", the download ran with progress, the package verified
and unpacked, and playback started at 36:2, continued through the surah,
moved the page, and marked the recited word. Instrumented tests pin the
manifest contract and the per-surah file accounting
(`RecitationManifestTest`, 4 of 4).

## D-024: The page is the interface; the chrome is summoned

Date: the third session. The owner asked for a full rethink toward the
simplest, most beautiful reader possible, in the spirit of the best work
Apple has shipped, and approved the direction in one line: make it the best
app ever, and I will review the screenshots.

What the reader sees: a Mushaf page that fills the screen, with no header,
no footer, and no counter over it. One tap on the paper fades in a quiet
chrome at the top and bottom edges, which steps back on its own after seven
seconds. A tap on a word selects that ayah and shows one row of actions:
save, play, copy, share, and more. A tap on the page's own margins, head
band, or foot band brings the chrome instead. A long press does the same as
a tap, so nothing is hidden behind an undiscoverable gesture.

Why: every control that sits over the text competes with the text. The page
of the Quran is the message; the app is a servant of it. A reader who wants
to act on an ayah taps the ayah, and a reader who wants to navigate taps the
paper. There is nothing else to learn.

What the reader pays: one extra tap to reach navigation, in exchange for a
page with nothing on it. The chrome remembers nothing between taps; it is
always the same three doors at the top (browse, search, settings) and the
same three at the foot (listen, mushaf or study, saved).

## D-025: Study mode is one continuous scroll

Date: the third session, same review.

What the reader sees: the whole Quran as one vertical scroll, ayah after
ayah, from Al-Fatihah to An-Nas. A surah opens with its ornament, its name,
its place and ayah count, a quiet "About this surah" door, and the
basmallah. Tapping any ayah selects it; the card holds the depth.

Why: the previous study view was a pagination of Mushaf pages, which cut
ayahs in half at page boundaries and made a translation read like a tablet
of fragments. A translation is read as a book, and a thought should not be
interrupted by a page break that exists only for the Mushaf's geometry.

What the reader pays: study mode no longer mirrors the printed page; it is
its own reading of the same text. The place is shared: the app stores the
ayah, and the Mushaf derives its page from it.

## D-026: Content is packs, and the reader chooses which are on

Date: the third session, same review. The owner asked whether the app could
be modular underneath: mushaf style, fonts, translations, tafsirs, and
reciters as interchangeable pieces, downloaded when the reader wants them.

What ships: the same complete, verified library as before (QPC V2 page
script, KFGQPC Hafs, Saheeh International, Ibn Kathir, As-Sa'di, Minshawi,
Husary), now described by a `pack` table. Translations and tafsirs live in
pack-namespaced tables (`translation.pack`, `tafsir_passage.pack`,
`tafsir_ayah.pack`), so a second translation is a new row, never a schema
change. The settings sheet lists the packs that are on, and search reads
exactly those. Recitations already worked this way, one package per surah.

What this buys: the app can offer a second translation or a third tafsir as
a download without touching the reader, the schema, or the search code. What
it costs today: one rebuild of the content database
(63,832,064 bytes, sha256 `e9a05ddd9b456a28e8cffa83dc2909159ce7b488bfb53c5ba14f2a43eaa8792a`),
which every gate re-verified.

## D-027: Search reads every enabled source, and stays instant

Date: the third session, same review. The owner asked that everything about
the Quran be searchable, and that search never lag.

What the reader sees: one field. Arabic text, every enabled translation,
every enabled tafsir, word by word meanings, surah names in Latin and
Arabic, and references typed as numbers ("2:255", "2:255", "surah 2"). The
result line says where the matches were found, and matched words are washed
exactly where they appear.

How it stays fast: index columns are folded once at build time by the same
normalizer the query goes through (`Search.normalizeForIndex`), so matching
is a plain scan with no per-keystroke index build. Tafsir, the largest
corpus, is loaded once into a folded in-memory index in the background after
the first page appears; a warm search over all sources measured 1.09 s on
the emulator and about a tenth of that on a phone. Every row a search
touches is read in one batched query per table, never one query per hit.

What is not done yet: no fuzzy matching, no ranking beyond Mushaf order, and
no search inside a single surah scope. All three are additive later.

## D-028: The content database lives in a Release, addressed by its hash

Date: the third session, after the pack work rebuilt the database and pushed
it past the sixty-megabyte mark. GitHub warns above fifty megabytes, and
every content rebuild would add another blob of that size to the history of
a project meant to stay thin and readable.

What changed: `content/quran.db` is no longer in git. Its SHA-256 and byte
count stay in `content/build-report.json`, which is small and is the pin;
`tools fetch` reads that pin, and if the database is missing or does not
match, downloads it from the project's own Release tagged
`content-db-<first eight hex of the hash>`, verifies the hash, and only then
puts it in place. The tag makes the asset content-addressed: a new build is a
new tag, never a replaced file, and an old pin keeps resolving.

What this buys: the repository stays small and every clone, every CI run,
and every release rebuilds against exactly the database the report names.
What it costs: one download per machine, cached by the pipeline, verified
byte for byte. This supersedes the "database committed" half of D-016; the
fonts stay in their own Release for the same reason.

## D-029: The reader's hands, the surah as the unit, and notes behind their markers

Date: the fourth session. The owner reviewed the rethink and decided four
things about how the app should feel, plus one about what it ships.

**A tap belongs to the reading.** Tapping anywhere on the page brings the
chrome or puts it away; no tap opens a panel by surprise. Asking about a
particular ayah is a long press: the ayah washes, the phone hums once, and
one row of actions appears (save, play, copy, share, more). This is the
model the owner asked for, and it is why nothing jumps under a reader's
finger while they are following the text.

**Study mode reads one surah at a time.** The owner asked whether a
continuous Quran or a surah-bounded scroll is better. A surah is the unit
the Quran itself gives, and readers read it as one: Al-Mulk, Ya-Sin, Al-Kahf
on a Friday. A scroll that never stops hides that structure and slides the
reader into the next surah without a word. So the study view scrolls
continuously through the current surah, then says "<name> complete" and
offers a single door: continue to the next surah. Al-Fatihah through An-Nas
still works end to end; it simply pauses where the Book pauses.

**Footnotes wait behind their markers.** No footnote text is shown in the
reading by default. A small muted superscript marks where a note belongs,
and tapping that marker opens the note in its own sheet with its ayah
reference. The setting that used to show a footnote block under every ayah
still exists, off by default, for readers who prefer a study page.

**The screen stays awake while reading**, as the owner confirmed.

**What ships, and what arrives on demand.** The app carries only the Quran
text and its page layout (the page fonts, the Hafs study font, the words,
the page geometry, and the navigation data). No translation and no tafsir
ship inside the app. Saheeh International and Ibn Kathir are offered as the
first downloads, and every later translation, tafsir, word list, script, or
reciter arrives the same way: from the project's own Releases, on the
reader's word, with the size shown before a byte moves, verified by SHA-256,
removable at any time.

This is the decision that turns the pack work from a later nicety into the
next thing to build, and it is why the shipped app becomes much smaller:
the reader downloads the depth they want instead of carrying everyone's.

## D-030: The app ships the Book only; the depth arrives on demand

Date: the fourth session, the owner's words: only the Quran text and its page
layout ship inside the app, and everything else is downloaded when the reader
wants it.

What ships: one pack of about ten megabytes holding the Quran text, its
words, its page geometry, the navigation data, and the 604 page fonts. That
is a complete, offline Mushaf: the app opens, turns pages, keeps the reader's
place, saves ayahs, and writes notes without a single byte from the network.

What arrives on demand, each with its size shown before anything moves and
its SHA-256 verified before it is used: Saheeh International (2.2 MB), Ibn
Kathir (23 MB), As-Sa'di (15 MB), the word by word list with the surah
introductions (4.7 MB), each reciter's timing data (1.7 MB), and each
surah's audio when a reciter is actually played.

How it works: the content build splits the audited database into one SQLite
file per pack. The app opens the core pack read-only and attaches every
installed pack as its own schema, so a translation query can only ever reach
a translation the reader has, and removing a pack removes the content with
the file. Packs are published to content-addressed Releases
(`pack-<id>-<hash8>`), and `content/catalog.json` is the menu the app reads.

What it costs: a new reader who wants English has one extra step, and the
app must say so kindly (the study view offers "Add a translation" where the
translation would be). What it buys: an app that is smaller, a repository
with no giant file in it, and a library that can grow by dozens of packs
without any of them touching the reader who does not want them.

The single database of D-028 stays useful as the build's working form and is
still fetched from its own Release for CI, but the app no longer ships it.

## D-031: Ten small modules with one-way dependencies

Date: the fourth session. The owner asked for an engineering architecture as
careful as the design: almost everything a plugin, giant files split into
small clean pieces.

What changed: the single app module became ten. `:core` stays pure Kotlin.
`:data` owns content and memory: the pack catalog and store, the content
database, settings, saved ayahs, recitation files and downloads. A new
`:content-assets` owns the fonts. `:ui-kit` owns the theme, the icons, and
the rich text views. Six feature modules each own one surface:
`:feature-mushaf`, `:feature-study`, `:feature-search`, `:feature-browse`,
`:feature-playback`, `:feature-settings`. `:app` keeps the wiring: the
activity, the view model that holds the reader's state, the screen that
composes the features, and the manifest where the permissions are audited.

The rule that makes it real: dependencies point one way. Features depend on
`ui-kit`, `data`, `content-assets`, and `core`, never on each other and never
on `app`. Gradle refuses to build a cycle, so the rule enforces itself, and
`docs/architecture.md` records it for readers.

What it cost: two feature composables had to stop taking the view model.
`SettingsSheet` now receives `AppSettings`, the pack list, and a record of
thirteen callbacks; `StudyList` receives the surah, its ayah numbers, the
settings, and a `loadRow` function. That is the point: a feature that cannot
see the app is a feature that can be read, tested, and previewed on its own.
The split also exposed dead code, which is gone.

What it buys: every feature is now small (one or two files), the boundaries
are physical rather than aspirational, and the next work (a second mushaf
script, another feature) has a place to live that is not the app module.

## D-032: What a build carries is decided by its variant

Date: the fourth session, after the module split. The first release bundle
after the split still contained all seven packs, forty-seven megabytes the
reader never asked for. The cause was a condition inside the asset task that
looked at the names of the requested tasks, and a task whose output was
already generated for a debug run was considered up to date, so its debug
outputs went into a release bundle.

The fix is structural, not another condition: the main assets hold the core
pack, the catalog, the fonts, and the recitation manifest, and only the debug
variant's own asset directory holds the rest. One build can no longer
inherit the other's packs, whatever order the tasks run in, and the rule is
visible in the build file instead of hidden in a boolean.

The sizes now: the release APK is 143.8 MB (the Mushaf page fonts are the
bulk, by design, because the app must render the Book with no network and no
downloads), the release bundle is 147.4 MB, and the shipped content inside
both is ten megabytes of Quran text and page layout. The debug APK carries
every pack so development and tests run offline.

## D-033: Content is grouped by language, and the night has a dimmer

Date: the fifth session. The owner asked that translations and tafsirs be
categorized by language, chose the three pieces of trust work worth doing
now, and asked which Bengali translations exist before choosing one.

**Content by language.** The settings sheet lists what can be added under
language headings, English first, then Arabic, then the rest by name. Under
English today: Saheeh International, Ibn Kathir, and the word by word list
with the surah introductions. Under Arabic: As-Sa'di. A second translation in
any language slots under its own heading with no code change, because the
catalog already carries each pack's language.

**An about page with credits and licenses.** The settings sheet opens a page
that names every pack in the catalog with its credit and license, the four
font families with their terms, the two reciters, and the app's own promises
(no ads, no trackers, no accounts; network only for a pack or a surah the
reader asked for). The page is built from the catalog, so it cannot drift
from what the app can actually offer.

**One quiet hint, once.** The long press is the richest gesture in the app
and the least visible. After the first page appears, a small card says
"Press and hold any ayah for its actions" for eight seconds, and it is never
shown again, on any device, for that reader.

**A screen dimmer.** Night themes plus a black room still glare. A three way
choice (Off, Dim, Darker) lays a translucent black over the reading surface
without blocking a single touch, and it is remembered like every other
setting.

## D-034: Bengali arrives as two packs, and word meanings are their own thing

Date: the fifth session. The owner chose Taisirul Quran (Professor Mozammel
Haque) as the Bengali translation and Tafsir Ibn Kathir (Bengali) as the
Bengali tafsir, and asked whether either carries word by word.

**Word by word is never part of a translation.** Saheeh International is a
sentence translation; the word meanings the app shows today come from QUL's
English Word by Word dataset, which is its own resource and its own pack
(`words-en`, 4.6 MB with the surah introductions). Taisirul Quran is the
same: a translation, no word meanings inside it. QUL keeps a Bengali word
list as its own dataset (resource 94), so if Bengali readers want word
meanings, that is a third pack, listed in the manifest as pending.

**The app now picks the word list by language.** The meaning panel asks for
the word pack that speaks the language of the chosen translation, and falls
back to English when that language has none. So the day a Bengali word pack
is installed, the card fills itself without a code change.

**Both Bengali packs are on demand**, like every other translation and
tafsir: nothing is bundled, the size is shown before a byte moves, the file
is verified by SHA-256, and removing it removes the content. In the settings
sheet they appear under a Bengali heading, between English and Arabic.

## D-035: Word by word is a reading aid, per language, off until asked for

Date: the fifth session. The owner asked whether word by word is offered at
all, whether it should be, and how.

**What exists.** Word meanings are their own dataset, not part of any
translation, in QUL and now in the app: the English word list with the surah
introductions is a pack (4.6 MB), and the Bengali one is listed and waiting
for its download. Tafsir Ibn Kathir in Bengali and Taisirul Quran in Bengali
are now built, published, and installable: verified end to end on a release
build, where Taisirul Quran downloaded over the network, verified its hash,
installed, and appeared as the Bengali translation in the study reading.

**Should we offer it.** Yes, for three reasons. It is the one aid that helps
a reader who cannot read Arabic at all, which is most readers. It costs
nothing to the reader who does not want it, because it is a pack. And it
makes a language pair complete: a Bengali translation plus Bengali word
meanings is a Bengali reading of the Quran, not a Bengali sentence next to
an Arabic puzzle.

**How it is offered, in three places, none of them in the way.**

1. Under each language in Settings, with its size, as it already is.
2. In the ayah card: when the word list for the reader's language is not
   installed, the door says "Add word by word" and opens the content list;
   when it is installed, the door opens the panel of words with meanings.
3. In the study reading: a new switch, "Show word meanings", off by default,
   lays each word with its meaning under the ayah, so a learner reads the
   Arabic word by word, ayah after ayah, instead of opening a card for every
   verse. The switch only does something when a word list is installed, and
   the app picks the list that speaks the language of the chosen translation,
   falling back to English.

## D-036: Word meanings speak one language per pack, and Arabic words read right to left

Date: the fifth session, after the owner downloaded the Bengali word list.

**One pack per language, one table for all of them.** The build no longer
keeps a single meaning column on each word. Word meanings live in a
`word_meaning` table keyed by (language, word), filled from every word list
the build has, and each `words-<language>` pack carries one language. English
is 4.6 MB, Bengali is 6.5 MB, and a third language costs one dataset entry
and one pack definition, with no schema change and no code change in the app:
the app already asks for the list that speaks the language of the chosen
translation.

**A real bug the Bengali library exposed.** `wordsPack(language)` ignored its
argument and always returned the English pack id, so meanings silently never
appeared for Bengali. It was found by following the feature end to end rather
than by reading the code, and it is now covered by an instrumented test.

**Right to left, like the Book.** The word by word aid lays the Arabic words
right to left, each with its meaning beneath it, so the row reads in the
order the ayah is recited instead of backwards.

**The books now in the library**, all on demand, none inside the app: Saheeh
International and Ibn Kathir in English, As-Sa'di in Arabic, Taisirul Quran
and Ibn Kathir in Bengali, word lists in English and Bengali, and the two
reciters with per surah audio.

## D-037: A launch lands on the page the reader left

Date: the seventh session. The owner asked for the best app there can be:
faster than anything, polished to the level of the best work Apple and
Google ship, and readable before any of that matters.

**What the app did.** On a cold start it showed the paper and the name of the
Book while the content database opened, then rendered the page from its fonts
and queries. The splash was honest but it was still a waiting room.

**What it does now.** After the reader rests on a page, the rendered bitmap
is written to the app's cache, keyed by page, pixel width, and theme. On the
next launch that picture is decoded first, before the content database opens,
and it is painted as the first frame. The real page replaces it as soon as it
is drawn, with no blank frame in between: the same picture is handed to the
pager as the placeholder for that page until the render lands. A jump of more
than two pages now lands at once instead of animating across three hundred
pages and rendering each one on the way.

**What it costs.** One bitmap on disk (a page of text is about seventy
kilobytes) and one in memory for a few hundred milliseconds. A page is only
written once the reader has rested on it for a third of a second, so swiping
through the Quran never touches the disk. If the picture is missing, stale,
or for a different width or theme, the app simply draws the page the way it
always did. The cache is a cache in the strict sense: nothing depends on it.

**Measured on the software rendered emulator**, which is the slowest Android
this app will ever run on: a released, minified build shows its window at 781
ms and reports fully drawn at 930 ms on a warm start, 889 ms and 1.10 s on the
very first launch after install, and the first photographed frame after the
system splash is already the Mushaf page. A phone with a real GPU and a warm
ART profile is faster than that by a wide margin.

## D-038: The page reads itself aloud, and every control is a real target

Date: the seventh session, the accessibility pass.

**A picture of text is not a text.** Mushaf mode draws a bitmap, so a screen
reader had one node for the whole page. That is not reading, it is being told
a page exists. The page now carries one invisible node per ayah, laid over the
words it names, in Mushaf order, each with its reference and its text, and an
action that opens the ayah's own row of actions. A TalkBack reader can move
ayah by ayah, hear each one, and act on it, exactly as a sighted reader does
with a long press. The nodes carry no pointer input, so a touch still belongs
to the page, and only the page the reader is on exposes them.

**Forty eight points, everywhere.** An audit against the design document's own
rule found the small text actions, the transport controls, the mode switch,
the browse tabs, the settings segments, and the pack actions all under the
minimum. Every one of them is now a real 48 dp target, and the ones that show
only a glyph now say what they are: the text size steps name their place in
the scale instead of reading "alef" five times. A switch row is now the whole
row, so the label and the switch are one node, which is how a screen reader
expects to meet a setting.

**Color is never the only signal**, and muted text is never below 4.5:1. The
quietest text on the page was an ayah reference at 2.8:1; the secondary tone
now comes from the theme's own `onSurfaceVariant`, which meets the target in
all four themes by construction.

## D-039: Every sentence is a resource, in the module that shows it

Date: the seventh session, before the interface can be translated into a
second language.

**The rule changed.** This file used to say that user facing strings live in
the app module's `strings.xml`. The code had already outgrown that: a feature
module cannot read another module's string resources, and a feature that took
its own strings as parameters was still holding English inside Kotlin. The
rule is now the one the code can keep: every sentence a reader can see is a
resource in the module that draws it, and no Kotlin file carries user facing
English. `app` keeps the shell's strings, each feature keeps its own, and
`ui-kit` keeps the two names of the reading modes.

**Only English ships.** Arabic is content, not a locale, and there is still no
localization infrastructure. What changed is that adding a second language is
now a `values-xx/` folder and a translator, not a hunt through a codebase.

## D-040: The reader's own work can leave the phone, and nothing else can

Date: the seventh session, the trust work.

**Saved ayahs and notes are the reader's work.** They lived in a private
database that only the app could read, which is good until the reader changes
phones. There are now two doors in Settings, under the reader's own heading:
Export writes a small JSON document through the system file picker, and Import
merges one back. The document names its own format and version, refuses
anything else, clamps a note to a sane length, brings a date from the future
back to now so one bad file cannot pin itself to the top of the list, keeps
the reader's own note when both sides have one, and reports what it did in one
line.

**What this changed about backup.** The app already refused Android's cloud
backup and device transfer; now it says so in the two rule files Android 12
and later read, so the refusal is explicit rather than incidental, and the
export door is the only way the reader's work moves. The privacy policy was
already true; now the manifest proves it.

## D-041: The app checks its own content, and a bad file cannot crash it

Date: the seventh session, the robustness pass.

**A damaged pack should be found, not suffered.** Every pack is verified when
it arrives; the app can now look a second time, on the reader's word, from
Settings: "Check installed content" reads each installed pack back and
compares it with the fingerprint the catalog recorded, then names anything
that no longer matches so the reader can remove and add it again. Nothing runs
on its own, and nothing is fetched.

**The one thing that could keep a reader from the text** is a core pack that
cannot be opened. That is now caught: the copy is discarded and written again
from the app's own signed assets, once, and if that also fails the reader gets
one calm screen with one action instead of a crash. The screen says what
happened, that their saved ayahs and settings are untouched, and offers to try
again.

**And the smaller doors are closed.** The manifest declares that the app
speaks no cleartext, the backup rules exclude everything, every pack download
verifies its SHA-256 before it is used, and a downloaded archive entry with a
path separator is refused. Lint is clean, with no warnings suppressed.

## D-042: A tag builds the release

Date: the seventh session, the release engineering.

**What was missing.** The runbook said how to build and upload by hand, and
the numbers it mentioned could drift from what the app actually carried. There
is now a `release` workflow: a `v<version>` tag checks itself against the
`versionName` the app carries (a tag that names a version the app does not
have is a red build, not a wrong bundle), runs the core tests and the content
gates, writes the upload keystore from repository secrets into a file the
build is pointed at with `-Pquran.keystore`, builds the signed bundle, proves
the signature with `jarsigner`, writes a SHA-256, and attaches both to a draft
GitHub release. Without the secrets it still builds, and says out loud that
the bundle is unsigned. The keystore never enters the tree, and the owner's
machine keeps working exactly as before.

## D-043: The theme owns the washes, and the system bars follow the theme

Date: the seventh session, the polish pass.

**Two colors left over from the first page.** The selection wash and the
recitation wash were constants in `ui-kit`, the same lapis on paper and on a
black room, where a dark blue wash on a dark ground is invisible. They are now
part of the page palette, so each of the four themes sets its own, and the
page, the study reading, and search all take them from the theme instead of
carrying a color of their own. The night themes get a light lapis, the day
themes keep the deep one, and the recitation wash stays stronger than the wash
under a chosen ayah so the two never read as the same thing.

**The status and navigation bars belong to the reader's choice**, not to the
system's idea of day and night. They were dark on dark in the night themes
because the platform decided from the system setting; the app now sets the
icon appearance from the theme it is actually drawing, and the launch window
has a dark twin for a reader whose system is dark. The page turn also casts a
shadow that follows the finger and marks its settle with one light tick, which
is what the design document promised and the code had not done.

**And the type scale is complete.** Only seven of Material's fifteen styles
were defined, so any surface that asked for one of the others drew in the
platform's own font. Every style the app can ask for is now the interface's
voice, and a heading in a tafsir can no longer arrive in Roboto.

**The credits screen shows the real licenses.** The catalog handed every pack
the same line, "See docs/content-sources.md", which is a note to go and read a
file, not a credit. The pack to dataset mapping now lives in one place in the
pipeline (`PackSources`), the catalog carries the licenses of the datasets a
pack actually contains, one per line, and the English word list's surah
introductions and the cross-check dataset are credited where they belong. The
pack files were rebuilt to prove it: their hashes and byte counts did not
move, so every published Release still resolves to exactly the file the
catalog names.

## D-044: What the audit found and fixed

Date: the seventh session, the correctness pass. These are bugs, not choices,
and they are recorded because a reader would have met every one of them.

**The word list ignored its language.** `wordsPackId("bn")` returned
`"words-"`, so a Bengali reader who had installed the Bengali word list was
shown "Add word by word" and an empty panel. The card now asks the database
which installed list speaks the language of the chosen translation, falls back
to English when that language has none, and search reads meanings from the
same list, so a Bengali reader can find a Bengali meaning.

**The tafsir index was never warmed.** The coroutine that prewarms it started
before the database existed and returned immediately, so the first search paid
for the whole index. It is now built after the library opens, on a worker,
while the reader is reading, which is what the design always said.

**A search excerpt could cut a word or lose its own match.** The window walked
forward from a word boundary instead of backward, and could end up excluding
the passage it was built around. It now widens to whole words, always contains
the first match, and a test pins the rule.

**A far jump animated across three hundred pages**, rendering each one on the
way. A jump of more than two pages now lands at once.

**Two duplicate lookups on the hot path.** The surah of an ayah was resolved
by rebuilding a 114 entry map on every call, and the study list read the whole
ayah table (6236 rows) at startup to answer questions that are arithmetic.
Both are gone: the surahs are indexed once, and the reader's library no longer
reads a table it does not need. The same pass removed a page render's
duplicate in-flight work, which is now one shared bitmap that every asker
waits on instead of a polling loop.

**A tafsir door said "English" for every language**, so the Bengali Ibn Kathir
was labelled English.

**The saved list read one row at a time**, with a "Loading..." line where the
ayah should be. It now reads every visible row in two batched queries and
shows the ayah itself, which is what a reader recognises before its reference.

**The store set never showed the chrome.** The screenshot tour tapped the
page and photographed it 450 ms later, which the software rendered emulator
had not drawn yet, so the capture that was meant to show the summoned chrome
was byte for byte the bare page (the committed set proves it: the two files
were identical). The tour now waits for the screen to stop changing before it
keeps a frame, and it retries a gesture that did not land. The capture also
no longer runs before the window is ready, which is what swallowed the first
tap of every run.

**And the little ones**: a `super` call lint says is empty, an unused legacy
page setting, an unused header drawing routine in the page renderer, two dead
translation lookups, a duplicated import, a "1 surahs" plural, a percentage
that could print a localized separator inside a store-facing sentence, and a
study list that said "Tap any ayah" where the gesture is a long press.

## D-045: The reader's place is one ayah, and the study list stopped losing it

Date: the eighth session. Reported by the owner: "after using the app, when I
close the app, it starts at the beginning of the surah, not the ayah I was
on." It was a real bug, and it was the study list's fault.

The study list draws a surah as an opening item, then one item per ayah, then
a closing line, and it wrote the reader's place from its own scroll position:
`firstVisibleItemIndex` used directly as an index into the ayahs. Off by one,
so the place drifted one ayah per settle, and sitting on the opening item
wrote the *first ayah of the surah*, which is exactly what the owner saw on
the next launch. Two more faults made it stick: the list wrote the place on
its first frame, before the jump to the reader's ayah had run, and the jump
refused to move when the target was fewer than three items away, so the
stale value won. The emulator said it plainly:
`compose place=34 initial=27 first=27` then `emit index=0 ayah=8`.

What changed:

- `core/AyahList.kt` is now the only place a list index and an ayah meet, with
  a test that round trips every ayah of a surah. Nothing else converts one.
- The list writes the place only when the *reader* moved it: a drag on the
  list is what marks a scroll as the reader's own, so a jump, a mode switch,
  and the first frame of either can never record anything.
- The jump to the reader's place is exact: the list scrolls whenever the
  measured position differs, with no "close enough" window.
- The Mushaf writes the place only when the page is a *new* one. Switching
  from the study view to the Mushaf page containing the reader's ayah keeps
  the exact ayah, instead of replacing it with the page's first ayah, so a
  mode switch no longer costs the reader several ayahs.
- The study view loads a whole surah in one pass (ayahs, words, translation,
  word meanings: three queries instead of three per ayah). Every item has its
  text before the list is measured, so the reader's place lands exactly, rows
  do not grow under the finger while scrolling, and the first frame after a
  jump is the reader's ayah.

The khatm question the owner raised was answered with a decision not to guess:
the app opens exactly where the reader was, and a khatm is a commitment the
app should be *told* about one day, not inferred from how someone arrived at
an ayah. No khatm machinery ships in this session.

## D-046: Settings is a hub of one row per category

Date: the eighth session, on the owner's instruction: "the settings page is
cluttered. all the categories should open in their respective pages."

Settings is now a hub of nine rows, each carrying where it stands right now
("Text · Arabic 30, translation 17", "Reciters · Al-Minshawi", "Your saved
ayahs · 12 saved"), and each opening a page of its own: Appearance, Text,
Reading, Reciters, Translations, Tafsirs, Word by word, Your saved ayahs,
About. Back steps out of a page before it closes the sheet, and the hub keeps
its own scroll, so closing a page or the credits behind it returns the reader
to the row they came from.

Three consequences worth recording:

- **Text sizes are per role.** One size for everything meant a reader who
  needed bigger Arabic got bigger footnotes with it. Arabic, translation,
  tafsir, and word by word each have their own five steps now (`data/TextSize`,
  multipliers over a base per role), the old single setting migrates to all
  four, and the Text page draws the reader's own ayah above the rows so a
  change is judged on the page it is about to change.
- **The screen dim is gone**, at the owner's instruction: the field, the
  setting, the draw pass, the row, the swatch preview, and the trap that
  documented it. The Night and Black themes cover the need.
- **About is the app only**: the version, the credits and licenses, and the
  content self check. The ads-and-trackers, source-code, and privacy rows are
  gone from it; the store listing carries the policy, and the credits keep one
  row for corrections and rights, which the content rules require.
- Translations and tafsirs are separate pages grouped by language, one
  translation is read at a time (a mark says which), every tafsir can be on at
  once, and the word list follows the language of the chosen translation
  rather than asking the reader to pick a language for meanings that would
  then be in a language they are not reading.

## D-047: One offer for audio, and adding a reciter chooses it

Date: the eighth session, on the owner's report that listening asked twice and
downloaded a reciter they had not chosen.

Listening needs two things that can each be missing: the reciter's word
timings (a pack) and the surah's audio (a package). They were asked for
separately, the first with no visible progress anywhere in the reader, and
installing a reciter did not select it, so "Add Husary" left playback on
Minshawi and fetched Minshawi's audio.

Now there is one offer, in the pill: the reciter's name, the surah, and the
combined size, with the reciter changeable right there (each option with what
it would cost). One tap downloads what is missing under one progress bar and
plays. Adding a reciter in Settings selects it, the Reciters page names each
reciter's downloaded surahs with their sizes, and the player no longer offers
a download that cannot help: without the timings, the audio is not a fix, and
the offer that cannot lead anywhere no longer appears.

## D-048: The reading modes are drawn, not named

Date: the eighth session. "The name Mushaf and Study may be wrong... we could
show icons for these." The segmented switch is now two drawn pictures: a ruled
page for the Mushaf, and a page with a reading under each ayah for the study
view. Each is named for TalkBack, and the name of the chosen one is said once,
quietly, above the switch when the mode changes, so a reader learns the pair
in one tap and never sees the words again.

In the same pass the ayah's Copy action is gone: the share sheet already
offers Copy first, so the app was offering the same thing twice.

## D-049: A paragraph's line height comes from what is in it

Date: the eighth session, on the owner's report that footnote markers overlap
the text at large sizes. A paragraph could carry Arabic at a much larger size
than its Latin text, and a superscript marker above the line, while its line
height came from the Latin size alone. Lines collided, and the markers sat in
the space they needed.

A paragraph now takes the taller of the sizes it actually contains, aligned
line boxes with no trimming, and markers at 0.65 of the base size. The first
cut gave every Latin paragraph the Arabic's room, which floated a translation
in white space; the runs decide, not the setting.

## D-050: The release hand-off, 0.2

Date: the ninth session, the release session. Version 0.2 (versionCode 2) went
to Google Play for review.

What the hand-off was: `versionCode` 2 and `versionName` 0.2, the version line
in `play-store/listing.md`, and the release notes written there at 499
characters, one unbroken paragraph. The full suite ran on this machine before
anything was pushed: core tests, data unit and instrumented tests, app unit and
instrumented tests, lint with no issues, the debug and release builds, and the
content gates (`verify`, `checkdb`, `search`). Two pushes followed, `258d69f`
and `64df043`, both green: the `build` workflow, the `pages` deployment, and
the `screenshots` workflow for phone, 7 inch, and 10 inch.

The bundle: 147,673,026 bytes, SHA-256
`2bf54fd1e66e974ca3a28bac340b795c08c7fd02cbd2be9f0128a21220958e15`, signed
with the owner's upload key, verified with `jarsigner -verify`, carrying the
core pack and no debug packs. `aapt2` on the release APK built beside it
confirmed versionCode 2 and versionName 0.2.

Two placements changed on the owner's word: the hand-off bundle now lands in
`play-store/aab/`, a folder of its own with a four line note that says what
lives there, and `.gitignore` covers that folder so a 140 MB binary can never
enter the repository. The runbook's step 5 names the new path. The hand-off
copy was deleted once the submission was confirmed.

The store screenshots were refreshed from the `Capture store screenshots`
run for the release commit, never captured by hand, and committed in
`64df043`: 14 PNGs for each of the three form factors, now showing the
settings hub, the study view with a translation and the word by word aid, the
ayah card with its tafsir door, and version 0.2.

Housekeeping closed the session: the hand-off bundle, every module build
directory, and the content working area (extracted sources, font packs, built
component files, the development audio sample) were deleted, about 3.3 GB,
leaving a 597 MB repository. The provenance (`content/raw/`), the shipped
assets (`content/quran.db`, `content/packs/`), and the store screenshots were
kept on purpose, and AGENTS.md carries the table that says how to bring back
everything that was removed.

## D-051: The reader's report, the tenth session

Date: the tenth session. The owner read the shipped 0.2 and reported what they
met. Eleven items, and one of them was a bug that had survived two sessions.

**The switcher moved to the top bar, as one door.** It sat in a bottom bar
that also carried Listen and Saved. The owner asked for the switch beside the
other icons at the top, and for the bottom bar to go. It is one button now,
and it offers the *other* mode: the icon shown while reading the Mushaf is the
study picture, and it becomes the Mushaf picture in study. The mode hint that
used to name the chosen mode in words is gone with the bar that held the
switch, because the icon is now the instruction.

**Listen and Saved left the reader's face.** Listening is the play action on
an ayah and in its card, which the owner pointed out was already there.
Saved ayahs are one of the Browse tabs, next to Surahs and Juz, where they
belong. The bottom bar had nothing left to hold, so it is gone, and the
chrome is a top edge only.

**Last Read is a Browse tab.** The owner asked for the places the reader has
been reading, so they can come back to an ayah they left. It is a new store
(`data/LastReadStore`, `last-read.db`, its own database like the reader's
saved work), written when the reader settles somewhere they chose: a page
they turned in the Mushaf, a place they stopped at in study, a jump they made
from a sheet, and the place they open the app on. One row per ayah, so a place
returned to moves to the top instead of piling up, and the list is capped at
twenty: one sitting is a handful of places, so that is a fortnight of moving
around or a month of surah by surah, and still a short scroll of recognisable
places rather than a log. Each row carries the ayah, the mode it was read in,
and when it was left ("3 days ago"), and a place can be forgotten.

**The audio offer can be dismissed.** The owner met the download pill and
could not make it go away. The offer now carries a "Not now" close beside
Download, which is the same door that cancels a download, so nothing ever sits
over the reading against the reader's wish. The offer still only appears after
a tap on Play, still names the reciter and the size, and is still the one
place the download is approved.

**The reading place stopped oscillating.** Reported as: open the app on the
last place, go to another surah through Browse, scroll, and the view jumps
back to the old page before it returns. Two causes:

- The settings DataStore is a flow, and `ReaderViewModel` was re-applying
  every emission through `applySettings`, which could carry the stored ayah
  back over a place the reader had just chosen. The view model owns the
  settings now: it writes every change itself and keeps its in-memory copy in
  step, and the flow is read once, while the library opens, and never again.
- The study list kept drawing the previous surah's rows under the new surah's
  name while the new rows loaded, and its place writer had no guard against
  its own programmatic scrolls. It now holds the loaded surah with its rows
  and shows the paper until *this* surah's rows are in hand, and a drag is
  what marks a write, cleared as it writes.

**Settings corrections.** The hub rows pointed down though the pages slide in
from the right, so the chevron points right. The mark of every choice moved to
the left of the name, where the eye lands on the state first. Translations are
checks, not radios: the owner was right that a reader may read more than one,
so more than one may be on, each in its own named column under the ayah, and
the first one turned on is the one search and share read. Tafsirs were already
checks, and reciters stay radios because one reciter is heard at a time.

**Text sizes.** The largest step, 1.6, was more than anyone reads at, and the
list had no room below 0.85 for a reader who wants more ayahs on one screen.
The steps are now 0.75, 0.85, 1, 1.2, 1.4. Sizes are stored as scale factors
rather than step indices, because an index means something different in a
different list: a reader who chose 1.6 (the top) lands on 1.4 (the top), not
on the third of five. The stored value is read off the raw preference map and
not through a typed key, because 0.2 wrote these as `Int` and reading an `Int`
through a `Float` key throws; a reader who updates the app must never meet a
crash for a text size.

**Show footnotes is gone.** A marker opens its note in its own sheet, which is
the better reading, so the setting that spilled every note under every ayah
was removed with its field, its row, and its pref.

**Export and import saved ayahs are gone.** The owner asked for the feature
removed completely, and it is: the two doors, the file plumbing
(`SavedTransfer.kt`), the JSON writer and reader, the notice type and its
strings, the Saved settings page and its hub row, and the two instrumented
tests that pinned them. `SavedStore` keeps saving, notes, and removal, and the
saved-ayah tests now pin exactly that.

## D-052: The release hand-off, 0.3

Date: the eleventh session, the release session. Version 0.3 (versionCode 3)
went to Google Play for review.

What the hand-off was: `versionCode` 3 and `versionName` 0.3, the version line
in `play-store/listing.md`, and the release notes written there at 496
characters, one unbroken paragraph. The notes reached the owner bare, with no
blockquote and no quotes around them, because Play Console's text box takes
whatever characters arrive and a wrapped paste would have carried the markers
into the store text.

The bundle: 147,657,235 bytes, SHA-256
`4b43a694ba565f776901000f7ec87b644be82e9b9153b4d023b830db189b1b27`, signed
with the owner's upload key (certificate SHA-256
`537d09d20300129e973b7945316bfe24cfadcfbc77eec5229cbf30170d9de521`, the same
key as 0.2 and the family's other apps per D-017), verified with
`jarsigner -verify`, carrying the core pack and no debug packs. `aapt2` on the
release APK built beside it confirmed versionCode 3 and versionName 0.3. The
hand-off copy was deleted once the submission was confirmed.

**Three things the pre-flight found, all of them older than this session.**
They are recorded because a release is where they surfaced, and each one would
have shipped broken:

1. **`tools fonts` had been broken since the pack split.** It read
   `content/quran.db` for a `translation` table, a `tafsir_passage` with a
   `source` column, and a `word.translation`; the split moved all three into
   `content/packs/*.db`. CI never ran it (it needs the raw QUL and QuranEnc
   exports), so nothing failed and nothing was checked. It now reads the built
   packs, and its old font paths (`app/src/main/res/font`) were corrected to
   `content-assets`.
2. **Its first honest run found a decision nobody had written down.** The
   Bengali translation and the Bengali Ibn Kathir are drawn by Android's own
   Noto, not by a bundled face, which is why they render correctly and always
   have. The gate now allows a script *by name*, in one allow-list, and fails
   on anything else: a new script is a decision, not an accident. All
   23,012,137 reading codepoints pass.
3. **The word by word test could fail with `The current thread must have a
   looper`.** It paired `createEmptyComposeRule()` with its own
   `ActivityScenario`, and the study list's prefetch scheduler throws when the
   composition lands on a thread without one. The compose rule owns the
   activity now, and library preparation is an `ExternalResource` chained
   outside it, so the app starts after the packs are in place.

**The tenth session's eleven items, as shipped.** The reader's place stopped
jumping back (the DataStore echo and the study list's stale rows are gone);
the mode switch is one icon in the top bar that offers the other mode, and the
bottom bar and its Listen and Saved doors are removed; Last Read is a Browse
tab with twenty places kept; the audio offer carries a "Not now" close; the
hub chevrons point right; choice marks sit at the left; translations are
checks like tafsirs, with more than one allowed and each drawn in its own named
column; the size scale is 0.75 through 1.4, stored as scale factors so an old
value keeps its meaning; Show footnotes is gone; and export and import are
removed from the app entirely.

**And the CI shape changed.** The screenshots workflow took the family shape:
the AVD is cached per form factor, the script waits for `/sdcard/Android`
before the test starts, and the app's instrumented tests run only there, on
all three store form factors. The data tests still run in `build.yml` on one
phone profile. The tablet legs had been failing on a 25 second wait that no
machine with a software renderer could meet; the waits are minutes now,
because a slow emulator is not a failing reading aid.

## D-053: The reader's report, the twelfth session

Date: the twelfth session. The owner read 0.3 on a phone and reported twelve
things they met, two of them crashes.

**Tapping Add on a second translation crashed the app. The cause was a name.**
`app` and `feature-settings` each declared a string called `pack_downloading`,
and `app`'s had two format arguments (it is drawn in the reader's own download
pill) while the feature's had one. Resource merging keeps one of a duplicate
name, so the settings page asked a two-argument string for one argument and
threw `MissingFormatArgumentException` inside `stringResource`, on the main
thread, in the middle of a download. `pack_preparing` and `action_save` were
the same collision waiting for a tap. They are renamed per module now
(`reader_pack_downloading` and `settings_pack_downloading`, `study_note_save`),
and `StringNameTest` in `core` walks every module's `strings.xml` and fails on
any name two modules both hold. A test cannot see the merged table, but it can
see the names.

**The same crash hid a second one: a row that only worked if the pack was
already installed.** Translations, tafsirs, and word lists were drawn as
choice rows whose `onClick` was wrapped in `if (pack.installed)`, so tapping
the radio of a pack the reader did not have did nothing at all, and the only
live control was the Add button, which crashed. One row type now serves all
four pack lists (`PackChoiceRow` in `SettingsRows`): radios for reciters,
checks for the rest, and a tap on the name or the mark does the one thing the
state allows. A pack that is not here is installed and then turned on, which
is what the reader asked for by tapping it.

**A downloaded surah's Remove now removes.** The count was read once into a
`produceState`, and nothing re-read it when the removal finished, so the row
stayed and the button read as broken. The list is held as state and re-read
from the device after every removal, so the row leaves when its files do.

**Automatic night mode.** Appearance gained one switch under the four pages:
"Follow the system dark mode". It resolves to Night when the system is dark,
and to the reader's own page in the light; a reader whose choice is itself a
night page is shown Paper by day, because a switch that changed nothing in
one of its two states would not be a switch. The resolution happens once in
`QuranApp` (`AppTheme.resolved`), so the status bar, the sheets, the launch
picture, and the page all agree; `isDark` moved to `data` beside the enum
where the pure function can be unit tested.

**The page shadow during a turn is gone.** The lift and its cast shadow made
the reading page look like a sheet of paper being picked up, and the owner
read it as a gimmick and asked for it out. A turn is now a plain horizontal
slide: no elevation, no shadow, no draw-phase offset lambda, no `dragOffset`
parameter at all.

**The study chrome steps aside while scrolling.** The study list reports the
start of a scroll to the reader screen, which puts the top bar away in the
same frame. A tap still brings it back, and the seven-second timer still
takes it away, so nothing else about the chrome changed.

**Last Read reads like a place.** Each row is the surah's name with its
reference, "An-Nisa 4:31", and the ayah's own long form beneath it, instead
of a bare key. The tab says Last Read, the sheet's four tabs are centered on
the sheet rather than hugging its left edge, and the browse title keeps its
own line above them.

**Search gained filters.** A row of chips under the field, one per source
(Quran text, surah names, references, translations, word meanings, tafsirs),
every one of them on until the reader turns it off. A source the reader does
not have is not offered, and the filter state is part of the search key, so a
filter change re-runs the query exactly like a keystroke does. `SearchRequest`
carries a `SearchSources` now, which the database honors per source.

**Lists are alphabetical.** Languages are ordered by the name the reader
reads (`languageName`), and the packs inside a language, and the reciters,
by name. The old order was English, then Arabic, then the rest, which is an
implementation order, not a reader's.

**Bengali is Bangla.** Wherever the app names the language, on the settings
page, in an ayah card's tafsir door, in the credits, and in the catalog's own
credit lines, it says Bangla. Older decision entries keep the older spelling,
because they record what was said at the time.

**The mode icons were redrawn as a pair.** The Mushaf glyph and the study
glyph were drawn in a different hand from the rest of the set (different
stroke weights, a lighter tone nothing else used, a different corner). They
now share one page outline at the family's stroke weight, and differ only in
the lines on the page: even rules for the printed page, an ayah over its
reading for the study page.

**One more spacing correction.** The mark of a choice and the name it selects
had four dp between them, which reads as one crowded glyph; the gap is
sixteen. Every pack row's action (Add, Remove, Retry) keeps the minimum touch
target and a wider clear space on each side.
