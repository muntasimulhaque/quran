# Store kit

Version: 3.9 (versionCode 40)

## Listing

Title (21 characters):
Quran: The Noble Book

Short description (under 80 characters):
The Quran, offline: Mushaf pages, translation, tafsir, search, recitation.

Full description (plain prose; Play strips markdown):

A Quran app built to be read. It opens where you left off, turns pages the
way a printed Mushaf does, and never asks you to learn its interface first.

Mushaf mode renders the 604 pages of the Madinah Mushaf exactly as the
King Fahd Complex typeset them, with the QPC V2 page fonts, so every page
matches the printed copy line for line.

Study mode gives each ayah its Saheeh International translation with the
original footnotes, word by word meanings, and two tafsirs: Ibn Kathir in
English and As-Sa'di in Arabic, with the Quran quotations set apart. Bangla
readers get the Taisirul Quran translation, Ibn Kathir in Bangla, and Bangla
word meanings, and the whole interface can be read in English or Bangla.

Search reads Arabic without diacritics and English without accents, so
typing allah finds Allah and isa finds Isa. Save any ayah, add your own
note to it, and find everything again under Browse: one list of the ayahs
you kept, with your note under the ayah it was written on, and a Last read
list that keeps the places you have been reading so you can return to one
you left.

The two readings are one door at the top of the page, and it always
offers the other one: the printed page, or the study view. There is no
bottom bar to learn. More than one translation may be on at once, and
each one draws in its own place under the ayah.

Recitation plays Minshawi or Husary, with the page following the reciter
and the word being recited marked. Audio is not bundled and not streamed:
when you tap Play on a surah, the app tells you its size and downloads
that one surah from the project's own release page. Surahs you have not
asked for are never downloaded. Everything you do download works offline
forever after.

There are no ads, no trackers, no analytics, and no account. The app is
free and its source code is public. It reaches the project's own release
page only for content: a pack or a surah's recitation you ask for, or a
quiet refresh of a pack you already have when a newer version ships, on
Wi-Fi only.

Credits: Quran text by the King Fahd Complex for the Printing of the Holy
Quran, audited against the Tanzil Uthmani reference. Translation by
Saheeh International via QuranEnc. Tafsir Ibn Kathir and the recitations
via the Quranic Universal Library by Tarteel. Tafsir As-Sa'di via
QuranEnc. Full credits and licenses are in the app and in the repository.

## Classification

- Category: Books & Reference
- Tags: Quran, Islam, Mushaf, Tafsir, Recitation
- Content rating: Everyone
- Contains ads: No
- In-app purchases: No
- Free: Yes, no account, no sign-in

## Data safety

- Data collected: none.
- Data shared: none.
- Data security: no account, no identifiers, nothing stored off the
  device.
- The app declares `INTERNET` for content from the project's own GitHub
  Releases: a translation, tafsir, word list, or recitation pack the
  reader asks for after seeing its size, and a quiet refresh of an
  installed pack when the app carries a newer version, on an unmetered
  connection only. Nothing is sent beyond the request. No analytics, no
  crash reporting, no advertising.
- Privacy policy URL:
  https://muntasimulhaque.github.io/quran/privacy.html

## Graphics

The icon and the feature graphic are made; the screenshots are captured by the
screenshot test on a real Android system, one folder per form factor Play asks
for.

- App icon: `icon-512.png`
- Feature graphic: `feature-graphic-1024x500.png`
- Phone screenshots: `screenshots/phone/` (1080 x 1920), eight of them
- 7 inch tablet screenshots: `screenshots/tablet7/` (800 x 1280), eight of them
- 10 inch tablet screenshots: `screenshots/tablet10/` (2560 x 1800), eight of them

