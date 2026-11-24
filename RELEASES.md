# Release notes

Every release of Quran: The Noble Book, newest first. The current release is
summarized on the [front page](README.md); the releases before it are written
in full below. The reason a decision was made lives in the KDoc beside the code
it decided, tagged `owner decision`, and in `git log`.

## 22.6

The whole JVM suite, `verify`, `checkdb`,
`search`, `storecards --check`, lint, and `assembleDebug` were green on
this machine before the push. The diff touches `feature-settings/`, so the
four accessibility classes were owed; they ran on `quran_phone` as a clean
install, ten cases and no failures. The five classes the recitation change
touches ran with them, fifty-five cases and no failures:
`BundledFatihaTest`, `FatihaPlayTest`, `ListenOfferDismissTest`,
`PlaybackServiceTest`, and `PlaybackPillTest`. All three store sets were
captured from this build, on `quran_phone` at 1080 x 1920, `quran_tablet7`
at 1200 x 1920 and 320 dpi, and `quran_tablet10` at its own 2560 x 1800, and
every frame of the twenty-four reads as the reading the app draws; the keys
differ from the last set only in the status bar's network glyph and the
sub-threshold antialiasing around it, because no frame carries a reciter
list. The Bangla leg ran green on the phone: the settings hub reads
তিলাওয়াতকারী over হুসারি, and nothing in the sheet is held to a line it
cannot draw. `content/quran.db` is two voices now and hashes
`a7f425d57cfe38156cbedc7c5d07190557933e253bc7d59505a0b838181e4f8f`,
rebuilt twice to the byte. The core pack moved to version 8 and publishes as
`pack-core-d7407645`, served and checked against the catalog's hash; the two
kept reciter packs reproduce the bytes they shipped under, `af264024b1b4` and
`9b09e6cb1b16`.
The content database publishes as
`content-db-a7f425d5`, and the first `build` run on `main` failed at
`fetch` because that release did not exist yet: `fetch` brings the database
from the release named for its hash, so the release is published before the
run. The rerun of that same commit 45379585918 was green.
The signed bundle comes from `build` run 45379585918, green on `main` for
the commit `5c82a2ea`: the unit tests, `checkdb`, `search`, `storecards
--check`, lint, and `assembleDebug` passed, and the signed bundle
`quran-22.6-vc228.aab` has SHA-256
`ff756fdd16034037db0871d0c38ef6acb1e59c07e11d023e6a690f7009f362a7`.
`jarsigner` reads `jar verified`, the bundle's own manifest reads
`versionName 22.6` and `versionCode 228`, and the upload certificate is the
same key 21.4 through 22.5 shipped under, SHA-256
`53:7D:09:D2:03:00:12:9E:97:3B:79:45:31:6B:FE:24:CF:AD:CF:BC:77:EE:C5:22:9C:BF:30:17:0D:9D:E5:21`.
The 22.6 bundle was submitted for Play review, and the hand-off
copy in `play-store/aab/` was deleted once Play had it.

The app offers two recitation voices, Husary and
Minshawi (owner decision, 22.6). Alafasy and Abdul Basit are retired: their
catalog rows, shipped timing packs, database rows, voice edges, source pins,
published manifest entries, About page credits, and tests are gone, and the
core pack moves to version 8, whose recitation map names the two voices the
catalog offers. The word pronunciation pack is unchanged. The Quran text,
both translations, both tafsirs, and the word lists are unchanged.

## 22.5

Search says what the reader meant (owner
decision, 22.5). A reader who typed `bakara` was offered `banana`, because
the surah they meant was not in the vocabulary the suggestion is chosen
from and the fruit was the only word within two edits; the surahs' names
are part of that vocabulary now, offered as the name the interface draws,
so `bakara` comes back as one tap on `Al-Baqarah`. A Latin term of five
letters and up is ranked by how it sounds before how it is spelled: `q`
folds into `k`, the vowels a transliteration never agrees on fall away, and
a final `h` falls away with them, so `bakara` and `baqarah` are one shape
and `koran` and `quran` another. A word that only sounds vaguely near is
refused rather than offered. The surah match reads the keys a name may be
typed by, so `yasin` finds Ya-Sin and `albaqarah` finds Al-Baqarah, whose
stored names carry a hyphen the reader does not type, and the word `surah`
no longer hides the surah it introduces: `surah baqarah` opens Al-Baqarah.
The vocabulary is counted in the index's own tokens, so a count is a count
of what search matches. The Quran text, both translations, both tafsirs,
the recitations, and the content database are unchanged.

## 22.4

The store set is the app's own face (owner decision, 22.4). The eight
screenshots Play shows are cards now: each is a real capture from the
screenshot test, whole and unedited, laid on the app's own paper inside an
outline drawn in its ink, under one line set in the app's Literata with the
app's Inter beneath it. The card's gold rule is the feature graphic's, so the
icon, the graphic, and the cards read as one face.
`./gradlew :tools:run --args="storecards"` composes all three sets from the
captures committed under `play-store/frames/`, and `storecards --check` is a
release gate: it compares the pictures the command would draw with the ones
the repository holds, at a tolerance wide enough for a platform's own font
rasterizer and narrow enough that one changed word fails. The app itself is
unchanged, and the accessibility classes were not owed: the diff touches
`tools/`, `play-store/`, and the records, and no path the trigger names.

## 22.3

Search recovers where it used to stop (owner decision, 22.3). A query that
finds nothing is offered the one word the reader's own library says is
nearest to what they typed, in their own sentence, and tapping it rewrites
that word and runs the search again. A reference past its surah's last ayah
answers with the surah's own count, and a surah past the hundred and
fourteenth with the Book's count, in place of "No matches". Words wrapped in
double quotes must sit together, and a quoted reference is a phrase and
never a place. `juz 30`, `para 30`, `পারা ৩০`, and `جزء 30` land on the ayah a
juz opens, and the row names the juz in the reader's own word. The
vocabulary is the Book's words, the enabled translations, and the word list,
warmed when the sheet opens, so the suggestion lands in a frame; nothing
leaves the phone. The text, both translations, both tafsirs, and the content
database are unchanged.

## 22.2

The page opens on Light (owner decision, 22.2, superseding 13.5). A fresh
install no longer follows the phone's own day and night: the Book opens as
its own warm page, the page the printed Book reads on, and System stays one
row away for a reader who wants the phone to choose. An install that has
opened before and never chose a page keeps the System it reads under, written
once before the new default can move it, so no update changes a reader's page
for a reason they cannot see. A language change now swaps a tafsir the reader
has chosen and never introduces one: until now, moving from English to Bangla
started a 56 MB tafsir download behind a reader who had never asked for a
tafsir, which the first open deliberately leaves off. Al-Fatiha's recitation
rides in Husary's voice alone, the default recitation; a reader who has
chosen another voice meets the same download offer every other surah makes,
0.7 MB for the surah. All four voices' word timings still ship, so every
reciter is ready offline. The Quran text, both translations, both tafsirs,
and the content database are unchanged.

## 22.1

The action color returns to lapis, measured deeper, and the dark theme's
chosen step is lifted where it never read. Lapis carried every action from
the first release until 22.0 turned it to the category's green; 22.1 turns
it back by the owner's word, into the ultramarine the old mushafs were
painted in, gold letters on a lapis ground, a pigment priced above gold.
Green's claim proved to be the 1837 dome and the flags rather than the text:
the one narration that names a beloved color is graded weak, and the dome
was wood, then white, then blue, before it was green. The day tone #1F3D76
reads 10.0 to 1 on the page, APCA 90.7, the grade the emerald was chosen
for, and it sits wider from the verse's own families than the emerald ever
sat from its green: the iqlab 0.097 OKLab away, the madda 0.115. The night
tone #A8C2EF reads 9.6 to 1, held at a chroma under the night gold's. The
chosen and found grounds follow the accent at the lightness they already
carried, the word mark's washes keep their pinned alphas, and every floor
the contrast tests enforce held unchanged: the seven rule colors on every
ground, the quiet voice, the floating papers, and the switch. One floor is
new: the chosen block's step over the sheet it is painted on is pinned at
4 L* in both grounds, because the 15.4 the night had carried since 21.0
stood 2 L* over the sheet the current surah and juz are read against, and
the owner could not find it (owner report, 22.1). The night's chosen block
moves to 18.4, 5 L* over the sheet, clear of the floating paper it never
touched. The paper, the ink, the gold ornament, the tajweed palette, the
icon, and every content byte are unchanged.

## 22.0

The app's one action color turns green. Lapis had carried every action since
the first release, and 22.0 turns the accent to emerald, the family of the
green dome, by the owner's word after a measured study of three greens:
emerald, malachite, and verdigris. The day tone is #004B1E, 9.9 to 1 on the
page; the night tone is #A4D2A9, 10.2 to 1 on the night page. The chosen and
found grounds keep the lightness they carried and turn with the accent, the
word mark's washes keep their pinned alphas, and every floor the contrast
tests enforce held unchanged: the seven rule colors on every ground, the
quiet voice, the floating papers, and the switch. The idgham, the verse's own
green family, sits 0.063 OKLab from the new accent, farther than any family
sat from the lapis. The paper family, the ink, the ornament gold, the
tajweed palette, the icon, and every content byte are unchanged.

## 21.9

Not now on the daily ayah offer means its word. The first tap moves the ask
to the end of the next surah the reader finishes, wherever that is, and the
second tap is the final answer, so the offer never nags a reader who keeps
saying no and never dies on one who meant later. Turn on, and the reminder
set in Settings, remain the final answer, and a fresh install meets the
offer exactly as before. The stored state is one surah number, absent on
every existing install, so no update is asked again and no migration runs.
The tile itself is untouched: same shape, same words, no string, color, or
frame moved. The Quran text, both translations, both tafsirs, the
recitations, and the content database are unchanged.

## 21.8

