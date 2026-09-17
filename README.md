# Quran: The Noble Book

A free, open-source, fully offline Quran reader for Android.

Read the Mushaf page by page, or study ayah by ayah with the Saheeh
International translation, word-by-word meanings, Tafsir Ibn Kathir, and
Tafsir As-Sa'di, with recitation by Sheikh Muhammad Siddiq Al-Minshawi
and Sheikh Mahmoud Khalil Al-Husary.

No ads, no trackers, no accounts, and no internet access, ever. The app
does not declare the `INTERNET` permission, so it is incapable of opening
a connection.

On Google Play as **Quran: The Noble Book** (coming soon).

## Status

Foundation and content pipeline. The working rules are in [AGENTS.md](AGENTS.md),
the settled choices and their reasons in [docs/decisions.md](docs/decisions.md),
and the exact content provenance in
[docs/content-sources.md](docs/content-sources.md).

All 26 source datasets pass checksum and structural verification, and the
text audit passes: 6235 of 6236 ayahs agree letter for letter with the
independent Tanzil edition, with one documented orthographic variant and
zero unexplained differences ([report](content/audit-report.md)). The
Mushaf rendering prototype passed as well: QPC V2 glyph text renders
page-for-page with the printed Mushaf, and D-011 records the resulting
architecture.

The content database is now built and reproducible: 6,236 ayahs, 77,432
words, 6,236 translations, both tafsirs with group resolution, four
recitations with segments, and search columns, at 43,933,696 bytes with
sha256 `00f9afa5f844e77138fd13a1929dc8e5300331f3b2c1f64e4d5af44a0e347d57`,
byte-identical across rebuilds. It ships in the repository at
`content/quran.db`, and `tools checkdb` verifies it without any raw
sources. Font coverage is proven for every study text codepoint and every
Mushaf page glyph. The 604 page fonts are published once as a GitHub
Release asset; `tools fetch` downloads them on a new machine and checks
the pinned SHA-256. No app code yet.

## The content

The Arabic text is the KFGQPC Hafs text used with the King Fahd Glorious
Quran Printing Complex fonts, audited word by word against the Tanzil
Project text. The translation is Saheeh International, issued by Noor
International Center and distributed by QuranEnc.com, version 1.1.2.
Scripts, layouts, tafsirs, metadata, and recitations come from the
Quranic Universal Library (QUL) by Tarteel. Every dataset carries its
source, version, and credit, and every license is honored.

## License

The code is MIT: see [LICENSE](LICENSE). The Quran text,
translation, tafsir, fonts, and recitations are not ours to license and
keep their own terms and credits, recorded in
[docs/content-sources.md](docs/content-sources.md).