The set is refreshed for 3.9 from capture run 37183334879: all three legs green on the first attempt, eight frames each, every frame compared with the 3.8 set by `cmp` and every changed frame read. The three settings frames carry 3.9 own answers: the headings read Interface, Reading, Recitation, Reminder, with the group names and the explanation lines as the change; twelve frames are byte-identical to the 3.8 set, and the other nine differ only in the status bar clock, the search caret, and subpixel rasterization of the dimmed page on the 10 inch ayah card. The set is refreshed for 3.6 from capture run 36982589370: the phone and 7 inch legs green on the first attempt, the 10 inch leg green on its second, after SavedNotesTest failed inside Compose own prefetch scheduler on a worker with no looper, a failure that leg has already shown once on the 3.5 line (run 36911766796), and the tour itself was green on all three legs with eight frames each. Every frame was compared with the 3.5 set by `cmp` (eighteen of the twenty four changed) and every changed frame was read. The changed frames carry 3.6 own reading: the word by word aid is a reading and not a control, so a pair is as wide as its own meaning and a row fills properly, where the 3.5 set left a short pair alone at the end of each row on the 48 dp touch target that went with the tap. The other changed frames are the status bar clock, and a subpixel shift of the whole text block on the surah opening frame. The three mushaf frames are byte-identical to the 3.5 set, which is the point: the reading did not move. The icon and the feature graphic are the qaf mark: the letter that begins the word Quran, drawn the way a reed pen draws it, the loop thinning where the nib runs flat and the tail tapering to a flat pen cut, in warm ivory on a deep ink field with a quiet light behind it, the two dots in the gold of an illuminated initial. The store icon, the adaptive layers, and the monochrome silhouette are one drawing; the feature graphic is the mark centered on the same field, 1024 x 500, 24-bit with no alpha. The set is refreshed for 3.5 from capture run 36958563721: the phone and 10 inch legs green on the first attempt, the 7 inch leg green on its third attempt after two environment-shaped losses (a content wait and a sheet freeze on a loaded runner, the same binary had passed the whole tour hours earlier), eight frames each, every frame compared with the previous set by `cmp` (fifteen of the twenty four changed) and every changed frame read. The changed frames carry the reading voice of 3.5: the English translation is set in its own optical cut, the word by word aid and the ayah card read as they do, the search frame is still a real answer (492 matches, the word washed), and the settings frame still speaks one grammar. The three mushaf frames are byte-identical to the 3.4 set, which is the point: the reading did not move. The set was refreshed for 3.3 from capture run 36758466353: all three legs green on the first attempt, eight frames each, twenty four frames compared with their artifacts by `cmp` (all twenty four changed) and every one of them read. This is the first complete set since 3.0, and it carries three defects that were in the store and in the app. The **page is whole**: it was drawn stretched to the glass, so on the 10 inch leg the frame showed the top two fifths of a page with two thirds of it below the screen, and every ayah on the lower lines could not be touched at all. The **footnote markers are on their line**: they were lifted three whole ems above the text they belong to, and 2:255, which the card frame photographs, carries six of them. The **search frame is a real answer**: the tour waited for any text holding "match", which the sheet's own "No matches." satisfies before a search has run, so the frame in the 3.3 capture was a sheet that had found nothing on a query with 492 matches. The settings frames carry 3.3's own answers: the font size on one line, every chevron on the margin a switch ends at, and Version 3.3 in About. The word by word aid is a row of pairs again. The set was refreshed for 2.8 from run 36448152056: the tablet10 leg's first attempt met the launcher ANR over the keyboard-heavy search frame, a known capture-environment failure, and the guard refused the frame; one rerun, and attempt 2 is green on all three legs. Every frame was compared with its artifact by `cmp` (24 matches) and every changed frame was read. The settings frames are the release's: the switch is now the last mark on every row, all switches on one line and all chevrons on another, and the About row reads Version 2.8. The other changed frames are the status bar clock, the tablet10 search frame's soft keyboard chrome (the class the 2.7 set already recorded), and subpixel antialiasing, a maximum delta of 2 of 255 on the tablet10 Mushaf frame.

