# Store kit

Version: 23.4 (versionCode 236)

## Listing

Title (21 characters):
Quran: The Noble Book

Short description (under 80 characters):
The Quran, offline: translation, tafsir, search, recitation.

Full description (plain prose; Play strips markdown):

A Quran app built to be read. It opens where you left off and never asks
you to learn its interface first. The English and Bangla translations ship
with the app, so the first page is readable offline, with no account and no
waiting.

The Arabic is the QPC Hafs print, word by word, with the rules of recitation
drawn in the printed mushaf's seven color families and a Tajweed page that
explains every color in English and Bangla. The reading gives each ayah its
Saheeh International translation with the original footnotes; when word
meanings are turned on, pressing and holding any word opens its meaning in a
quiet sheet, with the ayah named above it, the word's own recording a press
away, and its root and dictionary form with every place either stands in a tap
away; and the ayah card opens Ibn Kathir
in English, with the Quran quotations set apart. Bangla readers
get the Al-Bayan Simple Translation, Ibn Kathir in Bangla, Bangla word
meanings, and the surah names in Bengali script, and the whole interface can
be read in English or Bangla.

Search reads Arabic without diacritics, in the letters a reader types
even where the print writes its small waw, small yeh, or dotless alef-maksura
base, and past the page signs the print glues to a word. English is read
without accents, so typing allah finds Allah and isa
finds Isa. Save any ayah, add your own
note to it, and find everything again under Browse: one list of the ayahs
you kept, with your note under the ayah it was written on, and a Last read
list that keeps the places you have been reading so you can return to one
you left.

Browse, Search, and Settings are one tap away in the reading's chrome: a tap
on the page brings the bar, with the surah and juz, and a scroll puts it away.
There is no bottom tab bar to learn. More than one translation may be on at
once, and each one draws in its own place under the ayah.

Recitation plays Minshawi or Husary, with the page
following the reciter and the word being recited marked. Al-Fatiha is bundled
in Husary's voice, so the first Play on 1:1 in the default recitation needs no
network and asks for nothing; in another voice it downloads on your word, like
every other surah. Nothing is streamed: when you tap Play, the app tells you
its size and downloads that one surah from the project's own release page. The
word timings for both reciters ship inside the app. If you turn on Continue
to the next surah in the player's listening menu, the following surahs download
as the recitation reaches them, each with its own name and size on the player.
Everything you download works offline forever after.

There are no ads, no trackers, no analytics, and no account. The app is
free and its source code is public. It reaches the project's own release
page only for content: a pack, a surah's recitation, or a surah's word
pronunciations you ask for, or a quiet refresh of a pack you already have
when a newer version ships, on Wi-Fi only.

Credits: Quran text by the King Fahd Complex for the Printing of the Holy
Quran, audited against the QUL QPC text, with Tanzil as the independent
cross-reference. Translation by Saheeh International via QuranEnc. Tafsir
Ibn Kathir and the recitations via the Quranic Universal Library by Tarteel, and the recitations' word timings by quran-align.
The app's About page names each source and records each dataset's licensing
status from the content manifest. The source code is MIT.

## Classification

- Category: Books & Reference
- Tags: Quran, Islam, Tafsir, Recitation
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
  Releases. A download can continue in a foreground service with a progress
  notification and Cancel; the service sends nothing but the request. It
  fetches the tafsir of the language the reader chose, on an unmetered
  connection; a word list when word by word is turned on; a word
  pronunciation pack for the surah being read, when the reader presses a
  word's speaker or quietly on an unmetered connection; a recitation
  package the reader asks for after seeing its size (or the next surah after
  Continue is turned on); and a quiet refresh of an installed pack when the
  app carries a newer version. The refresh runs on an unmetered connection
  only. Nothing is sent beyond the request. No analytics, no crash
  reporting, no advertising.
- Privacy policy URL:
  https://muntasimulhaque.github.io/quran/privacy.html

### Play Console answers

- Does your app collect or share any of the required user data types? No.
- Data types selected: none.
- Is all user data encrypted in transit? No user data is collected; every
  request the app makes is HTTPS to the project's own Releases.
- Do you provide a way for users to request deletion of their data? No user
  data is collected or stored off the device; local reading state, packs,
  and recitations are removed by uninstalling the app or removing a pack or
  a surah.
- Independent security review: No.
- Prominent disclosure: not applicable.

