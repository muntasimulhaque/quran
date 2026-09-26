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

## D-054: CI signs the bundle, and the artifact stays private

Date: the twelfth session, on the release run.

The family's other five apps sign in CI and post the bundle to a
`latest-build` Release. This app did not: `build.yml` had no signing step,
and `release.yml` expected four secrets under different names
(`UPLOAD_KEYSTORE_BASE64` and friends) that were never created, so a `v*` tag
would have built quietly and published an unsigned bundle. The owner chose to
bring this app into the family shape, with one difference: the artifact stays
private.

**What was built.** A `signed-bundle` job in `build.yml`, running only on a
push to `main` (never on a pull request, so a fork can never reach the key).
It takes the four secrets, runs the core tests and the two content gates a
runner can run, builds `:app:bundleRelease`, and uploads the bundle and its
`SHA-256` to the run's own artifacts, which only someone signed in to this
repository can download and which GitHub deletes after two weeks. The four
secret names are the family's: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`.

**Why private, and why it matters more here.** A GitHub Release asset is
public: an unauthenticated request to the family's `latest-build` assets
follows a redirect and downloads the signed bundle, while the same request to
an Actions artifact is refused with 401. For a game, a downloadable build is
harmless. For this app the bundle carries the upload signature and the whole
content library, and a signed copy sitting publicly where a stale one could be
picked up by mistake is a risk with no upside. The private artifact costs one
command (`gh run download <run> -n quran-signed-aab -D play-store/aab`) and
expires on its own.

**The trap this closes.** `jarsigner -verify` exits 0 on a file that is not
signed at all and reports it only in its words (`jar is unsigned`), so a
script that reads the exit code would happily hand over an unsigned bundle.
The job reads the words and fails when `jar verified` is absent, and prints
the signing certificate's SHA-256
(`537d09d20300129e973b7945316bfe24cfadcfbc77eec5229cbf30170d9de521`, the shared
upload key per D-017) so the log shows which key signed the run. The
`build.gradle.kts` probe is unchanged: on the owner's machine an absent
keystore still degrades to an unsigned build rather than failing, because a
fresh clone must build, and the signature is proved afterwards either way.

**The tag workflow is gone.** `release.yml` was a second signing path that
could not sign: it read four secret names (`UPLOAD_KEYSTORE_BASE64` and
friends) that never existed and were not the family's names, so a `v*` tag
would have built quietly and attached an unsigned bundle to a draft release.
The owner chose to delete it rather than repair it, which leaves one signing
path: the `signed-bundle` job in `build.yml`. Tags are labels on commits now,
and no sibling app has a tag-triggered workflow either.

**Public repos and secrets.** The owner asked whether adding secrets to a
public repository exposes them. It does not: a GitHub Actions secret is
write-only, the API returns names and never values (this repo reported
`total_count: 0` before), and all five sibling repositories are public and
have held these same four secrets since they were created. The ways a secret
does leak are a workflow that prints it, a pull request workflow that runs
fork code with it, a compromised action, and a committed key file; the job
avoids all four. The owner then set the four secrets from the vault, and the
key was confirmed as the family's shared upload key before anything was
written to GitHub: alias `my-key-alias`, certificate SHA-256
`537d09d20300129e973b7945316bfe24cfadcfbc77eec5229cbf30170d9de521`.

## D-055: Readable text is one rule, and a gate now checks it

Date: the twelfth session, after the store screenshots were collected.

Collecting the store set is what found it. The tablet search frame showed the
reader `</p><h2>` in the middle of a tafsir result, and the frame only showed
it because this session also fixed the tour to wait for results instead of
photographing "Searching…". The bug predates the session and shipped in 0.3.

**The shape of it.** A tafsir is stored as a small HTML subset (`p`, `h2`,
`strong`, `q`, and so on) because its panels parse it. Search cut its excerpt
from the stored form and printed it through a view with no parser behind it,
so the tags reached the screen. `surah_info` had the same defect on the study
view's "About this surah", which called `plain` while `plain` only removed
footnote markers.

**The fix is one rule in one place.** `RichText.plain` is now the readable
form of anything: it removes tag-shaped runs, removes footnote markers,
collapses spaces, tidies the space a removed tag leaves beside punctuation,
and keeps a paragraph break. `Search.excerpt` takes its window from that
readable form and matches on it too, so the highlight lands on words the
reader can see. Every surface that cannot draw runs reads through `plain`:
sharing, a search excerpt, a tafsir result, a surah introduction.

