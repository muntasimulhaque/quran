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
