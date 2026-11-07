# Quran: The Noble Book

A free, open source Quran reader for Android. It opens where you left off
and keeps the Quran text itself at the center: no ads, no
trackers, no accounts, nothing collected, ever.

**Status:** 16.7 (versionCode 169) stands ready for Play review from commit `8b9304cd`. The signed bundle `quran-16.7-vc169.aab` has SHA-256 `699c1475a8765d5f7ddc002b804f879d5a45784da6f3cf2fb8e2e969be26a51a`, signed by the upload key `53:7D:09:D2:03:00:12:9E:97:3B:79:45:31:6B:FE:24:CF:AD:CF:BC:77:EE:C5:22:9C:BF:30:17:0D:9D:E5:21`. Build run `42400302041` is green: the gates, the debug build, the data instrumented tests, the app instrumented tests on all three form factors, and the signed bundle. Capture run `42400302006` is green on all three legs, eleven frames each (twelve on the ten inch screen), and every one of the eight was read. Content 1.5.0, the database `content-db-12c95dbe`, and the recitation packs are unchanged. The Quran text, both translations, both tafsirs, and the audio are unchanged.

**In 16.7.** The page belongs to the reader again. A tap on an ayah no longer selects it: the verse's number is the one door to its actions, and the menu it opens closes on any tap outside without choosing another verse. In the mushaf, the printed medallion is that door. The five-action capsule inside the verse is gone, and the number stands at the head again in the app's blue. The Quran text, both translations, both tafsirs, and the content database are unchanged.

**In 16.5.** The recitation pill's doors say their state. The chevron on the playing pill, on the download offer, and on the voice list of a pill with no recitation turns up while its popup is open, and the line speaks Expanded or Collapsed to a screen reader, the words the ayah's door and every disclosure row already use. The pill's three doors are drawn by the app's one chevron now, at their compact 14 dp measure, so their turn is the same rule every other door follows. The Quran text, both translations, both tafsirs, and the content database are unchanged.

**In 16.4.** The interface is one hand. The ayah's number is the app's blue, not a second gold action. The theme swatches keep a visible edge, every text button, chip, and AM/PM pair is one 36 dp pill, and a button that cannot answer has a real disabled state. Browse and Search share one chip and one disclosure row: a search result no longer carries a down chevron that opened a page, and the tafsir source line is the row name's 15. At 200 percent text, Browse chips keep their gap, the size rails keep their room, and ayah numbers grow instead of clipping. The snackbar's action is a pill and the remove-saved Keep is the solid answer. The Quran text, both translations, both tafsirs, and the content database are unchanged.

**In 16.3.** The reciter's word is drawn from the file's own voice, not from the export's marks alone. The build listens to every shipped ayah file and records its first and last voiced millisecond; the highlight waits for the first sound and clears at the last, instead of starting at 0:00 and holding through the closing pause. The reading sleeps to each word's own boundary, in the reader's pace, so it keeps up at 0.5x, 1x, and 1.5x, and the surah's word regions are kept from the playlist build, so the wash no longer blanks at an ayah turn. Where the export leaves a gap of more than six seconds, the wash rests instead of lying. The Quran text, both translations, both tafsirs, and the audio are unchanged; the content database is unchanged, and the recitation timing packs are unchanged, with the new edge table shipped beside them.

**In 16.2.** The recitation offer is a two-row capsule again: its line is a 48 dp door that opens the voice list for the surah at hand, with the chevron back, and Not now and Download stay centered under it. The playing pill is one line of words over the transport: the reciter and its pace, then the surah and its ayah. The word meaning sheet's head takes the page's left edge with the footnote's, its word is the Quran's own size and its gloss the translation's, and the word aid's own size is gone with its role. The Quran text, both translations, both tafsirs, and the content database are unchanged.

**In 16.1.** The recitation offer is a compact centered capsule: the reciter, the surah, and the size on one line, with Not now and Download centered under it. The reading runs under the status bar again, with no paper band under the clock. The footnote and word meaning sheets name themselves with the app's one sheet title and put the place under it at the meta size, and the Reciters page lists the bundled Al-Fatiha under both voices as a surah on the device. Send feedback opens the reader's mail app on the maker's inbox, with the project repository as the fallback. The Quran text, both translations, both tafsirs, and the content database are unchanged.

**In 16.0.** The page belongs to the text. The quiet folio line at the foot is gone, and with it the duplicate of the surah and juz that the top bar already carries. Aa leaves the bar, so the bar is Browse, the surah and juz, Search, and Settings, and the juz fits again on a phone; the text size lives in Settings with its live sample. The bar leaves when the reader's own drag begins and returns on a tap, with no timer. The recitation pill and the pack card are the only things at the foot, composed only while raised, and the reading ends at its own margin. The Quran text, both translations, both tafsirs, and the content database are unchanged.

**In 15.9.** Pressing and holding a word opens its meaning on its own sheet: the word at the reader's word-meaning size, its meaning under it, and the ayah named above, all on one center line. The reciter's word is a rounded wash again, not a recolored word, so the letters keep their own ink and their tajweed colors while the voice is on them. The wash is set at the strongest tint the seven rule colors can still be read on, and the ayah-level recitation wash is gone because the two together could not. The Quran text, both translations, both tafsirs, and the content database are unchanged.

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