**What `plain` deliberately does not touch.** A tag is only what looks like
one: `<` followed by a letter or a slash. The sources carry real angle
brackets in prose (surah 63's introduction has `Allah'&gt;` in the raw QUL
export, decoded to a bare `>` by the build), so a rule that stripped every
angle bracket would eat a character of the Quran's own commentary. Entities
are decoded by the content build, not here, and a word list's own brackets
(`disbelieve[d]`, `[the] Last`) are meaning, not markup.

**A new gate catches the class.** `tools search` grew a readable-text audit:
it takes every excerpt it can produce from every installed pack (465 of them
on the current content), checks that no tag survives, and checks that the
highlight still lands on a non-blank word. It also walks all 114 surah
introductions. It runs in CI's content gates, so this cannot come back
unnoticed. The gate is also how the Bangla highlighting question was settled:
its first run reported missing highlights in the Bangla translation, which
turned out to be the gate passing a raw, unfolded term where the app folds
what the reader types. The app was right and the gate was wrong.

**Version 0.5, not 0.4.** 0.4 (versionCode 4) was already submitted, so this
is 0.5 (versionCode 5): same session, next number. The signed bundle comes
from the `signed-bundle` job in `build.yml`, which is now the only signing
path (D-054), and the store set is refreshed from the `screenshots` workflow
for all three form factors.

**A screenshot leg can pass while holding a photograph of Android.** The
10-inch set came back from the first 0.5 capture with "Pixel Launcher isn't
responding" in all sixteen frames, and with the tour on the wrong screens
after it: the dialog had swallowed the taps. The workflow reported success.
Three things changed. The capture checks the window list before keeping a
frame, dismisses a dialog and retakes it, and fails the run rather than write
one into the store listing; the workflow waits for the emulator to settle and
clears a dialog before the tour starts; and the leg now requires the full
sixteen frames instead of one, so a run that stopped early is not a green
tick. The set was recaptured and every frame matches the artifact byte for
byte.

## D-056: The release hand-off, 0.5

Date: the twelfth session, the release session. Version 0.5 (versionCode 5)
went to Google Play for review, and 0.4 (versionCode 4) went in the same
session before it.

**What the hand-off was.** Version 0.5, notes at 235 characters, one unbroken
paragraph, pasted bare in chat with the size and the checksum and nothing
wrapped around the text. The bundle: 147,673,787 bytes, SHA-256
`9bf77817f6a9df05d0964d4683e944b043e008c34afd12516d16903ebf08bdd5`, signed
with the shared upload key (D-017), carrying only the core pack. It was built
and signed by the new `signed-bundle` job (D-054), pulled from the run's
artifact with one `gh run download`, and verified here with `jarsigner` before
being handed over. The hand-off copy was deleted once the owner confirmed the
submission.

**0.4 and 0.5 in one session, and why.** The reader's report was fixed and
raised to 0.4; while collecting the store screenshots for it, a real bug
surfaced (raw tafsir markup in search results, D-055), so the fix went out as
0.5 rather than reopening a submission Play already held. Two releases, one
session, and the same signed bundle path for both.

**The screenshot leg was fixed twice over.** The first 0.5 capture came back
with an Android dialog in every 10-inch frame and the tour on the wrong
screens, and the workflow still reported success. The capture now refuses to
keep a frame a system dialog is sitting over, the workflow clears one before
the tour starts, and a leg must produce all sixteen frames. The set was
recaptured, and every frame of all three form factors was verified byte for
byte against the artifacts before it replaced the committed set.

**The bundle and the screenshots are one delivery, and the screenshots come
first.** The owner said it plainly after 0.5: a set refreshed once they have
submitted to Play has nothing left to be used for, because the store already
holds the old one, so the work is wasted and the session has spent CI minutes
and an emulator run for nobody. Both are now collected, verified, and handed
over in the same message, and the hand-over happens before the submission,
never after it. That is where the ordering belongs: step 5 of the hand-off is
the screenshots and step 7 is the bundle and the set together, with step 6 in
between only because both downloads come from the same pipeline. The rule is
also in the traps, where a session looks when it is about to do the work, and
in `play-store/RELEASE.md` above the upload step.

**A doc-only commit no longer builds anything.** The owner caught this after
the session's close: `build.yml` had no `paths:` filter, so committing four
docs files ran an emulator boot for the data instrumented tests, a full R8
release build, and a signed bundle nobody needed. Every one of the family's
other five apps filters pushes by path with the comment "so doc-only commits
(README, AGENTS.md) don't burn a build"; this one simply never had. It does
now, covering every module, the build files, and the content the build reads
(`content/*.json`, which is what the gates and the asset copy use). A pull
request stays unfiltered on purpose: a pull request exists to be verified
before it lands, and there is no branch protection here, so a filter would
only let one report nothing.

The version bump and the catalog sit inside the filtered paths on purpose:
the commit that raises the version is the commit that builds the bundle, and
a later docs-only commit does not build a second one. Checked against the
last twelve commits: the four docs-only ones now skip, and every code commit
still builds.

## D-057: The reader's report, the thirteenth session

Date: the thirteenth session. The owner read 0.5 on a phone and reported
thirteen things, one of them a download offer that outlived its surah.

**The Mushaf icon is an open book.** The two mode glyphs had drifted into
lookalikes: two page outlines, both ruled. The Mushaf is now the printed Book
itself, two leaves meeting at the spine with a line of reading on each, drawn
at the same stroke weight as the study page beside it, so the pair reads as
two different things at a glance.

**Last Read names a place, and only a place.** The rows carried the ayah's
Arabic and its translation, which made the list a second reading surface: the
reader looking for where they were had to read every row to find it. The
Arabic and the translation are gone from that list; the surah, the ayah, the
mode, and the moment stay, and the text is one tap away. Saved rows keep the
text, because a saved ayah is the thing itself.

**The search filters wrap under a label.** The chips scrolled sideways, which
hid their own contents: a reader cannot turn off a source they cannot see, and
a source they cannot see is one they do not know is on. The group now carries
a "Search in" label and wraps to as many lines as it needs, so every filter is
visible at once. The label answers the other half of the question, which is
whether "Translations" beside a search field is a filter or a kind of result.

**The ayah card's footnotes are doors.** The card printed every translator's
note as a block at the foot of the page while the study reading opened one
from the marker the reader tapped. The card now opens the same sheet from the
same marker (`FootnoteSheet` moved to its own file and used by both), so a
note reads the same wherever it was reached from, and the block is gone.

**The ayah card's doors say that they are doors.** The disclosure arrow was
14 dp at 70 percent opacity, which was too quiet to be seen without a tap.
It is 20 dp at full contrast, it turns from down to up when the content
unfolds, and TalkBack now hears "shown" or "hidden" as the door's state. The
row that only opens Settings for a missing pack is an action, not a door, and
says nothing about a state it does not have.

**The word by word aid is set like the reading.** Its Arabic was drawn at a
flat 14 sp while its Latin meaning sat at 13 or 14, so the word read as a
footnote to its own meaning. The word is now derived from the meaning at the
ayah's own ratio to its translation (30 to 17), which comes to about 24.7 sp
at the middle step, and both scale together with the reader's one choice for
word meanings. The word is centered over its meaning rather than hung at the
edge, because the two are one unit. The study reading and the ayah card draw
the same component (`WordByWord.kt`), so the two surfaces cannot drift apart.

**The reading has a rhythm, and it is named.** The app draws no rules between
the parts of an ayah, so the gaps are the separators, and they had been nudged
one at a time. `ui-kit/theme/Space.kt` now names four steps (Tight 4, Line 8,
Block 16, Section 24) and the study reading, the ayah card, the browse rows,
and the settings pages use them: 16 dp from the ayah to its word list, 16 from
the word list to the translation, a line's height from the translation to the
reference, and a section between two ayahs. Settings rows gained the same
room, and the Appearance page now sets the swatches and the system switch
apart as two decisions rather than one crowded group.

**Two settings names say what they do.** "Text" was the font size page and
"The page" was the theme selector. They are "Font size" and "Theme" now, in
the hub, on the page, and in the code (`SettingsPage.FontSize`).

**The reciter row says what the download is.** A reciter's 1.6 MB pack is the
word timings that let the app mark the word being recited; the audio is
separate and comes one surah at a time. The row said "Installed · 1.6 MB",
which reads as a surah count that never arrives. It now says "Word timings
installed · 1.6 MB" (or "1.7 MB of word timings" before it is added), the page
carries a sentence saying what the timings are for, and the empty list says
that Play on any ayah adds a surah.

**A download offer belongs to its surah.** The reader could tap Play, leave
the offer unanswered, move to another surah, and find the first surah's offer
sitting over the second. `ReaderViewModel.leaveSurah` clears the listen offer
and the player's pending download whenever the reader's place lands in a
different surah, from a jump or from a page turn, using the in-memory surah
index so a page turn pays nothing for the check.

**A finished surah is not still being recited.** The player keeps its last
media item after `STATE_ENDED`, so the ticker kept marking the final ayah as
playing while the surah was over and the continuation offer was up. The
publish path now ignores an ended player, and the ended state clears the
playing ayah before the next surah is offered. The continuation offer also
gained the close button the other pills already had, because a request the
reader has not answered is not a trap.

**One Play, not two.** The ayah card ended with "Play from this ayah", which
the ayah actions bar already offers. It is removed from the card.

**The store set is eight frames now.** The owner's rule from the twelfth
session was that the set is the eight frames the listing names, so the tour
captures exactly those eight in that order (the Mushaf page, the chrome, the
study reading, the surah opening, search, the settings hub, Browse, the ayah
card), the word-by-word test no longer writes a ninth frame, the workflow's
count check moved to eight, and `play-store/listing.md` lists the same eight.

**Trimming the tour exposed a real race.** The word-by-word test used to take
a screenshot after its assertion, and that screenshot was the time the tafsir
prewarm spent finishing. Without it the test ended while the prewarm was still
reading a forty megabyte tafsir, the activity closed the database underneath
it, and the query threw out of the coroutine and took the process down. The
prewarm is speculative work: if the library closes while it runs there is no
index to keep, so it now stops where it is and logs it, while a failure with
the library still open propagates as before.

## D-058: The release hand-off, 0.6

Date: the thirteenth session, the release session. Version 0.6 (versionCode 6)
is handed over for Google Play, and the hand-off is the bundle and the
screenshots in one message, before the submission, as D-056 settled.

**What the hand-off was.** The version line, the notes, and the set are the
same release: 0.6 raises the version, `play-store/listing.md` carries the
notes under their own heading, and the screenshot set is refreshed from the
`screenshots` workflow's artifacts for all three form factors, each frame
verified byte for byte against the artifact with `cmp` before it replaced the
committed set. The signed bundle comes from the `signed-bundle` job in
`build.yml` (D-054) and is pulled into `play-store/aab/` for the hand-over,
then deleted once Play has it.

**The bundle.** 147,667,170 bytes, SHA-256
`8c620c231f219993713e5248469ae1d048e407e8bec7ccb048b5e1dc282f6bc3`, signed
with the shared upload key (D-017; the certificate's SHA-256 is
`537d09d20300129e973b7945316bfe24cfadcfbc77eec5229cbf30170d9de521`), carrying
only the core pack. It was built by the `signed-bundle` job on the push that
carried the fixes and the version bump, and pulled from that run's artifact.

**The set.** Eight frames per form factor, twenty-four in all, the eight the
listing names, every one compared with its artifact by `cmp`. The set is new
in three ways: the search frame shows the filters wrapped under their label
instead of scrolling off the edge, the study frame shows the word by word aid
at its new size with each word centered over its meaning, and the ayah card
frame no longer carries the translator's notes block or the repeated Play
action while its doors carry a visible arrow. The first capture attempt
failed on the phone and 7 inch legs, and the failure was real: the trimmed
tour exposed the prewarm race above, so the fix is what made the set
capturable.

**The submission.** The owner confirmed that Play had the submission, and the
hand-off copy was deleted from `play-store/aab/` in the same breath, so no
signed bundle sits in the repository or on the machine waiting to be uploaded
twice. `play-store/aab/` keeps only its own note about where a bundle comes
from and when it goes.

## D-059: The reader's report, the fourteenth session

Date: the fourteenth session. The owner read 0.6 on a phone and reported ten
things, the first of them a defect the screenshots had carried since the first
release.

**The Mushaf's lines were reversed.** `PageRenderer` measured each line's
words and then drew them from its left edge in word order, so every line of
the printed page read backwards: the reader saw the line's last word first.
Study mode was correct because Android shapes and orders a whole Arabic
string; the Mushaf draws one word glyph at a time, and the direction was left
to the caller. The renderer now starts each line at its right edge (a
centered line at the right edge of its own span) and walks left as it draws,
and the touch boxes are built from the same x, so a tap still lands on the
word it names. A reversed line still looks like Arabic, which is why four
sessions of screenshots had shown it; the owner's eye found it.

**The two mode icons are one pair again.** The open book drawn in D-057 read
as an inverse book: its pages met in a tall center instead of sagging into
the fold. The Mushaf is now the Book itself, open, with flat page heads and
feet and the spine dipping below the pages at the fold; the study reading
stays the single flat ruled page; both use the same 0.075 stroke as the rest
of the set (`pageOutline` had kept 0.085).

**A footnote is part of the text it annotates.** The footnote sheet was fixed
at `bodyLarge` with a 26 sp line, whatever the reader had chosen for the text
the note belonged to. `OpenFootnote` now carries the size and line height of
the text the marker came from, so the translation's notes follow the
translation size setting, and a tafsir's notes will follow the tafsir size
the day a tafsir carries any (`RichText.footnotes` only recognizes the
translation's `[n]` markers today). The sheet takes the two values as its
own parameters, so the caller names the text the note belongs to.

**About this surah is dismissed like a transient.** The expanded state lived
inside the opening item, so only the Hide link could put it away. It moved up
to `StudyRows`: a tap on the paper, on any ayah, or on the closing line
collapses the about and does nothing else that tap would have done, the
opening item is inert while the about is open, and Hide still toggles. The
state is saveable and keyed by surah, as it was.

**Opening a surah from Browse lands at its top, or where the reader left.**
`ReaderViewModel.openSurah` reads the newest place in that surah from Last
Read and jumps there when one exists. Otherwise it jumps to the first ayah
and raises a one-shot `startAtSurahOpening` request; the study list answers
it once by scrolling to its opening item and clears it. The request is a
separate `LaunchedEffect` and the place effect is not keyed on it, because a
place effect that re-ran when the request cleared would pull the reader back
down to the first ayah the moment the opening appeared. `AyahList` is
untouched. The Juz rows were found to open the start of the surah rather than
the ayah the row itself names; they now open `start.ayah` (2:142 for Juz 2).
The next-surah link and search keep `jumpToSurah`, which goes to the first
ayah.

**Browse has no title.** The four tabs name everything the sheet holds, so
"Browse" over them spent the first line of the sheet on a word the reader had
already chosen. The tabs keep their own room under the drag handle, and the
string is gone from the module.

**A Last Read row has one action.** The row is the door: tapping a place
opens it. The Open label beside Forget was a second door to the same room,
so Last Read rows keep only Forget. Saved rows keep their Open and Remove,
because the owner's report was about the last-read list and the two lists
are not the same list.

**A scroll back up in Browse cannot pull the sheet closed.** A pull down and
a scroll back to the top are the same finger movement: the list reports only
what it could not scroll, and by then it is already at its top, so the
leftover delta cannot tell the two apart and the sheet's own nested scroll
handling dragged the sheet with it. `core/SheetDragPolicy` decides by the
gesture's beginning: a drag that starts with the list scrolled keeps its
leftovers for the whole gesture, drag and fling alike, so the sheet never
sees them; a drag that starts with the list at its top passes through, so
pull to close is unchanged. The policy is pure and has unit tests;
`feature-browse/SheetDragGate` is the Compose connection that feeds it the
list's state and keeps its answers. One gate belongs to one tab's list.

**The reciter downloads are a door, and they belong to their reciter.** The
label gained the disclosure arrow the rest of the app uses for a door (down
closed, up open), the whole row is tappable, the block is indented to sit
under the reciter's name, the surah rows sit close together (each row is
already 48 dp tall because Remove carries its own touch target), and the gap
above the block is smaller than the gap to the next reciter. The page note
now says plainly that the small download is the word timings and not the
audio, which was the owner's question: the timings are one map for the whole
Quran for that reciter, while the audio is one surah at a time and can be a
hundred megabytes each.

**Verification.** The JVM suite (core 55, data 9, app unit), lint, the data
instrumented tests (16), and the app instrumented tests (11, the screenshot
tour included) are green. `checkdb` and `search` are green; `verify`,
`audit`, and `fonts` need `content/raw`, the owner's manual QUL and QuranEnc
exports, which this session's machine did not carry, and must run on the
machine that owns them before the hand-over.

## D-060: The release hand-off, 0.7

Date: the fourteenth session, the release session. Version 0.7 (versionCode 7)
is handed over for Google Play, and the hand-off is the bundle and the
screenshots in one message, before the submission, as D-056 settled.

**The bundle.** 147,678,451 bytes, SHA-256
`1f7b882981bc5319461a96dcde1d01d1f2762083c095c5a7a9e05d4116c2595b`, signed
with the shared upload key (D-017; the certificate's SHA-256 is
`537d09d20300129e973b7945316bfe24cfadcfbc77eec5229cbf30170d9de521`), carrying
only the core pack. It was built by the `signed-bundle` job on the push that
raised the version and carried the fixes, and pulled from that run's artifact
into `play-store/aab/quran-0.7-vc7.aab`, its checksum beside it.

**The set.** Eight frames per form factor, twenty-four in all, the eight the
listing names, every one compared with its artifact by `cmp` before it
replaced the committed set. The set changed where the reader's eye would: the
chrome frame carries the redrawn mode icons, the Mushaf frame reads right to
left, and the Browse frame has no title. The 7 inch leg failed its first
capture with a system dialog over `01-mushaf`; the capture guard refused to
keep it and failed the run rather than write the dialog into the listing, so
the leg was rerun and passed. The phone and 10 inch legs were green on the
first attempt.

**The hand-off.** The set was installed from the artifacts, never captured by
hand, and the bundle was flattened out of the artifact's workspace path into
`play-store/aab/` with its checksum verified locally. Both were handed over in
one message, before the submission.

**The submission.** The owner confirmed that Play had the submission, and the
hand-off copy was deleted from `play-store/aab/` in the same breath, so no
signed bundle sits in the repository or on the machine waiting to be uploaded
twice. `play-store/aab/` keeps only its own note about where a bundle comes
from and when it goes.

## D-061: The reader's report, the fifteenth session

Date: the fifteenth session. The owner read 0.7 on a phone and reported seven
things, one of them the part of the fourteenth session's fix that had not gone
far enough.

**The long press floats.** The ayah actions bar sat flush with the foot of the
page as a rounded bar. It is a full radius capsule with a 6 dp shadow now, the
shape the playback pill already wears, so a long press raises the actions over
the reading instead of adding a bar to it. `AyahActions` in the app module is
the one place both reading modes draw it from.

**The top bar reads smaller.** `ReadingTitle` moved the surah name from
`titleMedium` to `titleSmall` (16 to 15 sp) and the juz from `bodySmall` to
`labelMedium` (13 to 12 sp), and both reading modes draw through it.

**The Mushaf turns right to left.** The fourteenth session fixed the order of
the words inside a line but left the pager itself running left to right. The
`HorizontalPager` now carries `reverseLayout = true`: page 1 opens on the
right, the next page lies to its left, and a swipe to the right turns forward.
A still frame cannot show which way a page moved, which is exactly how the
reversed lines survived four screenshot sets, so `MushafTurnTest` in the app's
instrumented tests pins the direction: it swipes right to page 2 (Al-Baqarah)
and left back to page 1, and reads the exposed ayah nodes to know which page
is the one on screen.

**A scroll that comes home cannot close Browse.** The fourteenth session's
`SheetDragPolicy` judged a gesture by its first delta, but a fling that a new
touch interrupts never reports its end, so a record could stay open and its
beginning could judge the next gesture. The policy now lets a gesture that
finds the list scrolled own its leftovers, and `Modifier.sheetDragGate` watches
the finger landing (`PointerEventPass.Initial`, never consumed) so every touch
opens a fresh record. Pull to close from the top is unchanged, and the
policy's new cases have unit tests.

**Search filters say what they are.** The chips stay, because a row of toggle
chips is what a multi select scope filter is: Material's own `FilterChip`
exists for it, and checkboxes read as form controls, take more room, and scan
slower. What was missing was the affordance, so a chosen chip now carries the
app's drawn check beside its label. When every source is off, the status line
says "Turn on at least one filter to search." instead of a false "No matches.";
`SearchSources.any` is the one place that knows whether anything is on.

**Back steps out of a settings page.** The handler was registered in the
activity's window, but `ModalBottomSheet` lives in its own dialog window with
its own back dispatcher, which dismissed the sheet before the activity saw the
event. The handler now lives inside the sheet's content, where it registers on
the sheet's dispatcher after the sheet's own callback and wins. Appearance
returns to the hub, and only a back at the hub closes Settings. The emulator
run confirmed it: the frame after back was byte-identical to the hub frame.

**The downloaded surahs belong to the reciter's row.** The door lost the gap
above it: the reciter's `PackChoiceRow` gives up its 12 dp foot when a
downloads door follows, so the label sits about 14 dp under the reciter where
it used to sit about 26. The arrow sits immediately after a label that never
changes, `Downloaded surahs (N)`, with the arrow turning, instead of at the far
right of a row whose name flipped between two names.

**Verification.** The JVM suite (core 57, data 9, app unit), lint, and
`assembleDebug` are green; `checkdb` and `search` are green; the data
instrumented tests (16) and the app instrumented tests (12, the screenshot tour
and the new turn test included) are green on a phone profile. `verify`, `audit`,
and `fonts` still need the owner's manual QUL and QuranEnc exports, which this
machine does not carry (only the font pack is in `content/raw`), so they must
run on the machine that owns them before the hand-off is closed; `audit` and
`fonts` fail here with "no database for quran-script-kfgqpc; run verify first",
which is the missing export, not a content failure.

## D-062: The release hand-off, 0.8

Date: the fifteenth session, the release session. Version 0.8 (versionCode 8)
is handed over for Google Play, and the hand-off is the bundle and the
screenshots in one message, before the submission, as D-056 settled.

**The bundle.** 147,681,534 bytes, SHA-256
`b7cbe82f1c66ab66fed4ce8a7466f78956e3d74fc1886dec39b5bbd3a7bdce92`, signed
with the shared upload key (D-017; the certificate's SHA-256 is
`537d09d20300129e973b7945316bfe24cfadcfbc77eec5229cbf30170d9de521`), carrying
only the core pack. It was built by the `signed-bundle` job on the push that
raised the version and carried the fixes, and pulled from that run's artifact
into `play-store/aab/quran-0.8-vc8.aab` with its checksum verified locally
against the artifact's own file.

**The set.** Eight frames per form factor, twenty-four in all, the eight the
listing names, every one compared with its artifact by `cmp` before it
replaced the committed set. Two frames changed for the reader: the chrome
carries the smaller top bar, and the search filters show their checks. The
screenshots workflow was given `MushafTurnTest` too, so the page direction is
checked on all three form factors, not only on the machine that built the
bundle. All three legs were green on the first attempt.

**The hand-off.** The set was installed from the artifacts, never captured by
hand, and the bundle was flattened out of the artifact's workspace path into
`play-store/aab/` with its checksum verified locally. Both were handed over in
one message, before the submission.

**The submission.** The owner confirmed that Play had the submission, and the
hand-off copy was deleted from `play-store/aab/` in the same breath, so no
signed bundle sits in the repository or on the machine waiting to be uploaded
twice. `play-store/aab/` keeps only its own note about where a bundle comes
from and when it goes.

**The gates.** `checkdb` and `search` are green on this machine; `verify`,
`audit`, and `fonts` need the raw QUL and QuranEnc exports, which this machine
does not carry (only the font pack is in `content/raw`). Their failure here is
the missing export, not a content check: `audit` and `fonts` stop with "no
database for quran-script-kfgqpc; run verify first", and `verify` lists the
missing raw files. They must run on the machine that owns the exports before
the hand-off is closed, as the fourteenth session noted.

## D-063: The reader's report, the sixteenth session

Date: the sixteenth session. The owner read 0.8 on a phone and reported seven
things, and confirmed the fallback rule the reciter removal should use.

**The size sample is a short ayah.** The font size page previewed the reader's
own place, and a long ayah pushed the steps off the screen before the reader
reached them. The page now previews Al-Ikhlas 112:1 through
`ReaderViewModel.sizePreviewRow`, so the sample is there to judge a size and
nothing else.

**A removed reciter is never the default.** Removing the selected reciter left
it selected, so the settings row named a voice with nothing behind it. The
removal now writes a fallback in the same breath: the remaining reciter with
the most audio on the device wins, because that is the one the reader has
actually been listening to; with nothing downloaded anywhere the fallback is
Husary (`FALLBACK_RECITER`), and only then the list's own order. A removed
reciter is never chosen, even if a file of his lingers. `selectRecitation`
also moves an ayah already playing through the same door Play uses, so a
reciter whose timings are not on the device is offered, never reported
unavailable.

**The Reciters page downloads nothing and explains nothing.** Choosing a
reciter only chooses; the word timings and the surah's audio arrive together
from the Play offer, which is where the choice already was. The page keeps one
radio row per reciter, the downloaded surahs under it, and a Remove only for a
reciter that has content. The note about the timings is gone with the install
action it explained. The offer's reciter name gained a chevron, because the
dropdown had been there since 0.2 and could not be found. The downloaded surah
rows also sit tighter: their Remove actions keep a 40 dp target instead of
48 dp, while the door above them keeps the full target.

**The modes are one switch in the center of the top bar.** The two reading
icons sat at the head of the row of doors, reading as one more door. They are
one segmented switch now, centered, with the active mode marked and both
spoken as radio choices; the title keeps a reserve to its right so a long
surah name ellipsizes instead of running under it.

**Arabic inside a Latin tafsir stands at 1.4 to 1.** The tafsir's own size set
its Arabic too, and the same nominal size reads smaller in Arabic, so a quoted
passage vanished into the prose. `TextSize.tafsirArabic` gives the Arabic the
stand the study translation already gives its inline Arabic (0.8 of the 30 sp
Arabic against the 17 sp translation, 1.4 to 1), so the app has one answer to
how much larger the Arabic is than the text beside it. A block that carries
Arabic keeps the line height the Arabic needs, which is the behavior the
reading already had.

**The ayah card does not repeat the floating bar.** Save and Share sat at the
card's top right, one step after the pill that already carries both. They are
gone, and the card opens on its own reference.

**About this surah is pressed where it opens.** The whole opening was one
press target, so a press lit the area from the Arabic name down to Hide. Only
the paragraph and its About/Hide control are targets now; a tap on the heading
still dismisses the about or brings the chrome through the list.

**Verification.** The JVM suite (core 57, data 9, app unit), lint, and
`assembleDebug` are green; the app instrumented tests (12, the screenshot tour
included) are green on a phone profile, and each behavior was checked by hand
on the emulator: the centered switch, the short size sample, the chevron and
dropdown on the offer, the Reciters page with its tightened rows, removal
falling back to Husary, the larger tafsir Arabic, the trimmed ayah card, and
the About area.

## D-064: The release hand-off, 0.9

Date: the sixteenth session, the release session. Version 0.9 (versionCode 9)
is handed over for Google Play, and the hand-off is the bundle and the
screenshots in one message, before the submission, as D-056 settled.

**The bundle.** 147,684,684 bytes, SHA-256
`85ea5bcf520d800476f9b490f470cdffa0db787185635662961db95ac80153dc`, signed
with the shared upload key (D-017; the certificate's SHA-256 is
`537d09d20300129e973b7945316bfe24cfadcfbc77eec5229cbf30170d9de521`), carrying
only the core pack. It was built by the `signed-bundle` job on the push that
raised the version and carried the fixes, and pulled from that run's artifact
into `play-store/aab/quran-0.9-vc9.aab` with its checksum verified locally
against the artifact's own file. The job's first run failed its core tests on
a transient plugin resolution error; the rerun was green.

**The set.** Eight frames per form factor, twenty-four in all, the eight the
listing names, every one compared with its artifact by `cmp` before it
replaced the committed set. Two frames changed for the reader: the chrome
carries the two reading modes as one centered switch, and the ayah card no
longer repeats Save and Share. All three legs were green on the first
attempt.

**The hand-off.** The set was installed from the artifacts, never captured by
hand, and the bundle was flattened out of the artifact's workspace path into
`play-store/aab/` with its checksum verified locally. Both were handed over in
one message, before the submission.

**The submission.** The owner confirmed that Play had the submission, and the
hand-off copy was deleted from `play-store/aab/` in the same breath, so no
signed bundle sits in the repository or on the machine waiting to be uploaded
twice. `play-store/aab/` keeps only its own note about where a bundle comes
from and when it goes.

**The gates.** All five content gates are green on this machine, including the
three the fifteenth session could not run: `verify`, `audit`, and `fonts` read
the raw QUL and QuranEnc exports in `content/raw` and passed, so the 0.8
caveat is closed.

## D-065: The reader's sixth report, the seventeenth session

Date: the seventeenth session. The owner read 0.9 on a phone and reported six
things, and a design discussion settled the top bar.

**The top bar is two rows, centered.** The one-row bar could not hold the
surah name whole on a phone: the title slot is `screen width - 276dp` once
the switch and the three 48dp doors are counted, so the longest of the 114
names (Al-Muddaththir, about 106dp) clipped below a 382dp screen. The bar is
two rows on every size now: the controls first (Browse at the head, the two
reading modes centered on the screen, Search and Settings at the end), then
the surah and its juz centered as one unit beneath them. The switch keeps the
exact position D-063 gave it, the name has the whole width, and the scrim
stays solid through both rows and gives way only under the name.
`ReadingTitle` centers the pair in its row and still draws the juz only when
it fits beside the whole name. Al-Muddaththir Juz 29 is whole at 320dp,
360dp, and 411dp. A bottom placement for the switch, in the ayah pill's slot,
and a title-first row order were both weighed and set aside: the controls
group in one row, the title reads as a caption over the page, and the switch
never moves. Material's large app bar puts the actions above the title for
the same reason.

**The shapes answer in the reading's own language.** The expanded surah
introduction was a bare `clickable`, so its ripple was a hard rectangle; it
is clipped to the 14dp rounded shape the ayah press uses. The word being
recited in the study reading was a `SpanStyle` background, which is always a
rectangle and a style boundary inside an Arabic word besides; it is drawn
now through the text layout's own bounding boxes as the same rounded wash the
Mushaf draws, so both modes share one word mark. The Mushaf mark was brought
to the same corner proportion.

**The end of a surah names the next one.** "Continue to X" was plain text
with no sign that it could be pressed. It is a card now: a quiet "Next surah"
label, the name, and a chevron that points the way the reading goes.

**The word list stays the translation's.** An earlier fix in this session
added a stored word list choice; the owner asked for it to be reverted and
the app made simpler. The meaning language is not a choice, there is no
selector, no stored pack, and no new preference: the aid speaks the language
of the first translation turned on, then English, exactly as it did before.
`WordByWordTest` still pins that path.

**Last Read keeps its twenty places.** The cap was measured rather than
guessed: the database is three 4KB pages, 12,288 bytes, with 0, 10, or 20
rows, and a row costs about 20 bytes. Capping to ten would save a quarter of
one page and halve the history, so the list stays a list of twenty places.

**Verification.** The JVM suite (core, data, app), lint, and `assembleDebug`
are green; the data instrumented tests (16) and the app instrumented tests
(12, the screenshot tour included) are green on the phone profile. The bar
was checked with Al-Muddaththir at 320, 360, and 411dp, and the rounded word
mark, the About shape, and the next-surah card were checked on the running
app.

## D-066: The release hand-off, 0.10

Date: the seventeenth session, the release session. Version 0.10 (versionCode
10) is handed over for Google Play, and the hand-off is the bundle and the
screenshots in one message, before the submission, as D-056 settled.

**The capture was rebuilt on the family's pattern.** The phone leg failed
five times with the emulator dying mid test (`device offline`, then `device
not found`), while the tablet legs passed and the build workflow's own phone
emulator passed beside it. The difference was the tour itself: it drove the
running app and kept frames with `Screenshot.capture()` through a settle loop
of up to twenty full-screen captures per frame, and that load is what took
the phone emulator down. A frame is now kept from the Compose root with
`captureToImage`, one capture per frame, with one retry for a stalled
PixelCopy. A sheet lives in its own window, which the Compose root cannot
PixelCopy, so the four sheet frames (search, settings, Browse, and the ayah
card) take one display capture each, the window list checked first. The whole
tour runs in about half a minute on a phone profile, and the dialog guard is
no longer needed for the root frames: a system dialog cannot enter a frame it
is not part of.

**The set.** Eight frames per form factor, twenty-four in all, the eight the
listing names, every one compared with its artifact by `cmp` before it
replaced the committed set. The set changed where the reader's eye would: the
chrome is two rows with the surah and juz centered under the controls, the
study frame marks the recited word with the same rounded wash the Mushaf
uses, and the About paragraph answers in that shape. All three legs were
green on the first attempt with the rebuilt capture.

**The bundle.** 147,691,199 bytes, SHA-256
`320f7b8de7ce0bcbff01eb97c185215df000efb1bb6dcf2ee6984b12b618b0af`, signed
with the shared upload key (D-017; the certificate's SHA-256 is
`537d09d20300129e973b7945316bfe24cfadcfbc77eec5229cbf30170d9de521`), carrying
only the core pack. It was built by the `signed-bundle` job on the push that
carried the rebuilt capture and pulled from that run's artifact into
`play-store/aab/quran-0.10-vc10.aab` with its checksum verified locally
against the artifact's own file.

**The gates.** All five content gates are green on this machine: `verify`,
`audit`, `fonts`, `checkdb`, and `search` read the raw QUL and QuranEnc
exports in `content/raw` and passed.

**The delay, and the lesson.** The phone leg failed five times and cost about
forty minutes, every failure reading as an emulator flake (`device offline`,
then `device not found`), every retry ending the same way. The working clue
was already on the machine: the family's other apps in `Documents/GitHub`
capture their store sets from the Compose root, one frame at a time, and never
drive the running app through full-screen captures. The workflow was the same
shape all along; the test was not. A leg that dies while a sibling leg passes
the same code is the capture until proven otherwise. Read the failing job
against the passing one, and read the family's repos, before spending a second
retry.

**The submission.** The owner confirmed that Play had the submission, and the
hand-off copy was deleted from `play-store/aab/` in the same breath, so no
signed bundle sits in the repository or on the machine waiting to be uploaded
twice. `play-store/aab/` keeps only its own note about where a bundle comes
from and when it goes.

## D-067: The reader's seventh report, the eighteenth session

Date: the eighteenth session. The owner read 0.10 on a phone and asked for
five things, one of them a defect in the version discipline itself.

**The top bar is one row again, with one mode door.** The two-row bar of
D-065 is gone. The reader's list is the single-row lineup with the two-choice
switch replaced by one icon: Browse and the mode door lead, the surah and its
juz sit centered between the two pairs, and Search and Settings close the row.
The mode door shows the mode the reader is not in (the study page while in
the Mushaf, the open Book while studying) and a tap takes them there; it wears
the accent because it is the reading itself, not another tool. Two doors on
each side is what keeps the title on the screen's own center, and the single
icon gives the name the room the old switch took. The trade is named and
accepted: on a 320dp phone the longest names still clip, which D-065 measured;
the owner chose the one row knowing that.

**The reciter chooser wears the pill's cloth.** The offer's dropdown was
Material's own menu, a different surface and a square shape, and it read as a
foreign sheet laid over the pill. It is now a rounded 20dp menu of the pill's
own surface color with a hairline, and the chosen reciter carries a check.
The rows inside it are the app's own rounded press targets, not stock menu
items.

**Word by word is one switch in the hub.** The Words page and its list of
language packs are gone. The hub carries the switch directly, and turning it
on fetches the word list that speaks the first chosen translation's language,
with the size on the row before the tap and the progress on the row during
it. The switch reads on only when the aid is on and its list is on the
device, so an on switch always means meanings are being drawn; a missing list
is named with its size, and the same tap adds it. The word list still follows
the translation's language (D-065), and the language counts as chosen even
before its pack is installed, so a Bangla reader is never offered the English
list while the Bangla translation is on its way.

**The interface speaks Bangla, and the choice is one.** A first-launch screen
asks for the language, each choice named in its own script, with the phone's
own language marked Suggested. The choice sets the interface, the translation,
the tafsir, and the word meanings together, through one `UiLanguage` model in
`data`, and it can be changed from a Language row at the top of the settings
hub. The content
packs are selected, never fetched: the sizes stay on the rows that install
them, and nothing is downloaded before the reader asks. `values-bn` carries
every string in all eight modules; the frozen names (`app_name`,
`first_paint_title`, `first_paint_subtitle`, `about_title`) are marked
`translatable="false"` and never move (D-001). The bundle disables language
splitting, because a split would hand a phone only the language it asked for
at install time and the in-app switch would find the Bangla strings missing.

**The locale lives on the Activity, not in a composition local.** The first
attempt wrapped the composition in a localized context. It worked for the
reading and failed for every modal surface: a sheet, a dialog, and a popup
are their own windows and take the Activity's resources, so the settings
sheet came up English over a Bangla reader. The locale is now applied in
`MainActivity.attachBaseContext`, from a synchronous mirror
(`LanguagePreference`) that the choice writes before it recreates the
Activity, so every window speaks the chosen language from its first frame.
The mirror is a SharedPreferences file only because `attachBaseContext` runs
before any coroutine can read the DataStore; the DataStore stays the source
of truth.

**A screen-level state read belongs in the screen's own scope.** With the
composition-local wrapper, the `when` that chose between the first paint, the
welcome, and the reader sat inside a nested lambda, and the switch from the
first paint to the welcome was missed deterministically on this machine until
a configuration change forced a recomposition. Reading `ready` and `failure`
in `QuranApp`'s own scope fixed it. The wrapper is gone now, but the lesson
stays: the state a screen switches on is read where the screen is, not one
lambda down.

**The version convention is written down.** The first version was 0.1 and
0.10 was a mistake: the tenth release of a major line is its `x.0`. The
release runbook now says 0.1 through 0.9, then 1.0, then 1.1 through 1.9,
then 2.0, and this session's release is 1.0 (versionCode 11).

**Verification.** The JVM suite (72), lint with no issues, the data
instrumented tests (16), and the app instrumented tests (12, the screenshot
tour included) are green on the phone profile. The welcome screen, the
Bangla reading, the settings hub, the search sheet, Browse, the word toggle,
and the reciter chooser were walked on the running app. All five content
gates are green on this machine.

## D-068: The release hand-off, 1.0

Date: the eighteenth session, the release session. Version 1.0 (versionCode
11) is handed over for Google Play, and the hand-off is the bundle and the
screenshots in one message, before the submission, as D-056 settled.

**The version name follows the runbook now.** 0.1 through 0.9, then 1.0; the
0.10 of the seventeenth session was the mistake the owner named, and it is
kept in the history rather than rewritten. The runbook carries the rule.

**The bundle.** 147,705,241 bytes, SHA-256
`2b13117d1bfe1358e8590bcecb72749ad28284bdb2d1e28cbad64f1f7bd20c3c`, signed
with the shared upload key (D-017; the certificate's SHA-256 is
`537d09d20300129e973b7945316bfe24cfadcfbc77eec5229cbf30170d9de521`), carrying
only the core pack and both interface languages (language splitting is
disabled, so a phone set to English still carries the Bangla strings the
in-app switch needs). It was built by the `signed-bundle` job on the push of
`8a484b6`, pulled from run 35535699655's `quran-signed-aab` artifact into
`play-store/aab/quran-1.0-vc11.aab`, and its checksum was verified locally
against the artifact's own file.

**The set.** Eight frames per form factor, twenty-four in all, from run
35535699669, all three legs green on the first attempt. Six frames changed
for the reader: the chrome is one row with the mode door, so every frame
with the chrome behind a sheet changed with it, and the settings hub gained
the Language row and the word by word switch. Every frame was compared with
its artifact by `cmp` before it replaced the committed set, and the frames
were looked at before they were installed.

**Verification.** The JVM suite (72), lint with no issues, and
`assembleDebug` and `bundleRelease` are green locally; the data instrumented
tests (16) and the app instrumented tests (12, the screenshot tour included)
are green on the phone profile; and all five content gates (`verify`,
`audit`, `fonts`, `checkdb`, `search`) are green on this machine. The
welcome screen, the Bangla reading, the settings hub, the search sheet,
Browse, the word toggle, and the reciter chooser were walked on the running
app before the push.

**The submission.** The owner confirmed that Play had the 1.0 submission, and
the hand-off copy was deleted from `play-store/aab/` in the same breath, so no
signed bundle sits in the repository or on the machine waiting to be uploaded
twice. `play-store/aab/` keeps only its own note about where a bundle comes
from and when it goes.

## D-069: The reader's eighth report, the nineteenth session

Date: the nineteenth session. The owner read 1.0 on a phone and reported
sixteen things, one of them fatal. This session fixed the fatal defect, the
one visible defect behind it, and the wording and layout the owner asked
for, and raised the release to 1.1 (versionCode 12).

**Turning Mushaf pages quickly killed the app.** The launch picture is
written on a worker after each settle, and two settles could race: one writer
was still compressing into `page.webp.part` while another had already renamed
it to `page.webp`, so the loser reached `copyTo` on a file that no longer
existed. `runCatching` covered `compress` but not the copy, so the
`NoSuchFileException` escaped the coroutine and took the process down on the
main thread. It is the stop-the-line class: the reader turned a page, turned
back, and the app closed. `PageCache.save` is now `@Synchronized` and guarded
whole, so no filesystem surprise can escape. Validating that fix on the
emulator surfaced a second race the first had hidden: the page LRU recycles a
bitmap while the writer compresses it, so `Can't compress a recycled bitmap`
threw where the file race used to. `PageRenderer.rememberStartupPage` now
copies the bitmap while the cache lock is held, writes the copy, and recycles
only the copy. Verified on the phone profile with the reported pattern (right,
right, left) and with hundreds of flicks: no crash, and the launch picture
now lands reliably where it silently failed before.

**The reciter chooser is the pill, exactly.** D-067 gave the chooser the
pill's surface and a 10 dp shadow and a hairline, and the reader still read
it as a foreign sheet because the pill carries neither. The shadow and the
border are gone; the menu is the pill's surface color, its rounded shape, and
nothing the pill does not have. The reference is the pill, not a Material
menu.

**Search wears the same drag gate Browse has.** A scroll back to the top of
the results pulled the sheet closed, the same defect Browse had and D-063
fixed there. `SheetDragGate` and its pure `SheetDragPolicy` moved from
`feature-browse` into `ui-kit`, beside the shared reader chrome, so both
sheets share one policy: a gesture that started with the list scrolled
belongs to the list, and only a gesture that started at the top can pull the
sheet down.

**The search order is Mushaf order, and it is now written down.** A reference
row (a query like `2:255`) leads, then surah-name rows, then ayah and tafsir
rows together sorted by ayah number, tafsir after an ayah at the same number.
There is no relevance ranking, by design: results stay in the order of the
Book.

**Settings are tidier.** The word by word switch moved under the Translations
list it is bound to; Follow the reciter moved to the top of the Reciters
page; Keep the screen awake moved into the hub beside the other whole-app
choices. The Reading page held only those two switches and is deleted, with
its page enum member and its strings.

**Bangla wording, corrected from the reader's list.** Last Read is now
সর্বশেষ পঠিত (the tab), Theme is থিম (was রূপ), Font size is ফন্ট সাইজ (was
লেখার আকার), the Arabic size row is কুরআনের আয়াত (was কুরআনের লেখা), the four
theme names are পেপার, সেপিয়া, নাইট, ব্ল্যাক, word by word is শব্দে শব্দে অনুবাদ
everywhere, and Remove is মুছুন in the settings packs too (the browse `action_remove`
was already correct; only `pack_action_remove` lagged). Sepia is সেপিয়া, not
সিপিয়া: Bengali Wikipedia and standard dictionaries use সেপিয়া for the
cuttlefish ink and the color, and সিপিয়া is a homeopathy-writing variant.

**Bangla surah and pack names: searched, not implemented.** QUL's
`metadata-surah-names` (resource 70) is English and transliterated only, and
QUL has no separate Bangla surah-name dataset. quran.com's chapter metadata
with `language=bn` carries a ready Bangla name for all 114 (Al-Fatihah is
সূচনা, Al-Baqarah is বকনা-বাছুর, Al-Ikhlas is আন্তরিকতা), but it is the name's
meaning, not a transliteration, and it is a new dataset with new terms. Bangla
transliterations (সূরা আল-ফাতিহা) are widely published but not as one pinnable
source. For packs, authentic renderings exist and are already in the app's own
About credits: Minshawi is মিনশাবী and Husary is হুসারী, with ইবনে কাসীর and
তাইসীরুল কুরআন for the two Bangla packs; the English and Arabic packs have no
authentic Bangla name and stay Latin. Localizing pack names is a content-side
change (a name field per pack or a name map), so it is content backlog and
not in this release. The owner asked to be told first, and was.

**Two reports were not reproduced, and that is said plainly.** The owner
reported (1) the first-launch language choice crashing once and (5) opening
Browse after switching to Bangla crashing once, both working on the second
open. Neither reproduced on this machine across repeated first-launch choices
and settings language switches, before or after the crash fix. The language
path does open a window where the in-memory settings name the new pack before
`reopenLibrary` has attached it, and a later session with the reader's report
in hand should harden that path rather than assume it clean. Nothing was
changed there blind.

**Verification.** The JVM suite, lint with no issues, and `assembleDebug` are
green locally; `checkdb` (10 pack files, the committed database) and `search`
(65 Bangla round trips, 63 Arabic, 465 excerpts) are green. The settings hub,
Theme, Translations with the word toggle, Reciters with the Follow switch,
installing a translation, and Remove were walked on the running phone profile.
The owner-machine gates `verify`, `audit`, and `fonts` need `content/raw`,
which this machine does not carry, and are left for the machine that owns the
raw sources.

## D-070: The screenshot workflow, made to not fail and not be slow

Date: the nineteenth session, after the reader's eighth report. The push
that raised 1.1 failed `screenshots.yml` on all three legs. The cause was
mine and it was avoidable: the tour waited on the visible string
`Appearance`, and the session renamed that row to `Theme`, so the wait timed
out over a correct copy change. The same push also exposed three older
weaknesses in the workflow, all closed here.

**The tour anchors on tags and content descriptions, never on copy.**
`ScreenshotTest.waitForTag("settings-hub")` waits for a stable
`Modifier.testTag` on the settings hub; the settings label can be renamed
forever without touching the tour. A tour that waits on user-visible copy is
a tour that fails on a wording change, which is not a defect. The one
remaining `waitFor` is a data string (a surah name), which does not move.

**The capture build carries only the packs its tests install.** A full debug
build bundles every pack (197 MB of assets, the APK over 380 MB uncompressed),
and dexing and installing that is most of a capture's wall time. The build
grew `-Pquran.devPacks=screenshot`, which keeps the five packs the two capture
tests install (Saheeh, English words, Ibn Kathir English, Taisirul Quran, Bangla
words) and drops the rest. The release bundle is untouched: this is a
property on the debug asset task alone.

**The workflow uses the house pattern.** `gradle/actions/setup-gradle`
restores the Gradle build cache, not only the dependency cache, so a fresh
runner does not recompile every module; the AVD is cached per form factor as
before; and the fixed `sleep 20` before the test became a bounded loop that
waits for the system to settle and dismisses a dialog if it must. Count and
dialog checks stay.

**The trigger watches every module that draws.** The old `paths` filter
named only `app/`, so a change to a `feature-*/strings.xml` label would not
recapture; that is exactly how a stale set could ship. It now names `app/`,
`ui-kit/`, `feature-*/`, and `content-assets/`.

**Verified.** `:app:assembleDebug -Pquran.devPacks=screenshot` builds the
five-pack APK (305 MB uncompressed, down from 383 MB); the three capture
tests (ScreenshotTest, WordByWordTest, MushafTurnTest) pass on the phone
profile and produce all eight frames, each the app's own surface. The settings
frame was looked at and shows the new hub. The next push runs the workflow on
all three legs.

## D-071: The 1.1 submission, and the hand-off closed

Date: the nineteenth session. The owner confirmed that Play has the 1.1
submission, so the hand-off copy of the bundle was deleted from
`play-store/aab/` in the same breath: no signed build sits in the repository
or on the machine waiting to be uploaded a second time, and the bundle was
already deleted by GitHub from the run's artifacts after its own two weeks.
`play-store/aab/` keeps only its own note about where a bundle comes from and
when it goes.

**What was submitted.** 1.1 (versionCode 12), 147704100 bytes, SHA-256
`b876e0f6011452b45202ff0731a9d9ee280153cb1a18722e3dc6d6fb88bde8d2`, signed
with the shared upload key (`537d09d2...de521`), carrying only the core pack.
The committed source at `b5c50ef` is byte-for-byte what that bundle was built
from: the only commit after the bundle's build (`89e803c`) touched
`play-store/screenshots/`, `play-store/listing.md`, and the docs, none of
which enters the app. The screenshots came from the same pipeline that built
the bundle, all three form factors, eight frames each, every frame compared
with its artifact by `cmp`, and both were handed over together, before the
submission.

**The screenshot workflow was rebuilt in this session (D-070).** The 1.1 push
failed every screenshot leg because the tour waited on the visible word
`Appearance` and the session renamed that row to `Theme`. The tour now waits
on a test tag, the capture build carries only the packs its tests install, the
workflow restores the Gradle build cache, and the trigger names every module
that draws. A warm run is 4m33s on all three legs, under the owner's
five-minute bar, and the workflow is written down in AGENTS.md so no machine
rediscovers it.

**What is left open.** The reader's two unreproduced reports (the first-launch
language choice, and Browse after switching to Bangla) stay open in D-069 and
at the head of the queue, not claimed clean. The owner-machine content gates
(`verify`, `audit`, `fonts`) were not run: this machine carries only the two
font zips under `content/raw`, not the owner's manual QUL and QuranEnc
exports, so they remain for the machine that owns the sources.
## D-072: The reader's ninth report, the twentieth session

Date: the twentieth session. The owner read 1.1 on a phone and reported six
things. The session fixed them, found the crash behind two of them, and raised
the release to 1.2 (versionCode 13).

**The language-change crash, found at last.** The reader reported that tapping
a language in Settings closed the app instantly, both ways, and that opening
Browse after a language switch closed it too. The cause was two defects that
had hidden each other. `openLibrary` loaded the pack catalog unmarked and
marked only a copy it threw away, so `ReaderViewModel.catalog` believed every
pack was missing; with word by word on, `ensureWordsPack` then re-fetched a
word list that was already on the device and called `reopenLibrary`, which
`close()`d the old `ContentDatabase` under any reader still querying it (a
study load, the tafsir index warming, a Browse column), and the exception
escaped a worker. The catalog is now marked with the installed set once, in
`openLibrary` and in `reopenLibrary`, so a present pack is never treated as
missing. For the swap itself, `ContentDatabase` now hands each query a read
ticket from its start to its cursor's close, and `close()` waits for the last
ticket before touching the connection; the view model publishes the fresh
library before retiring the old one, and retires it on a worker, so the main
thread never waits and no query is closed under it. `contentDatabase` is
Compose state, so every screen recomposes with the new library instead of
holding a retired one.

**The note belongs to the pill.** The note left the ayah card's More sheet. It
is now a Note action in the pill a long press raises, opening a small sheet
with just the editor, and the reader can reach it without scrolling a card.
The pill also lost the surah name and ayah reference it carried: the top bar
already names the surah, so the pill is now Save, Play, Note, Share, More.

**More shows only what the reading behind it does not.** From the Mushaf,
where the page carries neither translation nor meanings, the card holds the
translation with footnotes, word by word, and each tafsir. From the study
reading, where the ayah, its translation, and its meanings are already open,
the card holds only the tafsir doors (and, with none chosen, the door to add
one). The Arabic ayah and the reference are gone from the card in both modes
over the pill.

**The phone's back button puts the pill away first.** A `BackHandler` with the
pill raised clears the selection; only with nothing raised does back close the
app. Sheets keep their own back handling, which they already had.

**Settings lists read alphabetically.** The language page is sorted by the
name the current interface shows each language under, so Bangla sits above
English in an English interface. The translation and tafsir lists are grouped
by language in the alphabetical order of those language names, and the reciter
list stays alphabetical by name. Word meanings moved to the top of the
Translations page, above the list, so more translations under it can never
bury the switch.

**Bangla surah names: the source is named, the change is content backlog.**
QUL has no Bangla surah-name dataset (its `metadata-surah-names`, resource 70,
is English and transliterated only, and its surah-info set has no Bangla
entry). Two pinnable meanings-based sources exist: quran.com's chapter metadata
with `language=bn` (`সূচনা`, `বকনা-বাছুর`) and the `risan/quran-json` Bengali
translation (Muhiuddin Khan's translation, also meanings). Both are the name's
meaning, not a transliteration, and both are a new dataset with new terms, so
this stays content backlog with the sources written down here, as D-069 left
it. Authentic Bangla transliterations (সূরা আল-ফাতিহা) are widely published but
not as one pinnable source.

**Verification.** The JVM suite, lint with no issues, and `assembleDebug` are
green. The data instrumented tests pass (19, including a new
`PackCatalogTest`). The app instrumented run had search and the Mushaf turn
pass; the screenshot tour's leg died with the emulator (`device not found`)
mid-run, an environment failure, not an assertion. The whole report was walked
on the running phone profile: the pill with its Note action, back closing the
note then the pill then the app, More from both modes, the word-by-word switch
above the translations, and the language order.

## D-073: The 1.2 submission, and the hand-off closed

Date: the twentieth session, after D-072. The owner submitted 1.2 (versionCode
13) to Google Play for review, so this session is closed.

The bundle was handed over from the `signed-bundle` job of the green `build`
run for the push that raised 1.2: `quran-1.2-vc13.aab`, 147,700,590 bytes,
SHA-256 `d598a37e25a6613192de12a523e13f376e5bb3ae05f9889df72a4efbc1214762`,
signed with the shared upload certificate
(`53:7D:09:D2:...:0D:9D:E5:21`). The notes were pasted bare, 397 characters.
The hand-off copy was deleted once the submission was confirmed, so
`play-store/aab/` keeps only its own note.

The screenshot set was installed from run 35595201651 and every frame was
compared with its artifact by `cmp`. One thing is left named rather than
claimed clean: the tour was then changed to capture the ayah card from the
Mushaf (its fuller face), and that push was not re-collected before the
submission. The committed set is therefore from run 35595201651, where frame
08 is the study-mode card. The next session that touches the UI captures the
set again and closes this; it is not a defect in the app, only a frame chosen
before the tour was adjusted.

**Open at the head of the next session.** The reader's Bangla surah names
(researched in D-072, two meanings-based sources named, left as content
backlog), and the tour's ayah-card frame above.

## D-074: The reader's tenth report, the twenty-first session

Date: the twenty-first session. The owner read 1.2 on a phone and reported
seven things, each about a surface the ninth report had just reshaped.

**The Language page survives a change of language.** Tapping a language
recreated the Activity for the new locale, and the page the reader was
standing on was kept in plain `remember`, so it went with the old window and
the reader was returned to the reading. The open page and the open credits are
`rememberSaveable` now, and the choice itself is guarded: re-tapping the
language already chosen does nothing. The reader stays on the Language page,
now speaking the language just chosen.

**A switch keeps its distance from its own words.** The automatic night-mode
row and every other `ToggleRow` set its subtitle flush against the switch:
under a long Bangla subtitle the words ran into the control, and the two read
as one crowded shape. The text column now keeps the same 12 dp gap the choice
rows already keep on the other side of their trailing control, so one
measurement answers across the sheet.

**The font sizes run 0.65 to 1.2.** The largest step, 1.4, was more than the
reading needed and the list had no room below 0.75, so the top step was let
go and a smaller one arrived: 0.65, 0.75, 0.85, 1, 1.2. Sizes are stored as
scales, so a reader who chose 1.4 lands on 1.2, the top of today's list
(`data/TextSize`, pinned by `TextSizeTest`).

**About this surah loses its truncation and gains the ayah's own wash.** The
opened introduction was capped at 40 lines, so a long surah's about ended in
an ellipsis in the middle of a sentence; opened, the whole paragraph is the
reader's now, and only the closed preview ellipsizes. The press mark around
the paragraph is the ayah's own shape with the ayah's own room inside it, so
the reader never meets text touching the edge of a highlight.

**The ayah card loses its rules and gains its names.** From the Mushaf, the
translation, the word by word, and the tafsir were divided by horizontal
lines, furniture the app draws nowhere else. They are named now: a
"Translation" label over the translation lines (whose pack names stay on the
lines when more than one is on), "Word by word" already names itself, and a
"Tafsir" label opens the tafsir doors. `DoorRow` keeps no divider, so no rule
is left in the card.

**A word that acts wears a shape.** Save, Note, Add, Remove, Clear, Retry,
Close, Cancel, Download, and Open looked exactly like a heading, so nothing
told the reader which word answered a touch. `ui-kit/TextButton` is the one
text button now: a rounded shape of the theme's own quiet fill, a primary
tone for what the reader came to do and a quiet tone for what ends or
removes. Applied to the note editor (whose "Note" heading is named in
title type, so the name and the buttons are clearly different kinds of
word), the pack rows, the reciter page, the reader's pack progress pill, the
playback offer, Browse's Remove and Forget, Search's Close and Open, the
content problem's Retry, and the "About this surah" door itself.

**The Notes list, a fifth Browse tab, is where notes are found.** A note was
written and then had nowhere to be seen: the Saved list showed the note's
text under the ayah but could not say which ayahs had one. Browse now has a
Notes tab beside Saved, listing only the ayahs the reader wrote a note on,
newest note first. The store carries the note's own moment
(`saved.db` version 2, `note_at`, migrated from `created_at` for notes
written before the column; `SavedStoreTest` pins the migration). The list
shows the place and when the note was written, not the note's text: a note is
the reader's own writing, and the row's work is to name where it lives.
Tapping a row takes the reading to the ayah and opens the note over it, so
the words the note was written about are under the sheet. The five tabs wrap
as chips instead of one strip, the shape search already taught, so no label
is ever cut off at a large font scale or in Bangla.

**Verification.** The JVM suite, lint with no issues, and `assembleDebug` are
green. The data instrumented tests pass (22, including four new
`SavedStoreTest` cases for the note's moment and its migration). The app
instrumented tests pass on the phone profile (12), and the tour's ayah-card
frame now shows the card's new face. Every changed surface was also walked on
the running emulator: the language choice staying put and speaking Bangla,
the theme toggle's gap in both languages, the note editor's heading and
buttons, the Notes list and its tap, About this surah opened and closed, and
the card's labels.

**The store set is current.** The set captured at the end of this session
(run 35625457689) was installed for all three form factors, every frame
compared with its artifact by `cmp`. Frame 08 is the ayah card from the Mushaf
with its new Translation and Tafsir labels, frame 07 shows Browse's five
chips, so the frame D-073 left open is closed as well.

## D-075: The screenshot workflow is deterministic

Date: the twenty-first session, during the 1.3 hand-off. The 1.3 push failed
the 10 inch screenshot leg with `a system dialog stayed over 05-search`. Phone
and tablet7 passed the same code. This is the old trap in a new shape: the
workflow dismissed dialogs that were already up before the tour started, but a
loaded software rendered emulator can raise "Pixel Launcher isn't responding"
in the middle of the tour, and clearing a dialog after it appears loses that
race.

**The fix is at the device level.** The capture script now sets
`hide_error_dialogs 1` and `anr_show_background 0` before the test, so ANR and
crash dialogs are never drawn. The capture test still checks the window list
before keeping a frame as a second net. With the race gone, a red leg is a
real failure instead of an environment flake.

**The procedure lives in AGENTS.md and in the workflow, not in a sibling
repository.** AGENTS.md now carries the whole capture procedure: what a leg
must do, why each guard is in the script, how to collect and verify a set, and
what each failure line means (dialog over a frame, a tour anchor that moved,
`device offline`, a missing artifact). The instruction to read the family's
sibling repos is gone: the answer to a capture failure is written here once,
and a session that meets a new failure writes it here before rerunning.

**A red leg keeps its frames.** The Gradle exit code is captured and returned
after the frames are copied, so a leg that fails its test still uploads the
frames it took, and the failing frame can be read instead of guessed at.

**Verification.** The fix was pushed and both workflows ran green: the capture
in about six minutes on all three legs and the build. The 1.3 bundle was
handed over with the set.

## D-076: The 1.3 hand-off

Date: the twenty-first session, after D-074 and D-075. The owner asked for the
Play release, so the version was raised to 1.3 (versionCode 14) and the whole
runbook was walked.

**The release.** `versionCode` 13 to 14, `versionName` 1.2 to 1.3, and the
version line in `play-store/listing.md` updated. The notes are 410 characters,
one paragraph of plain prose, kept under their own heading as
`## Release notes (1.3, 410 characters)`. Every earlier release's notes stay
where they were.

**The gates.** The JVM suite, lint, and `assembleDebug` are green. Both
instrumented suites pass (data 22/22, app 12/12 on the phone profile). All five
owner-machine content gates were run on the owner's own raw exports:
`verify` (29 datasets, checksums and structure), `audit` (6236 ayahs against
Tanzil, 0 unexplained differences), `fonts` (628,169 study codepoints, 604
pages, 22,985,677 reading codepoints, all covered), `checkdb` (the committed
database at 128,966,656 bytes and 10 pack files), and `search` (round trips,
folding, 465 excerpts).

**The hand-off.** The bundle is `quran-1.3-vc14.aab`, 147,700,916 bytes,
SHA-256 `66dbc990261f41bdc31d42715a4a9d09ca3bb3acfcc2f60503a44777feffb1b1`,
signed with the shared upload certificate
(`53:7D:09:D2:...:0D:9D:E5:21`, verified with `keytool -printcert`). It was
built by the `signed-bundle` job of the green build run for `93efe44`, and the
app source in the tree is byte-identical to that commit: the commits after it
touch docs, the screenshots, and the workflow only. The screenshot set came
from run 35630768597 (the deterministic capture, D-075), all three form
factors, all 24 frames compared with their artifact by `cmp` before being
installed. Both are handed over together, before the submission. The hand-off
copy is deleted once the owner confirms the submission.

**The hand-off is closed.** The owner submitted 1.3 to Play for review, so the
hand-off copy of the bundle was deleted the same session, as the runbook
requires: the artifact stays in the build run and in Play, never sitting in the
repository. Nothing else moved after the submission.

## D-077: The reader's eleventh report, the twenty-second session

Date: the twenty-second session. The owner read 1.3 on a phone and reported
seven things, all of them about the app's small mechanics rather than its
shape. Nothing here is released yet; the tree carries the answers and the
build waits for the owner's word.

**Numbers in a numbered list end at one edge.** In Browse, the surah and juz
numbers set their digits left to right from the row's own padding, so "10"
began where "3" began and the name after it started deeper than the name of
row 3. Every number now measures into the width of the widest number its list
holds and sits flush to that width's right edge (`feature-browse/NumberedRows.kt`):
the digits end at one line, the names start at one place, and the width is
measured with the reader's own font scale so a large system text grows the
column instead of clipping it.

**A language is named in its own script with the English beside it.** The
welcome cards, the settings hub's Language row, and the Language page all read
`বাংলা (Bangla)` now (`ui-kit/Languages.kt`, `languageChoiceName`), while
English keeps its one name, because "English (English)" says nothing twice.
The platform writes both names; no list of languages lives in the code.

**Nothing a reader reads sits against a button.** The downloaded surah's size
touched its Remove, the search reference sat on Open, and the note's heading
ran into Clear. Each keeps the 12 dp the choice rows and the toggles already
keep between their words and their trailing control, so one measurement
answers across the app.

**The ayah card names every block it holds.** From the Mushaf, the words
section gained the label over it that the translation and the tafsir already
had, and the door under it no longer repeats the label: it says what is
inside, "Meanings" with the list's language beside it, the way a tafsir door
names its pack.

**A scroll back to the top never closes a sheet.** The pull gate was a list
affair until now, and a long tafsir scrolled down and then scrolled back up
pulled the card closed under the reader. `SheetDragGate` now answers over any
scroll position, and every scrolling sheet wears it: the ayah card, the
settings hub, all seven settings pages, and the credits. The same policy
Browse and Search already wore, so one gesture behaves the same everywhere;
verified on the emulator by scrolling the card down and up three times with
the sheet staying put.

**The pill reads Play, Note, Save, Share, More.** The order the owner asked
for, actions before the door.

**The note sheet says what the reader is about to do.** A fresh sheet is
"Take a note"; a sheet with a note already written is "Edit note". The bare
"Note" heading against a Save button left the reader to read both twice.

**Share carries a picture of the ayah.** The card is the app's own face: the
ayah in the Hafs face, the first enabled translation under it (the same pack
search and the plain text read, with footnote markers dropped because a
superscript with no door behind it is a number nobody can open), the
reference, and the launcher's mark beside "Quran" at the foot, with the
translation's name quiet at the other end. It is drawn on the manuscript
paper whatever theme the reader uses, at one fixed text scale, because a
picture is one artifact that leaves the app and lands in a chat. The capture
composes invisibly (`app/.../reader/AyahShare.kt`): the child is measured
taller than the screen so a long ayah is never cut, its drawing is recorded
into a graphics layer that is never drawn back, the node clears its
semantics, and two frames later the pixels are read back. The PNG is written
to one cache folder the app's own FileProvider exposes (`share_paths.xml`,
read-granted to the app the reader picks), with the plain share text riding
along as the caption. Anything that fails along the way falls back to the
plain text share, so Share is never a tap that did nothing.