The reminder's one-time offer, at the end of the first surah a reader
finishes, centers itself: its name, its sentence, and its two answers stand
on one axis under the surah's centered completion line, the same axis the
empty note keeps for a short block that must read as one. Not now still
leads and Turn on still trails, the one order every pair in the app keeps.
The tafsir offer inside the ayah card keeps its sheet's leading edge,
because it is a section of a page that starts there, not a card standing on
the glass. The Quran text, both translations, both tafsirs, the recitations,
and the content database are unchanged.

## 21.7

A long ayah's share card stands at the width a feed gives a picture instead of
standing narrow inside it.

**The card's shape.** The card was aimed at a 1:2 portrait, on the reasoning
that a chat clamps a tall picture's pixels and shows it whole, only smaller.
A feed clamps the picture's shape instead: Facebook and Instagram give a
picture the feed's own width only up to the 4:5 portrait, 1080 by 1350, and a
taller one is contained into a shorter box, standing whole but narrow with
the feed's ground where the width should be, or center-cropped where the
surface crops. That is what a shared Al-Baqarah 2:255 card met: it left the
app at 1:2.15 and Facebook stood it narrow beside a column of other posts.
The ladder now widens until the card stands inside 4:5, in the same 80 dp
steps, and the owner's own card shows the rule: 2:255 is 1:3 at 320 dp, and
at 560 dp it stands at 1.207, a 1,680 by 2,027 px picture at the capture's
3x, which a feed shows at its full width. An ordinary ayah already inside
the limit never moves; a card that overflows every rung, like 2:282, keeps
the widest card as before, and a feed will still contain it, small but whole
with a tap to the full picture. The preview and the capture read the one
rule, and the JVM test that pins it carries both cards' recorded shapes, the
2:255 one taken from the capture's own print on the phone.

## 21.6

The reading scrolls back up the way it scrolls down, and a first launch reaches
the text in about half the time.

**The reading's window.** A lazy list builds the row ahead of the reader and
throws the row behind them away. Scroll down and every row is new; scroll back
up and every row is new again, so a reader who returns to a verse pays for it
twice: the shaped Arabic line, the translation's runs, and the words a screen
reader speaks are all built again on the frame the block comes back. The list
now keeps a whole screen of rows on either side of the glass (owner decision,
21.6), which is the only thing in this release the reading does differently.
Measured on the phone by counting the blocks a return rebuilt, with a
temporary counter in the block itself: two at the list's default, one with the
window, and never more than one. The same counter settled the diagnosis: no
block recomposes during a scroll, so every block a return meets is a fresh
composition, which is what made the count worth taking. The bar's rule, the
place, the taps, and the sheets are untouched.

**The measured profile.** What the app shipped as its profile was a
hand-written list of nineteen rules naming the launch path by hand, and nothing
of the reading. It is the profile the benchmark module generates now, by
walking the app the way its own note always promised: launch, scroll down, and
scroll back up. The return was missing from the journey, so the page the
reading rebuilds was never compiled. Cold start on the phone, five cold starts
each way through `am start -W`, measured a median of 4504 ms before and 2559 ms
after, with the spread tightening from 3.3 to 6.3 seconds to 2.3 to 2.7. The
startup metric itself cannot read this emulator's frame timeline, so the wall
clock is the number here and the artifacts are unchanged: no dependency, no
permission, no content, and no string.

**A second number for the scroll.** The benchmark module read the scroll in one
direction only. It now measures the same gesture in both directions, with the
frame timeline for a real phone and the choreographer's own frame for a machine
whose GPU is the bottleneck, and the walk back is part of the profile's journey.

## 21.5

The rules that had no gate now have one, and the quiet refresh's reason is
written down.

Six decisions lived in modules whose JVM suites the release gates never ran:
the five answers the settings hub and the app ask about a translation, a
tafsir, and the word list; the line under the words switch; the order of the
word sheet's grammar; the clock's digits and period marker; and the medallion's
ink, its place on the glass, and its touch target. They moved into `:data`,
`core`, and `ui-kit`, where the gates already look, and each carries a
sentence-named test: `PackChoicesTest`, `WordAudioLineTest`,
`GrammarFactsTest`, `ClockTextTest`, and `SpanInkTest`, with the About
page's Bangla rule pinned in `PackNamesTest`. The event is invisible: no
string moved modules, and the settings rows, the word sheet, the clock, and the
ayah's door draw exactly what they drew.

The quiet refresh keeps its place and gains its record (owner decision, 21.5).
It runs when the app opens, on an unmetered connection, and only for packs
already on the device, so a reader who never added a pack never pays a byte for
it; a newer version is named by the catalog that ships inside the app, so it
can only arrive with a new release, and none is baked into the package. There
is no manual check and no button, because the catalog cannot change between two
launches of one build. The dead force switch and the sentence that promised the
check are gone.

The accessibility classes are a release rule now: a session whose diff touches
the reading, the search sheet, the settings module, the ui-kit, or the reading's
own chrome runs `AccessibilityTest`, `LargeTextTest`, `ReadingSemanticsTest`,
and `KeyboardFocusTest` on the phone before the hand-off, and the hand-off
says so. This session ran them, 11 tests and no failures.

The build moves to AGP 9.5.0. The project pins every dependency by hash, so the
move meant regenerating `gradle/verification-metadata.xml` over the whole task
graph: 59 components entered, all in the AGP family, and the file was checked
component by component and artifact by artifact against the one it replaced, so
nothing was lost. What the file could not see from this machine was the other
platforms: the regeneration pins the AAPT2 binary the host downloads, and CI's
Linux build refused the file at its first task. The Linux and macOS copies were
added from Google's own Maven, cross-checked against the SHA-1 that repository
publishes, and proved locally by resolving both classifiers through the
verification gate; the verification file now names all three platforms. Two
deprecation warnings remain in the instrumented suite (the v2 compose test
APIs), and nothing gates on them.

The whole JVM suite, `checkdb`, `search`, lint, and the debug build are
green on this machine. No visible surface changed, so the three store sets
stand, and no content and no permission moved.

The 21.5 bundle came from `build` run 44931558235, green on `main` for the
commit `a4520eca`, and `quran-21.5-vc217.aab` has SHA-256
`8eb68658f288a41314a993ea29c1941c0611ba5345f78281d3a60db01b5de175`.
`jarsigner` reads `jar verified`, and the upload certificate is the key 21.4
shipped under, SHA-256
`53:7D:09:D2:03:00:12:9E:97:3B:79:45:31:6B:FE:24:CF:AD:CF:BC:77:EE:C5:22:9C:BF:30:17:0D:9D:E5:21`.
The run after the version commit was red at dependency verification, because
the regenerated file held only this machine's AAPT2 binary: the other two
copies were added from Google's own Maven and proved by resolving both
classifiers through the gate, and the run that built the bundle is the one
after that fix. The bundle was submitted for Play review, and the hand-off copy
in `play-store/aab/` was deleted once Play had it.

## 21.4

The toolbox catches up, and the pin that held the sheet library goes.

The newest compose BOM, 2026.11.01, carries Compose 1.13.0 and material3
1.5.0 stable, and it requires compiling against the 37.1 platform. The
compile platform is 37.1 now, declared as `compileSdk = 37` with
`compileSdkMinor = 1` in all ten modules, while `targetSdk` stays 37 and
`minSdk` 24 for the reasons their own notes give. Material3 had been
pinned by hand to 1.5.0 stable for two releases while the BOM carried
1.4.0, and the pin's comment said when to move: the session that raises
the compile platform to 37.1 and proves it. This is that session. The pin
is gone, the BOM carries the sheet's physics, and the comment beside the
version records the move.

The move changes the version of one library that ships native code, so the
16 KB page rule was re-proved by measurement rather than assumed. The
bundle carries the same two native libraries as 21.3, eight ABI copies in
all, and every LOAD segment of every copy is aligned at 2^14; `zipalign
-c -P 16 4` reports verification successful.

The whole JVM suite, 91 classes and 521 tests, lint, and the debug build
are green on this machine with the new platform and the new BOM, and four
instrumented classes ran on the phone after the move
(`SheetStillnessTest`, `SentenceTouchTest`, `AboutLanguageTest`,
`SettingsRowOrderTest`), all green. No visible surface changed, so the
three store sets stand as captured for 21.2, and no content and no
permission moved; no dependency was added or removed, and the versions the
BOM carries moved with it.

## 21.3

The language gets the phone's own door, and the sheet library settles.

Android 13 and later put a per-app language row in the system's settings for
any app that declares its locales. The app declares English and Bangla now, in
`app/src/main/res/xml/locales_config.xml`, so a reader who goes looking for the
language where Android keeps every app's language finds this one. It is the
same one choice as the app's own Settings row, and `chosenLanguageTag` in
`:data` is the rule that keeps them one: the platform's list first, because it
is the door the reader touched last when they used that screen; the mirror the
app's row writes when the platform holds none; the phone's language for a fresh
install; and a tag the app carries no strings for is skipped rather than
obeyed. The write goes both ways: the app's row hands its answer to
`LocaleManager` on Android 13 and later, and a choice made in the system's row
reaches the translation, the tafsir, and the word meanings through the same
path the app's row has always used. Eight cases in `LanguageDoorTest` hold the
rule.

The material3 pin names 1.5.0 stable now. The predictive back work the plan
expected turned out to be unnecessary: Material3 1.5's `BottomSheet` registers
its own `PredictiveBackHandler` and binds the gesture's progress to the sheet's
translation, so a back swipe peels a sheet away under the finger and a
cancelled swipe puts it back. The app's own back handlers are only the ones
that move between pages inside a sheet, where nothing leaves the glass, and
`AppSheet` now says so. The BOM stays at 2026.09.00: the newest BOM carries
Compose 1.13.0, which requires compiling against android-37.1, and the app
compiles against android-37.0.

The 16 KB page rule was measured rather than assumed. Two native libraries
ship, `libandroidx.graphics.path.so` and `libdatastore_shared_counter.so`, and
all eight ABI copies carry a 2^14 LOAD alignment; `zipalign -c -P 16 4` reports
verification successful.