The set was refreshed after the craftsmanship pass and again
for 1.9, all three legs green first try once the workflow's own
script bug was fixed, for all three form factors, in the eight
frames the store lists. Every frame was
compared with its artifact by `cmp` (24 matches) and read for its content
before it replaced the committed set. It carries the pass's changes: the
study reading holds a readable measure on the wide screens, the surah
opening is built to the printed page's model with one About button, the
Browse numbers and the search hint are legible at full strength, a floating
control wears a floating tone, and settings shows the Listening row.
The remaining frame differences
between runs are the status bar's own clock, the text cursor's blink in the
search frame, and a few pixels of subpixel antialiasing.

The set is refreshed for 2.1 from the release push: Settings gained the Show
translation, Show tafsir, and Show word meanings switches, and the search
results name each word meaning under the translation. The set stays at eight
frames per form factor, and no frame was added or removed. Every frame was
compared with its artifact by `cmp` (24 matches) and every changed frame was
read before it shipped: the settings frames carry the three switches and
Version 2.1, the tablet search frames show the new word meaning block, and
the Browse and ayah-card frames differ from 2.0 only in the status bar
clock, which the numeric compare confirms.

The set is refreshed for 2.2 from run 36164556475 (all three legs green after
one rerun of the phone leg, whose first attempt met a launcher ANR over the
search frame and kept only four frames; the guard refused to ship the
dialog). Every frame was compared with its artifact by `cmp` (24 matches)
and every changed frame was read before it shipped. The settings frame is
the release's: Show translation and Show tafsir now carry the chevron that
opens their own list with the chosen pack's name under them, the separate
Translation and Tafsir rows are gone, and the Daily ayah switch is new,
off by default. The tablet search frames show the translation highlight
standing alone, with no repeated word meaning block under it. The phone
Mushaf, Browse, and ayah-card frames differ only in the status bar clock
and subpixel antialiasing, which the numeric compare confirms (a maximum
delta of 4 of 255 on the Mushaf page).

The set is refreshed for 2.4 from run 36299617648 attempt 2: the phone
leg's first attempt timed out waiting for the study reading to draw in
`SavedNotesTest` (the known loaded-emulator failure class; the tour and
the other seven tests on that leg were green), the two tablet legs were
green first try, and the phone leg passed on the rerun. Every frame was
compared with its artifact by `cmp` (24 matches) and every changed frame was
read before it shipped. The Browse frames lose the Notes and Go to Ayah
chips and show four tabs; the settings frames carry the chevron after the
switch on Show translation, Show tafsir, and Daily ayah, and the About row
reads Version 2.4. The Mushaf, chrome, study, and surah opening frames are
byte-identical to 2.3, and the search and ayah-card frames differ only in
the status bar clock and subpixel antialiasing.

The set is refreshed for 2.5 from run 36306854024 attempt 2: the tablet10
leg's first attempt met a launcher ANR over the search frame, which the
window guard refused to ship, and the phone leg's first attempt timed out
in `SettingsVisibilityTest` waiting for the study reading to draw (the
loaded-emulator class; all eight frames were kept), while the tablet7 leg
was green first try; both failed legs passed on the one rerun. Every frame
was compared with its artifact by `cmp` (24 matches) and every changed frame
was read before it shipped. The settings frames are the release's: every
switch now ends at one line and every chevron sits at one place, whatever a
row carries, and the About row reads Version 2.5. The Mushaf, chrome,
study, and surah opening frames are byte-identical to 2.4, and the search,
Browse, and ayah-card frames differ only in the status bar clock, the search
cursor's blink, and subpixel antialiasing (the largest content delta is 4 of
255 on the phone Mushaf page).

The set is refreshed for 2.6 from run 36341047052 attempt 1, all three legs
green first try, no reruns. Every frame was compared with its artifact by
`cmp` (24 matches) and every changed frame was read before it shipped. The
settings frames are the release's: the plain rows are compact again, the
switches and chevrons still run down one column, and the About row reads
Version 2.6. The phone and tablet7 search, Browse, and ayah-card frames
differ only in the status bar clock and the search cursor's blink; on
tablet10 the Mushaf differs only in subpixel antialiasing (a maximum delta
of 2 of 255) and the chrome and study frames in three pixels. The surah
opening frame is byte-identical on all three.