**Verification, and what this machine could not prove.** The JVM suite, lint,
and assembleDebug are green; the data instrumented tests pass (22) and the
app instrumented tests pass (12) in two full runs on this code. On the
running emulator the walk confirmed the pill's new order, the welcome's
`বাংলা (Bangla)`, the settings hub, the study card's Tafsir block, the note
sheet's "Take a note", the share end to end (the PNG's contents and the
system chooser showing "Sharing image" with the card), and the scroll gate
keeping the card open through three scroll-up gestures. After the emulator
crashed once this session its input injection went unreliable in the top band
of the screen, so the Browse numbers, the Language page, and the Mushaf card's
Word by word label were not photographed locally; the frames the screenshots
workflow runs on CI are the ones to read for those, and the crash
(`performMeasureAndLayout called during measure layout`, once, on a cold
boot) plus one earlier `SnapshotStateObserver` threading failure are named
here as environment findings to watch for in CI rather than assumed clean.

**The tour anchors on its own sheets, and a sheet frame settles before it is
kept.** CI collected this session's set green, but frame 07 was the reader
instead of Browse on all three form factors and frame 08 missed the card: the
tour's Browse wait matched the reader's title sitting behind the sheet, so it
passed before the sheet existed, and a display capture taken right after a
click photographs the sheet's window before its first draw and its opening
animation have settled. Browse and the ayah card gained test tags
(`browse-sheet`, `ayah-card`) the way the settings hub already had one, the
tour waits on those tags, and a sheet frame is now kept only when two display
captures in a row are identical. The loop only photographs the screen, never
drives the app between captures, and the window list is still checked before
every frame.