No content changed and no permission changed. No visible surface on the
reading, the card, the search sheet, or the settings page moved, so the three
store sets stand as captured for 21.2.

## 21.2

The day page's browns give way to old gold and warm gray.

The app's colors were audited end to end again, this time for the thing the
owner's eye kept catching: brown. Measured in OKLab, a brown is a dark
orange, and the day page carried three of them plus a sepia cast on
everything else. The ornament gold, drawn at every surah's arrival, every
verse's medallion, the juz labels, and the share card, was #7A5A12 at L* 40
and hue 82, a bronze. The ghunnah rule was #874306 at hue 54, a dark brown
word inside the verse. The beige tertiary container was never drawn. And the
word, the reading's ink, the quiet voice, the hairline, and the switch's
track all carried the paper's hue at b* 4.7 to 6.5, so a page meant to be
warm paper read as a sepia photograph.

The gold is #856E18 now, L* 47 in the gold sector, the lightest clear gold
that keeps a margin over 4.5:1: it measures 4.71:1 on the page and 4.95:1 on
a sheet, APCA 70.6 and 74.1. The ghunnah is #844800, hue 60, the farthest
amber the palette's own floors allow: the deuteranopia distance to the
qalaqah comes down from 0.030 to 0.027 against the 0.02 floor, and the
closest normal pair is now the silent gray at 0.096 against the 0.095 floor.
Both still hold on the machine, and the KDoc records where that headroom
went.

The neutrals keep the paper's hue and give up about half their chroma, by a
third at the word: the first light ink the audit proposed measured the
control edge at 2.99:1 against the 3:1 a boundary owes, and `ContrastTest`
caught it, so the word keeps the lightness it had (L* 15.3) and is not
lighter. The day's two undrawn Material containers align with the new
family, and the night's two do the same; the night's own khaki tertiary
container was the only brown in that scheme.

What does not move is the point: the page ground, the night family, the
action blue, the chosen and found grounds, and the six other tajweed families
all keep their tones. The night frame is pixel-identical to 21.1's on the
phone and the 10 inch.

The whole JVM suite, lint, the debug build, `checkdb` against the committed
database's SHA-256, and the search gate are green on this machine. A visible
surface changed, so the three store sets were captured again on the phone,
the 7 inch, and the 10 inch, each from a clean install. No content changed,
so every pack and the content database are as they were and no catalog hash
or tag moved, and no permission or app dependency moved.

## 21.1

The found word wears the reading's rounded mark.

The search sheet's matched runs were the app's only sharp rectangles. Two
mechanisms drew them: the Arabic match line painted one rectangle per line of
each range, and every prose snippet, the translation, the reader's note, the
word meaning, and the tafsir passage, still carried a background span, which
Compose always draws square. The same matched word was rounded a tap later,
when the result landed on the reading, because the reading's word mark, the
reciter's word, and the word sheet's sounding word were already drawn as one
rounded box per line.

There is one wash recipe now, and it is the reading's: a pad of 8 percent of
the ink box's height wider each side, half a pad trimmed above and below, and
a corner at 26 percent of that height. It lives in `ui-kit` beside the wash
that draws it, the reading's mark is its name for one range, and the search
sheet's Arabic lines and prose snippets draw the same box. The prose snippets
lose their background span and keep the Medium weight the matched run
carries, so the ink and the ground still name the same words, and the ranges
the two read are folded once, so they cannot disagree. A match past an
ellipsized snippet's cut washes nothing rather than a stray box at the
ellipsis.

The reading did not move: its box and its union were lifted verbatim, and the
visible-end clamp never triggers on a line that is not ellipsized. The
field's own selection keeps its separate 6 dp radius, and the tajweed color
painter keeps its glyph boxes untouched.

The whole JVM suite, lint, the debug build, `checkdb` and the committed
database's SHA-256, and the search gate are green on this machine. The
arithmetic is pinned by a new JVM suite, five tests, so the box and the
folding are measured without a device, and a two-test evidence class,
`SearchMatchCaptureTest`, photographs a matched word on both of the sheet's
paths. A visible surface changed, so the three store sets were captured again
on the phone, the 7 inch, and the 10 inch, each from a clean install. No
content changed, so every pack and the content database are as they were and
no catalog hash or tag moved, and no permission or app dependency moved.

## 21.0

The chosen and found grounds wear the accent, not the paper.

The app's colors were audited end to end: every ground, ink, wash,
ornament, and rule color, in both grounds, listed and measured. Four tones
were the odd ones out. The ground behind the chosen thing (the ayah whose
menu is open, the row Browse is standing on, the opened surah introduction,
and the word the word sheet names) and the ground behind the found thing (a
search match and the words a reader selects in a field) were warm paper
tones, beige and tan on the day page and brown at night. The night's found
ground was the most saturated brown on any interface surface in the app,
and the pair read as one hue family too many beside the page, the accent,
and the ornament.

19.1 had moved those grounds off the accent because a lapis tint at 8
percent over cream measured a neutral gray, and it chose the paper's own
warm family instead. The tone now is not that tint: it is opaque, in the
accent's own family, at the lightness the warm tones carried, with the
chroma that family had named (5 and 8 by day, 8 and 10 at night). On the
day page that is #E6EBF5 behind the chosen thing and #DAE2F1 behind the
found one; at night, #1E2832 and #2E3C49.

Because the lightness did not move, the pairs the contrast test measures
landed where they were: the reading ink reads 10.6 to 1 on the chosen
ground, the quiet word 5.8 to 1, the closest of the seven rule colors 5.4
to 1, and the night's pairs hold on the same footing. The floating paper's
edge over the chosen block improved with the change, from dE76 8.7 to 8.9
by day and from 5.9 to 11.9 at night, because the block no longer shares
the floating paper's warm hue. `ContrastTest` and `WashTest` pass unchanged.

The ornament gold is not a highlight and does not move: the juz and surah
names, the medallion's digits, and the share card's name keep it. The paper
family, the tajweed palette, and the never-drawn Material containers stay as
they are. The decision that moved is written where it lives, in the KDoc
tagged `owner decision, 21.0`, which records what 19.1 refused and why, and
`git log` holds the road between them.

The whole JVM suite, lint, and the debug build are green on this machine,
with `checkdb` reproducing the committed database's SHA-256 and the search
gate green. A visible surface changed, so the three store sets were
captured again on the phone, the 7 inch, and the 10 inch, each from a clean
install. No content changed, so every pack and the content database are as
they were and no catalog hash or tag moved, and no permission or app
dependency moved.

## 20.9

Three edges of the app's own machinery, closed.

An end-to-end audit of the app's network path, its storage, its SQL, its
intents, and its platform surface found no exploitable hole and no crash. The
downloader checks every redirect against one host family before the connection
that would use it is opened; every pack is hashed against the catalog inside
the signed app before it joins the library; every value that reaches SQL is
bound, and the content connection is read-only; every name that reaches the
disk passes one canonical-path gate; and every pending intent and receiver is
the app's own. Three edges were still worth closing. None of them was
reachable by a reader, and each is closed where the thing that decides it
lives.

The recitation manifest's parser accepted a row that named a package of no
bytes. The size is the transfer's own ceiling and the progress line's
denominator, and the word pronunciation manifest's parser already refused one.
The rule is one pure function now, beside the parser and not inside it, so a
machine without a screen pins it: a row must carry a `.zip` asset, a surah the
Book has, a real size, and a SHA-256, and a row that names no size is refused
rather than read as "size unknown".

A response that carried no length left a pack's or a recitation's total at
minus one, because the total was the header's length plus the offset the part
had already reached. Both call sites draw a non-positive total as an
indeterminate line, so a reader never saw it, but the arithmetic should not
produce a number it cannot mean: it answers the catalog's size, then the
header's, then zero.

The daily reminder arms each day under its own request code, and the reader's
local day number is the code. A clock or a time zone change re-arms the alarm
under the new numbering, and the alarm already armed under the old one kept
its slot: on a phone that traveled, a reminder could arrive twice on the day
of the move. The path that turns the reminder off swept three day slots; it
now sweeps five, and the path that arms one sweeps the same five before it
arms. A cancel of a slot that was never armed is a no-op, and the sweep comes
before the arm, so two applies that interleave still end with the later one's
arm standing.

The whole JVM suite, lint, the debug build, `checkdb` and the committed
database's SHA-256, and the search gate are green on this machine. The
reminder's arming changed, so `DailyReminderTest` and `DailyAyahToggleTest`
ran on `quran_phone` after a clean install: ten tests, zero failures. The full
app instrumented suite was skipped. No visible surface moved, so the three
store sets stand. No content changed, so every pack and the content database
are as they were and no catalog hash or tag moved, and no permission or app
dependency moved.

## 20.8

The prose states its floor, and the opened introduction wears one ink.

The app's colors were audited end to end and not one was replaced. Every ground,
ink, wash, ornament, and rule color was measured against the reading research
and held: the warm white page that is neither white nor parchment, the reading
ink held at 12:1 rather than the 14:1 a label wants, the night page lifted off
black and warmed so a pale letter cannot bloom on it, the one desaturated action
color, the ornament gold that is read and never tapped, and the seven tajweed
families in one contrast band with their color-vision floors. The palette was
already the right side of every trade; the research says so, and the new test
keeps it there.

Two findings did come out of the audit. The first is a defect. `PagePalette`
promises that the surah's opened introduction is the reading's ink at a step
back, and the night ground kept that promise while the day ground applied the
same alpha to the interface's word. One number landed two tones, and at night
the introduction measured Lc -56.7 and read weaker than the captions beside it
at -65.2, on the longest prose on its page beside the tafsir. One base and one
alpha on both grounds puts it at 84.3 on the day page and -64.3 at night: above
every caption on its own ground, and still a clear step under the verse.