The set is refreshed for 2.7 from run 36395366904 attempt 1, all three legs
green first try, no reruns. Every frame was compared with its artifact by
`cmp` (24 matches) and every changed frame was read before it shipped. The
settings frames are the release's: the About row reads Version 2.7 on both
tablet form factors (the phone's About row sits below the sheet's visible
area, so its frame differs only in the status bar clock). The tablet10
search frame differs only in the soft keyboard's chrome below the results,
which are unchanged; the Mushaf frames differ only in subpixel antialiasing
(a maximum delta of 4 of 255 on the phone), and the rest only in the status
bar clock and the search cursor's blink. No frame was added or removed; the
set stays at eight per form factor.

The workflow runs the capture test on three emulator profiles (phone, 7 inch,
10 inch), caches the AVD per profile so only the first run of each pays for
creating the emulator, waits for the emulated storage to mount before the test
starts, and uploads each set as its own artifact, so the store images always
match the shipped build. Run it from the Actions tab, or let it run when the UI
changes.

What each set shows, in order:

1. The Mushaf page
2. The chrome: the mode door, Browse, Search, and Settings
3. The study reading with its translation
4. The surah opening
5. Search with the matched word marked, and the filters under the field
6. The settings hub
7. Browse, Surahs
8. The ayah card, with its tafsir doors

## Release notes (3.9, 312 characters)

The daily ayah now arrives at the minute you set, even with your phone locked and asleep, and a restart no longer costs the morning. Settings is quieter: the explanatory lines under the groups, the options, and the rows are gone, and every row keeps its name and where it stands. No ads, no trackers, no account.

## Release notes (3.8, 378 characters)

Each day now brings a different ayah: the reminder deals from a shuffled deck rather than walking the Book in order, and every ayah still comes once before it starts over. The reminder asks for its notification on the first screen, so the ayah of the day can arrive from your first morning, and a morning your phone held on to is no longer lost. No ads, no trackers, no account.

## Release notes (3.7, 491 characters)

A note is a note and a save is a save: writing a note on an ayah now lights the note and leaves the bookmark alone, and the bookmark is lit only where you pressed Save. The two popups on the playing pill, the reciters and the listening answers, open in the middle of the pill instead of at its ends. Settings is regrouped: language, theme, and font size sit together under the interface, and the reading, the recitation, and the reminder each keep their own. No ads, no trackers, no account.

## Release notes (3.6, 485 characters)

The reciter can be changed from the playing pill: the name beside the place opens every reciter with what it would still cost for the surah, and a surah that is not on the device is asked for by size as before. The pill no longer carries the end of the audio on its face, because the switch already says which one you chose. Word by word is a reading again: the tap that looped a single word is gone, and every pair is drawn as wide as its own meaning. No ads, no trackers, no account.

## Release notes (3.5, 480 characters)

Bangla now has its own type: the interface speaks Noto Sans Bengali and the reading speaks Noto Serif Bengali, both bundled with the app, with line spacing measured for Bengali script so nothing clips. The English reading face now draws every size in the cut its designer made for that size. About shows the full credit and license of the Saheeh International translation. And on Android 13 to 15 the back gesture now previews the sheet it closes. No ads, no trackers, no account.

## Release notes (3.4, 388 characters)

The mushaf page breathes: the printed rule now stands off the text on all four sides, so the page reads as a page. Settings finally speak one language: every row wears its value under its own name, and every row that opens something ends in the same arrow, on the About page too. Search, word by word, tafsir, recitation, and the daily ayah are unchanged. No ads, no trackers, no account.

## Release notes (3.3, 484 characters)

Search is instant now: one word finds its ayahs, translations, word meanings, and tafsir in a single pass, even a common word like mercy. Word by word reads as a row of word and meaning pairs again. The mushaf page keeps its rule clear of the text, the playing pill centres the reciter, the place, and the controls, and settings rows keep their value on one line with every arrow on one line. The daily reminder asks for one permission instead of two. No ads, no trackers, no account.

