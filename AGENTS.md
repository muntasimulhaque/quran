# Quran: working rules

**The first task of every session, before reading this file and before any
other command: `git fetch`, then `git pull origin main`.**

A free, offline Android Quran reader. Two reading modes on one text: the
Mushaf page and a study view with Saheeh International, word-by-word, and
Tafsir Ibn Kathir, with recitation by Al-Minshawi and
Al-Husary. No ads, no trackers, no accounts, no network. Code is MIT; each
content dataset keeps its own source, version, and license.

Store title: `Quran: The Noble Book`. App title and launcher name: `Quran`.
Package: `io.github.muntasimulhaque.quran`, permanent.

**The code is the source of truth.** Why a thing is the way it is lives in
the KDoc beside that thing, and a settled choice is tagged
`owner decision`, so `git grep "owner decision"` finds them. This file is
the only rulebook: the law, the commands, the map, the vocabulary.

If this file and the code disagree, the code wins and this file is fixed in
the same change. Do not grow this into a second codebase written in prose.

## This file is not the final word

The rules are the current best understanding, not the ceiling. If a change
you believe in contradicts one, do not drop it silently and do not
implement against the rule either: name the conflict, make the case, and
let the owner decide. An overridden rule is updated here, never left as a
dead letter.

## Every session

1. **Pull first**, before anything else, including reading this file: `git
   fetch`, then `git pull origin main`, in that order. Never in the same
   block as a read of this file, which races the copy you are about to
   update. The owner works from more than one machine, and a session that
   has not pulled has not started.
2. Do the work, then ask one question: anything else? The build waits for
   the owner's word, and no `versionCode` moves until the session is done
   and the owner says so.
3. Session end: bring the README status and the release notes current,
   update the Map against `settings.gradle.kts`, and leave the tree clean.

## The law (non-negotiable)

1. **Offline except one thing.** `INTERNET` serves exactly one purpose:
   content from the project's own GitHub Releases. The reads are the language the reader chose, fetched without another
   question: the chosen language's translation arrives at once, and its
   tafsir follows on the same choice, waiting for an unmetered connection
   when the phone is on cellular (owner decision, 4.5); the word list is off
   by default, and the reading toggle that turns it on is the ask for that
   language's list. The reads are also a recitation package for one surah
   after the reader taps Play and approves the shown size (or after they
   have turned on Continue to the next surah, which is their word, given
   once), and a quiet refresh of a pack they already have, only when the app
   carries a newer version, only unmetered, only after the first page is
   readable. Nothing is fetched before the reader's word, no other host is
   ever contacted, and there is no
   analytics or telemetry of any kind. Everything else works with no
   connection. No WebView.