The second is the rule that would have caught the first. `ContrastTest` now
carries APCA-W3 0.1.9 beside its WCAG ratios, its OKLab pair floors, and its
cone simulation, because the 2.x ratio cannot describe a dark ground: the
library's own documentation says it cannot be used to design one, and the night
pairs are where that matters. Five tests pin the day prose at Lc 90 and the
night's at -75, the quiet voice, the action color, and the ornament at 60, the
introduction on both grounds and on the block it opens over, and the found
ground the reading is scanned on rather than read on. Twenty-three pairs now
fail on the machine rather than on a reader's eyes.

Three pairs are recorded and deliberately not asserted, because each one is a
decision the app already took and lifting it would undo that decision: the night
rule colors, which 17.1 pulled off the top of the scale to stop the bloom a pale
saturated letter makes; the word mark's rule colors, which are that same band
drawn a step louder; and the night quiet voice on the found ground, where no
quiet word is ever drawn. The KDoc says why each stands where it does, so a
later session inherits the reasoning rather than re-deriving it.

Two sentences were also settled where they were only ever re-derived. A press
is not a fill: Material resolves a bare ripple to the ink of the thing pressed,
read from material3 1.5's bytecode, so the app's three ripple tones are one rule
and not three. And the accent's container is cool because it is the accent and
not a ground, which is what 19.1 meant when it retired the cold grounds and kept
the cold accent.

The opened introduction's ink moved one step, so the three store sets were
captured again on the phone, the 7 inch, and the 10 inch, each from a clean
install. No set moved: the introduction is not in a store frame, and the two
frames that differ do so at a text caret's blink and a status bar icon's
repaint. The full app instrumented suite was skipped.

## 20.7

The type audit keeps every face, and the Bangla ladder says what its digits do.

Every face the app draws was audited against its job: the KFGQPC Hafs face
for the Quran's own words, Amiri Quran for the display lines, Kitab for the
tafsir's Arabic prose, Literata and Inter for English, and the two Noto
Bengali faces for Bangla. No face moved. The Quran's face is the one QUL
distributes and the one Quran.com draws with; Amiri Quran is final at 1.003;
Kitab is the classical naskh for long Arabic prose; Literata was commissioned
for continuous reading on screens; Inter is the screen interface voice whose
tabular figures the value rails were built on; and Noto Serif Bengali 3.000 is
the 2025 Universal Thirst redesign, with Noto Sans Bengali 3.011 its interface
pair. Inter 4.1 exists upstream with Cyrillic and symbol fixes and small
kerning changes; the app stays on 4.001, and the next time Inter matters is
the time to take it.

One finding is now written beside the code it decided. The Bangla value styles
ask for tabular figures, and neither Noto Bengali file carries a `tnum`
feature. The ask stays, because the ladder asks it by job and the face decides
what it can draw: the sans face's digits are uniform by design, so a Bangla
count, size, or reference cannot jitter, and the serif's digits appear only in
static text, so no number rail ever meets them. The font gate's allow-list
also says what draws Bangla content now, the bundled Noto Serif Bengali rather
than the platform's own face.

No screen changed, so the three store sets stand, and no instrumented leg ran.

## 20.6

The page reads four translations, the reminder reads one, and the library
attaches only what a screen is showing.

A reader who keeps several translations, tafsirs, word lists, and voices
could meet a missing pack without ever removing one: the app attached every
pack on the phone to one SQLite connection, and that connection holds ten.
The pack past the tenth was left out, and the daily reminder, which opened
the library without naming a reciter, could lose the translation it was
about to show. The library now attaches the packs the screen reads and
nothing else, and the reminder asks for its one translation alone.

A reading holds four translation columns. A fifth installed translation
keeps its file and its row, says why it waits, and takes the place of one
turned off; four is what leaves the page's words, its root, its voice, and
its tafsir cards inside the same ten.

The app's own instrumented suite is honest again. Three tests had drifted
from decisions the app had already made: a size in decimal megabytes, the
ayah door's floor, and the question before a removed voice. The notification
test now starts from a clear shade and names the marks it saw instead of
their resource numbers.

No reader sees the rest: Gradle moves to 9.9.2, which refuses a version
catalog's text as code; the gates job keeps its own 45 minutes; CI retries
a rate-limited registry instead of failing the run; the tools name a missing
source instead of dying on a null; and the prose keeps one dialect, with the
attributes file naming the release that made its decision.

## 20.5

A failure says Retry, a download says Downloading, and every decision in the
code names the release that made it.

"Try again" stood on two surfaces, the content error page and the word sheet's
places page, while every other retry in the app said Retry. One act has one
word now. The word pronunciation transfer said "Getting" where every other
transfer says Downloading, and it says Downloading too: a pack is added and a
sound is downloaded, and that rule now holds without an exception.

The app's own record of why it is the way it is was dated by working session
in a hundred and fifty places: "this session", "an earlier session",
"the theme and consistency session". A session is a fact about the person who
wrote the line; a release is a fact about the app. Each of the hundred and
fifty now names the version that shipped it, and a new test fails the
build if a session date appears again. The notification's mark is named for
what it draws, the touch floor and the press circle come from the theme at
every call site, and the one haptic in the app now says why it is the only one.

The site said Al-Fatiha shipped in two voices. It ships in all four.

## 20.4

Choosing another voice on the playing pill no longer offers a download the
device already holds.

On Al-Fatiha the fault was plain: all four reciters' timing packs ship inside
the app, and the surah's audio travels in its assets for all four, so there
was nothing to fetch for any voice. Yet every switch offered a package, and
the tap that answered it played at once, because the download was a file that
was already there. It came back on every repeat, and for every voice,
including the one that had just been left.

The price a voice is offered at was read from the library's attached packs,
and since 20.0 only the chosen voice's timings are attached, for the one
connection's sake: a voice that is not being heard keeps its rows on disk and
its schema out of the library. That rule is right and the price was wrong, so
the price now asks the device. `packOnDevice` answers what is here, the
listening offer reads it, and the attach guard is untouched: every query still
runs only on a schema that exists.

The pill's own chooser never reopened the library over the voice it had just
chosen, so it priced against the previous voice's rows. It now takes the same
door the settings page takes, which also makes the switch itself honest: with
the timings already here, choosing a voice plays at once with no offer at all.
An offer still stands where a surah's audio genuinely is not on the device,
and it now names the audio's size alone.

Two legs hold it: a voice that is not attached still reads as a pack on the
device, and the reader's own round, an ayah playing in Husary while another
voice is chosen on the pill, ends with no offer on the glass. Both were red
before the fix and green after it. The unit tests, lint, and the debug build
are green on this machine, and the two legs ran on the phone with the attach
guard's own class beside them.

No committed store frame carries the listening panel's voice rows, so the
three store sets stand and no capture leg ran. No content changed, so every
pack rebuilt byte for byte and every catalog hash and tag is unchanged, and no
permission or dependency moved.

## 20.3

The Bangla interface was read word by word, against the English that sits
beside it in each module's `values-bn`, and it now says what it means.

A note is নোট and only a translation's footnote is টীকা, so the ayah door no
longer calls the reader's own writing a commentary, which is the drift a gate
was added to stop and the one place it had survived. Two instructions named a
Play button the app does not have; they now name the শুনুন the reader sees, and
the tap they ask for is the চাপুন used everywhere else, so প্লে and ট্যাপ have
left the app. The recovery message says কিছুই হারিয়ে যায়নি rather than
হারায়নি, which put "nothing" in the losing seat of the sentence. And "the
daily ayah cannot arrive" is আসবে না in all four places that say it, the three
settings sentences having already chosen that word.

Search had two words for one act, অনুসন্ধান on the door and hint and খোঁজা inside
the sheet, and now has one: খুঁজুন, খোঁজা হচ্ছে, যেখানে খুঁজবেন. The parity gate
allowed exactly one phrase to be read two ways across modules, that hint and the
long search shortcut, and with the app saying one word the exception is deleted:
a literal two modules share must now read the same in Bangla with no list of
exceptions beside it.

Smaller corrections, each one word: the Set button says নির্ধারণ করুন, because
ঠিক করুন reads as repair; the Font size row is লেখার আকার, since সাইজ was the
one hybrid in a page whose rows are all লেখা; the tafsir offer carries the
English's depth; and three grammar labels spell their Arabic the way the corpus
declares it (রদ for حرف ردع, ফাজাআ for حرف فجاءة, ইসমে ফিল for اسم فعل). House
spellings were settled where the app contradicted itself: এখনও, যেকোনো, and the
plural suffix -গুলো.

In the catalog, the word roots pack has the Bangla name it lacked, শব্দের মূল,
so a Bangla reader who adds roots no longer meets an English title on the About
page, and the pronunciation pack's credit says লাইব্রেরি থেকে like the other
eight packs that render the same English "via the Quranic Universal Library".
Both moved through the pipeline's registry rather than the catalog file: the
packs rebuilt byte for byte and every hash and tag is unchanged, so the only
edit in `content/catalog.json` is those two Bangla fields.

The English interface was not touched, so no English frame moved and the three
store sets Play has stand. The unit tests and lint are green, the Bangla capture
leg photographed the reading and the settings hub on the phone, and the class
that owns the time picker ran there too, since a button's word is the one thing
in this release that grew.

## 20.2

The app's content is a pack, and it now treats it that way everywhere. Every
pack's display name, in English and in Bangla, and the translation, the tafsir,
and the word list each interface language reads, come from
`content/catalog.json` instead of from tables inside the app. Adding,
replacing, or retiring a translation, a tafsir, a reciter, a word list, or a
word pronunciation set is now a row in the content pipeline's registry, a
build step, and a release: no app code names a pack.

Nothing on the glass moved. The English and Bangla names are the same
sentences in the same places, the reciters are still named by the short names
the pill always drew, and the catalog's name for the word lists now reads "Word
meanings", the sentence the settings rows already showed.

A new gate keeps it that way. `NoPackIdLiteralsTest` fails the build when a
pack id is written outside the content layer, so the next feature cannot
quietly reach for one translation by name. That layer's own vocabulary is the
core pack's constant, the convention builders for a reciter's and a word
list's id, and the wire names a pack type round-trips through.

