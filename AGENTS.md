# Quran: working rules

**The first task of every session, before reading this file and before any
other command: `git fetch`, then `git pull origin main`.**

A free, offline Android Quran reader. Two reading modes on one text: the
Mushaf page and a study view with Saheeh International, word-by-word, Tafsir
Ibn Kathir, and Tafsir As-Sa'di, with recitation by Al-Minshawi and
Al-Husary. No ads, no trackers, no accounts, no network. Code is MIT; each
content dataset keeps its own source, version, and license.

Store title: `Quran: The Noble Book`. App title and launcher name: `Quran`.
Package: `io.github.muntasimulhaque.quran`, permanent.

**The code is the source of truth.** Why a thing is the way it is lives in
the KDoc beside that thing, and a settled choice is tagged
`owner decision, <n>`, so `git grep "owner decision"` finds them. Everything
else has one file and one job:

| File | What only it can say |
| --- | --- |
| `AGENTS.md` | the law, the commands, the map, the vocabulary |
| `docs/decisions.md` | the ADRs, D-001 onward: why this and not the alternatives, and each session's record |
| `docs/design.md` | the reading surfaces, the type, the color, the budget |
| `docs/architecture.md` | the module rules and the one direction between them |
| `play-store/RELEASE.md` | the release runbook, the secrets, the signature proof |
| `docs/screenshot-failures.md` | every way a capture leg can fail, with its class and its fix |

If this file and the code disagree, the code wins and this file is fixed in
the same change. Do not grow this into a second codebase written in prose: a
session's story belongs in `docs/decisions.md` as an entry, never as a new
section here.

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
3. Session end: append the session's entry to `docs/decisions.md`, bring the
   README status and the release notes current, and leave the tree clean.

## The law (non-negotiable)

1. **Offline except one thing.** `INTERNET` serves exactly one purpose:
   content from the project's own GitHub Releases, per D-023 as amended in
   D-105 and D-109. The reads are a content pack the reader asks for after
   seeing its size, a recitation package for one surah after the reader
   taps Play and approves the shown size (or after they have turned on
   Continue to the next surah, which is their word, given once), and a quiet
   refresh of a pack they already have, only when the app carries a newer
   version, only unmetered, only after the first page is readable. Nothing
   is fetched at launch, no other host is ever contacted, and there is no
   analytics or telemetry of any kind. Everything else works with no
   connection. No WebView.
2. **Permissions: media, notifications, the reminder's exact time, and that
   one network use.** Exactly `INTERNET`, `POST_NOTIFICATIONS`,
   `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, and
   `SCHEDULE_EXACT_ALARM` (the daily reminder, D-114). The app asks for
   `POST_NOTIFICATIONS` and nothing else: the exact alarm grant is never
   asked for and never named in the UI, and the reminder takes the best alarm
   the phone allows (D-130). Library-merged permissions are documented, not
   fought. A new permission needs the owner's sign-off written here first.
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
   Study mode honors the system font scale; Mushaf mode zooms. Contrast
   targets live in the theme and are measured.
8. **Two UI languages, one content language each.** English and Bangla ship
   together, the interface strings in each module's `values-bn`, and the
   reader's choice sets the translation, the tafsir, and the word meanings
   too (D-067). Arabic is data, not a locale; RTL support stays on. A new
   language needs a real translation pass, a `values-xx` folder, and the
   same review the Arabic content gets. The frozen names (`app_name`,
   `first_paint_title`, `first_paint_subtitle`, the store title) are never
   translated.
9. **Reader first.** The app opens where the reader left off. There is no
   home screen, no dashboard, and no permanent tab bar; index, search,
   library, and settings are sheets raised from a slim bar.
10. **Simple to the bone.** Any age, any device. No account, no wizard
    beyond one optional first screen, no dialog before reading, no feature
    that asks a choice before the text. If a feature adds friction, it is
    cut. Every added choice must earn its place against the reading.
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
- **Small pieces.** Files under 400 lines, functions under 40. Split early;
  a name that says the idea beats a name that says the screen.
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

- Every Gradle call begins with `export JAVA_HOME="/c/Users/zn/jdks/jdk-17"`
  (Temurin; Android Studio's own JBR is Java 25 and the modules'
  `jvmToolchain(17)` finds no match in it). `local.properties` reads
  `sdk.dir=C:/Users/zn/AppData/Local/Android/Sdk` and is gitignored.
- The Android SDK here has the command-line tools, an Android 35 ATD
  system image, and a `pixel35` AVD installed, and the emulator still cannot
  start: "x86_64 emulation currently requires hardware acceleration",
  because the Windows Hypervisor Platform is off and turning it on needs a
  reboot. So the instrumented suites are CI's unless the owner enables it.
  `gh` is on the PATH and authenticated, so the content Releases can be
  published from here.
- On the other machine (`Dev Pro`) the JBR is the JDK, `adb` and the
  emulator are under `C:/Users/Dev Pro/AppData/Local/Android/Sdk`, and the
  AVDs are `Pixel_4_35`, `Nexus_7_35`, `Pixel_C_35`, and `api27`. Start one
  headless with `-no-window -gpu swiftshader_indirect` and wait for
  `sys.boot_completed` to report `1`.
- That machine runs Gradle on `~/.jdks/jdk-17.0.19+10` (Temurin), not on the
  JBR: with the JBR the daemon died inside `jvm.dll` under
  `compileDebugAndroidTestKotlin`. The emulator there dies of the tour's load
  on a full debug build (class 2 of `docs/screenshot-failures.md`), so the
  tour runs against the lean `-Pquran.devPacks=screenshot` APK. AGP uninstalls
  the app when `connectedDebugAndroidTest` returns, so a frame is only
  readable this way: install both APKs, `am instrument -w -e class
  io.github.muntasimulhaque.quran.ScreenshotTest -e additionalTestOutputDir
  /data/user/0/io.github.muntasimulhaque.quran/files/shots` with MSYS path
  conversion off, then `adb exec-out run-as io.github.muntasimulhaque.quran
  cat files/shots/<name>.png`. The output directory has to be inside the
  app's own storage: `/sdcard` root is EPERM for the app, and a directory
  under `Android/data/<package>/files` does not exist until something calls
  `getExternalFilesDir`.
- MSYS rewrites `/sdcard/...` arguments, so prefix `adb shell`, `adb push`,
  and `adb pull` with `MSYS_NO_PATHCONV=1`, and the same for `gh api` (with
  its leading slash dropped). adb is a Windows binary: `/tmp/x` is
  `C:\tmp\x` to it and to the read tool alike.
- Two tool calls in one block run in parallel. Never edit a file and read
  it in the same block, and never two reads whose order matters.
- `python3` is intercepted by the Store alias. The working interpreter is
  the harness's own under
  `C:/Users/zn/AppData/Local/hermes/tools/`, with Pillow, fontTools,
  arabic-reshaper, and python-bidi installed by hand (D-112). Nothing in
  the build depends on it.

## Build, test, verify

```bash
./gradlew :core:test :data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
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

