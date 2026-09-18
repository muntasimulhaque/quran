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