The pipeline has one door. `tools/PackSources.kt` is the single registry of
packs, holding each pack's datasets, its two names, its credit, its version,
and whether the app ships it; the catalog, the credits page, and the released
files all read it, and the language defaults are a table beside it whose ids
the build checks. `tools help` prints the runbook for each pack type, so a new
voice or a new translation has an order to follow rather than a hunt.

The gates are the unit tests, lint, and `assembleDebug`, green here and in
`build.yml`. Nothing the reader sees changed, so no instrumented class ran and
no capture leg did, and the three store sets Play has stand. Every pack
database rebuilt byte for byte and every catalog hash is unchanged.

## 20.1

The app's colors were audited end to end, and the audit moved no tone. Every
ground, ink, hairline, ornament, accent, wash, and tajweed family was read
against the research and against its own measured floors, and each one stands:
the warm day page, the lifted night ground, the mint and coral and honey of the
rule colors, the one lapis accent, and the gold that never answers a tap. Two
strays were not tones at all but shares no scale names, and both are gone.

The unanswered choice ring, the empty circle on a row that has not been kept,
wore sixty percent of the accent, a number declared nowhere and measured only by
accident. It now wears the ground's quiet voice, the same tone the off switch's
knob wears, so every unanswered control in the app is one color and the chosen
one differs by hue and lightness rather than by a share of the accent. Both
grounds already hold that pair, and the contrast test keeps it.

The end of a surah closes in its own words. The faint gray rule that stood over
the completion line was a hairline at seventy percent, too faint to read as a
mark and too quiet to close anything the line does not say, and a gold closing
mark was weighed and set aside: the turn stays the quieter breath, the
completion line and the next surah's card alone. Surah 114 now ends as every
other one does, with nothing left open.

The tajweed group's own note told a wider story than the print does. No mapping
is universal across publishers, and the app's anchors follow the subcontinent's
thirteen-line convention that its own readers learned; the two places the page
differs are held by the tests that forced them, and the note now says so, so a
later session cannot quietly walk the hues toward another house's legend.

The gates are the unit tests, lint, and `assembleDebug`, green here and in
`build.yml`. The three store capture legs ran on the three AVDs, and the eight
frames of each were committed. The Bangla leg was skipped, because no Bangla
string changed. Content is unchanged from 20.0 at 1.9.0, and no color value
moved. Version 20.1 (versionCode 203).
## 20.0

Two more voices join the recitation, Mishari Rashid al-Afasy and Abdul Basit
Abdul Samad, both murattal, so the reader chooses from four: Minshawi, Husary,
Alafasy, and Abdul Basit. Their word timings ship in the app beside the other
two voices', Al-Fatiha's audio is bundled in every voice so the first Play on
1:1 needs no network whichever voice the reader chose, and every other surah
still downloads one voice's package at a time from the project's own Releases.
Both voices' per-surah packages are published as their own releases, and the
voice edge table, the word timing pins, and the recitation manifest all cover
four voices now.

The voice the reader chose is the one whose timing pack is attached beside the
core. SQLite allows ten attached databases, and four voices are four packs: a
reader who holds both tafsirs, both word lists, and the root could not have
held a fourth voice too, and the capture leg met the ceiling as the word list
reading as not installed while the reading still drew. The parent commit ran
the same leg green, which is what named the cause. The fix is the one attached
voice: the core pack now carries a recitation map, four rows naming every
voice's audio folder, so any voice's files can be named without attaching that
voice's pack, and the voices the reader did not choose keep their files and
their listing while their segments wait for their turn. The core pack moves to
version 7 for the map, and choosing a voice reopens the library over it before
the ayah that is playing is asked for again.

The one recording QUL numbered a word short, Abdul Basit's Al-Mujadila 22, gets
its closing word from the measured tail: the span between the export's own
last region and the last voiced millisecond the audio holds, the same 81,140
the voice edge table carries, and a test pins the two to agreement so the
number can never drift from the audio it was measured from.

The gates are the unit tests, lint, and `assembleDebug`, green here and in
`build.yml`. A change that touches the player ran the affected classes on the
phone: `BundledFatihaTest`, `FatihaPlayTest`, and `PlaybackServiceTest`, green,
with the bundled Fatiha pinned for all four voices and the chosen voice pinned
as the only one attached. The three store capture legs ran on the three AVDs,
and the eight frames of each, captured by the green leg, were committed; the
Bangla leg photographed the Bangla reading and settings after the interface
gained the two voices' names. Content is 1.9.0: the database
`content-db-9f808fdf`, the core pack `pack-core-45b53fe7` at version 7, the
reciter packs `pack-reciter-minshawi-af264024`,
`pack-reciter-husary-9b09e6cb`, `pack-reciter-alafasy-0cc69f8b`, and
`pack-reciter-abdulbasit-e25ca017`; the word packs, the root, and the word
pronunciation pack are unchanged. Version 20.0 (versionCode 202).

## 19.9

The reading stops paying for the screen when nobody is reading. A sheet, an
ayah card, a note, or a confirm used to hold the screen on for as long as it
stood, so a phone left on Settings stayed lit until the battery died; the
raised control now gets five minutes, and the page gets five minutes after the
last touch instead of ten. The note and the search field renew the window as
the reader types, so a long note or query does not go dark under the hands that
are writing it. The listening window is unchanged at a minute, because the
recitation is a foreground service and the page is not needed to hear it.

A reading that is covered now keeps still. The wash, the reciter's follow, and
the ayah's mark are gated on the window being started and the page being the
glass, so a sheet over the page no longer recomposes a hidden ayah at every
word; the player takes the place checkpoint instead. The checkpoint is thirty
seconds rather than ten, and the place is written once when the page goes out
of sight and once when the reader closes the window, so the writes fall to a
third without moving the resume out of its ayah.

The wash itself moved out of composition. An ayah's line was rebuilt, its word
ranges and medallion with it, at every word the reciter reached; the line is
built once now and a word-position map gives the wash its range, read in the
draw. A word boundary redraws one line instead of rebuilding its
AnnotatedString, and the painter keeps the geometry it already had.

Two speculative reads moved to where they are wanted. The tafsir index, 18 MB
of folded prose, was built on every launch for a search the reader might not
make; it is built when the Search sheet opens, where the field is already empty
and the sheet says it is searching. The surah after this one was read when this
one opened, for a turn the reader might not take; it is read when the last
visible ayah is within twelve of the end, so the turn still lands on text.

The gates are the unit tests, lint, and `assembleDebug`, green here; the same
suite runs in `build.yml`. The instrumented suites were skipped: this release
changes when the screen sleeps, when the page recomposes, and when two
speculative reads start, not a surface the tests drive. No visible surface
changed, so the three committed store sets stand. Content is 1.8.1 and
unchanged. Version 19.9 (versionCode 201).

## 19.8

The reading does less work while it moves. The bar's travel was a
number the whole screen read, so every scroll delta rebuilt the page and every
ayah on the glass; it is Compose state now, read by the bar in its own layout
pass, so a drag re-lays one band out while the page stands still. A settle no
longer rebuilds the bar at each frame either: the two things a rebuild is owed
for, the bar going away for good and the tag a test finds it by, are derived.
The screen's own derived answers, whether a translation is on, whether the word
list is on the device, the next surah's name, and the ayahs the reader wrote a
note on, are remembered against what they are made of instead of walking the
reader's saves on every rebuild. The view model is marked stable, so the
callbacks the reading hands down can be remembered rather than rebuilt.

Each verse's tajweed colors are measured once per shaped line and kept. The
geometry of every color family was rebuilt on every redraw, which is a
`getBoundingBox` for every character of the verse, and the wash that moves while
the reciter reads was the loudest case. `TajweedPaint` and `TajweedPainter`
split that geometry from the painting, and a block holds one painter, so a
redraw that changes no word reuses the boxes it already has.

The block, the Arabic line, and the doorway write their own place on the glass
only when it has changed; a lazy item is keyed by the ayah's own number instead
of a string built per item per composition; and the theme builds its reading
voice per language and its ladder per language and type scale, so a rebuild
hands no surface a new `Typography`.

Long-press selection is gone from the translation line and from the tafsir
passage in the ayah card. Every translation line of every ayah on the glass
carried the platform's own selection machinery, created as the ayah arrived and
thrown away as it left, with a finger watcher for the long press that opened it.
The Quran's own line was never selectable, so the feature copied the English or
the Bangla sentence and never the verse. `SelectionClearer`, both
`DisableSelection` wrappers, and the per-line provider go with it, and Share is
the one door that leaves the app with the whole verse. The word-by-word long
press stays: it is a gesture, not a selection. The contrast check that guarded
the selection mark stays too, pointed at the reciter's word wash and the search
arrival, which draw the same tone behind the Quran's letters.

The gates are the unit tests, lint, and `assembleDebug`, green here and in
`build.yml`. The instrumented suites are a signal: 12 classes and 32 tests ran
on `quran_phone`, 31 green, and both red legs are red at the parent commit as
well, so neither is this change. The three store capture legs ran on the three
AVDs, and the eight frames of each were read and committed. Version 19.8
(versionCode 200).

## 19.7

Search now meets the reader's own spelling. The exact index still answers first, and it is never widened: a query the print already holds returns exactly what it returned before. Only when it names no ayah at all does the recall index answer, over a new column that removes the long vowels and the hamza seat from both sides. الصلاة, الشيطان, القرآن and إبراهيم now reach their verses; الرحمن keeps its exact 45. The tools search gate has seven recall probes beside the exact ones, and `ContentSearchTest` runs 24 tests on the phone, including the recall and a reference that is now the only answer. A typed reference no longer drags the tafsir sources beside it, so `kahf 10` is 18:10 and nothing else.