The declaration above is the repository's record; the Play Console itself
is submitted by the account owner.

## Graphics

The icon and the feature graphic are made; the store cards are composed from
real captures by `./gradlew :tools:run --args="storecards"`, one folder per
form factor Play asks for. Every screen inside a card is the screenshot test's
own capture of the whole display, unedited. The card around it is the store's:
the headline in the app's Literata, the sub-line in its Inter, the paper ground,
the gold rule, and the device outline drawn from the theme's own colors, in the
app's own family. The cards are English (owner decision, 22.4). This
set was captured on this session's legs, all three form factors, and composed
from those frames.

- App icon: `icon-512.png`
- Feature graphic: `feature-graphic-1024x500.png`
- Phone cards: `screenshots/phone/` (1080 x 1920), eight of them
- 7 inch tablet cards: `screenshots/tablet7/` (1200 x 1920), eight of them
- 10 inch tablet cards: `screenshots/tablet10/` (2560 x 1800), eight of them
- The frames the cards are composed from: `frames/phone/`, `frames/tablet7/`,
  and `frames/tablet10/`, the same eight each

All three sets are current: the frames behind them were captured on
`quran_phone`, on `quran_tablet7` at 1200 x 1920 and 320 dpi, and on
`quran_tablet10`, in one session. The seven inch frames are the 600 dp page,
taken with `wm size 1200x1920` and `wm density 320`. The bar is the demo's on
every leg: 9:41, a full battery, one network group, and no notification icons.
The demo bar draws the phone as connected without repeating the mobile group.

Play's current rule for preview assets is a JPEG or 24-bit PNG, 320 to 3840
pixels on each side, and a long side no more than twice the short side. All
three sets stand inside it: the phone set is the 9:16 shape, the seven inch set
is 1200 x 1920, and the ten inch set is 2560 x 1800, 1.42:1. The 16:9 and 9:16
shapes are Play's recommendation for large-screen placements, not a submission
requirement, so the ten inch set keeps the shape its real screen gives it.

The icon is the app's own mark: an open book on its
rehal, the owner's own reference drawing, redrawn clean for this app as
uniform line art on the reference's own reading: the pages are sail arcs
diving into a spine that ends at the V where the two pages meet, each plank
folds onto the page's outer edge, runs down beside the book, crosses below
it, and its leg and the closing arc meet at one shared tip, so each foot is
one clean tapered face. The whole drawing is in the theme's ink `#292622` on
the theme's paper `#FBF9F4`, one line weight on every icon surface, the
color the reader already sees inside the app; the cover's burgundy belongs
to the cover, and the store cards' ornament gold stays on the card grounds
where it was measured. The store and legacy tiles put the mark at 64 percent
of the canvas; the adaptive foreground keeps its own 41.5 percent canvas
measure because the launcher mask leaves 72 dp of the 108 dp layer visible.
The launcher carries the mark at five densities and in its adaptive
layers, the background the paper alone and the monochrome layer the book's
silhouette, drawn with a clean scaled cutout rather than a stair-stepped
offset so a themed home screen keeps the shape at 24 dp, and
`icon-512.png` is the same drawing at 512. The reminder's small icon carries
the silhouette with no ground, because the shade reads only its alpha channel
and paints the shape in the notification color. The share card carries the
drawing in the card's own ink beside the name, on the card's own paper, so the
card's foot and the home screen show one drawing. The feature graphic is
the page itself: one printed page of the theme's paper, its head the
colophon of the app's own mark beside its own name in Literata with the
book's name tracked beneath and one flat gold rule, and its body the whole
Ayat al-Kursi, set at the width Play's 16:9 crop keeps, in the theme's ink
with its tajweed shapes and its closing medallion exactly as the ten inch
capture drew them. There is no gradient, panel, device, or slogan anywhere:
the Book is the promise and the verse is the proof. The file is
1024 x 500, 24-bit PNG with no alpha.

Play Console alt text:

- App icon: "An open Quran on a rehal, drawn in dark ink on warm paper."
- Feature graphic: "The Quran's opening mark and name above Ayat al-Kursi,
  set in dark ink on a warm paper page."

The cards are current, composed on this machine in one pass from the frames
behind them: the phone card at 1080 x 1920, the seven inch card at
1200 x 1920, and the ten inch card at 2560 x 1800, all three lapis, every card
read. The capture inside a card is the whole display, and the card's own
drawing is the theme's colors and the app's two faces, so a card can never
drift from the app it sells.