## Release notes (3.2, 295 characters)

The app has a new face. The icon on the home screen, the themed icon, the mark in the notification shade, and the mark at the foot of a shared ayah are now the letter that begins the word Quran, drawn with a reed pen's own contrast in ivory and gold on deep ink. No ads, no trackers, no account.

## Release notes (3.1)

Word by word reads as a grid now: the words stand in columns, each over its
own meaning. The ayah card opens with its ayah, the mushaf page carries a rule
so a page has an edge, and the bar no longer dims the surah's name on it. Tap
any word in the study reading to hear it on repeat, and the app starts faster
with a startup profile built in. Settings and the reader's own surah are
headings a screen reader can jump by, and a sheet is a page, not a floating
card. No ads, no trackers, no account.

## Release notes (3.0, 464 characters)

Repeat the surah: the whole surah begins again when it ends, from the pill while a recitation plays and from Settings under Repeat the ayah. What happens at the end of the audio is one choice, so repeat the ayah, repeat the surah, and continue never fight each other. The pill's words keep a full line on a phone, so the surah, its ayah, the pace, and the repeat are no longer cut short. Settings rows end with the mark they carry. No ads, no trackers, no account.

## Release notes (2.9, 458 characters)

The daily ayah now arrives at the minute you chose, even on a locked or sleeping phone, if you let the app keep that exact time on the Daily ayah page. The app now follows your phone's own light and dark mode the moment it changes, with the follow system switch on. Settings reads as a list: names no longer squeeze, rows say what stands rather than explain themselves, and every text size and playback step is easier to tap. No ads, no trackers, no account.

## Release notes (2.8, 328 characters)

Settings is tidier: on every row that carries a switch and a door, the switch is now the last mark, so all switches run down one line and all chevrons another. The app has a new face too: the icon and the store art are redrawn as the shamsa at the center of a mushaf cover, in gold on deep navy. No ads, no trackers, no account.

## Release notes (2.7, 354 characters)

Search now offers a matched surah name before a typed reference. The daily reminder centers its Arabic, and a translation or tafsir you already have updates quietly on Wi-Fi when the app carries a newer version, so Check installed content can repair what it finds rather than only reporting it. Bangla wording is cleaner. No ads, no trackers, no account.

## Release notes (2.6, 487 characters)

Settings reads tighter: the list rows keep their compact height, and the switch for translation, tafsir, or the daily reminder lives on its hub row alone, so the page that lists them no longer repeats it. Under Repeat the ayah, Continue to the next surah plays the surah after the one you are hearing on its own, downloading it when it is not on the device. It is off by default. The English Ibn Kathir tafsir carries QUL's correction of a typo at 2:238. No ads, no trackers, no account.

## Release notes (2.5, 388 characters)

When you share an ayah as text, the words now read left to right, with the English translation on the left and its period where it belongs, while the Arabic keeps its own right-to-left reading inside its line. The ayah grid that opens from a surah names it in the same type as the Browse list, and every Settings switch and chevron now sits in one column. No ads, no trackers, no account.

## Release notes (2.4, 398 characters)

Browse is simpler: tapping a surah now opens its ayah numbers, with the place you left in that surah already marked and in view, so there is no separate Go to Ayah. Saved and Notes are one list: every ayah you keep, with the note you wrote on it under it, and removing a saved ayah that carries a note asks first. In Settings, the chevron now sits after the switch. No ads, no trackers, no account.

## Release notes (2.3, 444 characters)

A switch now does what it says: the daily reminder, Show translation, and Show tafsir each toggle from their own switch, and the rest of the row opens the list it belongs to. The daily reminder comes on with the app at a time you pick on the clock, word meanings sit above the translation, Go to Ayah shows the surah name in Arabic with its length, and search no longer prints a Quran line that matched nothing. No ads, no trackers, no account.

## Release notes (2.2, 462 characters)

