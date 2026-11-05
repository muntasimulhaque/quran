# Quran: The Noble Book

A free, open source Quran reader for Android. It opens where you left off
and keeps the Quran text itself at the center: no ads, no
trackers, no accounts, nothing collected, ever.

**Status:** 15.8 (versionCode 160) is ready for Play review from commit
`b9ddbfe9`. The signed bundle `quran-15.8-vc160.aab` has SHA-256
`8dd5c52bcc0e3bc882d18fffe18efce73fd4b040010b9cb2b25ca27ac0a2332d`, signed by the
upload key `53:7D:09:D2:03:00:12:9E:97:3B:79:45:31:6B:FE:24:CF:AD:CF:BC:77:EE:C5:22:9C:BF:30:17:0D:9D:E5:21`.
Build run `41982572110` is green and the store capture is run `41982572303`.
Content 1.5.0, the database `content-db-12c95dbe`. The Quran text, both
translations, and both tafsirs are unchanged; the recitation is the original
MP3 set under the `recitation-minshawi` and `recitation-husary` tags. The app
now carries Al-Fatiha in both voices and both reciter timing packs.

**In 15.8.** Al-Fatiha's seven ayahs for both reciters now ship inside the app, so the first Play on 1:1 works offline with no download offer. The two reciter timing packs ship too; every other surah still downloads its audio on the reader's word, with the word timings already in the app. The Quran text, both translations, both tafsirs, and the content database are unchanged.

**In 15.7.** The recitation is the original 128 kbps stereo MP3 set again, the same Al-Minshawi and Al-Husary recordings and word timings the app shipped before 15.6. The 64 kbps Ogg Opus encode is withdrawn from the app; its packages remain published for 15.6. Each surah is about twice the size, and a surah downloaded in 15.6 downloads again. The Quran text, both translations, and both tafsirs are unchanged.

**In 15.6.** The recitation was re-encoded in Ogg Opus at 64 kbps mono, about half the size, with the same word timings. 15.7 restores the original MP3 set.

**In 15.5.** The app opens in the phone's language, with both translations already on the device, and no language screen. The reminder is offered after the first surah, not before the text. A quiet folio line keeps the surah and juz on the page; the full chrome is on tap, with no timer. Aa opens the type page, the reading has two sizes, a tap on a word shows its meaning, and a tafsir you do not have is offered where you ask for it. Search shows the verse for a reference result, the ayah menu is paper, and the audio offer has a clear Not now. Tajweed colors and word meanings start off.


**In 15.4.** The Font size sample is a type specimen again: the ayah and its translation, without the translation's name, and with the translation shown even when the reading's Show translation switch is off, because the page is there to judge the size of the type. The reading still names each translation column when the rule calls for it. The Quran text, every translation, every tafsir, every pack, and the content database are unchanged.

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

## Content notes

Saheeh International's note 403 is missing from the source of 7:143: both
QuranEnc and the QUL copy carry markers 401 and 402 and no 403. The app
draws 403 as a plain superscript that opens nothing, and no note was
invented for it. A verified note from a Saheeh source would travel through
`tools/` like every other content change.