The eight are a reader's path through the app rather than the app's own map, and
the first one is the app's face: the surah's name, its juz, and the three doors,
over the verse with its translation and its tajweed colors. The chosen thing on
the page wears the accent's own family, so the one block that says "this verse"
says it in the accent's voice. The seven tajweed families hold one weight, the
paper ink is the reading's own 12:1 tone, and the night page's colors sit below
the ink instead of glowing above it. A verse's number is its head, in the app's
lapis, with the app's one chevron beside it, and the printed medallion closes a
verse only on a page that draws the Quran alone: so a verse carries one number,
and that number is the door. The last frame is the recitation playing: the pill
names the voice and the place, and the word under the voice is washed on the
verse. The share card's Share image is the solid accent, the one answer that
surface exists to ask for.

The fifth frame is the Tajweed page and not the settings hub, which was the
first choice. The hub's last row carries the version, and a store frame that
dates itself is wrong the day the next release ships: on a phone the About row
falls below the fold, on a tablet all ten rows fit, so a frame built on the
phone's luck broke on its own tablet. The page that explains the first frame's
colors takes the slot, and no frame carries the version line.

The workflow runs the capture test on three emulator profiles (phone, 7 inch,
10 inch), caches the AVD per profile so only the first run of each pays for
creating the emulator, waits for the emulated storage to mount before the test
starts, and uploads each set as its own artifact. Its frames are signal: the
committed frames are the ones captured here, because a hosted emulator's status
bar and its timing are its own, and the cards are recomposed from those frames
with `storecards`. Run it from the Actions tab, or let it run when the UI
changes.

What each set shows, in order:

1. The reading with its own chrome up: the surah's name, its juz, and the three
   doors. The card says "Built to be read." with "Offline. No ads. No account."
   under it.
2. The tafsir open on its passage, with the source line above it. The card says
   "Tafsir, a tap away."
3. Search reading a real match, with the found word washed in the English
   translation. The card says "Search in English, Bangla, or Arabic."
4. Saved: the reader's own notes under the places they were written on. The card
   says "Save ayahs and take notes."
5. The Tajweed page: seven color families, each with a word from the reading and
   its rules. The card says "Tajweed colors with rules."
6. The reading at night. The card says "The same page, gentle at night."
7. The share card the app hands out, with Share image as its solid answer. The
   card says "Share any ayah as an image."
8. Recitation playing on Al-Fatihah, with the pill at the foot and the word
   being recited washed on the verse. The card says "Listen and follow every word."

The last frame is last on purpose. It is the one frame that leaves a player
running, so every still page is taken before it and the player is stopped after
the capture.

## Release notes (23.4, 465 characters)
23.4 removes the Storage page from Settings. It added your downloads up and offered to clear every recitation at once, but it could not free a byte by itself, and every download is already removed where it is named: on the reciter's list, the words page, or the pack's own page. The check that refuses a download when the phone is full is unchanged. Text, translations, tafsirs, recitations, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (23.3, 481 characters)
Closing About this surah now returns you to the surah's head. Read one screen or ten into it, and the tap puts the document away with the page back at the door under the basmalah and the first ayah below it, not at the ayah the document had scrolled past. If you have already scrolled past the introduction into the ayahs, the tap leaves the ayah you reached. The Quran text, translations, tafsirs, recitations, and content database are unchanged. No ads, no trackers, no accounts.

## Release notes (23.2, 404 characters)
The surah introduction now closes with a tap anywhere on it. The Hide button at its foot is gone, and the door under the basmalah appears only while the introduction is closed. A drag still scrolls the page, so a long introduction is not lost to a stray press. The basmalah, the Quran text, the translations, tafsirs, recitations, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (23.1, 430 characters)
23.1 keeps a download moving when you leave the app. A surah, a word set, or a content pack carries on with a progress notification and Cancel, resumes after a dropped connection, and checks free space before it starts. A recitation now asks before it uses mobile data, and Storage shows what your downloads weigh. Text, translations, tafsirs, recitations, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (23.0, 381 characters)
23.0 makes playback easier to read at large text sizes. The listening pill now grows and reflows with the phone's font setting, while the reading and its controls stay familiar. The store set now shows a real search match and live recitation, with clearer captions. Text, translations, tafsirs, recitations, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (22.9, 379 characters)
22.9 gives the store a new face. The feature graphic is the page itself: the app's own mark and name at the head, a gold rule under them, and the whole Ayat al-Kursi beneath, in the reading's own ink on its own paper. The app is unchanged: the same text, translations, tafsirs, recitations, and content database, with every setting where it was. No ads, no trackers, no accounts.