The recitation download no longer deletes the surahs already here. The commit moves one surah's files into the reciter's shared folder, and `RecitationCommit` has three JVM tests: a second surah joins the first, a repeated surah replaces only its own files, and an empty package moves nothing. A partial commit removes only what it moved; the whole-surah check refuses a half surah and the next download repairs it.

The daily reminder card is offered once and only while the reminder is off. Setting the reminder in Settings writes the answer, so the card does not return; "Not now" dismisses the offer and never turns off a reminder set elsewhere. `AppSettings.reminderOffer` and three tests pin the state.

The five English surah names QUL spelled against the common form are reviewed: Al-Anbiya, Ash-Shura, Al-Mujadilah, As-Saff, Ad-Duha. The old spellings still find their surahs, and `checkdb` pins the five. The introductions' internal references now open on a tap through `ReferenceLink`; the tafsir's printed `(9)When` draws as `(9) When`; a Bengali gloss with no letter is not a meaning; and the surah introduction prefers the reader's language's pack before the core's English.

The first frame follows the interface language on an RTL phone, so a fresh install on an Arabic phone is LTR English rather than a mirrored English page. The Last Read database no longer drops its table on any version bump. Download sizes are decimal MB, the number a carrier reports: Al-Baqarah's recitation reads 186 MB. A reference outside the Book says so, the download queues require a usable unmetered connection, and the launch re-arm runs on the window's lifecycle scope with the application context.

Content is 1.8.1: database `1f7c5339`, the core pack `d7797f77` (version 6), and `meta.words` is 77,429; the word packs, root pack, recitations, translations, and tafsirs are unchanged, and the new content database and core pack are published. The About page now states each source's licensing status as the source gives it; the tafsir and recitation rights remain the sources' own and unverified.

The gates are the unit tests, lint, and `assembleDebug`, green on this machine; the same suite runs in `build.yml`. The three store sets were recaptured, five tests on each form factor and green, and the eight frames of each were read. Version 19.7 (versionCode 199).

## 19.6

The app's own prose is one dialect. The comments and the KDoc were
written on more than one machine over many releases, and 61 words had drifted
into British spellings, `centre`, `grey`, `behaviour`, and `labelled` among them,
against the one style rule the repository states; they are US English now,
identifiers included.

The decisions themselves were dated by the phrase "this session", 862 times. A
reader a year from now could not tell which session it was, so could not find
the commit, which is the record this file leans on. Every one of them now
carries the release that shipped it, `owner decision, 6.6`, `owner report, 15.0`,
`owner decision, 19.3`, taken from the commit that wrote the line, and the rule
is written down.

Two gates hold what the tree now says. `CopyParityTest` reads every module's
`strings.xml` and its `values-bn` and fails when a translatable sentence has no
Bangla twin, when a Bangla sentence names nothing, or when one English sentence
reads two ways in Bangla: a saved ayah's note was `নোট` in the reading and
`টীকা` in search, which is the word for a commentary. `DeviceTestNamesTest` fails
the build over a backticked test name under `src/androidTest`, because the
Android runtime cannot hold a space in a method name: such a test compiles,
installs, and then the runner cannot call it.

The reading's own surface is the same pixels. The question before a removal was
drawn twice, once in the reading and once in Settings, and the second copy
described itself as the first one's shape; it is one `ConfirmSheet` now, with the
same two answers and the same tags the tests read. `ReadingList` took 51
parameters, in which a value that belongs to the reading stood beside a callback
that belongs to the screen; what it shows is `ReadingListState` and what it may
do is `ReadingListActions`. The pill's word gap and its gutter, the three rows'
bottom air, the search field's end, the time page's gutters, and the air of the
add door and the two surah frames are the theme's own numbers now or are named
where they are drawn. `SearchHistory` is `SearchHistoryStore`, the sixth store,
and the last two files without a KDoc have one. One sentence that ends in a
period now does: the share sheet's failure line had lost it, in both languages.

Every settled decision in this release is tagged `owner decision, 19.6`, and
nothing about the Quran changed: the text, both translations, both tafsirs, the
recitations, the word lists, the roots, and the content database are 1.8.0, and
the word pronunciation pack is 1.0. Version 19.6 (198).

## 19.5

The ayah's number is a door the reader can see. The reference at the head of a verse wore the action color and the app's one turn, and a turn that ends a line of small type is the same cue the pill's doors carry, so it taught only the reader who had already met the pill, and the first control a reader needs was borrowing its lesson from a feature they may never have opened. The number now wears the app's quiet pill, the reference's own line plus 8 dp above and below, filled with the quiet tone the text buttons wear, and it holds the accent's chosen ground while its menu is open, so the door reads as a control before the tap and as a chosen control after it. Every verse also closes with the medallion the QPC face draws, on a page that carries a translation as well as on the page that draws the Quran alone: the same verse ended two ways depending on a setting, and the difference was the print's own roundel. On the page that draws the Quran alone the medallion is still the verse's number and its door, so that page keeps its rhythm and its tap. `AyahDoorShapeTest`'s bounds are unchanged, and the full JVM suite, lint, and `assembleDebug` are green here in 9m29s. The store's phone, 7 inch, and 10 inch sets were recaptured on the three AVDs, 5 tests each and green, and the eight frames of each were read and committed. The Quran text, both translations, both tafsirs, the recitations, the word lists, the roots, and the content database are unchanged at 1.8.0. The word pronunciation pack is 1.0. Version 19.5 (197).

## 19.4

The bar is the reading's head. It is a 48 dp row with 4 dp above and below it, and the reading begins at its bottom, so no verse is ever behind it. The reader's first scroll is spent moving the bar, not the page; once the bar is away, the page scrolls. A backward scroll brings the bar back before the page moves, and the line the reader is returning to arrives below it. A released bar finishes the nearer end in 200 ms, or at once when the phone asks motion to be reduced. A tap on the page shows the bar, and it stands on every arrival, when a sheet closes, and when the reader comes back to the app. The 19.1 sentence and `barTaught` are gone with the tap they taught; the bar itself is the sign. The bar's arithmetic is pinned by `ReaderBarTest`; the full JVM suite, lint, and `assembleDebug` are green. The store's phone, 7 inch, and 10 inch sets were recaptured, and the store rule now names all three every time. The full app instrumented suite was not run. The Quran text, both translations, both tafsirs, the recitations, the word lists, the roots, and the content database are unchanged at 1.8.0. The word pronunciation pack is 1.0. Version 19.4 (196).

## 19.3

A raised sentence now leaves on the reader's first touch. "Note
saved", "Note cleared", a removed save with its Undo, a failed share, and the
permission notice were the app's one raised thing that ignored a touch: they
waited out Material's timer while the reader tapped an ayah under them, and the
reading's own page tap already put away the offer and the paused recitation
beside them. One rule now lives in `ui-kit` beside the sentence: a touch that
is not the paper puts it away and still reaches whatever it landed on, the paper
is spared so the touch can never take the pill's Undo, and the timer is the
backstop. The reading and Browse share one holder, so a third surface cannot
forget it, and the paper's own dismiss is Material's, spoken by the platform in
the reader's language. The last two sentences the app said in an Android bubble
are gone with it: a phone with no settings screen keeps its line on the Daily
ayah page and loses the door, and the reading says it once where the door was
tapped, because a fact about the phone is state and not a moment. The Quran
text, both translations, both tafsirs, the recitations, the word lists, the
roots, and the content database are unchanged at 1.8.0. The word pronunciation
pack is 1.0. The store's eight frames stand: the sentence draws as it did, and
only its dismissal changed. Version 19.3 (195).

## 19.2

The recitation pill's key is the verse's own number again. Since
14.2 it printed the ayah's running number, the Book's count from 1 to 6,236
that the content database and the reading's place are keyed by: 16:28 read
16:1929, and the listening panel's spoken place said "Ayah 1929". The
builder already held the verse's own number beside the running one, so the fix
is one small table filled when the surah is built and one pure function,
`PlayingPlace`, where the two numbers meet; it is pinned with 16:28 (running
1929) and 2:255 (running 262). The running number stays where it belongs: the
database key, the recitation rows and their word timings, the saved and
last-read stores, and the reading's position. The Quran text, both translations,
both tafsirs, the recitations, the word lists, the roots, and the content
database are unchanged at 1.8.0. The word pronunciation pack is 1.0. The store's
eight frames stand: the playing pill's key is in none of them. Version 19.2
(194).

## 19.1

The verse's number stands on its verse. The head keeps its 48 dp
target, but its content sits on the target's floor, so the room inside the box
falls above the number and the head hugs the line it names instead of floating in
the gap between two verses. One number per verse follows: the printed medallion
closes a verse only on a page that draws the Quran alone, so a page with a
translation carries the reference at its head as the one number, and that number
is the door. The chosen verse and a found word wear the page's own warm tone now
instead of a tint of the accent, which measured a cold gray over cream for the
chosen block and a real blue, hue 252, for a search match: the two surfaces that
say "this one" were the only cold things in a warm app. `PaperSelected` and
`PaperMatch` hold the lightness of the tints they replace, so every pair measured
before still holds, and `Wash` keeps only the three fills that really are a tint.

The Tajweed page explains itself in one line, ends a rule's name with a period
instead of a middot, draws its rules at 13, and puts a family's rules 12 dp under
their head against 24 dp between families. Word meanings are offered where the
reading arrives, the menu's Tafsir row lost a parameter that was always true, and
the first surah a reader opens carries one sentence naming the tap that brings the
bar, retired by the first tap. The share sheet's Share image is the solid answer,
a place left more than four weeks ago is named by its date, a count carries its
noun, a download of 20 MB or more asks before it goes, and the theme swatch's
press is drawn on the circle rather than on the box around it. The store set is
eight surfaces rather than four photographs of one verse, captured in one pass
with the same chrome on every frame.

The Quran text, both translations, both tafsirs, the recitations, the word lists,
the roots, and the content database are unchanged at 1.8.0. The word pronunciation
pack is 1.0. Version 19.1 (193).

## 19.0