**The set.** The recapture ran green on all three form factors (run
35696938701); all twenty-four frames were pulled, compared with their
artifacts by `cmp`, installed into `play-store/screenshots/`, and read for
their content before they were kept: search, settings, the Browse sheet with
its aligned number column, and the Mushaf card with its three labels, each
present in each leg. The eight-frame count and the numbered list in
`play-store/listing.md` stand unchanged.

## D-078: The 1.4 hand-off

Date: the twenty-second session, after D-077. The owner said "go for play
release", so the runbook was walked.

**The release.** `versionCode` 14 to 15, `versionName` 1.3 to 1.4, and the
version line in `play-store/listing.md` updated. The notes are 432
characters, one paragraph of plain prose, kept under their own heading as
`## Release notes (1.4, 432 characters)`; every earlier release's notes stay
where they were.

**The gates.** The JVM suite, lint, `assembleDebug`, and `assembleRelease`
are green. Both instrumented suites pass (data 22/22, app 12/12 on the phone
profile). `fetch`, `checkdb`, and `search` are green, and `checkdb` proves
the content is untouched: the committed database verifies at 128,966,656
bytes with the same SHA-256 the 1.3 release carried. `verify`, `audit`, and
`fonts` could not run on this machine: `content/raw` now holds only the two
font packs, and the manual QUL and QuranEnc exports those three gates read
are absent (with `content/work/verify` holding only the font extractions).
They ran green at the last release against content that is byte-identical to
today's, and CI's own gates ran green on this push, but the three are named
here as not run rather than as passed: restoring the exports from the copy
that has them and running the three is owed to the next content change, and
the owner may want it before submitting.

**The suite on this machine fought back.** A wiped emulator needed one
reboot and the workflow's own `hide_error_dialogs` setting after a systemui
ANR window stuck over the screen; with those, the tour and all twelve app
tests pass locally.

**CI.** The build run (35702564152) is green: data instrumented, gates and
debug build, and the signed bundle. The screenshots run (35702564098) went
red once on the 10 inch leg with `a system dialog stayed over 05-search`, a
boot-time ANR window the suppression had not yet met; the rerun is green on
all three legs, and a red leg keeps its frames, so nothing was guessed at.

**The hand-off.** The bundle is `quran-1.4-vc15.aab`, 147,738,638 bytes,
SHA-256 `5dce62ed7086efac2cfee6857c1073991d13f81bbb069b5730cb2cd09f711f4b`,
matching the checksum file the artifact carries. `jarsigner -verify` says
`jar verified`, and the certificate is the shared upload key
(`53:7D:09:D2:...:0D:9D:E5:21`). It sits in `play-store/aab/`, which is
gitignored, and is deleted once Play has it. The screenshots came from the
same push: run 35702564098, all three form factors, all twenty-four frames
compared with their artifacts by `cmp` and every sheet frame checked for its
content before it was installed. Both are handed over together, before the
submission.

**The hand-off is closed.** The owner submitted 1.4 to Play for review, so
the hand-off copy of the bundle and its checksum were deleted the same
session, as the runbook requires: the artifact stays in the build run and in
Play, never sitting in the repository.

## D-079: The 1.5 hand-off

Date: the twenty-third session. The owner said "go for release with the
other changes you made", after calling off the Bangla surah-name work, so
the runbook was walked with two fixes and nothing else.

**What ships.** About this surah keeps its headings, Name, Period of
Revelation, and Theme, on their own lines: `core/RichText.paragraphs` keeps
every source block its own paragraph, and the study view draws the
introduction through it, with `tools search` auditing the drawn form. Opened
from the Mushaf, the ayah card reads word by word first, then the
translation, then the tafsir, the order the study reading already uses.
`versionCode` 15 to 16, `versionName` 1.4 to 1.5, and the version line in
`play-store/listing.md` updated. The notes are 338 characters under
`## Release notes (1.5, 338 characters)`.

**The Bangla names, decided and dropped.** The session researched a full
Bangla surah-name list (Bengali Wikipedia base, adapted per the Arabic and
English columns, the five name divergences and the article and hyphen rules
worked out and checked against Bangla usage), and the owner decided the
release ships without it. Nothing of that work entered the code; the list
lives only in this record's session, and a future release can pick it up.

**The gates.** The JVM suite, lint, and `assembleDebug` are green. All five
owner-machine content gates ran green on this machine this time: `verify`
(29 datasets), `audit` (no unexplained differences), `search` (465 excerpts
clean), `fonts` (604 pages, 88,186 glyph codepoints, 22,985,677 reading
codepoints), and `checkdb` (the committed database verifies at 128,966,656
bytes, SHA-256 `5c5988fa2916eb1cc905d01ddb9b4ef9319d0ca19c945bf0140eaff32296cea9`).
That closes the three D-078 left owed.

**CI.** The build run (35771681314) is green: instrumented data tests, the
gates, the debug build, and the signed bundle. The screenshots run
(35771681366) is green on all three legs, in about six minutes each. The
instrumented app tests and the tour ran there, not on this machine; the
owner asked to leave the emulator out.