## Release notes (22.8, 481 characters)
22.8 settles what you feel rather than see. Five hand-drawn marks were measured against their own grid and redrawn, and the test now reads every mark back. The reading bar's settle carries the speed of your finger instead of a fixed trip. A landscape phone's camera no longer stands over a line, and the download capsule at the foot now shares the player's measure. Text, translations, tafsirs, recitations, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (22.7, 491 characters)
The recitation follows the page by measured time now. Both reciters' word timings were cut against a different edit of the recordings than the audio the app plays, so on about five percent of ayahs the mark waited through the first word and drifted inside the verse; both voices now read timings measured against the same recordings, and the marked word lands with the voice. Text, translations, tafsirs, recitations, and word lists are otherwise unchanged. No ads, no trackers, no accounts.

## Release notes (22.6, 314 characters)
22.6 offers two recitation voices, Husary and Minshawi. Alafasy and Abdul Basit retire, and the two that remain keep their timings inside the app, so a reciter is ready offline. Word pronunciation, the word lists, both translations, both tafsirs, and the Quran text are unchanged. No ads, no trackers, no accounts.

## Release notes (22.5, 404 characters)
22.5 makes search know the surahs. Typing bakara now answers with Al-Baqarah, not a word that only looked similar: yasin finds Ya-Sin, and surah baqarah opens Al-Baqarah. A suggestion is offered only when it sounds like what you typed, so a near miss no longer swaps in an unrelated word. Text, translations, tafsirs, recitations, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (22.4, 307 characters)
22.4 gives the store a new face: eight cards, each composed from a real screen and naming one thing the app does, from the reading to recitation. The app is unchanged: the same text, translations, tafsirs, recitations, and content database, with every setting where it was. No ads, no trackers, no accounts.

## Release notes (22.3, 433 characters)
22.3 makes search recover. A misspelled word comes back as one word to tap, rewritten in your own sentence; a reference past a surah's end says how many ayahs it has; words in "double quotes" must sit together; and juz 30, para 30, or পারা ৩০ lands where the juz opens. Answers arrive in a frame, and nothing leaves the phone. Text, translations, recitations, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (22.2, 474 characters)
22.2 opens a new install on the light page, the warm paper of the printed Book, and leaves every existing reader's page exactly as it was. A language change now carries the tafsir you chose to the new language and never downloads one you did not. Al-Fatiha's recitation ships in Husary's voice; another reciter's Fatiha downloads on your word, like every other surah. Text, translations, recitations, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (22.1, 465 characters)
22.1 returns the app's one action color to lapis, the blue of the illuminated Qurans, measured deeper: gold beside ultramarine on the same warm paper. The chosen verse's ground and the found word's ground follow it, and the dark theme's current surah and juz now stand out. The page, the ink, the gold ornament, and the tajweed colors are unchanged. Text, translations, tafsirs, recitations, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (22.0, 430 characters)
22.0 turns the app's one action color green. Every control that answers a tap, the chosen verse's ground, the found word's ground, and the word the reciter is saying now wear a deep emerald, measured to the same contrast floors as before. The page, the ink, the gold ornament, and the tajweed colors are unchanged. Text, translations, tafsirs, recitations, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (21.5, 495 characters)
21.5 keeps the reading as it was and tightens the rules beneath it. Pack updates stay quiet and Wi-Fi only, touching only packs you already have; a newer pack arrives only with an app release, and there is no button. The pack choices, the word sheet's grammar order, the clock's digits, and the ayah medallion now sit under the test gates, and the build moves to the newest Android toolchain. Text, translations, tafsirs, recitations, and content are unchanged. No ads, no trackers, no accounts.

## Release notes (21.4, 376 characters)
21.4 brings the build toolbox up to date: the compile platform is 37.1, and Compose 1.13.0 with Material 3 1.5.0 stable arrive together, so the sheet library's hand pin is gone. The app reads exactly as before, and the 16 KB page rule is re-proved on the shipped libraries. Text, translations, tafsirs, recitations, and content are unchanged. No ads, no trackers, no accounts.