A place on the word sheet's list is a door out of the sheet, and
the phone's Back now gives the sheet back. The root's count and the dictionary
form's count open a list of places, and a tap on one puts the sheet away and
moves the reading to that ayah; Back from there returns the sheet on the page
and at the row the reader chose, opened at their place and marking it, because
the reading stands there now. It was not: a place was the app's one arrival
with no way back, so Back from the ayah the reader had been sent to closed the
app, which is what the search results, Browse, and the settings pages already
did not do. The sheet's head names the word's own surah, never the one on the
glass, since a sheet that Back gave back may stand over a surah the reading has
since left. The word pronunciation manifest is marked so a checkout can never
rewrite its endings: with a CRLF copy its byte count and its checksum both
moved behind the content gate, which is the one thing that file's two numbers
must not do. The Quran text, both translations, both tafsirs, the recitations,
the word lists, the roots, and the content database are unchanged at 1.8.0. The
word pronunciation pack is 1.0. The store's eight phone frames stand.
Version 19.0 (192).

## 18.9

The word sheet's counts are doors. The root's count and the
dictionary form's count open a places page inside the same sheet: every place
grouped by surah, the word as the page writes it, the reader's own place washed
and named, and a tap that puts the sheet away, moves the reading to that ayah,
and washes the word. The page is lazy and keeps the sheet's drag gate, and both
its back mark and the phone's Back return to the word. The root's count is the
pack's own rows now, not the source's field, so the count and the list are one
number: the fourteen roots that disagreed were corrected, and a count of one
says one word and one form. The word packs are QUL 3 and the root pack QUL 2,
carrying the lemma and root indexes the page reads. Content stays 1.8.0 and the
database rebuilds to the same SHA-256; the morphology source moves to
Quranpedia's 2026-11-14 export, and the build restores the nine rows at 70:1 and
78:30 from the export's own previous fuller labels. The store's eight phone
frames stand. Version 18.9 (191).

## 18.8

The Font size page gives each name its own line and the rail the page's width under it, so the two ends that say which way the scale runs stay at every text size; the fixed name column and the rule that could drop the marks at a large text size are gone. The words page's switch is named **Word meanings and pronunciation**, the long press is taught in a quiet line under it, and the one list is a file row with Add or Remove and no choice mark. Downloaded pronunciation is a door that counts the surahs and lists each one with its own Remove, the reciter page's own shape; the page no longer offers one Remove for all. Every pair of buttons puts the quiet word first and the filled answer last, so the reminder card, the tafsir offer, the share sheet, and the download band read the same order as the time page and the note sheet. About keeps its one page and its one door, as 6.0 left it. The Quran text, both translations, both tafsirs, the recitations, the word lists, the roots, and the content database are unchanged at 1.8.0. The word pronunciation pack is 1.0. Version 18.8 (190).

## 18.7

The word sheet's speaker stands on every word, and the press is the ask. A word whose file is on the device sounds; a word whose surah is not here yet fetches that surah on whatever connection the reader is on, counts the transfer under the meaning, and plays when the pack lands; a word the recitation runs together says it has no recording of its own; and a transfer that fails says so and asks for another press. The speaker used to be a snapshot taken when the surah's rows were built, so a pack that arrived while the reader stayed in the surah never reached the sheet: that is gone by construction, because the question is asked at the press. The Settings page keeps its name and its switch, loses the sentence that was cut after two lines and the Book-wide size line, and gains one row for the voices on the device with the one act they allow, Remove. Removing the word list no longer deletes downloaded pronunciation. The Quran text, both translations, both tafsirs, the recitations, the word lists, the roots, and the content database are unchanged at 1.8.0. The word pronunciation pack is 1.0. Version 18.7 (189).

## 18.6

The word meaning sheet's speaker becomes a recording of that one word. The app no longer cuts a word out of the verse being recited: the speaker plays a file made for the word, cut from Mishari Al-Afasy's recitation at quran-align's word boundaries, and stops at its end; a recitation that was playing waits at the word and is given back. The Settings page is now **Word meanings and pronunciation**. Its one switch fetches the meanings, the roots, and the pronunciation for the surah being read; the surah of the tap arrives on the network the reader is on, a later surah waits for Wi-Fi, and Al-Fatiha's 29 words ship in the app. The pack is 352 MB for the Book, one surah at a time; a word the recitation runs together has no speaker. The Quran text, both translations, both tafsirs, the recitations, the word lists, the roots, and the content database are unchanged at 1.8.0. The word pronunciation pack is 1.0. Version 18.6 (188).

## 18.2

The word meaning sheet's speaker no longer loses the top of its press. The head stays inside the sheet's scroll, as every sheet's head does, and the scroll gives its 48 dp action target the room it reaches above the name; the note sheet's Clear and Save get the same room. The word and gloss stay centered. The Arabic word, root, and dictionary form are trimmed to their ink, so the gaps are the app's 16, 8, 24, 4, and 16 instead of the reading's 2.0 line box, and the first fact opens a block. The facts keep one label voice, one Arabic voice, and one Latin voice: the grammar phrase is 13, and the root's transliteration sits on the Arabic's baseline. Content is unchanged at 1.8.0; version 18.2 (184).

## 18.1

The word sheet reads as one page. Its name and the ayah's place stand
4 dp apart, the footnote sheet's own gap, because a sheet's head is now measured by
its name: the speaker's 48 dp target reaches outside that line instead of making the
head taller, and the note sheet's Clear and Save hang from its head's line the same
way. The word and its gloss stay centered, and the three facts under them read as
facts: Root, Dictionary form, and Grammar, each a label with its value 4 dp under it
and its count under that, on the page's left edge, in both languages. The facts'
Arabic is 20 of the Quran's 26, one step under the reading's inline stand, so the
root no longer outshouts the gloss it belongs to. The counts say what they count: a
root's words in the Quran, a dictionary form's words and forms. English says
Dictionary form where it said Lemma, as Bangla already did, and the grammar's parts
join on the app's middot. The Quran text, both translations, both tafsirs, the word
lists, the root pack, the audio, and the database are unchanged; content stays
1.8.0. Version 18.1 (183).

## 18.0

The word meaning sheet studies the word. Under its root it names how many
words in the Book speak that root, then the word's dictionary form with the Quran's own
two counts for it, then the grammar: what the word is, its case or mood, its gender and
number, its voice, and a verb's pattern. The lines stand directly under the root, as the
root does, and only when the word pack carries them, so a pack held from before draws the
sheet it always drew. The word's own voice is a real clip now: one media item whose end is
the word's end, on its own player, so no stop is watched at a millisecond, nothing plays on
past the word, and a recitation that was playing waits at the word and is given back when it
ends. Content 1.8.0 adds the grammar and the dictionary forms (77,429 words, 4,763 lemmas)
to the word packs, from the Quranic Arabic Corpus via Quranpedia; the Quran text, both
translations, both tafsirs, the root pack, and the audio are unchanged.

## 17.9

The word meaning sheet's speaker is a sample, not a session. A word's pronunciation no longer raises the playback capsule, so nothing is left paused behind the sheet or in the shade after the word ends; the sheet washes the word while it sounds, and the saved listening place is untouched. A recitation the reader started still has its pill. The Quran text, both translations, both tafsirs, the word lists, the root pack, and the audio are unchanged; content stays 1.7.0.

## 17.8

The word root reaches the readers who already had word meanings on. The root pack was published with 17.7, but the app only asked for it while installing the word list itself, so a list already on the phone never requested it. It now joins the queue once and arrives on Wi-Fi after the first page, and turning word meanings off and on fetches it at once. The word sheet's speaker keeps to its word: the stop is sent with the media item it belongs to, so the service no longer takes it off when the word's own playback starts. The Quran text, both translations, both tafsirs, the word lists, the root pack, and the audio are unchanged; content stays 1.7.0.

## 17.7

Four small things the reading had wrong are right. A voice chosen on the playing pill is heard from the ayah being heard and not from the one Play was pressed on: the request was a snapshot, so a voice picked three ayahs in began again at that first ayah, and after the audio had carried on into the next surah it took the reader out of it. A saved ayah no longer wears the bookmark beside its number. The mark answered no touch and it was the third place for one fact, so the keep is said where the tap that makes it lives, on the menu row that reads Saved while the ayah is kept, and the verse still speaks it to a screen reader, the one reader who cannot see a lit row. The word sheet names the word's root, the letters and their reading on the English page, and it offers a speaker that says that one word in the reader's reciter and stops at its end, drawn in the set's own hand and standing at the head of the sheet beside the title. Content 1.7.0 adds the roots of 50,298 words; the Quran text, both translations, both tafsirs, and the audio are unchanged. A transliteration line was built and is not shipped: the Bangla one was not right, and one language alone is not the two this app offers.

## 17.6

The page stands on its own edge, 20 dp instead of 28. One number moved and every surface followed, because the reading, the search sheet, the ayah card with its tafsir, the footnote and the word sheet, the note, share, Browse, and settings all read the app's one gutter. The platform's own compact margin is 16, so 28 had every phone running 12 dp inside its own paper at every edge, and at the shipped translation size a line on the 411 dp phone held about 44 characters, under the 45 to 75 a line of reading is drawn for. The width of a page did not move with it: a wide glass still holds the same 600 dp measure, so the ten inch screen reads the lines it read before, and a sheet is still the page plus its two gutters, so the ten inch sheet is 640 dp and its words land on the reading's words behind it. The wash of a chosen ayah now stands 12 dp from the glass and a 48 dp control keeps 8. Three lines that had drifted onto edges of their own stand on the one edge: the search field's head, which ran 10 dp outside its own rows; a settings page's back mark, which stood at 18; and a choice row's ring, which stood at the old 22. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 17.5