A new daily reminder brings one ayah to your notifications at the hour you choose, with its translation when you read with one, and a tap opens it in the study reading. Go to Ayah now centers your own ayah so it is easy to find, a search row no longer repeats a translation match as a separate word meaning, Show translation and Show tafsir each open their own list from the same row, and a long ayah share card is no longer cut. No ads, no trackers, no account.

## Release notes (2.1, 425 characters)

Arabic passages in tafsir now wrap from the right, so a quoted verse reads in order on every line. Notes in Browse preview what you wrote, so you can find one without opening them all. Settings gains Show translation and Show tafsir switches, and the shared ayah card no longer prints the translation's name. Go to Ayah is a normal Browse tab now, and search results name their word meanings. No ads, no trackers, no account.

## Release notes (2.0, 482 characters)

Share now shows the ayah card before you send it, with one button for the picture and one for the words, so the text never rides along with the image. The playback pill keeps its distance from the screen edge, and the pace and repeat are one tap away while an ayah plays. Tafsir paragraphs with a quoted Arabic line no longer stretch every line of the paragraph apart, search results run from the verse outward, and Go to Ayah opens as its own page. No ads, no trackers, no account.

## Release notes (1.9, 363 characters)

Listening now has its own page: choose the pace of the recitation from 0.5x to 1.5x, or repeat one ayah until you stop it, and both are remembered. On a wide screen the study reading keeps a comfortable line length instead of stretching across the glass. Quiet labels and the gold page ornaments are easier to read in every theme. No ads, no trackers, no account.

## Release notes (1.8, 425 characters)

Browse now puts Go to ayah beside the lists, one tap from any tab, opening on the surah and ayah you are in. Word by word reads as a gloss of the ayah above it, quieter and tighter, instead of looking like a second verse. In the study reading the actions bar ends with Tafsir and its own mark; from the Mushaf it stays More. Ayah references and quiet labels are easier to read in every theme. No ads, no trackers, no account.

## Release notes (1.7, 320 characters)

Search now reads references by name too, like baqara 255 beside 2:255. Browse gained Go to ayah: pick a surah, tap a number, with your own place already chosen. Saved rows show when they were saved. The word meanings are named the same everywhere, and their door names only its language. No ads, no trackers, no account.

## Release notes (1.6, 438 characters)

Saving and noting are now separate: a note no longer appears under Saved, and Remove sits beside every note in Browse. Surah numbers in Browse are whole again (100 was showing as 10) and read in the digits of your language. The Saved list shows just the surah and ayah, tapping a note highlights its ayah, About this surah wears the same highlight as a long-pressed ayah, and Forget reads মুছুন in Bangla. No ads, no trackers, no account.

## Release notes (1.5, 338 characters)

About this surah keeps its headings now: Name, Period of Revelation, and Theme each stand on their own line instead of running into the paragraph they head. Opened from the Mushaf, the ayah card now reads word by word first, then the translation, then the tafsir, in the order the study view already uses. No ads, no trackers, no account.

## Release notes (1.4, 432 characters)

Share now sends a picture of the ayah with its translation, its reference, and the app's mark, with the plain text along as the caption. Browse's surah and juz numbers line up so every name starts in the same place. The ayah card names its word by word section, scrolling back up no longer closes a sheet, the ayah actions read Play, Note, Save, Share, More, and the note sheet opens as Take a note. No ads, no trackers, no account.

## Release notes (1.3, 410 characters)

Changing the language now keeps you on the Language page, in the language you just chose. The ayah card reads under Translation and Tafsir names instead of lines. About this surah no longer cuts off when opened. Words that act, like Save and Remove, now look like buttons. Browse has a Notes tab listing every ayah you wrote a note on, and font sizes now run 0.65 through 1.2. No ads, no trackers, no account.

## Release notes (1.2, 397 characters)

Long-press an ayah and the pill now has a Note action, and the phone's back button closes it instead of the app. More shows only what the reading behind it does not: from the Mushaf the translation and word by word, from study the tafsir. Settings lists read alphabetically, and word meanings sits at the top of Translations. Fixed a crash when switching language. No ads, no trackers, no account.

## Release notes (1.1, submitted, 387 characters)