## Release notes (21.3, 343 characters)
21.3 puts the language in the phone's own settings too: on Android 13 and later, choose English or Bangla there and the app follows, with the app's own language row keeping the same one choice. The sheet library moves to its stable release. Text, translations, tafsirs, recitations, and content are unchanged. No ads, no trackers, no accounts.

## Release notes (19.9, 485 characters)
19.9 is lighter on the battery. The screen turns off five minutes after you stop touching the page, and a sheet left open no longer keeps it lit forever. While the page is hidden, the reciter's wash and the reader's place wait, and the place is saved every thirty seconds instead of ten. Tafsir search builds when you open Search, and the next surah is prepared near this one's end. Text, translations, tafsirs, recitations, and content are unchanged. No ads, no trackers, no accounts.

## Release notes (19.8, 447 characters)
19.8 makes the reading lighter under the finger: the page no longer rebuilds itself while you scroll, and each verse paints its tajweed colors from work already done instead of measuring every character again on each redraw. Long-press selection is gone from the translation and the tafsir passage; the verse still leaves the app through Share. Text, translations, tafsirs, recitations, and content are unchanged. No ads, no trackers, no accounts.

## Release notes (19.7, 493 characters)
19.7 makes search find the words you actually type, including prayer, Satan, the Quran, and Abraham; exact matches still come first. Downloaded recitations keep every surah, so a second download no longer deletes the first. The daily reminder card stays away once the reminder is set, and Not now never turns it off. English surah names are corrected, the surah introductions' references open on a tap, and download sizes read in decimal MB. Content is 1.8.1. No ads, no trackers, no accounts.

## Release notes (19.5, 388 characters)
19.5 makes the ayah number a button you can see. The verse's number at the head now wears the app's quiet pill, and it takes the accent while its menu is open. Every verse also ends with the printed medallion the Quran page uses, whether you read with a translation or the Quran alone. Text, translations, tafsirs, recitations, and content are unchanged. No ads, no trackers, no accounts.

## Release notes (19.4, 417 characters)
19.4 makes the top bar the page's head. It stands when you arrive and at the top of a surah, steps away as you read forward, and returns on the first backward scroll or a tap. The reading begins below it, never under it, so no verse is ever behind it. The first-surah sentence is gone with the tap it had to teach. Text, translations, tafsirs, recitations, and content are unchanged. No ads, no trackers, no accounts.

## Release notes (19.3, 458 characters)
19.3 makes a message leave when you touch anywhere else. Note saved, a removed save with Undo, a failed share, and a permission notice no longer wait out a timer over the page you are reading, and the tap you made still counts. The one sentence you could not put away is gone too: a phone with no settings screen is now said on the Daily ayah page itself. Text, translations, tafsirs, recitations, and content are unchanged. No ads, no trackers, no accounts.

## Release notes (19.2, 400 characters)
19.2 fixes the ayah key on the recitation pill. Since 14.2 it showed the Book's running number, so 16:28 read 16:1929 and the listening panel said Ayah 1929. The pill now reads 16:28 and the panel says Ayah 28, the number you know. Every other number the app keeps is unchanged, and so are the text, both translations, both tafsirs, the recitations, and the content. No ads, no trackers, no accounts.

## Release notes (19.1, 497 characters)
19.1 puts the number on its verse: at the head of the line, not floating between two verses, and one number per verse, the one you tap. The chosen verse and a found word are the page's warm tone instead of blue. Word meanings are offered where the reading arrives, the first page says once that a tap brings the bar, Saved names a date instead of 104 weeks ago, and a large download asks first. Text, translations, tafsirs, recitations, and content are unchanged. No ads, no trackers, no accounts.

## Release notes (18.8, 490 characters)
18.8 gives the size controls the page. Each name stands over its rail, and the two end marks stay at every text size. The words page names its switch after the feature, teaches the long press under it, and draws its one list as a file row. Downloaded pronunciation is now a door that lists each surah with its own Remove. A pair of buttons reads the quiet word first and the answer last. Text, translations, tafsirs, recitations, and content are unchanged. No ads, no trackers, no accounts.