- `fetch` runs once on a machine that lacks the page fonts (about a minute)
  and is offline after that. Never delete or replace the `qpc-v2-fonts`
  Release: its SHA-256 is pinned in `content/manifest.json`.
- `verify`, `audit`, and `fonts` read only `content/raw` and
  `content/work/verify`, so they run on the owner's machine only. `ls
  content/raw` before a release starts: if the manual QUL and QuranEnc
  exports are gone, say so before the release begins rather than at gate
  time (D-078).
- The committed `content/quran.db` is the shipped content and a local
  rebuild must reproduce its SHA-256 exactly. If it does not, stop and find
  out why before committing anything.
- **Verify by process exit code**, never by grepping piped output.
- CI runs the JVM suite, the content gates it can run, the data instrumented
  tests, and the signed bundle in `build.yml`, which filters pushes by path
  so a doc-only commit triggers nothing. The app instrumented tests run in
  `screenshots.yml` on the three store form factors, and that workflow names
  six classes: `ScreenshotTest`, `WordByWordTest`, `MushafTurnTest`,
  `TafsirDirectionTest`, `SavedNotesTest`, `SettingsVisibilityTest`. Every
  other app test runs on a session's own emulator or not at all, and two of
  them had been red for nobody to see (D-116), so a session that changes
  the settings sheet, and every release session, runs the whole app suite
  rather than only the six.
- Do not hand-drive the app to verify UI. After an emulator crash the
  injected input in the screen's top band can go dead while the rest keeps
  working, captures return older frames, and the clock jumps. The loop is:
  JVM suite and lint locally, push, then read the tour's frames from the
  capture run. Every UI claim in a hand-over is backed by a CI frame or a
  compose-test run, never by hand-taps.
- Toolchain: JDK 17, current AGP, Kotlin, Gradle, and the SDK levels in the
  build files. Versions live in `gradle/libs.versions.toml`, never here.
  The Gradle wrapper is committed.

## Release

The full runbook is `play-store/RELEASE.md`; the capture's failure classes
are `docs/screenshot-failures.md`. What belongs here is the law around it.

1. **The owner's word first.** One build carries the whole session, and the
   bundle and the screenshots are one delivery: collected, checked, and
   handed over together, before the owner submits anything. A set refreshed
   after the submission has nothing left to be used for.
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
   in the later run and in nothing else (D-093). The run's artifact carries
   the bundle and its SHA-256, and GitHub deletes it after two weeks, so a
   signed build is never sitting in public. Delete the hand-off copy in
   `play-store/aab/` once Play has it.
5. **Signing is proved by its words.** `jarsigner -verify` exits 0 on an
   unsigned file, so the check reads "jar verified" and prints the
   certificate's SHA-256 (`53:7D:09:D2:...:0D:9D:E5:21`, D-017). The
   `signed-bundle` job fails when the four secrets are absent rather than
   producing an unsigned artifact, because "it built" must not be mistaken
   for "it is signed".

## Map

| Path | What is there |
| --- | --- |
| `core/` | pure JVM Kotlin, zero `android.*` imports: references, search normalization, rich text parsing, models |
| `data/` | read-only content access, the saved-ayah and last-read user databases, the page font store, preferences |
| `app/` | the shell: activity, view model, the screen that composes the features, the shell's strings |
| `feature-*/` | one reading surface each (mushaf, study, search, browse, playback, settings), each owning its own strings and icons |
| `ui-kit/` | the shared look: theme and palettes, the hand-drawn icons, the rich text views, the small formatters |
| `content-assets/` | the shipped assets the app reads: the 604 page fonts, the study and UI faces, the core pack, and the pack catalog |
| `tools/` | the offline pipeline: fetch, verify, audit, build, fonts, packs, audio |
| `content/` | `quran.db` (built and committed), `packs/`, `manifest.json`, `recitation-manifest.json`, `audit-report.md`; `raw/` is local and gitignored |
| `docs/` | the ADRs and the design, this file's siblings in the table above |
| `play-store/` | listing, screenshots per form factor, the hand-off AAB, the runbook |
| `benchmark/` | the startup profile's generator, development only, never in the bundle |
| `.github/workflows/` | `build.yml` (gates, data tests, signed bundle), `screenshots.yml` (the store set) |

## Glossary

- **the content database**: the read-only SQLite built by `tools/` from the
  manifest, shipped as an asset, replaced wholesale on update.
- **the manifest**: `content/manifest.json`, the one place a dataset's
  source, version, license, and checksum are recorded.
- **Mushaf mode**: the 15-line page, glyph-rendered, swiped.
- **study mode**: the ayah-by-ayah reader with translation, word-by-word,
  and tafsir.
- **the page / the line / the word / the glyph**: the Mushaf geometry, from
  page down to one word and its rendered shape.
- **the reference**: an ayah key in `surah:ayah` form, for example 2:255.
- **the portion**: the reader's chosen daily amount. Never called a streak.
- **the study card**: the sheet one ayah opens: its text, the translation
  with footnotes, and the Words, Ibn Kathir, and As-Sa'di panels.
- **the text button**: `ui-kit/TextButton`, the one shape a word that acts
  wears. A heading, a name, or a label is bare type; anything that answers a
  touch is a rounded shape.
- **the search sheet**: one field over the Arabic text, the translation, and
  the surah names; results stay in Mushaf order.
- **the pack**: one downloadable set of content or recitation, addressed by
  its own hash. The app reads a pack and never hotlinks one.

## Frozen choices (do not reopen without approval)

Appealable: bring a genuinely better idea to the owner and, if approved,
implement it and update this list. The reasoning is `docs/decisions.md`.

- The name and the three strings are frozen (D-001).
- A settings row's value is under its name, on every row in the sheet, and
  never in a column at the right (D-134).
- Reader-first, no tab bar (D-008).
- Both reading modes ship together (D-008).
- The reading modes are one switch in the top bar, and the reader's other
  doors are Browse, Search, and Settings; there is no bottom bar (D-051).
- Last Read is the fourth Browse tab beside Surahs, Juz, and Saved, and a
  note lives inside Saved with the ayah it was written on (D-051, D-074,
  D-101).
- A surah row in Browse opens its own ayah grid; there is no separate Go to
  Ayah tab (D-101).
- Text sizes are 0.65, 0.75, 0.85, 1, 1.2 (D-051, D-074).
- Saheeh International is the only translation (D-004).
- Ibn Kathir and As-Sa'di are the tafsirs (D-005).
- Minshawi and Husary are the only reciters (D-007).
- No streaks, no gamification (D-009).
- The design direction is the manuscript language described in D-010.
- Content is sourced from QUL and QuranEnc under D-003, with every license
  honored and a takedown path in About.
- Continue to the next surah is off by default and is the reader's one-time
  word for the packages that follow the surah being heard (D-105).
- What happens at the end of the audio is one answer, not three switches:
  repeat the ayah, repeat the surah, or continue, and turning one on turns
  the other two off (D-118).
- The app never asks for the phone's exact alarm grant, and no settings row
  or button offers it: the reminder takes the best alarm the phone allows
  and says nothing about the difference (D-130).

## Traps

D-129 in `docs/decisions.md` holds the classes that cost a session to find,
one entry each, with the file, test, or decision that owns the detail. Five
of them are worth reading before any run:

- **Read the frames a red leg kept before touching anything.** The log says
  which assertion failed; the frame says what the reader was looking at.
  Three wrong diagnoses in a row were solved by one kept frame.
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
| `content/work/fonts-v2`, `qpc-v2-font` | `./gradlew :tools:run --args="fetch"` |
| `content/work/db`, `json`, `qpc-v2.db` | `./gradlew :tools:run --args="build"` |
| `content/work/audio-dev` | `./gradlew :tools:run --args="audio sample"` |
