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

Foundation, content pipeline, and the first working app skeleton.

All 26 source datasets pass checksum and structural verification, the text
audit passes with 6235 of 6236 ayahs letter-identical and zero unexplained
differences, and the content database is reproducible and committed at
`content/quran.db`.

The app now runs on the reader-first skeleton:

- opens where the reader left off, in the remembered mode
- Mushaf mode renders the real QPC V2 pages, swiped page by page, with an
  LRU page cache so a turn is a texture draw
- study mode shows the page's ayahs with the Saheeh International
  translation
- an Index sheet jumps to any surah by number, name, place, and length
- the manuscript theme: paper, ink, lapis chrome, gold ornaments, Inter,
  Literata, Amiri Quran, and the KFGQPC Mushaf typefaces
- position and mode are remembered with DataStore

The app declares no permissions and no network access. The release APK is about 145 MB because the 604 page fonts and the
content database ship inside it. That makes it a complete, sideloadable
Quran: one install, offline from the first second, no Play delivery
library, and no permissions beyond the app private one from androidx
core. The smaller-listing alternative, a fast-follow pack, was measured
and rejected for its library weight and five merged permissions (D-018).

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