## Release notes (18.7, 498 characters)
18.7 puts the word sheet's speaker on every word. Press it and the word sounds; if its surah's voices are not on the device, the pack downloads then, the size counting under the meaning, and the word plays when it lands. A word the recitation runs together says it has no recording. The words page now offers to remove the downloaded voices, and its cut-off sentence and Book-wide size are gone. Text, translations, tafsirs, recitations, and content are unchanged. No ads, no trackers, no accounts.

## Release notes (18.6, 498 characters)
18.6 gives the word sheet its own voice. Its speaker now plays a recording made for that word, cut from Mishari Al-Afasy's recitation at quran-align's word boundaries, and stops at its end. The word meanings page is now Word meanings and pronunciation; its one switch fetches the meanings, roots, and pronunciation for the surah you read, later surahs on Wi-Fi, and Al-Fatiha ships. Text, translations, tafsirs, recitations, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (17.8, 487 characters)
17.8 brings the word root to readers who already had word meanings on, and keeps a word's voice to that word. The root was in 17.7's content, but an existing word list never asked for it; it now arrives once on Wi-Fi after the first page, and turning word meanings off and on fetches it at once. The speaker on the word sheet stops at the word's end instead of playing on into the verse. Text, translations, tafsirs, word lists, and audio are unchanged. No ads, no trackers, no accounts.

## Release notes (17.7, 496 characters)
17.7 rights four things. A voice chosen on the playing pill is heard from the ayah playing now, not the one Play was pressed on. A saved ayah no longer wears a bookmark beside its number; its menu row says Saved, and the verse still tells a screen reader. The word sheet names the word's root and offers a speaker that says it in your reciter, stopping at its end. The content adds the roots of 50,298 words; text, translations, tafsirs, and audio are unchanged. No ads, no trackers, no accounts.

