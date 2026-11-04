# Quran: The Noble Book

A free, open source Quran reader for Android. It opens where you left off
and keeps the Quran text itself at the center: no ads, no
trackers, no accounts, nothing collected, ever.

**Status:** 15.2 (versionCode 154) was submitted for Play review, and its
bundle is `quran-15.2-vc154.aab` (12,872,032 bytes, SHA-256
`4ef1974b9fce98dfaa0ba67f57af7cb0c4fc5d9421b666a0cdf9a46d7019f850`,
`jar verified`, certificate
`53:7D:09:D2:03:00:12:9E:97:3B:79:45:31:6B:FE:24:CF:AD:CF:BC:77:EE:C5:22:9C:BF:30:17:0D:9D:E5:21`),
built by CI run 41690302010 on main from
`7e6648b6` and taken from that run's `quran-signed-aab` artifact. All five
jobs were green. The store set is from capture run 41690301971: all three legs
green, eight frames each, every frame read, and no frame carries the version
line any more, because the settings hub is not one of the eight. Content 1.5.0,
the database `content-db-12c95dbe`. Owner gates: checkdb, audit, search, fonts,
and packs are green; verify reports the three raw exports that were already on
this machine, while the shipped content is unchanged. Play has the bundle;
the local hand-off copy was deleted.

**In 15.3.** Your language leads. A reader who keeps two translations on reads the one in their language first, in the reading, the daily reminder, and the share card, and a column in the other language always says which pack it is. The Font size sample shows that same lead line and its name, and follows the Show translation switch, and the Translations page lists the reader's language first. The Quran text, every translation, every tafsir, every pack, and the content database are unchanged.

**In 15.2.** Settings becomes one list. A row says where a setting stands and
opens the page that changes it, so the four reading rows are no longer half a
switch and half a door: the switch stands at the top of the page it governs,
next to the list it shows. The word list gains that page, with its size, its
Add, and its switch. The switch itself is drawn here now, 44 by 24 on the
app's own grid, so the last borrowed shape in the interface is gone and a
switch row and a door row end on one line. One failure, one sentence: a failed
download, a failed save, and a full phone say the same words wherever they
happen, and the reading and Browse share one Snackbar. The store set is
rebuilt around a reader rather than the app's own map. The Quran text, every
translation, every tafsir, every pack, and the content database are unchanged.

**In 15.1.** The search sheet's recent searches and the tafsir card lose their last two rough edges. Recent searches no longer carry Clear all: every word keeps the 48 dp X it already showed, so removing a word is one visible act and a list of five is not a second, bulk question. The caption stands with its chips instead of a Section under the filter door, and the chip capsule is now the label's line plus 8 dp above and below, so it is 36 dp at the default and grows with the system font scale instead of clipping at 200 percent; the filter chips keep their row gap at that size too. On a tafsir card with more than one source, an open passage now ends on the card's block break before the next language, so the next source no longer reads as a caption to the passage above. The Quran text, every translation, every tafsir, every pack, and the content database are unchanged.

## Content notes

Saheeh International's note 403 is missing from the source of 7:143: both
QuranEnc and the QUL copy carry markers 401 and 402 and no 403. The app
draws 403 as a plain superscript that opens nothing, and no note was
invented for it. A verified note from a Saheeh source would travel through
`tools/` like every other content change.