**The hand-off.** The bundle is `quran-1.5-vc16.aab`, 147,736,943 bytes,
SHA-256 `afa01b1d7953f51fb9c4c0fe4d6a99b80b742ba899592d904ca3b942d9571d5e`,
matching the checksum file the artifact carries, and `jarsigner -verify`
says `jar verified`. The screenshots came from the same push: all
twenty-four frames compared with their artifacts by `cmp` and installed
into `play-store/screenshots/`, the ayah card frame carrying the new order.
Both are handed over together, before the submission.

**The hand-off is closed.** The owner submitted 1.5 to Play for review, so
the hand-off copy of the bundle and its checksum were deleted the same
session, as the runbook requires: the artifact stays in the build run and in
Play, never sitting in the repository.

## D-080: The reader's twelfth report, the twenty-fourth session

Date: the twenty-fourth session. The owner read 1.5 on a phone and reported
eight things, each on a surface the earlier reports had left.

**About this surah wears the ayah's wash.** Opened, the introduction now
sits on the same lapis a long-pressed ayah wears, `primary` at 7% alpha,
the exact value `AyahBlock` gives its selected wash, and its press answers
in the same lapis through `material3.ripple` instead of the gray an
unstyled `clickable` flashed. The rounded shape and the inset were already
the ayah's; the color was the part that was not.

**Bangla About this surah cannot ship from QUL.** The owner asked whether
the same source carries the surah introductions in Bangla. It does not:
QUL's surah-info section holds six resources, English (3), Urdu (4), Tamil
(5), Italian (6), Malayalam (7), and Indonesian (454), with no other page,
which is what D-072 already recorded for the surah-name dataset. No content
was changed and no hand-written translation entered the repository; the
item stays content backlog until a source is named and reviewed the way the
Arabic content is.

**Browse's surah numbers keep their last digit.** The number column was
sized by measuring "114" and drawing every number into that width, and the
digits of the interface face are not one width: "114" is narrower than
"100", so every 100 to 109 row was clipped to "10" (and 110 to 114 to
"11"). The column now measures every number the list actually draws and
takes the widest. The numbers also follow the interface's own digits:
`numberLabel` formats through the composition's locale, so a Bangla reading
gets ১০০ where an English one gets 100, and the ayah labels of the three
reader lists format through the same locale. `BrowseNumbersTest` pins both:
the width "100" gets must be greater than the width "114" gets (equal
widths is the old bug), and after the language choice recreates the
Activity, ১০০ exists in the Bangla sheet.

**Save and Note are two marks, not one.** A note used to be written by
saving the ayah behind it, so every note turned up in Saved; and Save on a
noted ayah deleted the note with the row. `saved.db` is version 3: a
`saved` column, set by Save alone, with Note writing its own row. A note
never enters Saved, a save keeps its note, unsaving keeps the note, and
clearing the note keeps the save. The migration marks a note row whose note
moment is the row's own moment as note-only (the old single insert wrote
both), and leaves a later note saved, because a save that predates its note
cannot be told from an edited note and nothing the reader saved may leave
the list. `SavedStoreTest` grows both migration cases and the new
behaviors.

**The reader's three lists are place lists.** Saved now shows the surah
name and the ayah number and nothing else: the ayah's text, its
translation, and its note are gone from the row, as Last Read and Notes
already were. The redundant Open label is gone from all three, because the
row is the door. Notes gains the Remove the owner asked for, so a note can
be taken back where it is listed, and Forget in Last Read is মুছুন in
Bangla like every other removal.

**A note opened from Browse wears the wash under it.** The note sheet opens
over the reading the row jumped to, and the ayah it belongs to now carries
the same wash a long press leaves, driven by the open note itself rather
than by the pill's selection. When the note closes, the wash goes.

**Verification.** The JVM suite (core, data, app), lint, and
`assembleDebug` are green. The data instrumented tests pass (28, including
the new store cases and both migrations) and the app instrumented tests
pass (13, the new `BrowseNumbersTest` among them), on the phone emulator
with the workflow's own dialog settings. The new look was not in the 1.5
store frames: the tour never opens About, and its Browse frame starts at
surah 1. The release followed in the same session (D-081), and its capture
installed the new set.

## D-081: The 1.6 hand-off

Date: the twenty-fourth session, after D-080. The owner read the eight
fixes, answered "ok" to each, and said "go for play release".

**What ships.** `versionCode` 16 to 17, `versionName` 1.5 to 1.6, and the
version line in `play-store/listing.md` updated. The notes are 438
characters under `## Release notes (1.6, 438 characters)`: Save and Note
are separate, Browse's surah numbers are whole and in the reader's digits,
the Saved list names a place without repeating its text, a note highlights
its ayah, About this surah wears the long-pressed ayah's wash, and Forget
reads মুছুন in Bangla.

**The three owner gates are owed, and the owner chose to ship anyway.**
Step 0 found `content/raw` holding only the two font zips and
`content/work/verify` holding only the font extractions, so `verify`,
`audit`, and `fonts` cannot run on this machine; the manual QUL and
QuranEnc exports are gone and no copy was found under `Documents/GitHub`,
`Downloads`, or `Desktop`. The two gates that can run were run: `checkdb`
reports `content/quran.db` at 128,966,656 bytes, SHA-256
`5c5988fa2916eb1cc905d01ddb9b4ef9319d0ca19c945bf0140eaff32296cea9`, the
same bytes D-079 recorded when all five ran green, and `search` passes with
the same counts (465 excerpts, 63 Arabic round trips, 65 Bangla). The
session touched no content, only code. The owner was asked before the
version moved and chose (a): proceed and name the three owed, exactly as
1.4 shipped (D-078).

**The suite.** JVM suite, lint, and `assembleDebug` are green. Data
instrumented 28/28. The first app instrumented run lost its emulator
mid-suite (`device 'emulator-5554' not found` in the run's own XML, not an
assertion); one reboot later the same run is 13/13, so the environment was
named and the symptom did not persist, as the runbook allows.

**CI.** The build run (35823592271) is green: data instrumented, the gates,
the debug build, and the signed bundle. The screenshots run (35823592289)
is green on all three legs.

**The set.** All twenty-four frames were installed into
`play-store/screenshots/<form>/` and every one was compared with its
artifact by `cmp`. Every changed frame was read before it shipped: the
settings frames carry "Version 1.6", the Browse frames carry the wider
number column, the search frames differ only in the text cursor's blink,
the phone Mushaf frame differs by subpixel antialiasing only (mean 0.02
levels per channel, the tablets byte-identical), and the ayah card frames
differ only in the few pixels of the reading behind the sheet.

**The hand-off.** The bundle is `quran-1.6-vc17.aab`, 147,736,755 bytes,
SHA-256
`5de7f297eca1cc2f7ef3598137b88e0c25fb7debb79e47a19858f280c8c32ced`,
matching the checksum file the artifact carries. `jarsigner -verify` says
`jar verified.`, and the certificate is the shared upload key
(`53:7D:09:D2:...:0D:9D:E5:21`). The bundle and the screenshots are handed
over together, before the submission.

**The hand-off is closed.** The owner submitted 1.6 to Play for review, so
the hand-off copy of the bundle and its checksum were deleted the same
session, as the runbook requires: the artifact stays in the build run and in
Play, never sitting in the repository.

**One correction after the hand-off.** The owner asked why the session
setup said `git pull` fails on this machine. It does not: the clone on
September 20 wrote `branch.main.remote = origin` and
`branch.main.merge = refs/heads/main` into `.git/config` (the file's
timestamp is three seconds after the clone), `git branch -vv` shows
`[origin/main]`, and plain `git pull` answers "Already up to date." The
September 22 note that it fails was a misdiagnosis of some other pull
trouble, and AGENTS.md now records what is true, with the one-time
`git branch --set-upstream-to=origin/main main` as the repair for a machine
that ever lacks the tracking.

## D-082: One name for the word meanings, references that name their surah, and a picker for any ayah

Date: the twenty-fifth session. The owner asked for three things: an
explanation of what a moment under each Saved row would take, a full
coherent rename of the word-meaning aid, and a way to any ayah that needs
neither scrolling to it nor knowing its reference. After the explanation
the owner named the fix exactly, a moment per mark, and said to build it
for this reader rather than guard other installs; all three are in the
tree.