## Release notes (17.6, 490 characters)
17.6 gives the page more of the glass. Every surface keeps 20 dp at its edge, not 28, so the reading, search, tafsir, footnotes, the word sheet, browse, and settings run wider. The page itself is unchanged: a wide screen still holds the same column, and the ten inch tablet reads the lines it read before. The search field's head, the back mark, and a choice ring now stand on that line. Text, translations, tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (17.5, 445 characters)
17.5 makes the floating paper the sheet's own white. The pill, the offer, the snackbar, and the menus they open stand 2 L* over the day page instead of 4.4 L* under it, so they read as paper raised over the page and the ivory slip is gone. The night's floating paper steps up one tone so its menu clears the washed verse by the same distance. Text, translations, tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (17.4, 417 characters)
17.4 makes the floating paper warm paper. The pill, the offer, the snackbar, and the menus they open wear the page's own tone a shade under it and warmed, so they read as an ivory slip laid on the page instead of the gray veil they became, and a menu over a washed verse keeps a clean edge. The night is unchanged. Text, translations, tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (17.3, 430 characters)
17.3 takes the tint out of the floating paper. The pill, the offer, the snackbar, and the menus they open keep the 1.23:1 step from the day page and give up the sepia that came with it, so a menu over a washed verse reads as paper one step in shade rather than as a warm cast, and the accent stays blue. The night is unchanged. Text, translations, tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (17.2, 485 characters)
17.2 makes the floating paper readable. The pill, the offer, the snackbar, and their menus wear one warm tone a step from the day page, the step the night page already held, so a light pill no longer vanishes into the paper. The recited word keeps its tint without the accent edge, and the download offer is one 64 dp row naming the reciter, the surah, and the size before Download. Text, translations, tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (17.1, 473 characters)
17.1 makes the colors read for the long sitting. The seven tajweed families hold one weight on the page, so no rule is louder than its neighbor, and the night colors are calmer, the green no longer saturated. The reading ink is softer on paper, the recited word gains a fine accent edge, and selecting a verse marks it in the page's own tone so its colors stay readable. Text, translations, tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (17.0, 482 characters)
17.0 makes the recitation pill one row. It stands 64 dp, the height its other voices already wear: the voice and its pace over the place and its key, with play or pause at the end. The two skips left the glass, where they cut the place, and are actions TalkBack finds on that mark; the notification keeps them. The pill and the offer wear the paper's tone, not the sheet's white. Text, translations, tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (16.9, 484 characters)
16.9 tidies the reading's edges. The ayah's number answers a press with a pill that leaves the digits room. A saved or noted mark on the page with the translation off rides the verse's first line, so a marked verse no longer stands apart. The listening panel no longer repeats the place the pill already says. The word meanings switch now fetches the word list. The Quran text, both translations, both tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (16.8, 427 characters)
16.8 makes the reading say where the actions are. Every ayah's number now carries the app's small chevron, the mark the recitation pill's doors already wear, so the head reads as a door instead of a citation. The chevron turns up while that ayah's menu is open, and the number keeps no capsule around it. The Quran text, both translations, both tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (16.7, 493 characters)
16.7 gives the page back to the reader. A tap on an ayah no longer selects it: the verse's number is the one door to its actions, and the menu it opens closes on any tap outside without choosing another verse. In the mushaf, the printed medallion is that door. The five-action capsule inside the verse is gone, and the number stands at the head again in the app's blue. The Quran text, both translations, both tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (16.5, 412 characters)
16.5 makes the recitation pill's doors answer. When the voice list or the listening panel is open, the chevron now turns up, and the line says Expanded or Collapsed to TalkBack, like every other door in the app. The offer, the playing pill, and a pill with no recitation all read the same. The Quran text, both translations, both tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (16.4, 494 characters)
16.4 makes the interface one hand. Every action is one blue, every chip and door shares one measure, and the theme swatches keep a visible edge. Search results lose the arrow that pointed the wrong way, the snackbar's action is a pill, and the safe Keep is the solid answer. At 200 percent text, Browse chips keep their gap, size rails keep their room, and ayah numbers no longer clip. Quran text, translations, tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (16.3, 478 characters)
16.3 makes the reciter's word follow the voice. The highlight waits for each ayah's first sound and clears when the voice stops, instead of starting early and lingering. It moves at the screen's own rate, so it keeps up at any speed, and it no longer blinks when the ayah changes. Where the timing cannot explain a long stretch, it rests instead of sitting on the wrong word. Text, translations, tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (16.2, 443 characters)
16.2 gives the recitation offer its voice back: tap the reciter's name to choose who reads the surah, with each voice's size in view. The offer and the playing pill are one line of words over one row of controls. The word meaning sheet's head moves to the page's left edge, its word is the Quran's own size, and its gloss the translation's. Text, translations, tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (16.1, 491 characters)
16.1 makes the quiet parts fit together. The recitation offer is a compact centered capsule: the reciter, surah, and size on one line, with Not now and Download under it. The reading runs under the clock again. Footnotes and word meanings share the app's sheet title and place line, the Reciters page lists the bundled Al-Fatiha, and Send feedback opens your mail app. The Quran text, both translations, both tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (16.0, 417 characters)
16.0 gives the page back to the text. The quiet line at the foot is gone, and the bar that holds Browse, Search, and the surah and juz now leaves when you start scrolling and returns with a tap. Aa leaves the bar; the text size lives in Settings, with its sample. The page runs to its own foot. The Quran text, both translations, both tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (15.9, 393 characters)
15.9 opens a word's meaning on its own sheet when you press and hold it: the word, its meaning, and the ayah named above, all on one center line. The reciter's word is a rounded wash again, so its letters keep their own ink and tajweed colors while the voice is on them. The Quran text, both translations, both tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (15.8, 388 characters)
15.8 ships Al-Fatiha's seven ayahs in both voices inside the app, so the first Play on 1:1 works offline with no download offer. The two reciter timing packs ship too; every other surah still downloads its audio on your word, with the timings already on the device. The Quran text, both translations, both tafsirs, and the content database are unchanged. No ads, no trackers, no accounts.

## Release notes (15.7, 373 characters)
15.7 restores the original recitation files: 128 kbps stereo MP3, the same Al-Minshawi and Al-Husary recordings and word timings the app shipped before 15.6. The 64 kbps Opus encode is withdrawn. A surah is about twice the size, and one downloaded in 15.6 downloads again. The Quran text, both translations, and both tafsirs are unchanged. No ads, no trackers, no accounts.

## Release notes (15.6, 261 characters)
15.6 re-encodes the recitation in Opus at 64 kbps mono, about half the size, with the same word timing. The Quran text, both translations, and both tafsirs are unchanged. The new packages download one surah at a time as before. No ads, no trackers, no accounts.

