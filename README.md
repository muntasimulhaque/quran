# Quran: The Noble Book

A free, open source Quran reader for Android. It opens where you left off
and keeps the Quran text itself at the center: no ads, no
trackers, no accounts, nothing collected, ever.

**Status:** 15.1 (versionCode 153) is on Play, built by CI run 41641937581 on
main from `cfca1d816626a19987077854ecd5f23c6640cb66` and taken from that run's
signed bundle `quran-15.1-vc153.aab` (12,885,566 bytes, SHA-256
`1c5908c78f1e886a153c46a436b76b1ca7ec55becf2069296758adb4a30bd38c`,
certificate `53:7D:09:D2:03:00:12:9E:97:3B:79:45:31:6B:FE:24:CF:AD:CF:BC:77:EE:C5:22:9C:BF:30:17:0D:9D:E5:21`),
green in all five jobs: 191 app tests across two shards and 55 data tests.
Content 1.5.0, `content-db-12c95dbe`.

**In 15.1.** The search sheet's recent searches and the tafsir card lose their last two rough edges. Recent searches no longer carry Clear all: every word keeps the 48 dp X it already showed, so removing a word is one visible act and a list of five is not a second, bulk question. The caption stands with its chips instead of a Section under the filter door, and the chip capsule is now the label's line plus 8 dp above and below, so it is 36 dp at the default and grows with the system font scale instead of clipping at 200 percent; the filter chips keep their row gap at that size too. On a tafsir card with more than one source, an open passage now ends on the card's block break before the next language, so the next source no longer reads as a caption to the passage above. The Quran text, every translation, every tafsir, every pack, and the content database are unchanged.

**In 15.0.** The search sheet reads as one page. A source that is on wears the accent's tint and the accent's own ink instead of a solid button, so seven sources no longer stand over the results as seven pressed shapes; the door names the one source that is off ("Everything but Translations"), opens itself when none is on, and offers "Turn everything on" while any is off. Its words stand on the page's 28 dp line, the empty note is centered as one block, and "kahf 10" no longer splits across the line that teaches it. Recent searches lead with the query and Clear all offers Undo; no matches teaches the forms the field accepts, a failed search offers its own Retry, and the field keeps its spoken name after the reader types. The Close beside the field is gone: the field takes the page's width and the sheet closes by its handle, the scrim, or Back, the way every other sheet does, and opening the source door puts the keyboard away so the chips have the room. The Quran text, every translation, every tafsir, every pack, and the content database are unchanged.

**In 14.9.** A pack's state is never a blank column. A translation or tafsir the reader never chose wears the off ring, so after Remove the row reads as an off choice rather than a row with a hole where its mark was. Add and Retry are now pills in the same shape, tone, and type as Remove, and they end on the same 22 dp rail, so one row does not change its grammar with its file; the word also stands inside the page's 28 dp line instead of 14 dp outside it. A downloaded surah's Remove joins that rail. The chosen-but-missing row stays a button whose mark is the check, because its only act is Add. The Quran text, every translation, every tafsir, every pack, and the content database are unchanged.

## Content notes

Saheeh International's note 403 is missing from the source of 7:143: both
QuranEnc and the QUL copy carry markers 401 and 402 and no 403. The app
draws 403 as a plain superscript that opens nothing, and no note was
invented for it. A verified note from a Saheeh source would travel through
`tools/` like every other content change.