**The word-meaning aid is called one thing now: "Word meanings".** The
Mushaf ayah card's block label read "Word by word" over a door that read
"Meanings", the settings page said "Show word meanings", the size page
said "Word by word", and the search filter said both across the two
languages. The card's label is "Word meanings" and the door under it now
names only the list's language ("English"), the way a tafsir door names
its pack, so the two lines say what the block is and what is inside it
without repeating each other. The size row, the pack type, and the search
filter follow in both languages (settings_size_words, pack_type_words,
search_filter_words, and the card's add row), and the old
`card_words_meanings` string is gone. D-077's reasoning for the door still
holds: the door names what is inside, never repeats the label above it.

**A reference may name its surah.** Search already read "2:255", "2.255",
"2 255", "surah 2", and their Arabic digit forms, and docs/design.md
promised "baqara 255" while the parser only took digits. `Search.nameReference`
now reads a last word that is an ayah number and a name in front of it
("baqara 255", "al kahf 10", "النور 24", with the same "surah" prefix
the numbered form accepts), `Search.nameKey` and `Search.surahNameForms`
fold a name to letters and digits and also try it without its article
("baqara" for "Al-Baqarah"), and `ContentDatabase.surahByName` resolves the
name against the same three name columns the surah list matches, preferring
a name over a longer name that merely begins with it ("nas" is An-Nas, not
An-Nasr) and the shortest such match next. The resolved reference draws the
same "Go to" row the numbered form does, so the reader who knows "Al-Kahf,
ayah 10" but not "18" is one field and one tap away. The empty search
prompt now names both forms ("references like 2:255 or baqara 255") so the
form is visible where it is typed. A name that matches no surah, or an ayah
past its end, is not a reference and the query stays whatever else it was
searched as.

**Browse can raise a picker for any ayah.** The surah list now leads with a
quiet "Go to ayah" action (`feature-browse/BrowseSheet.kt`). It swaps the
sheet's tabs for a two-step picker in the same window: the surah already
chosen is the reader's own, the selector row above the grid changes it
through the same 114-row list Browse draws, and the grid is the surah's
ayah numbers, the reader's own ayah filled and spoken as current. The grid
is laid out adaptively so a phone gets six columns and a tablet more, and
it opens on the reader's ayah, so a jump within a long surah is one tap on
a number and never a scroll. The back arrow steps from the surah list to
the grid and from the grid to the tabs; a number calls the same `onAyah`
path every Browse row uses, so the sheet closes and the reading jumps.
`SheetDragGate` gained a `LazyGridState` constructor so a scroll back to
the top of the grid still cannot pull the sheet closed. No new screen, no
sixth tab, and the top bar keeps its one row.

**Each mark keeps its own moment.** The row's `created_at` is only the
first of the two marks to arrive, so a note written first dated the save,
and an unsave followed by a save did not move it. `saved.db` is version 4
now: `saved_at` is written when the Save mark goes on and cleared when it
is removed, so a re-save carries the moment of the current save, while
`note_at` keeps doing the same for a note. The migration fills saved rows
with their row's own moment, the best true answer left on the device, and
leaves note-only rows empty. The Saved list draws "Saved <moment>" the way
Last Read and Notes draw theirs and reads newest first by the save's own
moment, so an ayah first noted a year ago and saved today is today's save.
`SavedStoreTest` pins the column, the re-save, and the migration, and the
schema version moved with the migration in the same session.

**Verification.** The JVM suite, lint, and `assembleDebug` are green. The
data instrumented suite is 31/31, the three new cases among them (a save's
own moment, a re-save's new moment, and the version 3 to 4 backfill). The
four affected app instrumented classes ran on the phone emulator:
ContentSearchTest 10/10 (the new named-reference test among them),
WordByWordTest, BrowseNumbersTest, and GoToAyahTest, which lands on 2:12.
The first run failed GoToAyahTest on the test's own assertion, not the
app: it looked for the text "Al-Fatihah", which also names the surah in
the reading behind the sheet, and the third match was the picker's own
selector; the assertion now anchors on the selector's test tag, and the
rerun is green. The store set is stale after this session: the Browse
frame gains the "Go to ayah" row and the ayah-card frame reads "Word
meanings" over the language door, so the next capture refreshes 07 and 08,
by the workflow's artifacts and never by hand.

## D-083: The 1.7 hand-off

Date: the twenty-fifth session, after D-082 and the owner's word to ship.
The raw sources were restored to `content/raw` by hand (24 QUL downloads,
`english_saheeh.zip` and `arabic_saadi.json` from QuranEnc, and the Tanzil
XML with `marks`, `sajdah`, `alef`, and `tatweel` set), and all five owner
gates ran green this time: `verify` (29 datasets, checksums and structure),
`audit` (6236 ayahs, 0 unexplained differences, one accepted orthographic
variant), `fonts` (coverage passed), `checkdb` (the committed database
unchanged), and `search`. The three gates 1.6 left owed are closed.

The JVM suite, lint, and `assembleDebug` are green; the data instrumented
suite is 31/31; the four affected app classes pass. The full local app run
lost its emulator twice (`device 'emulator-5554' not found` in the run's
own XML, empty failure bodies, not assertions), so the local tour is not
evidence and CI's capture is the authority.

The store set is from run 35841954697: all three form factors, every frame
compared with its artifact by `cmp`, and every changed frame read. The
Browse frame carries the Go to ayah row, the ayah-card frame reads Word
meanings over the language door, and the settings frame reads Version 1.7;
the Mushaf frames differ by subpixel antialiasing (max delta 4 levels) and
the search frames by the cursor's blink only. The 10 inch leg failed once
on the boot-time dialog over 05-search and passed on the one rerun the
runbook allows (D-078).

The bundle is `quran-1.7-vc18.aab`, 147,801,933 bytes, SHA-256
`be0174e036add42fbf84c62f1914da228a600da801ea18287e2092a7458cd195`,
`jar verified`, the certificate the shared upload key. The bundle and the
screenshots are handed over together, before the submission.

**The hand-off is closed.** The owner submitted 1.7 to Play for review, so
the hand-off copy of the bundle was deleted the same session, as the
runbook requires: the artifact stays in the build run and in Play, never
sitting in the repository.

## D-084: Go to ayah is a tab-row door, and every door names what is behind it

Date: the twenty-sixth session. The owner read the 1.7 surfaces and asked
five questions about them: the Go to ayah picker's depth, whether the word
by word aid should replace the ayah it glosses, whether an ayah's reference
belongs at the top or the foot of its block, whether the study pill's More
button is an extra step with one destination, and whether per-ayah icons or
a kebab would beat the long press pill. The answers, and what the tree
carries now.

**Go to ayah moved into the tab row.** It was a quiet text button over the
surah list, so getting to the picker meant Browse, then the row, then the
surah selector only if the reader was not already where they wanted to be.
The door is now a chip beside the five list chips, one tap from any tab.
It is a door, not a state: it never takes the chosen fill, and closing the
picker returns the reader to the tab they came from, the way an action
stands among tabs without becoming one. The picker itself is unchanged:
the reader's own surah already chosen, the grid opening on their ayah and
marking it, and one tap on a number to land. The picker's header reads one
name for the whole picker (`browse_go_to_ayah`); `browse_choose_surah` is
gone, because the 114-row list that opens from the selector is obviously a
list of surahs and a second name for the step was a second name for the
same door.

**The word by word aid stays under the ayah, as its gloss.** The owner's
report was real: with the switch on, the ayah appears twice, once as the
line and once as the tiles. But the two are not the same text. The line is
the verse read as one: shaped words, joined cursives, the unit the reader
memorizes and recites. The tiles are the word list: each word centered over
its meaning, which is a study view of the words and not a recitable verse.
Removing the line would also take the playing word's wash with it, because
that wash is drawn from the line's own `TextLayoutResult`; the tiles have
their own layout and are columns, which is the shape the wash was drawn not
to be. So the aid stays, and reads as the annotation it is: the tiles'
Arabic steps one tone down from the verse's ink, the meanings take the
theme's own secondary tone instead of a quiet alpha, and the break above
the aid is `Space.Line` rather than `Space.Block`, so the word list groups
with the line it glosses rather than floating as a second verse. The one
surface where the aid has no line above it is the ayah card opened from the
Mushaf, and there the component now keeps the reading ink (`gloss = false`),
because there the aid *is* the Arabic. If the owner wants a meanings-only
reading, the honest place for it is the card, which already shows no ayah
from the Mushaf.

**The reference stays at the foot of the block.** It is a footnote to the
verse, not a heading: the reader reads the Arabic, then the translation,
then finds the number to know where they are. At the top it would be the
first thing after the Arabic and would push the translation down; at the
foot it closes the block and always sits where the reader is, however many
lines the ayah took. This is also what print does, and what the card does
the other way around on purpose: the card names the ayah first because the
card is a mode switch, not a reading. The same pass found the reference at
0.45 alpha at 2.44:1 on the sepia ground, under the design document's own
4.5:1 rule for muted text, so the reference, the surah metadata line, and
the surah-complete line now take `onSurfaceVariant`, the theme's own
secondary tone, and I recomputed their contrast on every ground before
choosing it: 6.2:1 on paper, 6.1:1 on sepia, 8.3:1 on night, each well past
the 4.5:1 rule. The card's own quiet labels (the tafsir range at 0.7 alpha
and the note hint at 0.5) came out below the same rule when measured and
are left named here, not quietly blessed: they are the next pass's work,
because this session's scope was the study reading.

**The deeper door names what is actually behind it.** In the study reading
the ayah card holds only the tafsir doors, so the pill's last action reads
**Tafsir** with a new `Icon.Tafsir`, a scroll drawn in the same hand as the
rest of the set and deliberately not book-like, because the Mushaf and the
study reading are already two book marks. In the Mushaf the card holds the
word meanings, the translation, and the tafsirs together, so the action
stays **More** with its dots: a tafsir glyph there would promise a tafsir
and land on four blocks. Same slot, two honest names, chosen by the reading
the pill was raised over.

**The pill stays.** Per-ayah icons would draw 6236 toolbars and would be
impossible in the Mushaf, where the page is a picture of text; a kebab
would promise secondary actions and teach a second way to raise one thing.
The long press stays the one gesture, with its shown-once hint. The one
weakness, discoverability, is answered where the app already answers it:
the hint on the first read, and in the Mushaf by a semantics layer that
gives every ayah a node whose action raises the same row. The study block
is one node that answers a real long press only; giving it the same
accessibility action is a known gap, named here rather than implied fixed.

**Verification.** The JVM suite, lint, and `assembleDebug` are green after
the change. No emulator was available this session, so the instrumented
claims wait for CI: `GoToAyahTest` now taps the chip in the tab row (its
tag moved with the door), and the tour still reaches the card from the
Mushaf, where the action still reads More. The store set is stale again:
frame 07 gains the chip in its tab row, and 03-study changes with the aid's
tone and spacing, so the next capture refreshes both, from the workflow's
artifacts and never by hand.

## D-085: The 1.8 hand-off, and what a screenshot leg's failure classes are

Date: the twenty-sixth session, after D-084 and the owner's word to ship.

**All five owner gates ran green on the restored sources**: `verify` 29
datasets (checksums and structure), `audit` 6236 ayahs with 0 unexplained
differences and one accepted orthographic variant, `fonts` coverage passed
(628169 study codepoints, 604 pages, 22985677 reading codepoints), `checkdb`
(the committed database unchanged, 128966656 bytes), and `search`. The JVM
suite, lint, and `assembleDebug` are green; the data instrumented suite is
31/31 and the app instrumented suite 16/16 on the phone emulator, the new
`AyahActionsTest` and the screenshot tour among them.

**The store set is from run 35883297322 attempt 2** (the tablet7 leg on its
second attempt), all three form factors, every frame compared with its
artifact by `cmp` (24 matches, 0 mismatches) and every changed frame read
before it shipped: the settings frame reads Version 1.8, the Browse frame
shows the Go to ayah chip in the tab row, the study frames show the aid
reading as a gloss with a legible reference, and the tablet10 search frame
differs from its predecessor by the study reading visible behind the sheet
(the aid and reference changes) plus the status bar clock. The ayah card
keeps its Word meanings and Tafsir doors from the Mushaf.

**Why the tablet7 leg failed, and why the answer is written down.** The leg
died in the `Capture on emulator` step, inside the emulator action's own
`Install Android SDK`, with `Error on ZipFile unknown archive` while
preparing `system-images;android-35;google_apis;x86_64`: a truncated
download on that runner. The action then terminated a machine that had
never started and reported `could not connect to TCP port 5554: Connection
refused`. The phone and tablet10 legs passed the same commit in the same
run, which is what makes it infrastructure and not the app. One rerun of
the failed leg is the whole fix, and the rerun's frames are the set.

The owner asked why this keeps happening and why the lesson was not already
in AGENTS.md. The history says it does not keep happening in one shape:
of the last 24 screenshot runs, 19 are green and 5 red, and the reds split
into three real classes. A leg can die **before our script** (this run: a
truncated download in the action's setup), **in the tour** (a node the UI
moved, a wait whose anchor never came: runs 35596449680 and 35569779756),
or **under capture load** (`device offline` while a sister leg passes:
35524134742, 35526311219). The runbook had entries for the fourth case only
(a dialog in a frame) plus load and missing-artifact notes. It now names
all of them, says which are rerun-once and which are fixed in the session
in the tour, and records the log-reading trap found here: after a rerun,
`gh run view --log --job <id>` serves the latest attempt's log under the
original failed job id, so the jobs API's `run_attempt` is the record and
the artifacts endpoint takes an `attempt` parameter.

**The bundle** is `quran-1.8-vc19.aab`, 147803132 bytes, SHA-256
`7a2fe75a33790fe17b90d90767908940c5e59af769b6cf3f19deae91d5f29205`,
`jar verified`, signed with the shared upload key
(`53:7D:09:D2:...:0D:9D:E5:21`). The bundle and the screenshots are handed
over together, before the submission.

**The hand-off is closed.** The owner submitted 1.8 to Play for review, so
the hand-off copy of the bundle was deleted the same session, as the
runbook requires: the artifact stays in the build run and in Play, never
sitting in the repository. `play-store/aab/` keeps only its own note.

## D-086: The craftsmanship pass, grounded in measurement

Date: the twenty-seventh session. The owner asked for the most beautiful
reading experience the app can give: Apple-level polish, coherence, the
best legibility and accessibility the medium allows, the work questioned
from the ground up. The session audited every surface against the design
document and against measured contrast, and made the changes below. The
guiding rule of the pass is the project's own: a tone with meaning takes
the theme's own token, and the token is measured before it is chosen.

**The quiet alphas D-084 named are closed, and the class was swept.** D-084
measured the card's tafsir range (0.7 alpha, 3.3:1 on sepia) and its note
hint (0.5, 2.2:1) and left them as the next pass's work. Both now take
`onSurfaceVariant`, the theme's secondary tone, 6.1:1 or better on every
ground. A sweep of every `color.copy(alpha = ...)` used on text found five
more of the same defect that nobody had named: the Browse surah and juz
numbers (0.7, 3.3:1), the search result's source name (0.7), the search
field's hint (0.55, 2.5:1), the shared card's reference and translation
name (0.75, 3.7:1, and its app name at 0.85, 4.6:1), and the footnote
markers in the translation body (0.75). All of them now take
`onSurfaceVariant` or `onSurface` at full strength. The values were
computed, not eyeballed: 0.85 of `onSurfaceVariant` measures 4.63:1 on
paper, 0.8 measures 4.13:1, 0.75 measures 3.69:1, 0.7 measures 3.31:1, and
0.55 measures 2.45:1, so nothing under full strength survives the 4.5:1
rule on the day grounds. The unselected radio and check marks in settings
rose from 0.35 of the accent (1.8:1, invisible) to 0.6 (3.0:1 and up on
all four grounds), which is what a control boundary owes.

**The ornament gold is now read, so it is measurable.** Gold draws the
Mushaf's page number, its juz, the surah names on the page, and the
ornamental name at the head of the study reading, so it is text and not
decoration. The paper gold sat at 3.22:1 and the sepia at 4.11:1, under
the design document's own 4.5:1 rule. Both were darkened without changing
the hue: `#856411` on paper (now 4.64:1 on paper, 5.04:1 on the paper
surface) and `#7E5C1C` on sepia (5.08:1). Night and black already met the
rule at 5.84:1 and 5.35:1 and are untouched. The Mushaf's own pixels are
unaffected: those are the page font's glyphs, not the ornament tone, which
the app draws only for the foot band, the surah name, and the ornament.

**The color scheme is now fully spoken for.** Every role Material draws
from was left to its default before, and a default is a color nobody
chose. All four schemes now name their containers, `outline`/
`outlineVariant`, `inverse*`, and `scrim`, and `surfaceTint` is transparent
in all four so no sheet picks up a blue wash from Material's tonal lift.
The new `surfaceContainerHigh` is the *floating* tone: the ayah pill, the
playback bar, and the reciter chooser now wear it with a soft shadow, so a
control that floats over the page reads as floating instead of as printed
into the page with a hard rectangle. That is the one place the design
document's "quiet is beautiful" had been met with a box, and it is gone.

**The study reading now has a measure.** The ten inch study frame laid a
translation across the full 2560 px of the glass, well over two hundred
characters a line, where the eye stops returning to the margin on its own.
`Reading.MaxMeasure` (620 dp) and `ReadingFrame` cap every reading column
at a readable measure and center it when the screen is wider than the
measure; the study list, which keeps its paper full width so a drag still
scrolls and a tap on the margin still brings the chrome, draws its rows
into the centered column. Settings' sheet adopts the same cap, so a settings
row's two ends no longer sit a foot apart on a tablet. This is the single
largest legibility win of the pass, and it is invisible on a phone.

**The study surah opening is an arrival.** The head of a surah was three
lines of centered type. It is now built to the printed page's own model,
the model the Mushaf already draws on its surah line: the Arabic name in
the ornament gold at 40 sp, a short gold rule under it, then the simple
name and the place in quiet type, the basmalla below, and the About button
under that. The rule is ornament and carries no meaning, which is why it is
gold and not a separator.

**The study block is now actionable for TalkBack.** D-084 named the gap:
the Mushaf gives every ayah a semantics node whose action raises the pill,
but the study block answered a real long press only, so a screen reader
could read an ayah there but not act on it. The block now exposes the same
custom accessibility action (`study_ayah_actions`), so the one gesture the
reading is built around works in both readings.

**Verification.** The JVM suite, lint, and `assembleDebug` are green after
every change. No emulator was used this session; the instrumented claims
wait for CI. The store set is stale: the study frames, the settings sheet,
the Browse numbers, and the ayah pill all changed visibly, so the next
capture refreshes them from the workflow's artifacts.

**Named and not done, so nothing is implied fixed.** (1) Mushaf zoom. The
design document promises pinch zoom and the hard constraints require it,
and the app has none. It is deliberately not in this pass: the page is a
pre-rendered bitmap and the pager owns horizontal drags, so a correct zoom
needs a `PageWindow` in `core` (scale and a clamped center, tested), rendering
only the visible window, and a way to tell a pan from a page turn that does
not break `MushafTurnTest`. A rushed zoom on the crash-sensitive page is
worse than a named gap. This is the next session's first work. (2)
Surah-arrival motion, recitation speed and repeat, and similar reading aids
are backlog, not this pass.

## D-087: The listening page, and what a Mushaf font size really is

Date: the twenty-seventh session, after D-086.

**The owner asked how other apps give the Mushaf a font size, and the
answer is three different things.** (1) Most apps scale the page bitmap:
the glyphs grow and soften, the whole-page view is lost, and it is a zoom
under another name. That is the thing this app does not want. (2) The
honest approach re-renders the glyphs at a larger em. The QPC V2 page fonts
are vector outlines, so a bigger size draws bigger, equally crisp glyphs;
but the page is pre-justified (each line sums to 15.6 em, D-011), so a
larger em means the line no longer fills the page and the 15 lines must be
re-justified and the page allowed to grow taller than the printed one. This
is a layout engine, and it is the feature to build if the owner wants a
larger Mushaf. (3) Some apps switch to a flowing Unicode face (QPC Hafs,
IndoPak) in which case "Mushaf mode" becomes continuous text and is no
longer the printed page. The owner said no to zoom. The re-layout in (2) is
the named next feature, to be built as a pure tested `PageLayout` in `core`
before any of it touches the page.

**The listening page.** The recitation had no pace and no repeat, so a
reader memorizing an ayah had to tap Play at the end of every pass, and a
reader who wanted a slower voice could not have one. Both are now real,
remembered choices:

- `AppSettings.playbackSpeed` (0.5 through 1.5, in the five steps the text
  sizes use) and `AppSettings.repeatAyah`, both in DataStore. ExoPlayer's
  `setPlaybackSpeed` keeps the pitch, so a slower recitation is slower, not
  deeper, and `REPEAT_MODE_ONE` loops the one item so the surah-end offer
  does not appear while the reader is repeating.
- The controller applies both when it connects, so the first ayah of a
  session plays the way the last one was being heard, and the settings read
  once at launch is what tells it.
- Settings gained a **Listening** page (speed, repeat) under Reciters, and
  the hub's own row summarizes both. `SpeedRow` is the same segmented control
  the text sizes use, so one control is learned once. `speedText` moved to
  `ui-kit/Formats`, because the page that chooses the pace and the pill that
  reports it must name it the same way.
- The playback pill says "1.5x" and "repeating" beside the reference, and
  only when they are not the ordinary ones: a reader who set a pace a week
  ago and forgot, or who turned repeat on and wondered why the reading would
  not move on, reads the answer in the moment instead of hunting settings.

**Two related reading-polish changes in the same pass.**

- **Reduced motion is now honored.** The design document promised it (page
  turns and scroll animations become jumps) and nothing read the system
  setting. `ui-kit/rememberReducedMotion` reads the animator duration scale
  on a worker, once, and the Mushaf's programmatic page jump and the study
  list's follow-the-reciter scroll use `scrollToItem` rather than
  `animateScrollToItem` when it is on. A swipe is the reader's own finger and
  is never changed.
- **The first screen keeps the reading's measure.** The language welcome was
  full width on a tablet; it now holds `Reading.MaxMeasure`, the same cap the
  study reading and the settings sheet use.

**Verification.** The JVM suite, lint, and `assembleDebug` are green. No
emulator this session; the instrumented claims wait for CI. The store set is
stale (the study frames, settings, Browse numbers, the ayah pill, and the new
Listening page all changed visibly), so the next capture refreshes it.

**Named and not done (D-087).** The segmented controls that choose text
sizes and the playback pace draw 38 dp and 46 dp cells, under the design
document's 48 dp target. They are read as one row, and the fix is structural
(an outer 48 dp touch box with the visual cell inside it), so it is a change
to a captured, tested control that cannot be verified without a screen. It is
the next pass's work, named rather than quietly left.

## D-088: The screenshot workflow's failure modes, and the script bug under them

Date: the twenty-seventh session, after the second 1.9 capture went red.

The owner asked a fair and pointed question: why does the screenshot
workflow keep failing, and why is nothing learned between failures. The
answer, from the logs of every red run this workflow has had, is that it
fails in five classes and that the class which kept costing the most was our
own bug and had destroyed the evidence for the rest. All five are now
catalogued in `docs/screenshot-failures.md`, which is the file to read
before diagnosing a red leg; a new mode is added there in the same session
that fixes it.

**The script bug, which is the real finding.** The emulator runner feeds
**each line of `script:` to its own `/usr/bin/sh -c`**. The workflow was
written as `set +e` / `./gradlew ...` / `gradle_status=$?` / `set -e`, which
only works if the lines share one shell. They never did: `set +e` did not
protect the Gradle line and `gradle_status=$?` read the status of nothing.
When the tour failed, the runner stopped the block at the Gradle line, the
`mkdir store-shots`, the `find ... -exec cp`, and the `exit` never ran, and
the upload step had nothing to upload. The runbook's promise, that a red leg
keeps its frames to be read (D-075), had been false since the day it was
written, and the missing `store-screenshots-tablet10` artifact is the proof
(runs 35463781932 and 35377607496 lost theirs the same way). The fix: the
Gradle status is written to a file on the Gradle line itself
(`... ; echo $? > /tmp/gradle_status`), every collection line is guarded so
it cannot abort the script, and the last line still returns the saved status
so a red tour is still red with its frames safe in the artifact.

**The window guard was asking the wrong question.** `captureScreen` searched
the whole `dumpsys window windows` dump for `isn't responding`, which is true
whenever a stale or suppressed window record carries the phrase, not when a
dialog is in front. The 10 inch leg failed `a system dialog stayed over
05-search` twice for a dialog that was not there. It now reads
`mCurrentFocus`/`mFocusedWindow`, takes the package that owns focus, and
fails only when that package is not ours, naming it in the message: the next
red leg says which window stole the screen.

**The five classes**, in full in the new document: runner infrastructure
(rerun once, then report an outage), device drop under load (make the
capture lighter, never rerun), the tour's anchors (a real finding, fixed in
the session), a window that is not ours (the guard above), and our own
script (the bug above). Rerun is allowed only for the first class and, with
a lighter capture, the second.

**Verification.** `:app:compileDebugAndroidTestKotlin` is green and the
workflow YAML parses with the fixed script. The capture is re-run after this
push; the fixed shape is proven by a red leg now printing `Collected:` and
`frames: N` and keeping its artifact, and by the intruder message naming a
real package if one ever appears.

## D-089: The 1.9 hand-off run

Date: the twenty-seventh session, on the owner's word to go for a Play
release.

Step 0 of the runbook passed first: `content/raw` holds all 29 datasets (the
24 QUL exports, the two QuranEnc files, and the Tanzil XML) and
`content/work/verify` holds all 29 extractions, so `verify`, `audit`, and
`fonts` could run on this machine rather than being owed the way 1.4 and 1.6
left them.

**All five owner gates ran green.** `verify` 29 datasets (checksums and
structure), `audit` 6236 ayahs with 0 unexplained differences and one
accepted orthographic variant, `fonts` coverage passed (628169 study
codepoints, 604 pages, 22985677 reading codepoints), `checkdb` (the
committed database unchanged, 128966656 bytes, `5c5988fa...`), and `search`
(63 Arabic round trips, 82 non-ASCII codepoints folding cleanly, 465
excerpts free of markup). The JVM suite, lint, and `assembleDebug` are also
green.

**The version.** `versionCode` 20, `versionName` 1.9, per the runbook's
counting (1.8 then 1.9, no 1.10). The bundle's own manifest was read back to
confirm: versionCode 20, versionName 1.9.

**The release notes** are 363 characters, no apostrophes, quotes, or dashes,
and they lead with the one thing a reader will notice first: the Listening
page. They are kept under their own heading in `play-store/listing.md`, with
1.8's notes intact below.

**CI.** Build run 35980906492: the data instrumented suite, the gates and
debug build, and the `signed-bundle` job, all green. Screenshot run
35980906679: all three legs green first try on the repaired workflow.

**The store set** is from run 35980906679, all three form factors, every
frame `cmp`'d against its artifact (24 matches) and every changed frame read
before it shipped. Only the settings frames changed in substance (Version
1.9 and the Listening row); the rest of the differences are the status bar
clock, the cursor's blink, and subpixel antialiasing.