Fixed a crash when turning Mushaf pages quickly. The reciter chooser now matches the pill it opens from, and scrolling back to the top of search results no longer closes the sheet. Settings are tidier: word by word sits under Translations, following the reciter sits with the Reciters, and keeping the screen awake is in the hub. Bangla wording improved. No ads, no trackers, no account.

## Release notes (1.0, submitted, 439 characters)

Choose your language on the first screen: English or Bangla, and the app, the translation, the tafsir, and the word meanings all follow it. The top bar is one row again, with a single icon that takes you between the Mushaf and the study view. Word meanings are one switch in Settings, which fetches the word list your translation speaks. The reciter chooser now matches the pill in color and rounded shape. No ads, no trackers, no account.

## Release notes (0.10, submitted, 365 characters)

The top bar is two rows now: the reading modes stay at the center of the screen, and the surah and Juz sit centered beneath them, whole on every phone. The word being recited in the study view is marked with the Mushaf's rounded wash, About this surah answers in the same shape, and the end of a surah offers the next one as a card. No ads, no trackers, no account.

## Release notes (0.9, submitted, 496 characters)

The two reading modes are one switch at the center of the top bar, and the Play offer lets you pick the reciter, with the size shown. The Reciters page no longer downloads or explains the word timings; they come with the first surah you play, and removing a reciter selects another. The font size page previews a short ayah, Arabic inside a tafsir reads larger than the prose, the ayah card drops its repeated Save and Share, and downloaded surah rows sit closer. No ads, no trackers, no account.

## Release notes (0.8, submitted, 494 characters)

Pages now turn the way a printed Mushaf turns: the next page lies to the left, so a swipe to the right goes forward. The ayah actions bar floats as a rounded pill. The top bar reads a little smaller, and search filters show a check when they are on. Downloaded surahs sit closer to their reciter under one label with a turning arrow. The phone back button returns from a settings page to Settings, and scrolling back up in Browse does not pull the sheet closed. No ads, no trackers, no account.

## Release notes (0.7, submitted, 487 characters)

Every Mushaf page now reads right to left, as the printed page does. The Mushaf and study icons were redrawn, Browse lost its title, and a surah opened from Browse lands on your last place in it or at its top. Footnotes follow the size of the translation, About this surah closes with a tap anywhere, Last Read rows lost their Open button, reciters gained a clearer downloaded-surahs list, and a scroll back up in Browse no longer pulls the sheet closed. No ads, no trackers, no account.

## Release notes (0.6, submitted, 377 characters)

Footnotes in the ayah card open from their marker now, like the study reading. The word by word aid is larger, its Arabic centered over each meaning. Search filters wrap so none are hidden, settings have more room, and reciter rows say what the download is. A download request leaves with its surah, and a finished surah no longer stays marked. No ads, no trackers, no account.

## Release notes (0.5, shipped, 235 characters)

Search results no longer show raw markup where a tafsir was matched; every result now reads as plain prose, and a matched word stays highlighted. The surah introduction in study mode reads as prose too. No ads, no trackers, no account.

## Release notes (0.4, shipped, 423 characters)

Fixed a crash when adding a translation, tafsir, or word list. Tapping such a row now adds it and turns it on, and removing one downloaded surah works. The page no longer lifts with a shadow during a turn, the top bar hides while you scroll, and the Mushaf and study icons were redrawn to match. Appearance can follow your phone dark mode. Last Read, search filters, and alphabetical lists. No ads, no trackers, no account.

## Release notes (0.3, shipped, 496 characters)

The reading place no longer jumps back when you come from another surah. The mode switch is one icon in the top bar that offers the other view, and the bottom bar is gone: Saved sits in Browse beside the surahs and juz, and listening is the play action on any ayah. Browse has a Last read tab with your last twenty places. More than one translation may be on at once, text sizes gained a smaller step and lost the largest, and the download offer can be dismissed. No ads, no trackers, no account.

## Release notes (0.2, as shipped)