2. **Permissions: media, notifications, the reminder's exact time and its
   survival of a reboot, and that one network use.** Exactly `INTERNET`,
   `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`,
   `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `SCHEDULE_EXACT_ALARM` (the daily
   reminder's exact time), and `RECEIVE_BOOT_COMPLETED` (re-arming the
   reminder after a reboot). `USE_EXACT_ALARM` is never declared: Play
   accepts it for an alarm clock or a calendar only, and rejected the 3.9
   build over it. The app asks for `POST_NOTIFICATIONS` and for the phone's
   own exact-alarm screen, and nothing else: the notification once at the
   reminder card that ends the one screen before the reading, the exact
   alarm once there too when the reader turns the reminder on, and again at
   each of the two acts that set the reminder, and only on the phones that
   withhold it; the exact alarm is not asked where the notification was
   refused, because an exact alarm with nothing to show is a promise the
   phone cannot keep. The exact
   alarm is no row in the settings hub, and the reminder wears no alarm icon
   (owner decision). Library-merged permissions are documented, not fought. A
   new permission needs the owner's sign-off written here first.
3. **No ads, no trackers, no analytics, no accounts.** AndroidX, Kotlin,
   Media3, Room, DataStore, and Glance only. Every new dependency is
   proposed here first.
4. **The sacred text is untouchable.** Not one byte of Quran text,
   translation, or tafsir is edited by hand. Content changes only through
   `tools/`, and every dataset carries its source, version, checksum, and
   license in `content/manifest.json`.
5. **The app must not crash.** No `!!`, no unchecked casts, no swallowed
   exceptions. State survives rotation, backgrounding, and process death. A
   Play vitals crash is a stop-the-line event.
6. **Nothing may feel like AI slop.** No placeholder copy, dead buttons,
   lorem text, stock iconography, generic Material defaults. Every string is
   a real sentence, every color is chosen, every icon is drawn for this app.
7. **Accessibility is a rule, not a feature.** TalkBack works end to end.
   Study mode honors the system font scale; the Mushaf page is drawn from
   the Book's own words at the reader's own text size, set to the same five
   steps every sized text uses, and a page set taller than the glass is
   panned (owner decision, this session: the page's usual size holds all
   604 pages inside the room the pager gives, measured over every page of
   the Book; the phone's own magnification still covers anything beyond it).
   Contrast targets live in the theme and are measured.
8. **Two UI languages, one content language each.** English and Bangla ship
   together, the interface strings in each module's `values-bn`, and the
   reader's choice sets the translation, the tafsir, and the word meanings
   too. Arabic is data, not a locale; RTL support stays on. A new
   language needs a real translation pass, a `values-xx` folder, and the
   same review the Arabic content gets. The frozen names (`app_name`,
   `first_paint_title`, `first_paint_subtitle`, the store title) are never
   translated.
9. **Reader first.** The app opens where the reader left off. There is no
   home screen, no dashboard, and no permanent tab bar; index, search,
   library, and settings are sheets raised from a slim bar.
10. **Simple to the bone.** Any age, any device. No account, no wizard
    beyond one optional first screen and the one notification permission asked
    from it, no other dialog before reading, no feature that asks a choice
    before the text. If a feature adds friction, it is cut. Every added
    choice must earn its place against the reading.
11. **Fast to the point of invisible.** Cold start lands on readable text
    with no spinner. A page turn is a pre-rendered swipe, not a render.
    Nothing blocks the main thread, ever.
12. **Daily reading, not engagement.** A daily portion, a quiet reminder,
    and a widget. No streaks, no badges, no social, no guilt.
13. **No AI attribution anywhere.** No `Co-Authored-By` trailers, no
    "generated with" footers, no name in contributors, commits, or code.
14. **Owner's law.** The build waits for the owner's word; one build
    carries the whole session.

## Style

- **No em-dashes.** Not in chat, release notes, commit messages, code
  comments, or this file. Use commas, colons, parentheses, or a sentence
  break. `NoEmDashTest` scans the repo and fails the build.
- **US English**, everywhere: color, gray, center, license, behavior.
- **Store text is plain prose.** Play Console mangles quotes, markdown, and
  dashes. Release notes fit the 500-character field, counted before
  hand-off, and are handed over as a bare paragraph: no blockquote, no code
  fence, no quotes, no label on the same line.
- **The release note in the hand-over message is bare text.** The note is
  given once, in the message itself, standing on its own as plain
  paragraphs with nothing around it: no blockquote, no quotation marks, no
  heading or label of its own, no horizontal rule, no vertical bar, and no
  copy inside a table. The owner copies it from the message straight into
  the console's box, and anything wrapped around it has to be unwrapped
  before it can be pasted.
- **Small pieces, no numbers.** Keep modules small and the codebase
  modular: for code, the codebase is all you need, so memory and docs
  stay out of it, and at most a little maintained map says where what
  is. Split early; a name that says the idea beats a name that says
  the screen.
- **User-facing strings** live in a `strings.xml` in the module that draws
  them, nowhere in Kotlin: `app` for the shell, each feature for its own
  surface, `ui-kit` for the two mode names. Arabic content stays data.
  Colors live in the theme, nowhere else.
- **Small edits** are `node -e` replacements, and a CRLF file needs `\r?\n`
  in the pattern or the replace silently no-ops. Grep afterwards to confirm
  the change landed.

## This machine

Environment facts the harness does not tell you, each one worth a failed
command to rediscover.

- Every Gradle call begins with
  `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`. The
  Android Studio JBR is the only JDK on this machine and the build is shaped
  so that is enough: no module pins `jvmToolchain`, every module targets
  Java 17 bytecode and lets the running JDK compile it (owner decision).
  `local.properties` reads `sdk.dir=C:/Users/zn/AppData/Local/Android/Sdk`
  and is gitignored.
- The Android SDK here has the command-line tools, the Android 35
  `google_apis` image, and three display-on AVDs, the three store profiles:
  `quran_phone` (Pixel 2), `quran_tablet7` (Nexus 7), and `quran_tablet10`
  (Pixel C). The old ATD AVDs (`pixel35`, `pixelc`) and their image were
  removed because their display was off and no frame could be read from it
  (owner decision). **The emulator runs here**: `emulator-check accel` reports
  `WHPX(10.0.26300) is installed and usable`, and the instrumented suites are
  not CI's only home. Start one headless with `-no-window -no-audio -no-boot-anim
  -gpu host` and wait for `sys.boot_completed` to report `1`: this machine has
  a dedicated GPU (an AMD Radeon), so the host renderer is both faster and
  closer to a real device than `swiftshader_indirect`. A full `:app:connectedDebugAndroidTest`
  takes about six minutes here and is green on `quran_phone`, the two legs the
  ATD image could not draw included. `gh` is on the PATH and authenticated, so
  the content Releases can be published from here.
- On the other machine (`Dev Pro`) the JBR is the JDK, `adb` and the
  emulator are under `C:/Users/Dev Pro/AppData/Local/Android/Sdk`, and the
  AVDs are `Pixel_4_35`, `Nexus_7_35`, `Pixel_C_35`, and `api27`. Start one
  headless with `-no-window -gpu swiftshader_indirect` and wait for
  `sys.boot_completed` to report `1`.
- That machine runs Gradle on the Android Studio JBR
  (`/c/Program Files/Android/Android Studio/jbr`), which is the only runtime
  installed on it: the Temurin JDK was removed and every Gradle call there
  begins with that `JAVA_HOME` (owner decision). The emulator there dies of
  the tour's load
  on a full debug build, so the
  tour runs against the lean `-Pquran.devPacks=screenshot` APK. AGP uninstalls
  the app when `connectedDebugAndroidTest` returns, so a frame is only
  readable this way: install both APKs, `am instrument -w -e class
  io.github.muntasimulhaque.quran.ScreenshotTest -e additionalTestOutputDir
  /data/user/0/io.github.muntasimulhaque.quran/files/shots` with MSYS path
  conversion off, then `adb exec-out run-as io.github.muntasimulhaque.quran
  cat files/shots/<name>.png`. The output directory has to be inside the
  app's own storage: `/sdcard` root is EPERM for the app, and a directory
  under `Android/data/<package>/files` does not exist until something calls
  `getExternalFilesDir`. The same two commands work here, and a whole
  capture can be kept without it.
- Two facts worth a failed command of your own to learn: the `google_apis`
  AVDs draw a real display headless, so `Screenshot.capture()`,
  `adb exec-out screencap -p`, and a popup's own `captureToImage()` all
  return pixels, and a menu's shadow can be read on the glass (the old ATD
  image returned a black frame for the first and a blank window for the
  last); and AGP uninstalls the app when `connectedDebugAndroidTest` returns,
  so anything it wrote under the app's own storage is gone by the time you go
  to read it (the `am instrument` command and the `run-as` read above are how
  it is read anyway).
- MSYS rewrites `/sdcard/...` arguments, so prefix `adb shell`, `adb push`,
  and `adb pull` with `MSYS_NO_PATHCONV=1`, and the same for `gh api` (with
  its leading slash dropped). The same rewrite bites `gh run view --json ...
  -q`: a `/` inside a jq string becomes `C:/Program Files/Git/`, so a
  completed run never equals `completed/success` and a green run reads as
  still running for as long as the wait is allowed to last. Prefix `gh run
  view` with `MSYS_NO_PATHCONV=1`, or watch with `MSYS_NO_PATHCONV=1 gh run
  watch <id> --exit-status`. adb is a Windows binary: `/tmp/x` is
  `C:\tmp\x` to it and to the read tool alike.
- Two tool calls in one block run in parallel. Never edit a file and read
  it in the same block, and never two reads whose order matters.
- `python3` and `python` are both intercepted by the Store alias: an
  interpreter exists at
  `C:/Users/zn/AppData/Local/hermes/tools/python-3.14.7+20260901-win32-x64/`
  (Pillow, fontTools, arabic-reshaper, and python-bidi were installed by hand
  into it), and there is no py launcher, no python.org install, and no Store
  package anywhere else on PATH. Those hand-installed packages have been
  reset once mid-session, leaving pip alone; the one-command recovery is
  `uv venv` against that interpreter, with the harness `uv` at
  `C:/Users/zn/AppData/Local/hermes/tools/uv-0.12.3-win32-x64/uv.exe`, then
  `uv pip install` the packages back. Nothing in the build depends on any of
  it, so a missing Python only ever costs a scratch tool of its own.

## Build, test, verify

```bash
./gradlew :core:test :data:testDebugUnitTest :ui-kit:testDebugUnitTest :feature-playback:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :data:connectedDebugAndroidTest   # saved store, last read, recitation manifest
./gradlew :app:connectedDebugAndroidTest    # the app surfaces and the screenshot tour
```

The content pipeline, from the repository root. Every one of these is green
at every step:

```bash
./gradlew :tools:run --args="fetch"           # the font pack onto a new machine, hash-checked
./gradlew :tools:run --args="verify"          # checksums and structure of every source
./gradlew :tools:run --args="audit"           # letter-level audit against Tanzil
./gradlew :tools:run --args="search"          # Arabic round trips and English folding
./gradlew :tools:run --args="build"           # content/quran.db and the packs, deterministic
./gradlew :tools:run --args="fonts"           # font coverage for every codepoint
./gradlew :tools:run --args="checkdb"         # the committed database and its report
./gradlew :tools:run --args="audio sample"    # the development sample (debug only)
./gradlew :tools:run --args="audio packs"     # one ZIP per reciter per surah, plus the manifest
./gradlew :tools:run --args="audio publish"   # uploads those packages to their Releases
./gradlew :tools:run --args="packs"           # the pack files and the catalog
./gradlew :tools:run --args="packs publish"   # the content packs, content addressed
```

- `fetch` runs once on a machine that lacks the Arabic text font (about a
  minute) and is offline after that. The `qpc-v2-fonts` Release stands
  unreferenced since the Mushaf became a text page: its SHA-256 is no longer
  pinned, and the font is no longer part of the app.
- `verify`, `audit`, and `fonts` read only `content/raw` and
  `content/work/verify`, so they run on the owner's machine only. `ls
  content/raw` before a release starts: if the manual QUL and QuranEnc
  exports are gone, say so before the release begins rather than at gate
  time.
- `content/quran.db` is the shipped content, built by `tools/` and fetched
  from its hash-addressed release on a fresh machine. A local rebuild must
  reproduce its SHA-256 exactly; if it does not, stop and find out why
  before committing anything.
- **Verify by process exit code**, never by grepping piped output.
- CI runs the JVM suite, the content gates it can run, the data instrumented
  tests, and the signed bundle in `build.yml`, which filters pushes by path
  so a doc-only commit triggers nothing. The app instrumented tests run in
  `screenshots.yml` on the three store form factors, and that workflow names
  six classes: `ScreenshotTest`, `WordByWordTest`, `MushafTurnTest`,
  `TafsirDirectionTest`, `SavedNotesTest`, `SettingsVisibilityTest`. Every
  other app test runs on a session's own emulator, which this machine has, so
  a session that changes the settings sheet, and every release session, runs
  the whole app suite rather than only the six. On `quran_phone` the whole
  suite is green, 80 of 80, including the share card's hardware read-back and
  the phone's own night mode, which the removed ATD image refused. On a cold
  google_apis run `SurahAyahsTest` can time out in `openBrowse` once, the
  ninety-second wait meeting a loaded boot, and it passes on a retry.
- Do not hand-drive the app to verify UI. After an emulator crash the
  injected input in the screen's top band can go dead while the rest keeps
  working, captures return older frames, and the clock jumps. The loop is:
  JVM suite and lint locally, push, then read the tour's frames from the
  capture run. Every UI claim in a hand-over is backed by a CI frame or a
  compose-test run, never by hand-taps.
- Bytecode target: Java 17; current AGP, Kotlin, Gradle, and the SDK levels
  in the build files. Versions live in `gradle/libs.versions.toml`, never
  here. The Gradle wrapper is committed.

## Release

1. **The owner's word first.** One build carries the whole session, and the
   bundle, the store note, and the screenshots are one delivery: collected,
   checked, and handed over together, before the owner submits anything. The
   note is always in the hand-over message itself, beside the bundle's own
   facts, never only in `play-store/listing.md`: the owner copies it into the
   console from the message, and a note that has to be fetched from a file is
   a note that can be pasted from the wrong release (owner decision). A set
   refreshed after the submission has nothing left to be used for.
2. **Version convention.** `versionCode` by 1; `versionName` up one tenth
   within the major line, 0.1 through 0.9, then 1.0, then 1.1 through 1.9,
   then 2.0. There is no 0.10 and never a two-digit minor. The version line
   goes to `play-store/listing.md` with the notes beside it, and the
   previous release's notes stay under their own heading.
3. **Screenshots before the hand-off, whenever visible UI changed.** The
   push starts the capture on its own, and the set is collected whole from
   one run: all three form factors, eight frames each, every frame
   `cmp`'d against its artifact, every changed frame read. A leg short of
   eight is not installed, and when nothing visible changed the hand-over
   says so and the capture is not run. The eight are the ones
   `play-store/listing.md` numbers and `ScreenshotTest` captures; they are
   kept in step, never hand-captured.
4. **The bundle comes from the newest green `build` run on `main`**, not the
   first one after the version bump: everything committed after the bump is
   in the later run and in nothing else. The run's artifact carries
   the bundle and its SHA-256, and GitHub deletes it after two weeks, so a
   signed build is never sitting in public. Delete the hand-off copy in
   `play-store/aab/` once Play has it.
5. **Signing is proved by its words.** `jarsigner -verify` exits 0 on an
   unsigned file, so the check reads "jar verified" and prints the
   certificate's SHA-256 (`53:7D:09:D2:...:0D:9D:E5:21`). The
   `signed-bundle` job fails when the four secrets are absent rather than
   producing an unsigned artifact, because "it built" must not be mistaken
   for "it is signed".

## Map

| Path | What is there |
| --- | --- |
| `core/` | pure JVM Kotlin, zero `android.*` imports: references, search normalization, rich text parsing, models |
| `data/` | read-only content access, the saved-ayah and last-read user databases, preferences |
| `app/` | the shell: activity, view model, the screen that composes the features, the shell's strings |
| `feature-*/` | one reading surface each (mushaf, study, search, browse, playback, settings), each owning its own strings and icons |
| `ui-kit/` | the shared look: theme and palettes, the hand-drawn icons, the rich text views, the app's choice row, the one floating lift, the small formatters |
| `content-assets/` | the shipped assets the app reads: the Arabic text face, the study and UI faces, the core pack, and the pack catalog |
| `tools/` | the offline pipeline: fetch, verify, audit, build, fonts, packs, audio |
| `content/` | `quran.db` (built by `tools/`, gitignored, fetched or rebuilt from its hash), `packs/`, `manifest.json`, `recitation-manifest.json`, `audit-report.md`; `raw/` is local and gitignored |
| `play-store/` | listing, screenshots per form factor, the hand-off AAB |
| `benchmark/` | the startup profile's generator, development only, never in the bundle |
| `.github/workflows/` | `build.yml` (gates, data tests, signed bundle), `screenshots.yml` (the store set) |
| `docs/` | the Pages site: the landing page and the privacy policy Play links to |

## Glossary

- **the content database**: the read-only SQLite built by `tools/` from the
  manifest, shipped as an asset, replaced wholesale on update.
- **the manifest**: `content/manifest.json`, the one place a dataset's
  source, version, license, and checksum are recorded.
- **Mushaf mode**: the 15-line page, drawn from the Book's own words at the
  reader's own text size, swiped.
- **study mode**: the ayah-by-ayah reader with translation, word-by-word,
  and tafsir.
- **the page / the line / the word**: the Mushaf geometry, from the page
  down to one word and the shape the page's own face gives it.
- **the measure**: the page's own line, 15.6 ems of its text wide, which the
  print justifies to and the reader's text size shares out.
- **the reference**: an ayah key in `surah:ayah` form, for example 2:255.
- **the portion**: the reader's chosen daily amount. Never called a streak.
- **the deck**: the day's ayah is one card of a shuffled 6,236, dealt by the
  day number and reshuffled every pass, so nothing about it is stored.
- **the study card**: the sheet one ayah opens: the Words, the translation
  with footnotes, and the Ibn Kathir panel. From the Mushaf it
  is the whole study surface; from the study reading it is only the tafsirs.
  The ayah is not drawn on it, because the page behind carries it.
- **the text button**: `ui-kit/TextButton`, the one shape a word that acts
  wears. A heading, a name, or a label is bare type; anything that answers a
  touch is a rounded shape.
- **the search sheet**: one field over the Arabic text, the translation, and
  the surah names; results stay in Mushaf order.
- **the pack**: one downloadable set of content or recitation, addressed by
  its own hash. The app reads a pack and never hotlinks one.

## Frozen choices (do not reopen without approval)

Appealable: bring a genuinely better idea to the owner and, if approved,
implement it and update this list.

- The name and the three strings are frozen.
- A settings row's value is under its name, on every row in the sheet, and
  never in a column at the right; a row that opens something ends in a
  chevron, whatever page it is on.
- Reader-first, no tab bar.
- Both reading modes ship together.
- The reading modes are one switch in the top bar, and the reader's other
  doors are Browse, Search, and Settings; there is no bottom bar.
- Last Read is the fourth Browse tab beside Surahs, Juz, and Saved, and a
  note lives inside Saved with the ayah it was written on.
- A surah row in Browse opens its own ayah grid; there is no separate Go to
  Ayah tab.
- Text sizes are 0.65, 0.75, 0.85, 1, 1.2.
- Saheeh International is the only translation.
- Ibn Kathir is the tafsir.
- Minshawi and Husary are the only reciters, and Husary is the reciter of
  a new install; a reader's own choice is never moved (owner decision, 4.5).
- The first screen is two steps on one screen: the language, then the daily
  reminder card that asks for the notification and the exact time. The
  chosen language's translation and tafsir arrive with the choice; the word
  list arrives with the toggle that turns it on. The first choice lands in
  the study reading of Al-Fatiha 1:1, and every launch after that opens
  where the reader left off (owner decision, 4.5).
- No streaks, no gamification.
- The design direction is the manuscript language: the page is the
  interface, chrome is summoned and never resident, one accent, hairlines
  over boxes.
- Content is sourced from QUL and QuranEnc, with every license
  honored and a takedown path in About.
- Continue to the next surah is off by default and is the reader's one-time
  word for the packages that follow the surah being heard.
- What happens at the end of the audio is one answer, drawn as one list
  with one leading radio mark and one 24 dp gap, in the pill and in the
  settings page alike (owner decision, 4.5; the mark and the gap are the
  app's one choice row, owner decision, forty-eighth session): continue to
  the next ayah (the default), repeat the ayah, stop after this ayah,
  continue to the next surah, or repeat the surah, in that order.
- **The app asks for the exact alarm only at the two acts that set the
  reminder, only while the phone is withholding it, and the reminder wears no
  alarm icon.** `SCHEDULE_EXACT_ALARM` is granted at install before Android
  14 and is asked for once after it, in the phone's own screen; the Daily
  page names it again only as state, with the one door that screen offers,
  and the hub has no row for it. `USE_EXACT_ALARM` is never declared: Play
  accepts it for an alarm clock or a calendar only, and a Quran reader is
  neither (owner decision).
- The day's ayah is dealt from a shuffled deck rather than walked in the
  Book's order: a different ayah every morning, every ayah once before the
  deck is filled again, and nothing stored (owner decision).
- The app's mark is an open book on its rehal: the owner's own reference
  drawing, redrawn clean for this app as uniform line art, the book in the
  theme's ink on the theme's paper, `#1C1B18` on `#F8F5EF`, the stand in the
  Mushaf's ornament gold `#856411`, with the pages filled in the paper so
  the book occludes the stand behind it and a paper margin keeps the stand's
  lines clear of the book's edge (owner decision, this session: the mark
  wears the app's own theme tones, and the cover's burgundy is not one of
  them). The launcher, its monochrome layer (the book's silhouette on each
  plank, sturdier than the mark), the store icon, the reminder's small icon
  (the silhouette alone, no ground, since the shade reads only its alpha and
  paints it in the notification color), the share card (the drawing in its
  own colors on the card's own paper), and the feature graphic all carry it.
  The drawing is sized so the whole mark sits inside the launcher's 66 dp
  circle, about half the canvas wide, so a mask leaves air around it instead
  of the mark filling the tile.
- The feature graphic is the same world at banner size: the paper field, the
  page's gold hairline frame, the mark at the left, and the store's two names
  at the right, Quran in Literata over The Noble Book in Inter. Every
  element stands inside the safe area, well clear of the edges Play may crop
  or overlay, and the file is 24-bit PNG with no alpha (owner decision, this
  session).

## Traps

The classes that cost a session to find live here, one entry each. Five of
them are worth reading before any run:

- **Read the frames a red leg kept before touching anything.** The log says
  which assertion failed; the frame says what the reader was looking at.
  Three wrong diagnoses in a row were solved by one kept frame.
- **A red leg on this machine is a fact to baseline, not a verdict.** Four of
  the app's own legs were red here for nobody to see, and every one of them was
  red on `main` too: `git worktree add` a copy of the parent commit and run the
  class there before believing that a change of yours did it.
- **A popup's place on the glass cannot be read by a compose test.** A popup
  draws in a window of its own, and every node inside it reports coordinates
  measured from that window's own corner, so a test can see how big a popup is
  and never where it hangs. Pin the rule that places it instead, as arithmetic
  in a JVM test, and pin the anchor it is placed from in the compose test that
  can see that.
- **A retry is not a diagnosis.** It is worth one when it names the failure
  the way the 10 inch tour step does, and worth nothing when it only
  re-waits.
- **Never install a set from a run with a leg short of eight frames.**
- **A gate that reads the content by table name is reading a contract, not
  a schema.** Change the build's shape and run every `:tools` gate, not
  only the ones CI runs.
- **One fact, one command, read once.** A failure is never guessed at
  twice: the second guess costs a capture run.

## Housekeeping

At the end of a session the hand-off copy, the build directories, and the
content working area are what may go, and each is one command to bring
back. Keep `content/raw/` (the owner's manual exports, the provenance of
every pack), `content/quran.db` and `content/packs/` (the shipped assets
the catalog pins), and `play-store/screenshots/` (the store set).

| Removed | Bring it back with |
| --- | --- |
| `play-store/aab/*.aab` | the `signed-bundle` artifact from the newest green `build` run |
| every `build/` directory | any `./gradlew` build |
| `content/work/verify` | `./gradlew :tools:run --args="verify"` |
| `content/work/fonts-hafs` | `./gradlew :tools:run --args="fetch"` |
| `content/work/db`, `json`, `qpc-v2.db` | `./gradlew :tools:run --args="build"` |
| `content/work/audio-dev` | `./gradlew :tools:run --args="audio sample"` |
