# Content sources

Where every dataset comes from, what it is for, and what has to be
downloaded before the app can be built. This file is the human-readable
companion to `content/manifest.json`, which is the machine-readable
source of truth.

Two rules from the owner decision (D-003) shape everything here: existing
licenses are honored, and every dataset must be swappable through
`tools/` without an app change.

## The owner's download list (one time, from QUL, no credentials shared)

Log in to qul.tarteel.ai and download each resource below with the format
in the last column. Put every file in `content/raw/qul/`. The build
verifies structure and records checksums; it never needs your login.

| # | Resource | QUL page | Recommended format | Used for |
|---|---|---|---|---|
| 1 | KFGQPC Hafs script, word by word | quran-script/312 | sqlite | Canonical Arabic text and word positions |
| 2 | QPC V2 Glyph, word by word | quran-script/61 | sqlite | Mushaf glyph codes, one per word |
| 3 | KFGQPC V2 layout (1421H print) | mushaf-layout/10 | sqlite | Page, line, and word ranges |
| 4 | QPC V2 Font | font/249 | ttf | The 604 Mushaf page fonts |
| 5 | QPC Hafs font | font/245 | ttf | Study mode Arabic font |
| 6 | English Word by Word Translation | translation/92 | sqlite | Word-by-word meanings |
| 7 | Tafsir Ibn Kathir (English) | tafsir/35 | sqlite | English tafsir |
| 8 | Tafseer Al Saadi (Arabic) | tafsir/24 | sqlite | Cross-check only; the shipped Arabic tafsir is the QuranEnc edition below |
| 9 | Surah Info, English | surah-info/3 | json | Surah introductions and context |
| 10 | Surah names | quran-metadata/70 | json | Arabic and translated surah names |
| 11 | Ayah | quran-metadata/69 | json | Ayah numbering and page mapping |
| 12 | Sajda | quran-metadata/64 | json | Sajdah markers |
| 13 | Juz | quran-metadata/68 | json | Juz navigation |
| 14 | Hizb | quran-metadata/67 | json | Hizb navigation |
| 15 | Rub | quran-metadata/63 | json | Quarter-hizb navigation |
| 16 | Manzil | quran-metadata/66 | json | Manzil navigation |
| 17 | Ruku | quran-metadata/65 | json | Ruku sections |
| 18 | Mutashabihat ul Quran | mutashabihat/73 | json | Similar-ayah feature, version 2 |
| 19 | Topics | ayah-topics/45 | sqlite | Topic discovery, version 2 |
| 20 | Recitation, Muhammad Siddiq Al-Minshawi | recitation/108 | sqlite | Minshawi murattal, with segments |
| 21 | Recitation, Mahmoud Khalil Al-Husary (murattal) | recitation/110 | sqlite | Husary murattal, with segments |
| 22 | Recitation, Mahmoud Khalil Al-Husary (Muallim) | recitation/112 | sqlite | Teaching recitation, with segments |
| 23 | Recitation, Mahmoud Khalil Al-Husary (mujawwad) | recitation/111 | sqlite | Optional later set |

Also useful, for cross-checking only: Saheeh International on QUL
(translation/193, `translation-with-inline-footnote.sqlite`) as an
independent copy of the translation we ship.

## Downloads that need no account

| Source | What | Where | License and credit |
|---|---|---|---|
| QuranEnc | Saheeh International, `english_saheeh`, version 1.1.2, with footnotes | https://quranenc.com/downloads/sqlite/english_saheeh.zip | Republishing allowed with: no modification, credit Noor International Center and QuranEnc.com, show the version, keep the footnotes, send corrections, update to the latest version, no inappropriate ads |
| QuranEnc | Tafsir As-Sa'di, `arabic_saadi`, version 1.0.0, passage ranges covering all 6236 ayahs | https://quranenc.com/api/v1/tafsir/range/arabic_saadi/1/1/114/6 | Same QuranEnc republishing terms, credited to QuranEnc.com |
| Tanzil | Uthmani text, version 1.1, used only as the audit reference | https://tanzil.net/download/ | Verbatim copy allowed, no changes, credit Tanzil and link tanzil.net; the owner joins the Tanzil text mailing list for critical updates |
| KFGQPC | Uthmanic Hafs and QPC V2 fonts, already in the list above | qul.tarteel.ai | Distributed free by the King Fahd Glorious Quran Printing Complex; use, copy, and distribute unmodified; bundled byte-for-byte with the notice |
| Google Fonts | Literata, Inter, Amiri Quran | fonts.google.com | SIL Open Font License; license files bundled in `docs/` |
| QUL audio CDN | The recitation MP3 or WAV files referenced by the QUL recitation exports (`audio_url`) | audio-cdn.tarteel.ai | Downloaded once at pack build time, checksummed, bundled; the app never streams |

## How the pipeline uses them

1. `tools/fetch` reads `content/manifest.json`, verifies every checksum,
   and fails the build on any drift.
2. `tools/audit` compares the KFGQPC word text against Tanzil at word
   level, lists every difference in `content/audit-report.md`, and fails
   unless each difference is either zero or explicitly recorded. First run:
   6235 of 6236 ayahs letter-identical, one accepted orthographic variant
   at 2:72, one segmentation note at 37:130, zero unexplained differences.
3. `tools/build` writes the read-only content database with precomputed
   normalized search columns, page and line geometry, word hit rectangles
   for Mushaf mode, and joined translation, word-by-word, and tafsir rows.
4. `tools/fonts` proves that every codepoint used by the Quran text is
   drawable by the bundled fonts, and extracts the page font set.
5. `tools/packs` builds the recitation asset packs from the verified
   audio, one pack per reciter, and records their contents and sizes.

## Credit lines shown in About

- Arabic text: King Fahd Glorious Quran Printing Complex, via the Quranic
  Universal Library (QUL) by Tarteel.
- Audit reference: Tanzil Project, tanzil.net.
- Translation: Saheeh International, issued by Noor International Center,
  distributed by QuranEnc.com, version 1.1.2.
- Tafsir: Tafsir Ibn Kathir (English) and Tafsir As-Sa'di (Arabic), via
  QUL.
- Recitation: Sheikh Muhammad Siddiq Al-Minshawi and Sheikh Mahmoud
  Khalil Al-Husary, via QUL.
- Fonts: KFGQPC (used unmodified, with notice) and the OFL fonts with
  their license files.