Opens on the exact ayah you left, in both reading modes, and paints that page before anything else loads. Settings is a hub: its own page for text sizes, reciters, translations, and tafsirs, each sized as you like. Listening asks once, names the reciter and the size, and lets you swap reciter in the offer. The modes are two drawn icons, and footnotes no longer collide at large sizes. Saved ayahs export and import, and Settings can check every pack on the device. No ads, no trackers, no account.

## Release notes (first release, as shipped)

First release. Mushaf and study modes, Saheeh International with
footnotes, word by word, Ibn Kathir and As-Sa'di, search, bookmarks and
notes, and recitations by Minshawi and Husary downloaded per surah on
request. No ads, no trackers, no account.

## Before each release

1. Raise `versionCode` by 1 and `versionName` by 0.1 in
   `app/build.gradle.kts` and update the version line at the top of this
   file, in the same commit that ends the session.
2. Run the owner-machine gates: `./gradlew :tools:run --args="verify"`,
   `audit`, `fonts`, `checkdb`, `search`, and the instrumented tests on an
   emulator.
3. Commit and push. The `build` workflow's `signed-bundle` job signs on every
   push to `main`, checks the content, and leaves the bundle in the run's own
   artifacts. Pull it down into `play-store/aab/`:

   ```bash
   gh run list --workflow=build --limit 1
   gh run download <run-id> -n quran-signed-aab -D play-store/aab
   ```

   The artifact is private and expires after two weeks, so nothing signed is
   ever public and nothing signed lingers.
4. Upload the bundle to the internal track, check the size report, then
   promote. Delete the copy from `play-store/aab/` once Play has it, so a
   stale bundle can never be uploaded twice.

The four secrets signing needs (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`) live in the repository's secrets, each value
from the upload key in the owner's vault. The workflow refuses to publish an
unsigned bundle: if the secrets are missing it fails instead of handing over
something Play would reject.

## Assets in this folder

| File | Size | Use |
|---|---|---|
| `icon-512.png` | 512 x 512 | store icon |
| `feature-graphic-1024x500.png` | 1024 x 500 | feature graphic |
| `screenshots/phone/*.png` | 1080 x 1920 | phone screenshots (8) |
| `screenshots/tablet7/*.png` | 800 x 1280 | 7 inch tablet screenshots (8) |
| `screenshots/tablet10/*.png` | 2560 x 1800 | 10 inch tablet screenshots (8) |

Screenshots: the Mushaf page, the summoned chrome, the study reading, the
surah opening, search, the settings hub, Browse, and the ayah card. The tablet
sets are produced by the same test on the taller and wider profiles.

## What the app carries, and what a reader adds

The app ships the Quran text and its page layout only: about ten megabytes,
a complete offline Mushaf that needs no network and no account. Everything
else is added by the reader, from the project's own GitHub Releases, with the
size shown before a byte moves and a SHA-256 check before it is used:

| Language | Translation | Tafsir | Word by word |
|---|---|---|---|
| English | Saheeh International (2.2 MB) | Ibn Kathir (23 MB) | 4.6 MB |
| Arabic | the Quran itself | As-Sa'di (15 MB) | |
| Bangla | Taisirul Quran (5.1 MB) | Ibn Kathir (47 MB) | 6.5 MB |

Recitations: Minshawi and Husary, one surah at a time (0.2 to 122 MB each),
plus 1.7 MB of timing data per reciter.

Content updates arrive on their own: when a pack you already have is
superseded, the app replaces it quietly on Wi-Fi, so the tafsir and
translation stay current without a tap.

## Store answers to have ready

* **Is the app free?** Yes, and open source (MIT).
* **Does it show ads?** No, and it has no analytics and no accounts.
* **Why does it need the internet permission?** To download a content pack
  or a surah's recitation the reader asks for, from the project's own
  releases, and to quietly refresh an installed content pack when the app
  carries a newer version, on Wi-Fi only. No other host is ever contacted.
* **Data safety**: no data collected, no data shared, nothing stored off the
  device. Notes and bookmarks stay in the app's private storage.
