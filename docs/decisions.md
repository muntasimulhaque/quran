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

Ibn Kathir (English, QUL resource 35) and Tafsir As-Sa'di (Arabic, QUL
resource 24). Both are ayah-linked, group-aware tafsirs from QUL. The
tafsir reader must resolve grouped commentary, where several ayahs share
one entry, exactly as QUL's data model describes.

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