## Release notes (15.5, 407 characters)
15.5 opens on the Quran, in your phone's language, with both translations already on the device. The reading keeps a quiet line with your surah and juz, Aa opens the type page, a tap on a word shows its meaning, and a tafsir you do not have is offered where you ask for it. Search shows the verse for a reference result. The audio offer has a clear Not now. Text, translations, tafsirs, and packs unchanged.

## Release notes (15.4, 375 characters)
15.4 makes the Font size sample a type specimen. The page shows only the ayah and its translation, without the translation's name above it, and it shows the translation even when the reading's Show translation switch is off, because the sample is there to judge the text size. The reading still names each translation column. Text, translations, tafsirs, and packs unchanged.

## Release notes (15.3, 402 characters)
15.3 puts your language first. With two translations on, the reading, the daily reminder, and the share card lead with the one in your language, and a column in the other language always says which pack it is. The Font size sample shows that same line and its name, and follows Show translation. The Translations page lists your language's packs first. Text, translations, tafsirs, and packs unchanged.

## Release notes (15.2, 431 characters)
15.2 makes Settings one list. Every row says where a setting stands and opens the page that changes it: the switches moved to the pages they govern, so no row is half a switch and half a door, and the switch is drawn in the app's own hand. Word meanings gain the page they were missing. A failed download, a failed save, and a full phone each say one sentence now instead of three. Text, translations, tafsirs, and packs unchanged.

## Release notes (15.1, 431 characters)
15.1 quiets the search sheet and the tafsir card. Recent searches lose Clear all: every word keeps its own visible X, and the caption sits with its chips. The chips now grow with the system text size instead of clipping, and the filter chips keep their row gap at large text. A tafsir passage now ends on the card's block break, so the next language no longer reads as its caption. Text, translations, tafsirs, and packs unchanged.

## Before each release

1. Raise `versionCode` by 1 and `versionName` by 0.1 in
   `app/build.gradle.kts` and update the version line at the top of this
   file, in the same commit that ends the session.
2. Run the owner-machine gates: `./gradlew :tools:run --args="verify"`,
   `build`, `checkdb`, `audit`, `search`, `fonts`, and `packs`, then the
   instrumented tests on an emulator. They all run from a clean
   `content/work`, and the content tags are published before the app build
   downloads them.
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
| `screenshots/phone/*.png` | 1080 x 1920 | phone cards (8) |
| `screenshots/tablet7/*.png` | 1200 x 1920 | 7 inch tablet cards (8) |
| `screenshots/tablet10/*.png` | 2560 x 1800 | 10 inch tablet cards (8) |
| `frames/phone/*.png`, `frames/tablet7/*.png`, `frames/tablet10/*.png` | the canvas each card carries | the captures the cards are composed from |



## What the app carries, and what a reader adds

The app ships the Quran text, both translations, every reciter's word
timings, and Al-Fatiha's audio in the default voice: a complete offline
reading that needs no network and no account. The tafsir, the word lists,
and every other surah's recitation are added by the reader, from the project's
own GitHub Releases, with the size named before a tap or on the player while a
Continue package downloads, and a SHA-256 check before it is used:

| Language | Translation | Tafsir | Word by word |
|---|---|---|---|
| English | Saheeh International (ships) | Ibn Kathir (29.5 MB) | 6.3 MB |
| Bangla | Al-Bayan Simple Translation (ships) | Ibn Kathir (56.0 MB) | 8.4 MB |

Recitations: Al-Fatiha ships in Husary's voice; the other voices' Al-Fatiha
and every other surah download one at a time (about 0.2 to 185 MB each).
The 1.7 MB of word timings per reciter ships with the app.

Content updates arrive on their own: when a pack you already have is
superseded, the app replaces it quietly on Wi-Fi, so the tafsir and the
word lists stay current without a tap. The Quran text and the translations
update with the app itself.

## Store answers to have ready

* **Is the app free?** Yes, and open source (MIT).
* **Does it show ads?** No, and it has no analytics and no accounts.
* **Why does it need the internet permission?** To download a content pack
  or a surah's recitation the reader asks for, from the project's own
  releases, to quietly refresh an installed content pack when the app
  carries a newer version, and to repair a damaged local pack, on Wi-Fi
  only. The app asks no other host; GitHub's download service may redirect
  the request to its own release storage.
* **Data safety**: no data collected, no data shared, nothing stored off the
  device. Notes and bookmarks stay in the app's private storage.