The floating paper is the highest paper, not a shade of the page. The pill, the offer, the snackbar, and the menus they open wear the day's sheet white, 2.0 L* above the page and 7.1 L* above the chosen block (dE76 7.2), so the ivory slip that stood 4.4 L* under the page is gone and a floating control joins the panel paper the app already uses for every sheet. No hairline is added: on the day the fill and the lift are the edge, and every sheet has always been held that way. The night's floating paper steps up from `#2E2B28` to `#322E2A`, 9.8 L* over its page and dE76 5.9 from its block, because the night's wash lifts the block above the sheet and the night shadow is invisible; the switch's track keeps `#2E2B28`. The contrast test now pins the direction, above the page and above the block, and the distance, and the old warmth rule is gone. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 17.4

The floating paper is the page's own paper, warmed. The pill, the offer, the pack row, the snackbar, and the menus they open stand 4.4 L* under the day page at three times its chroma, so they read as an ivory slip on the paper rather than the gray veil 17.3's neutral became: on a chosen ayah's block, which wears the accent's wash at L* 92.9 and chroma 0.71, that neutral stood 3.0 L* and dE76 4.2 away, and the eye read a smudge where a surface should be. The tone now stands dE76 8.4 from that block, a touch above it rather than under it, and the sheet stays the only pure white on the ground. The night is unchanged. The contrast test pins the warmth, the band the step may take, and the distance from the block a menu is drawn on, so an edit that sinks the paper into a shade or drains it into a gray fails on the machine. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 17.3

The floating paper is a step of lightness, not a tint. The pill, the offer, the pack row, the snackbar, and the menus they open keep the 1.23:1 step from the day page that 17.2 chose and give up the warmth that came with it: at 2.7 times the page's own chroma that tone read as a sepia laid over the paper, and a menu is only ever raised over an ayah's block, which wears the accent's wash at L* 92.9, where the old tone stood 2.9 L* and 1.08:1 under its own ground on the opposite hue. The tone now holds the page's hue and the page's own chroma, so the shape reads as paper one step in shade and its edge over the wash is one of lightness. The night is unchanged. The contrast test pins the warmth as well as the ratio, so an edit that tints the paper again fails on the machine rather than on a reader's eyes. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 17.2

The floating paper is a step from the page. The playback pill, the offer, the pack row, the snackbar, and every menu they open wear one warm tone, L* 90 against the day page and the same 1.23:1 step the night page already held, so a light pill reads as a control instead of disappearing into the paper; a menu that hangs from a pill wears that paper too, not the reading's own tone. The recitation's word keeps its tint and loses the fine accent edge, which read as a focus ring around the Quran's own line. The download offer is one 64 dp row, the playing pill's own height: the reciter over the surah and its size, with Download, and Not now leaves the glass because the page and Back already answer. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 17.1

The colors are read for the long sitting. All seven tajweed families sit in one band on the page, 6.2 to 7.6 against the paper and 6.7 to 8.1 on the night page, so no rule is heavier than its neighbor and a long read keeps one rhythm; the night tones came down off the top, and the green is a leaf green, no longer the one saturated color in the app. The reading's own ink steps under the interface's word on paper as it already did at night, 12.1:1 on both grounds, so an hour of paper reads as ink and a label still reads as a label. Selecting a verse or a passage now selects in the page's own word mark instead of Material's forty percent accent, so the seven colors stay readable through the selection. While listening, the recited word keeps its tint and gains the accent's fine edge, because the tint alone is capped by the seven colors and the place has to be found at a glance. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 17.0

The recitation pill is one row of 64 dp, the height its other voices already wear: the voice and its pace over the place and its key, with play or pause at the end. The two skips left the glass, where they cut the place, and are the actions the mark offers to TalkBack; the notification keeps them for everyone. The pill and the offer now wear the paper's control tone instead of the sheet's white, and the ayah's number lights a pill that keeps the card's air instead of meeting its edge. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 16.9

The reading's edges are quiet. The ayah's number lights the app's compact pill when pressed, with 8 dp of air on every side, so the flash no longer hugs the digits or the reader's marks. On the page with the translation off, a saved or noted mark rides the verse's first line in a rail of its own, so a marked verse keeps the page's rhythm instead of standing a head taller. The listening panel opens on its first question rather than repeating the place the capsule already says. The word meanings switch is the ask now: turning it on fetches the word list, with the transfer and any failure said under the switch. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 16.8

The ayah's number says it opens. Every ayah's head now carries the app's one chevron, 4 dp after the number in the same blue, the pair the recitation pill's doors already wear, so the head reads as a door rather than as the verse's citation. The chevron turns up while that ayah's menu is open. The number keeps no capsule, and the printed medallion at the end of the verse is untouched. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 16.7

The page belongs to the reader again. A tap on an ayah no longer selects it: the verse's number is the one door to its actions, and the menu it opens closes on any tap outside without choosing another verse. In the mushaf, the printed medallion is that door. The five-action capsule inside the verse is gone, and the number stands at the head again in the app's blue. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 16.5

The recitation pill's doors say their state. The chevron on the playing pill, on the download offer, and on the voice list of a pill with no recitation turns up while its popup is open, and the line speaks Expanded or Collapsed to a screen reader, the words the ayah's door and every disclosure row already use. The pill's three doors are drawn by the app's one chevron now, at their compact 14 dp measure, so their turn is the same rule every other door follows. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 16.4

The interface is one hand. The ayah's number is the app's blue, not a second gold action. The theme swatches keep a visible edge, every text button, chip, and AM/PM pair is one 36 dp pill, and a button that cannot answer has a real disabled state. Browse and Search share one chip and one disclosure row: a search result no longer carries a down chevron that opened a page, and the tafsir source line is the row name's 15. At 200 percent text, Browse chips keep their gap, the size rails keep their room, and ayah numbers grow instead of clipping. The snackbar's action is a pill and the remove-saved Keep is the solid answer. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 16.3

The reciter's word is drawn from the file's own voice, not from the export's marks alone. The build listens to every shipped ayah file and records its first and last voiced millisecond; the highlight waits for the first sound and clears at the last, instead of starting at 0:00 and holding through the closing pause. The reading sleeps to each word's own boundary, in the reader's pace, so it keeps up at 0.5x, 1x, and 1.5x, and the surah's word regions are kept from the playlist build, so the wash no longer blanks at an ayah turn. Where the export leaves a gap of more than six seconds, the wash rests instead of lying. The Quran text, both translations, both tafsirs, and the audio are unchanged; the content database is unchanged, and the recitation timing packs are unchanged, with the new edge table shipped beside them.

## 16.2

The recitation offer is a two-row capsule again: its line is a 48 dp door that opens the voice list for the surah at hand, with the chevron back, and Not now and Download stay centered under it. The playing pill is one line of words over the transport: the reciter and its pace, then the surah and its ayah. The word meaning sheet's head takes the page's left edge with the footnote's, its word is the Quran's own size and its gloss the translation's, and the word aid's own size is gone with its role. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 16.1

The recitation offer is a compact centered capsule: the reciter, the surah, and the size on one line, with Not now and Download centered under it. The reading runs under the status bar again, with no paper band under the clock. The footnote and word meaning sheets name themselves with the app's one sheet title and put the place under it at the meta size, and the Reciters page lists the bundled Al-Fatiha under both voices as a surah on the device. Send feedback opens the reader's mail app on the maker's inbox, with the project repository as the fallback. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 16.0

The page belongs to the text. The quiet folio line at the foot is gone, and with it the duplicate of the surah and juz that the top bar already carries. Aa leaves the bar, so the bar is Browse, the surah and juz, Search, and Settings, and the juz fits again on a phone; the text size lives in Settings with its live sample. The bar leaves when the reader's own drag begins and returns on a tap, with no timer. The recitation pill and the pack card are the only things at the foot, composed only while raised, and the reading ends at its own margin. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 15.9

Pressing and holding a word opens its meaning on its own sheet: the word at the reader's word-meaning size, its meaning under it, and the ayah named above, all on one center line. The reciter's word is a rounded wash again, not a recolored word, so the letters keep their own ink and their tajweed colors while the voice is on them. The wash is set at the strongest tint the seven rule colors can still be read on, and the ayah-level recitation wash is gone because the two together could not. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 15.8

Al-Fatiha's seven ayahs for both reciters now ship inside the app, so the first Play on 1:1 works offline with no download offer. The two reciter timing packs ship too; every other surah still downloads its audio on the reader's word, with the word timings already in the app. The Quran text, both translations, both tafsirs, and the content database are unchanged.

## 15.7

The recitation is the original 128 kbps stereo MP3 set again, the same Al-Minshawi and Al-Husary recordings and word timings the app shipped before 15.6. The 64 kbps Ogg Opus encode is withdrawn from the app; its packages remain published for 15.6. Each surah is about twice the size, and a surah downloaded in 15.6 downloads again. The Quran text, both translations, and both tafsirs are unchanged.

## 15.6

The recitation was re-encoded in Ogg Opus at 64 kbps mono, about half the size, with the same word timings. 15.7 restores the original MP3 set.

## 15.5

The app opens in the phone's language, with both translations already on the device, and no language screen. The reminder is offered after the first surah, not before the text. A quiet folio line keeps the surah and juz on the page; the full chrome is on tap, with no timer. Aa opens the type page, the reading has two sizes, a tap on a word shows its meaning, and a tafsir you do not have is offered where you ask for it. Search shows the verse for a reference result, the ayah menu is paper, and the audio offer has a clear Not now. Tajweed colors and word meanings start off.

## 15.4

The Font size sample is a type specimen again: the ayah and its translation, without the translation's name, and with the translation shown even when the reading's Show translation switch is off, because the page is there to judge the size of the type. The reading still names each translation column when the rule calls for it. The Quran text, every translation, every tafsir, every pack, and the content database are unchanged.

## 15.3

Your language leads. A reader who keeps two translations on reads the one in their language first, in the reading, the daily reminder, and the share card, and a column in the other language always says which pack it is. The Font size sample shows that same lead line and its name, and follows the Show translation switch, and the Translations page lists the reader's language first. The Quran text, every translation, every tafsir, every pack, and the content database are unchanged.

## 15.2

Settings becomes one list. A row says where a setting stands and
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