**The bundle.** `quran-1.9-vc20.aab`, 147817691 bytes, SHA-256
`3b9feb8591e57f5c06f84dacb1031890a0d14916cdaa7d25ef778641d4031dc2` (which is
the artifact's own recorded hash), `jar verified`, signed with the shared
upload key (`53:7D:09:D2:...:0D:9D:E5:21`). Read back from the bundle: it
carries `base/assets/content/core.db` and the 608 font files (the Hafs face
and the 604 page fonts), and no other pack. The bundle and the screenshots
are handed over together, before the submission, per the runbook.

**The submission is confirmed, so the hand-off is closed (D-089).** The owner
submitted 1.9 to Google Play for review, and the hand-off copy of the bundle
was deleted the same session, as the runbook requires: the artifact stays in
the build run and in Play, and `play-store/aab/` keeps only its own note. The
tree is clean.

## D-090: The owner's seventh reading report, and the measurements behind it

Date: the twenty-eighth session. The owner read 1.9 on a phone, sent two
screenshots, and asked seven questions. Six are answered in the tree this
session; the seventh (an Arabic interface) is deferred on the owner's word.
The owner approved the search order, the share sheet, the line-height
direction, and the per-moment Listening control before any code was written.

**1. The playback pill touched the glass.** The screenshot showed the
continue-offer pill's rounded ends at the very left and right edges of the
phone. The pill is centered in a full-width `Column` and had no horizontal
margin of its own, so a long line ("Continue to Al-Baqarah - 177 MB") pushed
it to the window's edges. A floating control keeps a gutter, and past the
gutter the content gives, not the margin. Both states of the bar (playing
and offer) now take 16 dp of horizontal padding **outside** the shadow, and
both text columns are the flexible thing (`weight(1f, fill = false)`, the
reciter name on one line, the status at most two, ellipsized), so the size
and the buttons never truncate.

**2. Share sent text with the picture, and the card was a mess of
alignments.** `ACTION_SEND` carried `EXTRA_STREAM` **and** `EXTRA_TEXT`, so
receivers posted the image with the text welded on as a caption, and some
posted both. Tap Share now raises a small sheet with a live preview of the
real card, and two doors: **Share image** (the default, the PNG alone, no
text attached) and **Share text**. The card is rebuilt as one centered
column: the reference, the Arabic, the translation, a hairline, then the
mark with **Quran: The Noble Book** (the store title, its own string; the
frozen `app_name` is untouched) and the translation's name. One alignment
system replaced the four the card had (Arabic right, translation left,
reference right, app name left). The store set is not captured by this
session.

**3. A paragraph with any Arabic breathed as a whole Arabic paragraph.**
Measured, not guessed. The app gave the whole paragraph `arabicSp * 1.9`,
which at the default tafsir size is 42.6 px of line for a 16 sp Latin
paragraph. The Arabic's own ink was then measured out of the sources' own
glyphs, the way the font gate measures coverage: the Arabic-only paragraphs
need 1.67 em at the median and 1.84 em at the 90th percentile, while the
Arabic inside the mixed paragraphs needs 0.97 em at the median and 1.32 em
at the very worst. The owner's report was about the mixed paragraphs, and
the two distributions are not the same size at all, which is the whole
defect. The fix lands where the owner asked, at the block level the tafsir
source already gives: a paragraph that is **only** Arabic keeps its full 1.9
line, unchanged, and a paragraph that **mixes** scripts now takes
`max(latinLine, arabicSp * 1.35)`: at the default size, 30.2 px of line
instead of 42.6, and still past the 1.32 em the corpus reaches at its worst.
Compose applies line height per paragraph and has no public per-line
override (read in the pinned Compose 1.12.0 source: `LineHeightStyleSpan`
bounds the whole paragraph, and `SpanStyle` has no line-height field), so
the unit of the decision is the block, exactly as the source writes it, and
the limit is named rather than hidden. A pure `core` function (`scriptMix`)
classes a block as LATIN, MIXED, or ARABIC, and its test found a real bug
while it was being written: a space between two Arabic words is its own run
in the parser, and counting it as Latin classed an Arabic paragraph as
mixed. Only letters decide the class.

**4. Search order.** It was: a typed reference first, then a matched surah
name, then everything else in one Mushaf-order list with a tafsir row sorted
after its ayah. The owner approved the kinds running from the verse
outward: reference, surah name, then the ayahs that matched in the **Arabic
text**, then **translation**, then **word meaning**, then **tafsir**, each
group still in Mushaf order. Within one grouped kind, an ayah whose
translation also matched still opens its translation below the Arabic line,
so a row never loses material it used to show. This is a `compareBy` in
`ContentDatabase.search` plus its own test.

**5. `Go to ayah` became `Go to Ayah`,** and the picker became a page: the
surah selector is a card with its own quiet fill, rounded shape, and 14 dp
of inner air; the header, selector, and grid share one gutter; the grid
opens `Space.Block` under the selector instead of nearly touching it. The
owner's "no gap between the surah name and the ayah" is closed.

**6. Listening on the pill, and why it is the same two values.** The owner
asked for the pace and repeat while an ayah plays. The honest model is one
value with two doors, not two values: the pill's control calls the same
`setPlaybackSpeed`/`setRepeatAyah` the Settings page calls, so the choice
applies at once, persists, and the Settings page reads it back. A separate
"this session only" value would be a second truth to explain and to keep in
step, and a reader who slows one ayah almost always means the next one too.
The pill's status line became the door: a quiet chip with a chevron opening
a menu in the pill's own cloth, holding the same five-speed segmented
control and the repeat switch as the Listening page. The offer bar has no
pace control: the reader there is deciding whether to download, not how to
hear. Two owners of the two setters on the pill (the toggle and the menu)
share one callback set, so no new state was added.

**7. Arabic as an interface language: deferred.** The design is sound and
nearly ready (the enum, the locale plumbing, and `supportsRtl` are in
place), but rule 8 needs a real translation pass over about 240 strings and
a mirrored-layout QA pass. The owner deferred it; nothing changed this
session, and this note is where it stands if a later session picks it up.

**Verification.** The JVM suite (core and data), the app's unit tests, lint,
and `assembleDebug` are green. New tests: `RichTextTest.scriptMix` in core
and a search-order test in data. The emulator was not driven by hand; the
instrumented claims wait for CI, and the store set will be refreshed by the
next capture because every surface above moved.

## D-091: The 2.0 notes, drafted and held

Date: the twenty-eighth session, after D-090.

The runbook's version rule gives the release after 1.9 the name **2.0**
(no 1.10 exists: the tenth release of a major line is its `x.0`), so this
entry exists so the session that cuts the release does not have to
rediscover the arithmetic: **the next version is 2.0, versionCode 21.**

The notes, 482 characters, written under `## Release notes (2.0)` when the
release is cut, and kept at that length because the Play field holds 500:

Share now shows the ayah card before you send it, with one button for the
picture and one for the words, so the text never rides along with the image.
The playback pill keeps its distance from the screen edge, and the pace and
repeat are one tap away while an ayah plays. Tafsir paragraphs with a quoted
Arabic line no longer stretch every line of the paragraph apart, search
results run from the verse outward, and Go to Ayah opens as its own page. No
ads, no trackers, no account.

**The store set.** Every surface in this report moved: the pill, the share
sheet, the picker, the tafsir's paragraphs, and the search results. The next
capture refreshes all three form factors, and the eight-frame set keeps its
count: no new frame is needed, because every changed surface is one the set
already shows, except the share sheet, which is a door inside the ayah
actions the eighth frame already captures. If the owner wants the share
sheet in the set, a ninth frame has to earn its place against the reading,
which is the runbook's own rule; it is not added here.

## D-092: The 2.0 release, prepared

Date: the twenty-eighth session, on the owner's word to go for a Play release.

Step 0 passed before anything moved: `content/raw` holds the manual exports
(27 QUL files, the two QuranEnc files, and the Tanzil XML) and
`content/work/verify` holds all 29 extractions, so the owner gates ran here
rather than being owed.

**All five owner gates green.** `verify` 29 datasets (checksums and
structure), `audit` 6236 ayahs with 0 unexplained differences and the one
accepted orthographic variant, `fonts` coverage passed (628169 study
codepoints, 604 pages, 22985677 reading codepoints), `checkdb` (the committed
database unchanged, 128966656 bytes,
`5c5988fa2916eb1cc905d01ddb9b4ef9319d0ca19c945bf0140eaff32296cea9`, and the
catalog matching 10 pack files), and `search` (63 Arabic and 65 Bangla round
trips, 82 non-ASCII codepoints folding, 465 excerpts free of markup).

**The version.** `versionCode` 21, `versionName` 2.0, read back from the
built release APK rather than assumed. The version line in
`play-store/listing.md` moved with it, and the notes sit under their own
`## Release notes (2.0)` heading, 482 characters, no dashes or smart quotes,
with 1.9's notes kept below.

**The suite.** The JVM suite, lint, `assembleDebug`, `assembleRelease`, and
`bundleRelease` are green. On the phone emulator: the data instrumented
suite 31/31 and the app instrumented suite 19/19, the new `PlaybackPillTest`
and the tour among them.

**A test of mine was wrong, and the finding is worth keeping.** The new
search-order assertion was written against one live query, and that query
returns so many translation matches that the 200-row cap hid every other
kind, so the assertion failed on its own assumption rather than on the
order. The order is a pure function now (`data/SearchOrder.kt`), pinned by
five JVM tests that build their own hits and cannot be fooled by what the
corpus returns; the instrumented test keeps only what a device can prove
(the kinds never run backwards and one kind keeps the Book's order). A
contract should be tested where it lives, not through whatever one query
happens to answer.

**One list, two doors, one declaration.** `SpeedSteps` moved from
`feature-settings` to `ui-kit/Formats`, because the pill's menu and the
Listening page now draw the same five paces: two copies of the list would be
two answers to one question, and the copy would only be found the day one of
them changed.

**The bundle.** Built locally from the vault keystore,
`app/build/outputs/bundle/release/app-release.aab`, 147824330 bytes,
`jar verified` (the PKIX warning is the self-signed upload key's own chain,
which is expected and is why CI prints the certificate's SHA-256 from the
authoritative artifact). Read back: versionCode 21, versionName 2.0,
`base/assets/content/core.db`, the Hafs face, 604 page fonts, and no other
pack. The hand-off artifact is CI's `signed-bundle` from the release push,
per the runbook.

**Still owed at hand-over.** The screenshots. Every surface this release
touches moved visibly (the pill, the share sheet, the Go to Ayah picker, the
tafsir's paragraphs, the search results), so the capture runs on the release
push and all three form factors are collected, `cmp`'d, and read before the
hand-over, which happens only once the CI run is green and the signed bundle
has been downloaded from it.

## D-093: The 2.0 store set, and the race it found

Date: the twenty-eighth session, on the release push.

The push ran build 36006955183 (gates and debug build, data instrumented
tests, and the signed bundle: all green) and screenshot run 36006955051
(phone, 7 inch, and 10 inch: all three legs green first try). All 24 frames
were collected and compared with their artifacts.

**A real finding the comparison caught.** The phone's ayah-card frame differed
from its predecessor by 15% of its pixels, far more than a clock or a cursor:
rows 928 to 1210, the card's Translation block, were missing. The tablet7 and
tablet10 cards differed from their predecessors by 0.03% and 0.02%. The cause
is the race the runbook already names for sheets, in a place it had not been
seen: the card's window exists and settles before its translations have been
read off the database on a worker, so a capture that keeps the first two
identical frames can photograph a card that is not yet whole. The reader
never sees it (the sheet is still animating open at that moment), but a still
frame does. The card now carries `ayah-card` only once its content is ready
and `ayah-card-loading` before it, and the tour waits, within a bound, for
the ready tag before it keeps the frame. A genuinely empty card still
photographs, because a device with nothing installed is a true state rather
than a race.

**The set is installed from run 36006955051**, all three form factors, every
frame `cmp`'d (24 checks): 24 of 24 match the artifact. Every changed frame
was read before it shipped. The changes are the release's own: the search
frames lead with a translation match instead of a tafsir row, which is the
new order working; the settings frames read Version 2.0; the Browse frames
carry the capitalized Go to Ayah chip; the pill, the picker, and the tafsir's
paragraphs moved as designed; and the phone and tablet search frames
otherwise differ only by the status bar's clock, the cursor's blink, and
subpixel antialiasing (frame 01's whole-image difference is a maximum channel
delta of 4, antialiasing and nothing else).

**The bundle, from the run that carries every commit.** The first download
came from build run 36006955183, the release commit; the card fix (D-093)
landed after it, so the bundle was replaced with the artifact of build run
36008294664, which contains the whole source. This is the trap in miniature:
the hand-off bundle must come from the newest green build on `main`, not the
first one after the version bump. `quran-2.0-vc21.aab`, 147837180 bytes,
`jar verified`, signed with the shared upload key whose SHA-256 is
`537d09d20300129e973b7945316bfe24cfadcfbc77eec5229cbf30170d9de521` (checked
with `keytool -printcert -jarfile`), and its delivered SHA-256 is exactly the
one the artifact recorded,
`523d22e233a5db9c7d40a74ce5f3804968712b9e665b9f17f78218e6baa9a66d`. Read
back: `base/assets/content/core.db`, the catalog, the Hafs face, the 604 page
fonts, and no other pack.

**Handed over together, before the submission.** The bundle (run 36008294664)
and the 24 screenshots (run 36008295034) are the delivery. The screenshots are
installed in `play-store/screenshots/`, every frame compared with its
artifact by `cmp` (24 of 24 match) and every changed frame read. The
hand-off copy of the bundle is deleted once the owner confirms the Play
submission, as every release before it has done.

**The submission is confirmed, so the hand-off is closed.** The owner
submitted 2.0 to Google Play for review, and the hand-off copy of the bundle
was deleted the same session, as the runbook requires: the artifact stays in
the build run and in Play, and `play-store/aab/` keeps only its own note. The
tree is clean. The signed bundle's record is above and in build run
36008294664; nothing in the repository depends on the binary.

## D-094: The owner's eighth reading report

Date: the twenty-ninth session. The owner read 2.0 on a phone, sent two
screenshots (the Go to Ayah picker and a search result for "mercy"), and
asked eight things. All eight are answered in the tree this session; no
version moved, because the release waits for the owner's word.

**1. The share card dropped the translation's name.** The card ended with
the app's mark, **Quran: The Noble Book**, and the translation's name under
it. The receiver of a picture needs neither; the line was small type with no
door behind it. `ShareCard` no longer carries `translationName`, and the
card's foot is the mark and the store's name alone. The share sheet's preview
draws the same composable, so the preview lost the line with the picture.

**2. Arabic paragraphs now wrap from the right, and the cause is named.**
The owner reported that an Arabic passage in a tafsir started at the left
once it wrapped. First the claim was checked, on the emulator, with a probe
that laid out an Arabic-only paragraph and read the line geometry back: the
paragraph resolved to `dir=Ltr` and every line after the first began at x=0.
The cause is Compose's own default, read in the pinned 1.12.0 sources:
`TextDirection.Unspecified` resolves against the *composition's* layout
direction (`resolveTextDirection`), so an English interface gives every
paragraph an LTR base no matter what it says. `TextDirection.Content` takes
the block's own first strong character instead. `RichBlocks` now states
`Content` on its headings and paragraphs, and `ArabicBody` states `Rtl` beside
its right alignment. Only all-Arabic blocks are affected in today's tafsirs;
a mixed paragraph is Latin-first by design, and the corpus was scanned (every
pack, 4,836 mixed blocks) and holds no Arabic-dominant mixed block that would
need a different rule. `TafsirDirectionTest` draws the real `RichBlocks` and
reads the last line's pixels; it fails on the old code and passes now, and it
runs in the capture workflow on all three form factors.

**3. The Go to Ayah gap survives the scroll.** The grid's `Space.Block` top
padding was *inside* the scrollable content, so scrolling to the reader's
own ayah carried it away and the clipped number pill touched the surah card.
The 16 dp now sits between the card and the grid, outside the scroll, and the
grid's content padding keeps only its horizontal and bottom room.

**4. Notes previews the reader's own words.** Saved and Notes drew the same
row, so the owner could not tell the note on Ayat Al-Kursi from any other
without opening them one by one. The Notes row now draws the note under the
place: two lines at most, in the reading face (Literata) at 14 sp and the
reading ink, while the place and the moment stay interface type. Two lines
are the reserved room, so a long note is ellipsized and every row keeps the
same height. Saved is untouched, and the two lists now differ at a glance.
`NotesPreviewTest` writes a note on 2:255 before the activity starts and
reads it back in the list.

**5. Go to Ayah is a tab, not a page over the tabs.** The owner asked why it
alone showed a back arrow and its own name while Surahs, Juz, and the rest
showed neither. It is now the sixth chip: the chips stay on screen, the chip
takes the chosen fill like the tab it is, and the grid hangs under the row
with no title repeating the chip's own name. Choosing another surah is the
one step with a head (Choose a surah) and a back, because it is the one step
with somewhere to go back to; the phone's back returns to the numbers before
it leaves the sheet. This supersedes D-084's rule that the door never takes
the chips' chosen fill, on the owner's coherence report. `GoToAyahTest`
follows the whole new path unchanged.

**6. A search row names its word meaning.** The line rode under the
translation in nearly the translation's own type, so it read as the
sentence's last words. It is now a block of its own under the row: the label
**Word meaning** in the speaking tone, `Space.Block` of air above it, and the
meaning under it in the reading size with the matched term washed, exactly
the shape the tafsir result's own header already had.

**7. No footer size, and the reason is the reading.** The footer is not
reading text, so it does not get a reading size. In the study reading the
reference at the foot of an ayah (and the tafsir range) is the interface's
own `labelMedium`, independent of the four reading steps and scaled by the
system font setting; in the Mushaf the page number and juz are drawn into the
page bitmap from the page's own glyph em (`PAGE_NUMBER_RATIO` 0.4 and
`HEADER_RATIO` 0.34), so they follow the printed page and nothing else; the
translate-on-tap footnotes already take the translation's size
(`FootnoteSheet`). A fifth size step would be a choice that does not earn its
place against the reading, per rule 10.

**8. What the reading draws is a switch, beside its size.** The word by word
switch moved out of the Translations page into the settings hub, and it is
joined by **Show translation** and **Show tafsir**, all three under Font size
where the reader already looks for how the reading draws. The pack pages keep
choosing content; the switches say what the page shows. Translation and
tafsir are on by default, so a reader who added one sees it. Word by word
stays off by default even though it is the same kind of switch: turning it on
is the door that fetches the word list, with the size on the row, and a
default-on switch would fetch a pack at first launch, which rule 1 forbids.
With tafsir hidden the study reading's pill drops its Tafsir action rather
than opening an empty card. Search is deliberately untouched: its own filter
chips already say what it reads, and the display switches are about the
reading surface. `SettingsVisibilityTest` hides the translation, proves the
reading has no translation, flips the switch in the hub, and waits for the
text to return; `ScreenshotTest.prepareTheLibrary` now pins both switches so
the tour cannot inherit a hidden state from another test.

**Verification.** The JVM suite (core, data, app unit tests), lint, and
`assembleDebug` are green. The app instrumented suite ran complete on the
phone profile: 22 of 22, including the new `TafsirDirectionTest`,
`NotesPreviewTest`, and `SettingsVisibilityTest`, the tour, and every older
test. Two surfaces in the store set changed visibly (Settings gains the three
switches; Search names its word meanings), so the next capture refreshes all
three form factors; no frame is added or removed. On the owner's word the release
followed in the same session: 2.1 (versionCode 22), with these notes drafted
as the release notes and every gate run before the push (D-095).

## D-095: The 2.1 release, prepared

Date: the twenty-ninth session, on the owner's word to go for a Play release.

Step 0 passed before anything moved: `content/raw` holds the manual QUL and
QuranEnc exports and the Tanzil XML, and `content/work/verify` holds all 29
extractions, so the owner gates ran here rather than being owed.

**Version and notes.** `versionCode` 21 to 22 and `versionName` 2.0 to 2.1,
in `app/build.gradle.kts` and on the listing's version line. The notes for
2.1 are 425 characters, one paragraph under their own heading, and the 2.0
notes stay under theirs.

**All five owner gates green.** `verify` 29 datasets; `audit` 6,236 ayahs
compared, 77,432 KFGQPC words against 77,433 Tanzil words, one accepted
orthographic variant, zero unexplained differences; `fonts` coverage passed;
`checkdb` the committed database unchanged, 128,966,656 bytes, SHA-256
`5c5988fa2916eb1cc905d01ddb9b4ef9319d0ca19c945bf0140eaff32296cea9`; `search`
465 readable excerpts with no markup.

**The local suite.** The JVM suite (core, data, app), lint, and
`assembleDebug` green; the data instrumented suite 31 of 31 and the app
instrumented suite 22 of 22 on the phone emulator, the three new tests
included.

**The push starts the rest.** `build.yml` runs the content gates and the
signed bundle, and the screenshot workflow runs because surfaces that draw
changed. The run IDs, the store set, and the bundle record follow in D-096
when they are in hand.

## D-096: The 2.1 store set and the 2.1 bundle

Date: the twenty-ninth session, on the release push.

The push ran build 36125481634 (gates and debug build, data instrumented
tests, and the signed bundle, all green) and screenshot run 36125481691
(phone, 7 inch, and 10 inch, all three legs green first try).

**The first capture was green and wrong.** Run 36121494370, the release
commit's own capture, passed on all three legs, and `cmp` then found the
phone's 07-browse and 08-ayah-card frames differed from 2.0 by nearly every
pixel. Reading them showed "Pixel Launcher isn't responding" over both: the
dialog never took focus, so the guard that reads `mCurrentFocus` saw nothing,
and it swallowed the back key that closes Browse, so frame 08 photographed
the ayah card opening over the still-open sheet, a screen two steps behind
the tour. This is class 4 in `docs/screenshot-failures.md` wearing a new
face, and both halves are fixed there and in the test: the guard now also
reads visible error dialogs out of their own window blocks (only
`mHasSurface=true` counts, so a stale record is never an intruder), and the
tour waits for the Browse sheet's tag to disappear after the back key, so a
swallowed key is a red leg and not a quiet wrong frame. A new
`theWindowGuardReadsVisibleDialogs` test feeds the guard a live dialog, a
dismissed one, and an ordinary window, because the guard cannot wait for a
real ANR to be tested. The fix ran build 36125481634 and capture 36125481691;
both green.

**The set is installed from run 36125481691**, all three form factors, every
frame compared with its artifact by `cmp` (24 of 24 match). Every changed
frame was read before it shipped, and the changed set is small: the settings
frames carry the three switches and Version 2.1, the tablet search frames
show the named word meaning block (the phone's search frame shows only the
Arabic above the keyboard, unchanged), and the Browse and ayah-card frames
differ from 2.0 only in the status bar clock, which the numeric compare
confirms (the differing bounding box is the clock on every form factor).

**The bundle, from the newest green build on main.** Run 36125481634
carries every commit, including the guard fix. `quran-2.1-vc22.aab`,
147,841,916 bytes, SHA-256
`a81cad7ef254409cdd24c5681f3541d9b2453709b88a17453f1bef8a51a1d6dd` (the
artifact's own recorded hash, recomputed from the file), `jar verified`,
signed with the shared upload key
(`53:7D:09:D2:03:00:12:9E:97:3B:79:45:31:6B:FE:24:CF:AD:CF:BC:77:EE:C5:22:9C:BF:30:17:0D:9D:E5:21`).
Read back from the bundle: `base/assets/content/core.db` and its catalog,
the 604 page fonts and the Hafs face, and no `assets/packs` entry at all.

**Handed over together, before the submission.** The bundle (run
36125481634) and the 24 screenshots (run 36125481691) are the delivery, with
the 2.1 notes pasted bare. The hand-off copy of the bundle is deleted once
the owner confirms the Play submission, as every release before it has done.

**The submission is confirmed, so the hand-off is closed.** The owner
submitted 2.1 to Google Play for review, and the hand-off copy of the bundle
was deleted the same session, as the runbook requires: the artifact stays in
build run 36125481634 and in Play, and `play-store/aab/` keeps only its own
note. The tree is clean. The signed bundle's record is above; nothing in the
repository depends on the binary.

## D-097: The owner's ninth reading report, and the daily reminder

Date: the thirtieth session. The owner read 2.1 on a phone and asked for
eight things: three defects, three wording and structure changes, one
new feature, and one content question. All eight are answered in the tree.
No version moved, because the release waits for the owner's word.

**1. Go to Ayah lands the reader's ayah mid-grid.** The picker's grid
scrolled to the reader's place with `scrollToItem(index)`, which puts the
target at the viewport's **top** edge. On a 286 ayah surah the reader's row
sat in the same corner as every row above it, so the place they came for was
readable only if they knew the number; the owner saw the top of the surah
instead. The grid now scrolls to the place and then centers it: the target's
cell height is measured from the grid's own layout info and the second
scroll moves it down by half the leftover viewport. The scroll is also keyed
on the place as well as the surah, so a jump that changes the reader's ayah
while the picker is composed can never leave the grid on a place that is no
longer theirs. `GoToAyahScrollTest` measures the cell against the grid's
center and fails on the old scroll with `grid center 1231.0, cell center
663.0`.

**2. A search row never says the same match twice.** The word meaning was
drawn as its own labelled block under every row that had one, so a reader
who searched "mercy" saw the translation's highlight and, under it, the same
match as a meaning (owner report). The block is now drawn only when it is
the row's **only** evidence of the match: `shouldShowWordMeaning` in `data`,
pure and pinned by `SearchMeaningTest` in both the JVM suite and against the
shipped database. A row that matched only a meaning keeps it, named, because
an unlabelled line under the ayah would be read as a translation.

**3. The reading switches and the packs are one row each.** The hub carried
a switch and, four rows below, a separate page row for the same thing, which
read as two controls for one decision. Each switch now carries the chevron
that opens its own list, with its own spoken name, so the translation's row
is the translation's door and the tafsir's row the tafsir's. Word meanings
is untouched: there is no list to choose from, and the aid's language
follows the reader's translation (D-046). The separate hub rows are gone.
`SettingsMergeTest` opens the list from the switch's own row and proves the
hub no longer repeats it.

**4. The share card is captured whole on every device.** The card is read
off a graphics layer with `toImageBitmap`, which produces a **hardware**
bitmap: a texture, capped by the device's own size limit and refused
outright by a software canvas ("Software rendering doesn't support hardware
bitmaps"). A long ayah at a large text scale passes 4,096 px, which is the
limit on much of the mid range hardware this app runs on, and the tail was
lost. The capture now takes the card in 2,048 px windows, copies each
window to a software bitmap, and stitches them into one bitmap the PNG
writer can encode. A single window covers every ordinary card, so the
common case pays nothing. The sheet's preview had a second defect of the
same symptom: its height cap sat *inside* the scroll, so a long card was
clamped at 340 dp with nothing left to scroll. The cap is on the viewport
now. `ShareCardCaptureTest` captures Al-Baqarah 2:282, three windows tall,
and checks the last row's pixels are painted; `SharePreviewScrollTest`
scrolls the translation's last words into view and fails on the old order.

**5. The theme swatch says what is drawing.** With automatic night mode on
and the phone in dark mode, Night draws the app while the stored day choice
is still Paper; the swatch row filled Paper, which read as the app ignoring
the switch (owner report). The filled swatch is the **resolved** theme now,
in the Appearance page and in the hub's summary, and the day page the reader
chose is named beside it so the choice is never lost. A tap still sets the
day page.

**6. One spelling of Tafsir in Bangla, and the listening word.** The strings
carried both তাফসীর and তাফসির; the QUL Bangla tafsir corpus holds 2,519 of
the first and none of the second, so তাফসীর is the spelling everywhere. For
the Listening page the formal `শ্রবণ` was first read as a word problem and
replaced with the everyday `শোনা`, and the owner then named the word they
had meant: `অডিও`, the transliteration of "audio", which is what the page
truly is (the reader's word had arrived garbled by the terminal). The page
reads `অডিও` now. The whole Bangla surface was swept for the same class of
drift; no other double spelling survives (D-097, corrected on the owner's
word).

**7. The daily reminder.** The owner asked for an ayah of the day: one
notification, carrying the ayah and, when the reader reads with a
translation, the first enabled one, with a tap opening that ayah in the
study reading, and a switch to turn it on. It is built to the house rules:

* **Offline, always.** The ayah is read from the content database that
  already ships on the device; the translation from a pack the reader
  installed. `DailyAyahContent` opens the same library the reading does,
  reads exactly one ayah and at most one translation, and closes it.
  Nothing is fetched and nothing is kept.
* **No new permission and no new dependency.** The reminder fires from an
  `AlarmManager` alarm through an unexported `BroadcastReceiver`, so its
  pending intent is the only thing that can wake it, and the manifest's
  permission set is byte-identical to 2.1's. WorkManager was the other
  candidate and was not taken: it is not in the allowed dependency list, and
  an inexact daily alarm is the smallest thing that does the job. The alarm
  is re-armed on the app's own launch, which corrects a reboot, a timezone
  change, and a Doze deferral without asking for a boot permission this app
  has never needed.
* **Deterministic and in order.** `core/DailyAyah` walks the Book by the
  local epoch day, `floorDiv` and `floorMod` so a pre-epoch instant or a
  negative day still lands inside 1..6236, with the reader's own timezone
  offset. `DailyAyahTest` in `core` proves the same day always gives the
  same ayah, the walk runs forward and wraps once, a full rotation reaches
  every ayah exactly once, and midnight is where the day turns.
* **One ayah, one translation, plain words.** A notification cannot open a
  footnote, so `RichText.plain` drops the markers and their notes; the
  reminder carries the readable form.
* **A tap is a reading.** The pending intent carries the ayah; the activity
  reads it once on cold start and jumps in study mode, and the same ayah is
  written down as the reader's place. Writing this found a real bug in the
  first cut: an absent extra defaulted to 0, which coerced to ayah 1, so
  every launch jumped to the first ayah. The default is a sentinel that
  cannot be a place.

**8. A test-suite trap the work uncovered.** Eight tests in the app suite
began failing together, and the cause was not the app: several of them
tapped the study page's exact `center` to bring the chrome, and at the
center of Al-Fatihah 1:1 sits a translation footnote marker, which is a
door. The tap opened the footnote sheet and every wait after it failed.
Every such test now taps the page's own left margin through one shared
`tapThePaper()` helper, which is where a reader taps too. The same flakiness
was proven present in the clean 2.1 tree before the change, so it is
recorded here as a trap rather than a regression.

**Verification.** `core:test`, `data:testDebugUnitTest`,
`app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug` are
green. The data instrumented suite is 31/31 and the app instrumented suite
is 36/36 on the phone emulator, the five new classes included:
`GoToAyahScrollTest`, `SearchMeaningTest`, `SettingsMergeTest`,
`DailyAyahTest`, `DailyAyahJumpTest`, `DailyAyahToggleTest`,
`ShareCardCaptureTest`, and `SharePreviewScrollTest`. The design document's
Browse, Search, and Settings sections carry the new rules. The store set
changes visibly (settings, search, and the share sheet), so the next capture
refreshes all three form factors.

## D-098: The 2.2 release, prepared

Date: the thirtieth session, on the owner's word "go for play release".

**The listening word is corrected first.** The owner's request had arrived
garbled by the terminal as `অও`; the word they had meant is `অডিও`, the
transliteration of "audio", and the Listening page reads it now. D-097's
paragraph 6 is corrected in place and the runbook's Bangla trap with it.

**Version and notes.** `versionCode` 22 to 23 and `versionName` 2.1 to 2.2,
in `app/build.gradle.kts` and on the listing's version line. The 2.2 notes
are 462 characters, one paragraph under their own heading, and the 2.1 notes
stay under theirs.

**All five owner gates green.** `verify` 29 datasets; `audit` 0 unexplained
differences; `fonts` coverage passed; `checkdb` the committed database
unchanged, 128,966,656 bytes, SHA-256
`5c5988fa2916eb1cc905d01ddb9b4ef9319d0ca19c945bf0140eaff32296cea9`; `search`
clean.

**The local suite.** The JVM suite (core, data, app), lint, and
`assembleDebug` green; the data instrumented suite 31 of 31 and the app
instrumented suite 39 of 39 on the phone emulator, the eight new classes
included.

**The push.** `build.yml` ran build 36164556479, green (gates, data
instrumented tests, and the signed bundle). The screenshot run 36164556475
went red on the phone leg alone: the tour met a launcher ANR over
`05-search`, the guard refused the frame, and the leg kept its four good
frames; both tablet legs passed the same code first try. That is the
class D-078 names, and one rerun is what the runbook allows for it: attempt
2 is green on all three legs.

**The store set**, from attempt 2 of run 36164556475, all three form
factors, every frame compared with its artifact by `cmp` (24 matches) and
every changed frame read. The changed set is the release's own: the settings
frame carries the merged switches with their chevron doors, the chosen
pack's name under each, and the new Daily ayah switch (off); the tablet
search frames show the translation highlight standing alone with no repeated
word meaning block; the phone Mushaf, Browse, and ayah-card frames differ
only in the status bar clock and subpixel antialiasing, with the Mushaf
page's whole-page difference measured at a maximum delta of 4 of 255.

**The bundle**, from the newest green build on main, run 36164556479:
`quran-2.2-vc23.aab`, 147,867,353 bytes, SHA-256
`ba4b9c1e4060a8fa16ef5424319ea2f8b17c9f7121fb83aff3961b4b5ec67c7d` (matching
the artifact's own recorded checksum), `jar verified`, signed with the
shared upload key
(`53:7D:09:D2:03:00:12:9E:97:3B:79:45:31:6B:FE:24:CF:AD:CF:BC:77:EE:C5:22:9C:BF:30:17:0D:9D:E5:21`).
Read back from the bundle: `base/assets/content/core.db` and its catalog,
the 604 page fonts and the Hafs face, and no `assets/packs` entry at all.

**Handed over together, before the submission.** The bundle and the 24
screenshots were the delivery, with the 2.2 notes pasted bare.

**The submission is confirmed, so the hand-off is closed.** The owner
submitted 2.2 to Google Play for review, and the hand-off copy of the bundle
was deleted the same session, as the runbook requires: the artifact stays in
build run 36164556479 and in Play, and `play-store/aab/` keeps only its own
note. The tree is clean. The signed bundle's record is above; nothing in the
repository depends on the binary.

## D-099: The owner's tenth reading report

Date: the thirty-first session. The owner read 2.2 on a phone and reported
five things: three defects of behaviour, one structure change, one question
about search, and one wording change that arrived mid-session. All five are
answered in the tree, and the search question is answered with measurements
rather than an opinion. No version moved, because the release waits for the
owner's word.

**1. A switch is a switch; the row around it is a door.** Show
translation, Show tafsir, and the daily reminder all carried a switch, and
the reader reported that tapping the row did something they did not ask for.
`ToggleRow` now splits the two: only the `Switch` toggles, and the rest of
the row, chevron included, opens the page the switch belongs to. The
reasoning is that a look is not a choice, and a control that answers a tap
with a different effect than its own label promises is two controls wearing
one shape. The two list pages carry their own switch at the head, so
choosing a pack from a page behind a switch is not a dead end: one value,
two doors, the same shape the audio page and the playback pill already use
(owner decision, 2.3). The switch also carries the row's title as its own
content description and a test tag, so TalkBack reads it as a switch and the
tests aim at it.

**2. The reading builds in the order the reader puts it together.** Word
meanings sit closest to the Arabic, so its row now sits above Show
translation, and the translation above Show tafsir (owner report, 2.3).

**3. The daily reminder is on from the first launch, and its moment is a
page.** Three reports in one: the reminder was off until asked, the hour was
a strip of twenty-four chips that filled the hub and held only the top of
each hour, and the time belonged beside the switch. The reminder now comes on
with the app (`SettingsStore.dailyAyah = true`, owner decision, 2.3) as a
silent low-importance channel, the hub carries one row whose subtitle is the
moment itself, and the moment is chosen on the `Daily` page behind that row:
the platform's Material clock in the app's own colors, with the time
available to be typed as well, which reaches every minute of the day. The
stored value is a minute of the day, not an hour (`dailyAyahMinute`,
`setDailyAyahTime`), and the old hour key is still read, so an install that
had chosen a time keeps it. The clock dialog sits inside the sheet as its own
window, the way a language choice already does.

**4. The phone's permission is asked for at two moments, and never for
nothing.** A default-on reminder cannot ask at launch (a dialog before
reading is cut by the law), so the permission is asked when the reader turns
the reminder on and when they move its moment, and at no other time. What
the app does not own is surfaced rather than hidden: `NotificationStatus`
reads the phone's own answer and refreshes it on every resume, and the page
says so in one sentence and names the way out as the phone's own
notification settings, with the pre-Android-8 page as the fallback.

**5. Go to Ayah says what it is standing for.** The card above the grid
carried the surah's Latin name alone, so a reader had to scroll the grid to
learn how long the surah was, and the surah step always opened at Al-Fatihah,
which hid the one row that answers "where am I". The card now carries the
surah's own Arabic name and its `place - n ayahs` line, the ayah cells are
48 dp (the design document's own floor for a thing a finger aims at, and the
picker's cells are the surface where the reader aims at a two-digit shape),
and the surah list opens on the reader's own surah and wears the same filled
mark the grid wears for their own ayah, so one shape means "the place you
are standing" on both steps. The picker moved to its own file
(`GoToAyahPicker.kt`), which also brings `BrowseSheet.kt` back under the
400-line rule (owner report, 2.3).

**6. Search: the word meanings stay, the decoration goes.** The question was
whether a Quran-text row that also carried a word meaning is redundant. It
is not, and the measurement says so. Against the shipped content database,
the English word "mercy" matches 144 ayahs through the translation and 148
through the word meanings, 139 of them the same ayahs, so only 9 are
reachable by the meanings alone, and a Latin query matches the Arabic text
zero times: on the other 139 rows the Arabic line was four lines of text
that had nothing to do with the search. In Bangla the balance is the other
way, because the Bangla translation and the Bangla gloss spell things
differently: "অহংকার" reaches 40 ayahs through the meanings alone and
"নামাজ" 4. So the decision is (owner decision, 2.3): keep the word
meanings, and stop printing Arabic that matched nothing. The Arabic is
drawn when it is the match (washed, as before), when the row has no other
evidence, which is a reference the reader typed, and otherwise not at all;
and a row whose only evidence is a word meaning now draws the **matched
word itself**, washed, in the ayah's own order, read from the core `word`
table in one batched pass beside the meanings (never one query per row). The
rule is pure and lives beside `shouldShowWordMeaning` in `data`, as
`arabicLineFor`, with its own tests in the JVM suite and against the shipped
database.

**7. প্রতিদিনের, everywhere the word is spoken.** The owner read
দৈনিক আয ় and asked for প ্ৰত িদ িন ের, which is the word the channel
description and the hub row already used. The settings title and the
notification channel's name now both read it, and a channel that already
exists on a reader's phone has its words refreshed: the platform has no
`updateNotificationChannel`, so re-creating the channel is the way, and the
system keeps whatever the reader chose in its own settings.

## D-100: The 2.3 release, prepared

Date: the thirty-first session, at the owner's word. 2.3 (versionCode 24)
answers the reader's tenth report (D-099) and is prepared for submission.

**The five gates ran on this machine before the release**, all green:
`verify` 29 datasets, `audit` 0 unexplained differences of the canonical
text, `fonts` coverage passed (22,985,677 reading codepoints, 604 pages),
`checkdb` (the committed database unchanged at 128,966,656 bytes, its ten
pack files verified), and `search` (63 Arabic round trips, 82 distinct
non-ASCII translation codepoints folding cleanly, 465 readable excerpts
carrying no markup). The manual QUL and QuranEnc exports were present in
`content/raw`, so nothing was owed on this release.

**The workflows.** `build` run 36263904415: the gates, the data instrumented
suite, the debug build, and the signed bundle, all green. `Capture store
screenshots` run 36263904407: all three legs green first try, which is also
where the app instrumented suite ran for this release, the tour included.

**The local instrumented runs and what they met.** Two full local runs of the
app suite each failed one class, a different one each time, and neither was a
defect: the first run's first test after install timed out waiting for the
study reading to draw (`BrowseNumbersTest`, `ComposeTimeoutException` after
90 s, no crash in its logcat), which is the cold-emulator class the runbook
names; the second run met the study list's prefetch scheduler on a loaded
emulator (`AyahActionsTest`, `The current thread must have a looper`), the
trap the runbook already carries. Both classes pass in isolation on the same
build, and CI's three legs ran the whole suite green, which is the authority
the runbook names. Neither finding is written off: they are the environment
classes, and the emulator here is left warm for the next session.

**The store set.** Run 36263904407, eight frames per form factor, every frame
compared with its artifact by `cmp`. A band-wise check flagged the same four
frames on all three form factors (search, settings, Browse, ayah card), and
the pixel measurement classed every one of them as antialiasing and clock
differences: worst average delta 0.04 of 255, worst single pixel 139, and no
frame differs in shape, copy, or layout. The settings frame's foot was read
back from the installed image and says `Version 2.3`.

**The bundle.** `quran-2.3-vc24.aab` from the newest green build on `main`
(run 36263904415, not the first run after the bump): 148,097,627 bytes,
SHA-256 `59d82244bf3858b0668cb643e56afee59cf34d4ddca6dfed73cbc2fc768af026`,
matching the artifact's own recorded checksum, `jar verified`, signed with
the shared upload key.

**A gap the release found, and its fix.** The capture workflow's `paths`
filter named only the modules that draw pixels, so the D-099 search
refinement, which changes what the search draws while living in `data`, did
not trigger a capture at all: the first push after it kept the old frames.
The filter now names `data/src/main/**` and `core/src/main/**` with the
reasoning written beside them, because a frame can change without a UI
module being touched.
