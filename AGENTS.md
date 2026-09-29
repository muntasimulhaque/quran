# Quran: working rules

**The first task of every session, before reading this file and before any
other command: `git fetch`, then `git pull origin main`. It is the most
important task in this file; a session that has not pulled has not
started.**

A free, offline Android Quran reader. Two reading modes on one text: the
Mushaf page and a study view with Saheeh International, word-by-word,
Tafsir Ibn Kathir, and Tafsir As-Sa'di, with recitation by Al-Minshawi and
Al-Husary. No ads, no trackers, no accounts, no network. Code is MIT;
each content dataset keeps its own source, version, and license.

Store title: `Quran: The Noble Book`. App title and launcher name:
`Quran`. Package: `io.github.muntasimulhaque.quran`, permanent.

**The code is the source of truth.** Why a thing is the way it is lives in
the KDoc next to that thing, and settled choices are tagged
`owner decision, <n>` so `git grep "owner decision"` finds them. This file
carries only what code cannot say: the law, the map, the project's private
vocabulary, and the release runbook. If this file and the code disagree,
the code wins and this file is fixed in the same change. Do not grow this
into a second codebase written in prose.

## This file is not the final word

The rules are the current best understanding, not the ceiling. If a change
you believe in contradicts one, do not drop it silently and do not
implement against the rule either: name the conflict, make the case, and
let the owner decide. An overridden rule is updated here, never left as a
dead letter.

## Every session

1. **Pull first. This is the most important task in this file, and it comes
   before everything else, including reading this file.** The first
   commands of the session are `git fetch`, then `git pull origin main`, in
   that order. The owner works from more than one machine, and no session
   reads a stale file or builds on a stale head.
2. Read this file after the pull, never before and never in the same block
   as it: reading AGENTS.md in the same block as the pull races it, and you
   end up reading the file you were about to update.
3. Do the work, then ask one question: anything else? The build waits for
   the owner's word. No `versionCode` moves until the session is done and
   the owner says so.
4. Session end: append the session's entries to `docs/decisions.md`, bring
   the README status and the release notes current, and leave the tree
   clean.

## Hard constraints (non-negotiable)

1. **Offline except one thing.** The app holds `INTERNET` for exactly one
   purpose, content from the project's own GitHub Releases, approved by the
   owner in D-023, amended in D-105, and amended again in D-109. The reads
   are: a content pack the reader asks for (a translation, tafsir, or word
   list) after they have seen its size and tapped Add; a recitation package
   for one surah after the reader taps Play and then approves the shown
   size, or after the reader has turned on Continue to the next surah, which
   is their word, given once, for the packages that follow the one being
   heard; and a quiet refresh of a pack the reader already has, only when the
   app carries a newer version of it, only on an unmetered connection, and
   only after the first page is readable. Nothing else is fetched at launch,
   no other host is ever contacted, and there is no analytics or telemetry of
   any kind. Everything else in the app works with no connection at all. No
   WebView.
2. **Permissions: media, notifications, the reminder's exact time, and that
   one network use.** The self-declared permissions are exactly `INTERNET`
   (the download above), `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`,
   `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, and `SCHEDULE_EXACT_ALARM` (the
   daily reminder, granted by the owner in the thirty-seventh session: the
   reminder is due at a minute the reader chose, and only an exact alarm
   reaches a phone that is locked and idle at that minute, D-114).
   Library-merged permissions are documented, not fought. A new permission
   needs the owner's sign-off written here first.
3. **No ads, no trackers, no analytics, no accounts.** AndroidX, Kotlin,
   Media3, Room, DataStore, and Glance only. Every new dependency is
   proposed here first.
4. **The sacred text is untouchable.** Not one byte of Quran text,
   translation, or tafsir is ever edited by hand. Content changes only
   through `tools/`, and every dataset carries its source, version,
   checksum, and license in `content/manifest.json`.
5. **The app must not crash.** No `!!`, no unchecked casts, no swallowed
   exceptions. State survives rotation, backgrounding, and process death.
   A Play vitals crash is a stop-the-line event.
6. **Nothing may feel like AI slop.** No placeholder copy, dead buttons,
   lorem text, stock iconography, or generic Material defaults. Every
   string is a real sentence, every color is chosen, every icon is drawn
   for this app.
7. **Accessibility is a rule, not a feature.** TalkBack works end to end.
   Study mode honors the system font scale; Mushaf mode zooms. Contrast
   targets live in the theme and are tested.
8. **Two UI languages, one content language each.** English and Bangla
   ship together: the interface strings live in each module's
   `values-bn`, and the reader's choice sets the translation, the tafsir,
   and the word meanings too (D-067). Arabic is data, not a locale; RTL
   support stays on. A new interface language needs a real translation
   pass and a `values-xx` folder, and its packs need the same review the
   Arabic content gets. The frozen names (`app_name`, `first_paint_title`,
   `first_paint_subtitle`, the store title) are never translated.
9. **Reader first.** The app opens where the reader left off. There is no
   home screen, no dashboard, and no permanent tab bar; index, search,
   library, and settings are sheets raised from a slim bar.
10. **Simple to the bone.** Any age, any device. Nothing stands between a
    person and the Quran: no account, no setup wizard beyond one optional
    first screen, no dialog before reading, no feature that asks a choice
    before the text. If a feature adds friction, it is cut. Every added
    choice must earn its place against the reading itself.
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
- **Store text is plain prose.** Play Console mangles quotes, markdown,
  and dashes. Release notes fit the 500-character field, counted before
  hand-off.
- **Release notes are handed over bare.** In chat the notes are pasted as the
  raw paragraph and nothing else: no blockquote, no code fence, no quotes, no
  label on the same line. Anything wrapped around them arrives in Play
  Console as literal characters, and the owner then pastes a paragraph full of
  `>` or backticks into the store. The heading, the size, and the file name
  go on their own lines above or below the paragraph, never touching it.
- **Small pieces.** Files under 400 lines, functions under 40. Split
  early; a name that says the idea beats a name that says the screen.
- **User-facing strings** live in a `strings.xml` in the module that
  draws them, nowhere in Kotlin: `app` for the shell, each feature for its
  own surface, `ui-kit` for the two mode names. Arabic content stays data.
  Colors live in the theme, nowhere else.

## Build, test, verify

The canonical suite (versions live in `gradle/libs.versions.toml` and the
module build files, never here):

```bash
./gradlew :core:test :data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :data:connectedDebugAndroidTest   # saved store, last read, recitation manifest
./gradlew :app:connectedDebugAndroidTest    # search, word by word, and the screenshot tour
```

CI runs the JVM suite and the content gates in `build.yml`, the data
instrumented tests there too on one phone profile, and the app instrumented
tests, including the screenshot tour, in `screenshots.yml` on all three store
form factors. `build.yml` filters pushes by path, as the family's other apps
do, so a doc-only commit (README, AGENTS.md, the play-store notes) triggers
nothing: an emulator boot, a signed release build, and the content gates are
too much to spend on a paragraph. A pull request is unfiltered on purpose,
because a pull request exists to be verified before it lands. **The store set
is eight frames per form factor** (phone,
7 inch, 10 inch), never more: the tour and the numbered list in
`play-store/listing.md` are the same eight, and a change to one is a change
to the other. The emulator is cold and software rendered, so the screenshot
workflow caches the AVD per form factor and waits for the emulated storage
to mount before it starts the test; a fixed sleep is not the answer (the
tablet legs were failing on waits that were tight for the slowest profile).

Content pipeline, from the repository root. Keep every one of these green
at every step:

```bash
./gradlew :tools:run --args="fetch"      # brings the font pack to a new machine, hash-checked
./gradlew :tools:run --args="verify"     # checksums and structure of every source
./gradlew :tools:run --args="audit"      # letter-level audit against Tanzil
./gradlew :tools:run --args="search"     # Arabic round trips and English folding
./gradlew :tools:run --args="audio sample"  # the development recitation sample (debug only)
./gradlew :tools:run --args="audio packs"   # one ZIP per reciter per surah, plus the manifest
./gradlew :tools:run --args="audio publish" # uploads the packages to their GitHub Releases
./gradlew :tools:run --args="build"      # writes content/quran.db, deterministic
./gradlew :tools:run --args="fonts"      # font coverage for every codepoint
./gradlew :tools:run --args="checkdb"    # verifies the committed database and its report
```

- The first build on a machine that lacks the page fonts runs `fetch`
  once (about a minute) and is offline after that. The font pack lives on
  the `qpc-v2-fonts` GitHub Release, pinned by SHA-256 in
  `content/manifest.json`; never delete or replace it.
- The committed `content/quran.db` is the shipped content. A local rebuild
  must reproduce its SHA-256 exactly; if it does not, stop and find out
  why before committing anything.

- Verify by process exit code, never by grepping piped output.
- **The app's instrumented suite is six named classes on CI.** The capture
  workflow runs `ScreenshotTest`, `WordByWordTest`, `MushafTurnTest`,
  `TafsirDirectionTest`, `SavedNotesTest` and `SettingsVisibilityTest` on its
  three legs, and `build.yml` runs only the data suite. Every other app test,
  the settings geometry ones included, runs on a session's own emulator or not
  at all, and two of them had been red for nobody to see (D-116). Any session
  that changes the settings sheet, and every release session, runs the whole
  app suite on the emulator rather than only the six.
- `local.properties` is gitignored; recreate it with the machine's SDK
  path when it is lost.
- Content is built by `:tools` from `content/`, verified against
  `content/manifest.json`, and committed only as the built database and
  fonts. Audio is fetched once into the pack build, never hotlinked.
- Toolchain: JDK 17, current AGP, Kotlin, Gradle, compile and target SDK
  and minSdk as in the build files. The Gradle wrapper is committed.

## Build, test, verify

**Session setup on this machine.** These are environment facts the harness
does not tell you, and each one costs a failed command to rediscover:

- Gradle needs `JAVA_HOME`, and the shell has no JDK on `PATH`. Prefix every
  invocation with `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`.
- `adb` is `C:/Users/Dev Pro/AppData/Local/Android/Sdk/platform-tools/adb.exe`
  and the emulator is
  `C:/Users/Dev Pro/AppData/Local/Android/Sdk/emulator/emulator.exe` with the
  AVDs `Pixel_4_35` (phone), `Nexus_7_35` (7 inch), `Pixel_C_35` (10 inch),
  and `api27`; neither binary is on `PATH`. Start one headless with
  `-no-window -gpu swiftshader_indirect` and wait for `sys.boot_completed` to
  report `1`.
- On the `zn` machine the same facts differ: Android Studio's JBR is Java
  25, and the modules' `jvmToolchain(17)` then finds no matching JDK, so a
  real JDK 17 lives at `C:\Users\zn\jdks\jdk-17` (Temurin, downloaded once
  from Adoptium) and every Gradle call begins with
  `export JAVA_HOME="/c/Users/zn/jdks/jdk-17"`; the SDK is at
  `C:\Users\zn\AppData\Local\Android\Sdk`, so `local.properties` reads
  `sdk.dir=C:/Users/zn/AppData/Local/Android/Sdk`; and there are no AVDs, so
  the instrumented suites run in CI only; `gh` is installed at
  `C:\Users\zn\gh\bin` and on the user PATH, authenticated, so the content
  Releases can be published from here.
- MSYS rewrites `/sdcard/...` style arguments into Windows paths. Prefix
  `adb shell`, `adb push`, and `adb pull` with `MSYS_NO_PATHCONV=1`, and the
  same for `gh api` (and drop its leading slash).
- The Store alias intercepts `python3`, and the working interpreter on this
  machine is the harness's own at
  `C:\Users\zn\AppData\Local\hermes\tools\python-3.14.7+20260901-win32-x64\python.exe`,
  with Pillow, fontTools, arabic-reshaper, and python-bidi installed into it
  by hand for the store-asset work (D-112). Nothing in the build depends on
  it; `node -e` is still the small-edit tool used here, for small file edits
  instead of writing a script file.
- `git pull` works here: the clone set `main` to track `origin/main`. If a
  machine ever says there is no tracking information, use
  `git pull origin main`, or set the tracking once with
  `git branch --set-upstream-to=origin/main main`.
- The working tree mixes LF and CRLF files. A `node -e` replacement anchored
  on `\n` silently no-ops in a CRLF file (replace returns the text
  unchanged and exits 0), which is how import edits went missing this
  session: match `\r?\n`, or use the edit tool (it normalizes), and grep
  afterwards to confirm the change actually landed.
- adb is a Windows binary: with `MSYS_NO_PATHCONV=1` a `/tmp/x` destination
  lands at `C:\tmp\x`, and adb cannot create `/c/tmp/...` at all. The read
  tool resolves `/tmp/...` Windows-style, so `/tmp/x` written by adb is read
  back as `C:\tmp\x`. Plan paths around that instead of debugging a
  missing file.
- Two tool calls in one block run in parallel: never edit a file and read it
  (or run two reads whose results must stay in order) in the same block.

**Verifying UI on this machine.**

- **Do not hand-drive the app to verify UI.** After one emulator crash this
  session, injected input in the top band of the screen (the whole top bar,
  through both `input tap` and the compose test's own injection) went dead
  while everything below it kept working, display captures returned older
  frames, and the device clock jumped. Chasing the cause cost hours and
  found nothing app-side. One reboot is allowed; if the symptoms persist,
  stop. The verification loop is: JVM suite and lint locally, push, then
  read the tour's frames from the screenshots run. The CI runners are
  healthy and are the authority; every UI claim in a hand-over should be
  backed by a CI frame or by a compose-test run, not by hand-taps.
- Before any local connected run, put the same device settings on the
  emulator that `screenshots.yml` sets (`settings put global
  hide_error_dialogs 1`, `settings put secure anr_show_background 0`) and
  check `dumpsys window windows | grep "Application Not Responding"`. A
  stuck ANR window fails the tour on every attempt and a reboot clears it;
  setting the flags alone does not remove a dialog already up. A freshly
  wiped or freshly booted emulator fails `searchIsFastWhenWarm` and can
  raise dialogs on its first run: let one run warm it before trusting any
  result.
- After a gradle `connectedDebugAndroidTest`, the app is uninstalled and
  its on-device frames are gone with it. Pull frames in the same breath, or
  drive the tests with `am instrument` against APKs you installed
  yourself.
- A screenshot decides nothing through the read tool unless its caption's
  `original WxH` matches the artifact's real size: the tool has served a
  same-named file from a different folder and has shuffled two reads in one
  block. Read one frame per turn from a uniquely named copy, and check a
  whole set with pixel signatures in python (the sheet's top edge, the
  lapis chips, the keyboard) instead of by eye.

## Release hand-off

The bundle and the screenshots are one delivery. They are collected, checked,
and handed over together, before the owner submits anything, because a
screenshot refreshed after the submission has nothing left to be used for:
the store already has the old set.

0. **Confirm the owner gates can run, first.** `ls content/raw` must show
   the manual QUL and QuranEnc datasets, not only the two font zips, and
   `content/work/verify` must hold the extractions. `verify`, `audit`, and
   `fonts` are part of a release and read only those files; if they are
   gone, say so before the release starts rather than at gate time (the
   twenty-second session lost an hour that way and shipped with the three
   named as owed, D-078).
1. Raise `versionCode` by 1 and raise `versionName` by one tenth within
   the major line: 0.1 through 0.9, then 1.0, then 1.1 through 1.9, then
   2.0. There is no 0.10: the tenth release of a major line is its `x.0`,
   and a version name is never written with a two-digit minor. Update the
   version line in `play-store/listing.md`.
2. Release notes go to `play-store/listing.md` as plain flowing text,
   one unbroken line per bullet, under 500 characters.
3. Run the full CI suite locally.
4. Commit, push, confirm CI is green. The `signed-bundle` job in `build.yml`
   signs on every push to `main` and leaves the bundle in the run's artifacts.
5. Screenshots, **before the hand-over, whenever visible UI changed.** The
   push in step 4 starts the `screenshots` workflow on its own when
   `app/src/main`, `app/src/androidTest`, or the app build file changed. Wait
   for it, then collect all three form factors and install them:

   ```bash
   gh run list --workflow=screenshots.yml --limit 1
   gh run download <run-id> -n store-screenshots-phone   -D <dir>
   gh run download <run-id> -n store-screenshots-tablet7 -D <dir>
   gh run download <run-id> -n store-screenshots-tablet10 -D <dir>
   ```

   A red leg keeps its frames (D-075): read the failure first, then
   `gh run rerun <run-id> --failed`; one boot-time ANR on the 10 inch leg
   cost exactly one rerun (D-078). `gh run download` nests the artifact's
   own path structure, so locate the file with `find` instead of assuming
   the destination's layout.

   Each artifact prefixes its frames with the form factor; strip that prefix
   into `play-store/screenshots/<form>/` and verify every frame against the
   artifact with `cmp` rather than installing them on trust, because a leg
   can pass while holding a frame nobody should ship. If nothing visible
   changed, say so in the hand-over and do not run it: a new set that is
   byte-identical is CI time spent for nothing.
6. Pull the signed bundle from the step 4 run into `play-store/aab/`:

   ```bash
   gh run download <run-id> -n quran-signed-aab -D play-store/aab
   ```

   The artifact carries the bundle and its `SHA-256`, and GitHub deletes it
after two weeks, so a signed build is never sitting in public and never
sitting around. **Take it from the newest green `build` run on `main`, not
the first one after the version bump:** everything committed after the bump
is in the later run and in nothing else, and 2.0's first download was the
release commit's bundle while the card fix that followed it was already in
the tree (D-093).
7. **Hand over the bundle and the screenshots in the same message**, with the
   size, the checksum, what changed in the set (or that nothing did), and the
   notes pasted verbatim. Delete the hand-off copy once the owner confirms the
   Play submission. Hand the notes over as a bare paragraph: no blockquote, no
   fence, no wrapping quotes, nothing on the same line as the text.

**The fast path, measured in the twenty-ninth session.** A release is about
fifteen minutes of wall clock, and the order below is what keeps it there.

- The local suite is about six minutes: the JVM suite and lint, the two
  instrumented suites on one emulator, and the five owner gates. Run it
  once, then push once.
- The push starts `build` (about five minutes) and `screenshots` (about
  four and a half) together on their own runners, so the wait is the longer
  of the two and never the sum. While they run, write the decisions entry
  and the listing's set paragraph; do not poll from the first minute.
- Never push twice for one release. A docs-only or `play-store`-only push
  triggers nothing; a test-only push triggers a second full capture for
  frames the first set already holds, so capture-test fixes ride the
  release commit or wait for the next one.
- Verify the set numerically before reading anything: the differing
  bounding boxes say which frames carry content and which carry only the
  status bar clock. Read the content frames, one per turn from uniquely
  named copies, then `cmp` all twenty-four against the artifacts. Reading
  the whole set by eye is the slowest step in the hand-off and the one that
  adds nothing.
- The capture leg itself is the next place to cut. Each leg spends one to
  two minutes on a cold boot because the workflow passes `-no-snapshot`;
  the AVD cache per form factor already exists. The first release task of
  the next session is to keep a booted snapshot in that cache and measure
  the leg with it, reading `docs/screenshot-failures.md` first if a leg
  goes red. The target is a leg under three minutes.

**The screenshot set is eight frames per form factor**, so phone, 7 inch, and
10 inch at eight each, twenty-four images in all. The tour (`ScreenshotTest`)
and the numbered list in `play-store/listing.md` are kept in step with that
count, so no frame is captured that the listing does not explain, and none is
listed that is not captured.

The eight are the reader's surface, in order: the Mushaf page, the chrome
over it, the study reading, the surah opening, search, the settings hub,
Browse, and the ayah card. Eight is the number the store shows first and the
number a session can keep honest; a ninth has to earn its place against the
reading, and the tour is trimmed rather than allowed to grow back. Never
capture a listing set by hand: the frames come from the `screenshots`
workflow's artifacts.

**Signing, and the trap in it.** The `signed-bundle` job fails when the four
secrets are absent rather than producing an unsigned artifact, because "it
built" must not be mistaken for "it is signed". `jarsigner -verify` exits 0
on an unsigned file and says so only in its words, so the job reads the words
and prints the signing certificate's SHA-256
(`537d09d20300129e973b7945316bfe24cfadcfbc77eec5229cbf30170d9de521`, the
shared upload key per D-017). Signing runs only on a push to `main`, never on
a pull request, and that job's actions are pinned by commit rather than tag.
The keystore and its properties never enter the repository. The owner's
machine can still build from the vault, where Gradle probes each drive and
layout the folder has worn; an absent keystore there degrades to unsigned,
never to a failed build.

The four secrets, and where their values come from, are in
`play-store/RELEASE.md` step 3c.

**A release is not only a version bump.** The version line, the notes, and
the screenshots must match what is being shipped, and the notes of the
release before it are kept under their own heading (`## Release notes (0.2)`)
rather than replaced. Refresh the committed screenshot set from the CI
artifacts, never by hand, and use `gh run download <run> -n
store-screenshots-<form> -D <dir>`.

The content gates that only the owner machine can run (`verify`, `audit`,
`fonts`) are part of the release, not decoration: a release is the moment to
run all of them, because a broken one will not fail in CI and will not be
noticed until it is needed. If one fails, fix it and note the failure here
before moving on.

## Store screenshots

The capture runs on GitHub's runners, so it is the same machine every time and
this section is the whole procedure. Nothing here depends on a local computer,
and nothing here is rediscovered by reading another repository: if a leg ever
fails, the fix belongs in this section and in `screenshots.yml` before the next
attempt.

**The one command that captures.** Push any commit that touches a module which
can draw (the `paths` filter in `screenshots.yml` names them all) or run the
workflow by hand; a warm run is about four minutes on all three legs. The
paths filter must name **every module that can put a pixel on the screen**
(`app/`, `ui-kit/`, `feature-*/`, `content-assets/`), because a UI label lives
in its feature's `strings.xml`: a filter that watched only `app/` once let a
settings rename ship a stale set. When a session has not changed a pixel, it
says so in the hand-off and does not run the capture.

**What makes a leg pass, in the workflow itself (do not remove these):**

1. `settings put global hide_error_dialogs 1` and
   `settings put secure anr_show_background 0` run before the test. This is
   the deterministic fix for the trap below: the dialogs are suppressed at the
   device level, so a red leg is a real failure rather than an environment
   flake. Dismissing dialogs after they appear loses the race on the 10 inch
   leg.
2. The script waits for `/sdcard/Android` before starting the test. A cold
   boot reports completion before its emulated storage is mounted, and the
   test's output directory breaks if it starts first.
3. The capture build passes `-Pquran.devPacks=screenshot`, so the debug APK
   carries only the packs the capture tests install. Every pack is 197 MB of
   install on every leg; the tour opens none of the rest.
4. The run is one `connectedDebugAndroidTest` invocation, on one line, with
   the test classes named. A second invocation would replace the first's
   additional output, and the emulator runner feeds the script to `sh`, which
   chokes on multi-line continuations. The exit code is kept and returned at
   the end, after the frames are collected, so a red leg still leaves its
   capture in the artifact to be read.
5. The `paths` filter and the capture build are why a run is minutes, not an
   hour: the Gradle build cache is restored (`gradle/actions/setup-gradle`),
   not only the dependency cache, and the AVD is cached per form factor.
6. A leg must produce every frame the tour captures (eight), and the capture
   test checks the window list before keeping any frame. A frame of Android is
   worse than no frame at all. Install a set only after comparing every frame
   with its artifact by `cmp`, and read every changed frame for its content
   before it ships: a green leg and a matching byte count say nothing about
   which sheet is in the picture.

**The tour itself.** It anchors on test tags and content descriptions, never on
user-visible copy: a tour that waited on the word `Appearance` broke when the
label became `Theme`, which is a copy change, not a UI defect. Tags are stable;
copy is not. `waitForTag` is the anchor; `waitForText` is for text the tour
itself types. It waits for two identical frames on this software rendered
emulator, never a fixed sleep.

**Collecting a set.** The workflow uploads `store-screenshots-<form>`, phone,
tablet7, and tablet10, every leg even when its test fails (`if: always()`):

```bash
gh run list --workflow=screenshots.yml --limit 1
gh run download <run-id> -n store-screenshots-phone   -D <dir>
gh run download <run-id> -n store-screenshots-tablet7 -D <dir>
gh run download <run-id> -n store-screenshots-tablet10 -D <dir>
```

Each artifact prefixes its frames with the form factor; strip that prefix into
`play-store/screenshots/<form>/` and verify every frame against the artifact
with `cmp` rather than installing them on trust, because a leg can pass while
holding a frame nobody should ship. If nothing visible changed, the capture is
not run and the hand-off says so.

**If a leg fails, the order of operations.** Read the failing job's log
(`gh run view --log --job <id>`), and read it in two passes: first the
step list, then the error. **Every failure mode this workflow has shown is
catalogued in `docs/screenshot-failures.md`, with its class, its evidence,
and its fix; read it before diagnosing anything.** The step list says which
of the four classes the failure is in, and they need different actions:

- **The leg died before our script ran (infrastructure, rerun it).** The
  failing step is `Capture on emulator` or `Create AVD and generate a
  snapshot to cache`, and the log shows the emulator action's own `Install
  Android SDK` failing, or `Error on ZipFile unknown archive`, or
  `adb -s emulator-5554 emu kill` answered by `could not connect to TCP port
  5554: Connection refused`. The runner truncated a download of the system
  image or the emulator; the app, the tour, and the capture's load are all
  innocent. Rerun the failed leg once. If the same leg fails in the same
  pre-script step again, stop and report a runner outage: no change in this
  repository can fix it, and twenty minutes of log reading has never found
  one. This is the class the 1.8 push met (run 35883297322, tablet7, `Error
  on ZipFile unknown archive`).
- `a system dialog stayed over <frame>` means the dialog suppression at the
  top of the script is missing or was removed; restore it. (The guard now
  names the window, as `a system window (<pkg>) stayed over <frame>`, and
  reads the focused window's owner rather than a stale record in the dump;
  see `docs/screenshot-failures.md`.)
- `Test <name> FAILED` with a node assertion means the tour anchored on
  something the UI moved or renamed; fix the anchor to a tag. Two signatures
  this has worn: `Failed to inject touch input ... could not find any node`
  for a `Switch to the Mushaf page` that the real app did not have up
  (the tour's first press), and `ComposeTimeoutException` for a wait whose
  anchor never appeared. Both are real findings about the tour, never a
  reason to rerun.
- `device offline` or `device not found` on one leg while another passes the
  same code is the capture's load, not the runner; make the capture lighter,
  do not rerun.
- A missing artifact means the script exited before `mkdir store-shots`.
  The emulator runner runs **each script line in its own shell**, so the
  Gradle exit status must be saved on the Gradle line itself
  (`... ; echo $? > /tmp/gradle_status`) and every collection line must be
  guarded, or the runner stops the script at the failed Gradle line and the
  frames are lost. `docs/screenshot-failures.md` has the whole history.

Rerun only after the failing step above is named, and only for the
infrastructure class or "the capture's load": a node assertion or a dialog
in a frame is fixed in the same session, in `screenshots.yml` or the tour,
never rerun away.

**Reading a rerun's logs and artifacts.** `gh run view --log --job <id>`
serves the **latest attempt's** log even when the id is the original failed
job's, so a green log under a red job id is the rerun, not a contradiction.
The API is the record: `gh api repos/muntasimulhaque/quran/actions/jobs/<id>
--jq '{run_attempt, conclusion}'` says which attempt an id belongs to, and
`gh api "repos/muntasimulhaque/quran/actions/runs/<id>/artifacts?attempt=N"`
lists each attempt's artifacts. A rerun's artifact is a valid source for the
store set once every frame is `cmp`'d and every changed frame is read, the
same as any other: attempt 2 of run 35883297322 is the 1.8 capture.

## Content rules

- Every dataset in `content/manifest.json` records: source name, URL,
  version or edition, license or permission basis, checksum, and the
  credit line shown in About.
- The canonical Arabic text and the Mushaf glyph system are decided in
  `docs/decisions.md` (D-006, D-011). The audit against Tanzil is part of
  the build, not a one-time act.
- Recitation files are downloaded once from the recorded URLs, verified
  by checksum, and bundled into asset packs. The app never streams.
- About carries a content sources and licenses screen, a content version,
  and a corrections and rights contact row, so any single dataset can be
  replaced in one update.

## Map

Where truth lives, by question (filled in as code lands):

| Path | What is there |
| --- | --- |
| `core/` | pure JVM Kotlin, zero `android.*` imports: references, search normalization, rich text parsing, models |
| `data/` | read-only content database access, the saved-ayah user database, the last-read user database, page font store, preferences, models; instrumented tests in `data/src/androidTest` |
| `app/` | the shell: activity, view model, the screen that composes the features, the shell's strings |
| `feature-*/` | one reading surface each (mushaf, study, search, browse, playback, settings), each owning its own strings and icons |
| `ui-kit/` | the shared look: theme and palettes, the hand-drawn icons, the rich text views, the small formatters |
| `tools/` | offline pipeline: content fetch, verify, audit, database build, font checks, golden renders |
| `content/` | `quran.db` (the built, committed content database), `recitation-manifest.json` (published recitation packages with their sizes and hashes), `manifest.json`, `audit-report.md`, `build-report.json`; raw downloads under `raw/` are local and gitignored |
| `docs/` | `decisions.md`, `content-sources.md`, privacy page, bundled font licenses |
| `play-store/` | listing kit, screenshots per form factor, hand-off AAB |
| `.github/workflows/` | `build.yml`, `screenshots.yml` |

## Glossary

Terms this project uses as private vocabulary.

- **the content database**: the read-only SQLite built by `tools/` from
  the manifest, shipped as an asset, replaced wholesale on update.
- **the manifest**: `content/manifest.json`, the single place where a
  dataset's source, version, license, and checksum are recorded.
- **Mushaf mode**: the 15-line page, glyph-rendered, swiped.
- **study mode**: the ayah-by-ayah scrolling reader with translation,
  word-by-word, and tafsir.
- **the page / the line / the word / the glyph**: the Mushaf geometry,
  from page down to one word and its rendered shape.
- **the reference**: an ayah key in `surah:ayah` form, for example 2:255.
- **the portion**: the reader's chosen daily amount. Never called a
  streak.
- **the study card**: the bottom sheet one ayah opens: its text, the
  translation with footnotes, and the Words, Ibn Kathir, and As-Sa'di
  panels.
- **the text button**: `ui-kit/TextButton`, the one shape a word that acts
  wears. A heading, a name, or a label is bare type; anything that answers a
  touch is a rounded shape (primary for what the reader came to do, quiet for
  what ends or removes).
- **the search sheet**: one field over the Arabic text, the translation,
  and the surah names; results stay in Mushaf order.
- **the pack**: a Play asset pack, one per reciter; the app reads it, the
  app never downloads it.

## Decisions: do not reopen without approval

Appealable; bring a genuinely better idea to the owner and, if approved,
implement it and update this list.

- The name and the three strings are frozen (D-001).
- Reader-first, no tab bar (D-008).
- Both reading modes ship together (D-008).
- The reading modes are one switch in the top bar, and the reader's other
  doors are Browse, Search, and Settings; there is no bottom bar (D-051).
- Last Read is the fourth Browse tab beside Surahs, Juz, and Saved; a note
  lives inside Saved with the ayah it was written on (D-051, D-074, D-101).
- A surah row in Browse opens its own ayah grid; there is no separate Go to
  Ayah tab (D-101).
- Text sizes are 0.65, 0.75, 0.85, 1, 1.2 (D-051, D-074).
- Saheeh International is the only translation (D-004).
- Ibn Kathir and As-Sa'di are the tafsirs (D-005).
- Minshawi and Husary are the only reciters (D-007).
- No streaks, no gamification (D-009).
- The design direction is the manuscript language described in D-010.
- Content is sourced from QUL and QuranEnc under the owner decision in
  D-003, with all existing licenses honored and a takedown path in About.
- Continue to the next surah is off by default and is the reader's one-time
  word for the packages that follow the surah being heard (D-105).
- What happens at the end of the audio is one answer, not three switches:
  repeat the ayah, repeat the surah, or continue. Turning one on turns the
  other two off (D-118).

## Traps with no code home

- A gate that reads the content by table names is reading a **contract**, not
  a schema: the pack split moved translations, tafsirs, word lists, and surah
  names into their own `content/packs/*.db` files, and `tools fonts` silently
  kept asking the old monolith for a `translation` table, a `tafsir_passage`
  with a `source` column, and a `word.translation`. It had been broken since
  that split and nothing noticed because it is an owner-machine gate, not a
  CI gate. When you change the content build's shape, run every
  `:tools:run --args=...` gate, not just the ones CI runs.
- A script that has been true for a year is not evidence it still runs. The
  same gate also pointed at `app/src/main/res/font`, which moved to
  `content-assets/src/main/res/font`.
- Reading text in a script no bundled face carries is a **decision**, not a
  tofu bug: Android draws Bangla with the platform's own Noto Sans Bengali,
  which is why the Bangla packs render. `tools fonts` now names every such script in one
  allow-list; a new script fails the gate until someone writes it down.
- `createEmptyComposeRule()` beside your own `ActivityScenario` can compose on
  a thread with no looper, and the study list's prefetch scheduler throws
  `The current thread must have a looper` on a loaded emulator. The supported
  pairing is a compose rule that owns the activity; put library preparation in
  an `ExternalResource` and chain it with `RuleChain.outerRule(...).around(...)`
  so the app starts after the packs are in place.
- The launch picture (`feature-mushaf/PageCache`, `cacheDir/last-page`) is
  keyed by page, pixel width, and theme name. A launch at another width or in
  another theme must miss it, not stretch it. Nothing depends on it: a miss is
  the old first paint. Two writes can race here (a settle and the flick after
  it), so `PageCache.save` is `@Synchronized` and guarded whole: the losing
  writer used to reach `copyTo` on a temporary the winner had renamed away,
  and the `NoSuchFileException` escaped a worker and killed the app. The other
  half of the same bug is the page LRU recycling a bitmap while the writer
  compresses it, so `PageRenderer.rememberStartupPage` copies the bitmap under
  the cache lock before writing. A crash the reader meets by turning a page is
  this class until proven otherwise.
- A screen capture on a software rendered emulator can lag the composition by
  seconds. The screenshot tour waits for two identical frames before it keeps
  one; never replace that with a fixed sleep. A test that waits for text does
  the same: a slow emulator is not a failing reading aid, so the waits are
  minutes, not seconds.
- A DataStore is a flow, and re-applying every emission over the in-memory
  state lets an old stored value overwrite a choice the reader just made. Read
  it once (while the library opens) and let the view model own the state from
  then on; that is what stopped the reading place from jumping back.
- A list that keeps drawing the previous item's data while the next loads will
  write the previous item's position. Hold the loaded key beside the data and
  draw nothing until they match.
- The pack catalog's license line is the licenses of the datasets that pack is
  built from, joined with ` · `, and the app splits on that separator to show
  one per line. `tools/PackSources.kt` is the only mapping from pack to
  dataset; do not hand-write a license into the catalog again.
- Android's text shaper breaks Arabic letter joining at any style boundary
  inside a word. Never color or style part of a word. Word-level
  boundaries are safe; glyph words are safest.
- Some devices ship SQLite without the `unicode61` tokenizer. Search uses
  precomputed normalized columns, never FTS tokenizers.
- Emulator screenshots and local captures prove nothing about Arabic
  shaping. Shaping and glyph fidelity are verified from CI artifacts and
  golden renders.
- A screenshot leg can pass while holding a photograph of Android, and it can
  also fail over one. A loaded software-rendered emulator raises "Pixel
  Launcher isn't responding", the dialog sits over every frame after it, and
  it swallows the taps the tour is making, so the tour ends up on the wrong
  screens: the 0.5 set once came back with that dialog in all sixteen 10-inch
  frames and the workflow still green, and the 1.3 push failed the 10-inch leg
  because the dialog appeared mid-tour. Clearing a dialog that is already up
  loses that race. The workflow now suppresses the dialogs at the device level
  (`settings put global hide_error_dialogs 1`, `settings put secure
  anr_show_background 0`) before it starts the test, which makes a red leg a
  real failure; the capture still checks the window list before keeping a
  frame and the leg requires every frame the tour captures (eight). Even so,
  install a set only after comparing every frame with the artifact: the guard
  catches the dialog, and `cmp` is what catches everything else.
- The bundle and the screenshots are one delivery, and the screenshots come
  first. A set refreshed after the owner has submitted has nothing left to be
  used for: the store already holds the old one, so the work is wasted and the
  session has spent CI minutes and an emulator run for nobody. Collect and
  verify both, then hand both over in the same message, before the submission.
- The store set is captured from the Compose root with `captureToImage`, one
  capture per frame, never by driving the running app with full-screen
  captures through a settle loop: that loop took the phone emulator down five
  times in a row while the tablet legs passed. A modal sheet is its own
  window, which the Compose root cannot PixelCopy, so the four sheet frames
  (search, settings, Browse, and the ayah card) keep a display capture only
  when two captures in a row are identical, with the window list checked
  first: a sheet's semantics exist before its window has drawn and its
  opening animation has settled, and a frame kept before that photographs
  the reader under the sheet (the Browse frame lost that race on all three
  legs once, and its wait matched the reader's own title behind the sheet,
  so the tour now waits on each sheet's test tag and never on text a sheet
  covers). A root frame cannot hold a system dialog,
  so only the sheet frames need that guard, and the whole tour runs in about
  half a minute on a phone profile.
- A screenshot leg that dies with `device offline` while another leg passes
  the same code is the capture load until proven otherwise, not the runner.
  Five reruns of the phone leg cost about forty minutes, and the fix was one
  commit that made the capture lighter. Read the failing job against the
  passing one, and read the family's sibling repos in `Documents/GitHub`
  before spending a second retry: each of them already carries the capture
  shape that works. A lighter test is also a faster one; the same tour went
  from a leg that sometimes killed the emulator to a green run in seconds.
- A leg can also die before our script, inside the emulator action's own
  setup: `Install Android SDK` failing with `Error on ZipFile unknown
  archive` is a truncated system-image download on that runner, and the
  `Connection refused` on the action's own terminate is the same outage
  finishing its sentence. The phone and tablet10 legs passing the same
  commit in the same run is the proof. That class is a rerun, once; a second
  failure in the same pre-script step is a runner outage to report, not a
  thing to debug. Do not read a tour defect into a leg whose test never
  started: the step list says whether the capture test ran at all.
- After a rerun, `gh run view --log --job <failed-id>` serves the latest
  attempt's log, so a green log can appear under the original red job id.
  `run_attempt` from the jobs API says which attempt is which, and the
  artifacts endpoint takes an `attempt` parameter. That matters twice: to
  avoid recording a rerun's log as the failure's evidence, and to know which
  artifact holds the frames that were actually installed.
- An emulator workflow is not a test: it is a machine. Cache the AVD per form
  factor, wait for `/sdcard/Android` to exist before starting the test (a cold
  boot reports completion before its storage is mounted, and the test's output
  directory breaks if it starts first), and keep the app's instrumented tests
  in that workflow only. Running them in `build.yml` as well pays twice for
  the same answer and is what pushed the slow tablet leg past its timeout.
- The QUL recitation export's `ayah_number` is a global 1..6236 counter;
  the real surah and ayah are the three-digit groups in the audio file
  name. Use the file name, and let verify check the counter.
- As-Sa'di's passage ranges overlap. The shortest passage containing an
  ayah is the one the reader should see.
- The KFGQPC source text is not NFC-normalized and must stay as published;
  normalize only the derived search columns.
- The tafsir source carries Arabic presentation forms and an Urdu heh.
  Display text unfolds presentation forms with NFKC (the Prophet's ligature
  U+FDFA is preserved), and Arabic letters Amiri Quran lacks fall back to
  the bundled Hafs font per codepoint. `tools fonts` mirrors that rule, so
  no codepoint can reach the screen as tofu.
- The reader never sees the raw tafsir HTML: `core/RichText.kt` parses it, and
  `RichText.plain` is the readable form for every surface that cannot draw
  runs (sharing, a search excerpt, a surah introduction). A tafsir search
  result printed `</p><h2>` for two sessions because the excerpt was cut from
  the stored HTML and handed to a view with no parser; the store screenshot
  finally showed it. `tools search` now checks every excerpt it can produce
  and every surah introduction for markup and for a highlight that lands. A
  tag is only what looks like one: the sources carry real angle brackets in
  prose, so never strip a bare `<` or `>`.
- Two modules that declare the same string name is a crash, not a merge: the
  resource table keeps one of them, and `stringResource` then throws
  `MissingFormatArgumentException` on the main thread when the arguments do
  not match the copy that survived. `pack_downloading` was declared by the
  app (two arguments) and by `feature-settings` (one), so tapping Add on a
  translation crashed on the download. Names shared across modules are fine
  only when the whole declaration is identical; `StringNameTest` in `core`
  fails the build on any other case, and it cannot see the merged table, only
  the names.
- English search is diacritic-insensitive: the translation writes Allāh and
  ʿĪsā, so `core/Search.normalizeEnglish` folds every string on both sides
  and the app matches over an in-memory folded index, never over raw
  offsets. `tools search` audits every non-ASCII translation codepoint and
  the Arabic round trip; change the fold and the gate together.
- The user database (`saved.db`) is hand-rolled SQLite, separate from the
  content database and never touched by a content rebuild. A schema change
  means bumping `DATABASE_VERSION` and writing the migration in the same
  session; `SavedStoreTest` pins the behavior. The same is true of
  `last-read.db` (`data/LastReadStore`, capped at twenty places, one row per
  ayah) and `LastReadStoreTest` pins its behavior.
- Android's `rawQuery` binds arguments positionally: a query whose IN clause
  is built from literals must be passed `null`, never the numbers. The
  instrumented search test exists to catch exactly this class of mistake.
- Recitation media ids are `"ayah:surah"`, built from the player's own items;
  the current word comes from the segment that spans the position, and its
  word-table position is `wordFrom + 1`. The player skips ayahs whose files
  are absent, which is why a partial pack plays only what is on the device.
  The id is read by `core/RecitationPlaylist`, never by string surgery at the
  call site, because a surah repeat is a seek to the index it returns and a
  partial pack is a surah that starts at its first ayah on the device, never
  at a later one.
- A surah's end arrives through two events, and a repeat that watches only one
  of them works half the time: `STATE_ENDED`, which is the playlist running
  out, and an automatic transition into the next surah, which is what happens
  when it is already on the device. `REPEAT_MODE_ALL` cannot stand in for
  either, because the playlist grows while the reader listens. And a
  deliberate Next or Previous is a seek, never an automatic transition, so it
  is never intercepted: the reader is never trapped in a surah (D-118).
- A floating control that floats over the page has one measure, and a name cut
  with an ellipsis is a defect, not a wrap. The playback pill spent 192 dp of
  a 369 dp phone on four 48 dp targets and left about 91 dp of text, which is
  fourteen characters: enough for a reciter's name and never enough for a
  surah. So the words keep a measure or they take a line of their own
  (`oneRowFloor` in `feature-playback/PlaybackBar.kt`), and the floor is
  computed from the row's own numbers so a control that changes width is one
  number changed in one place. The offer states carry two controls and keep
  their own row; a measure belongs to the state that needs it (D-119).
- A settings row's tail reserves a column only for a mark the row actually
  has. Holding the switch column empty on a row with no switch ended six hub
  rows with 64 dp of nothing at their right edge, and a reader reads that gap
  as the row not knowing where it stops. A row's own mark is now the last
  thing in it, and the test measures the chevron's *slot* rather than the
  centred mark inside it (D-120, which reverses the other half of D-111).
- The development audio sample lives in `content/work/audio-dev` and is
  bundled into debug builds only; release builds carry no audio until the
  owner decides how the recitations are delivered (D-022).
- `content/recitation-manifest.json` is the contract between the packaging
  tool and the app: one ZIP per reciter per surah, with its byte size and
  SHA-256. The app unpacks a package only when the hash matches, and the
  downloader refuses any entry with a path separator. Never delete or
  replace the `recitation-minshawi` and `recitation-husary` Releases: their
  assets are pinned by those hashes.
- Downloaded packages are flat ayah files named `SSSAAA.mp3` inside one
  folder per reciter. Removing a surah deletes only files with that
  surah's prefix in that folder, and nothing else.
- Never delete or replace the `qpc-v2-fonts` Release asset. Its SHA-256 is
  pinned in `content/manifest.json`, and a fresh clone fetches it from there.
- The study list draws one surah as an opening item, one item per ayah, and a
  closing line. Its indices and the ayah numbers meet only in `core/AyahList`,
  which has a round trip test: reading `firstVisibleItemIndex` as an ayah number
  is what silently moved the reader's place to the start of the surah, and it is
  the kind of mistake that ships.
- The study list draws nothing until the rows for the surah it was handed
  arrive, and it writes the place only from a drag of the reader's own. A list
  of the previous surah's rows under the new surah's name, or a write from a
  programmatic scroll, is what made the reading place jump back and forth.
- `ReaderViewModel` owns the settings after the library opens. `SettingsStore`
  is read once, while the library opens, and never collected again: an
  emission of an old stored place arriving during a jump is what moved the
  reader off the page they had just chosen.
- Text sizes are stored as scale factors (`Float`), never step indices, so a
  choice means the same thing under a changed list of steps. 0.2 wrote them as
  `Int`, so the preference is read off the raw map by key name, not through a
  `Float` key (reading an `Int` through one throws).
- The study list writes the reader's place only after the reader dragged it, and
  the Mushaf writes it only when the page is a new one. Anything else lets a
  jump, a mode switch, or the first frame record a place the reader never chose.
- Listening needs the reciter's word timings and the surah's audio, and the app
  asks for both once, in the pill, with the size and the reciter named in the
  same breath. Adding a reciter in Settings selects it: a reader who downloads
  Husary means to hear Husary.
- Play Core's asset delivery drags WorkManager, Room, and five merged
  permissions, and its R8 release needs extra keep rules. The page fonts
  ship in the base instead (D-018); reopen only with the owner.
- A modal sheet, a dialog, and a popup are their own windows and take the
  Activity's own resources, so a locale provided through a composition local
  reaches the reading and misses every sheet: the settings sheet came up
  English over a Bangla reader. The interface language is applied in
  `MainActivity.attachBaseContext`, read from a synchronous mirror
  (`LanguagePreference`) that the choice writes before it recreates the
  Activity, and the release bundle disables language splitting so both
  languages are inside every install.
- The state a screen switches on is read in the screen's own scope. With a
  wrapper around the screen, a `when` one lambda down missed the switch from
  the first paint to the welcome deterministically on this machine until a
  configuration change forced a recomposition. `QuranApp` reads `ready` and
  `failure` at its top level, where it decides the screen.
* When this machine's emulator crashes mid-session, injected input can go
  dead in a band of the screen (the top bar, through both `input tap` and the
  compose test's own injection) while the rest keeps working, display
  captures can return older frames, and the device clock jumps. A tour
  anchor that matches text *behind* a sheet (the reader's own title under
  Browse) passes without the sheet ever opening, so a green tour on this
  machine proves less than it looks like: read every collected frame, never
  trust a capture by its byte size or its status-bar clock, and treat one
  cold crash or one snapshot-observer failure as environment until CI says
  otherwise.
- A language choice recreates the Activity, so anything the reader was
  standing in that lives in plain `remember` goes with the old window: the
  whole Settings sheet closed and the reader landed back in the reading. The
  open page is `rememberSaveable` now. A modal sheet is its own window, and
  Material3 gives it a saveable id of its own (`Dialog:$dialogId`), so state
  inside a sheet survives the recreation too.
- A text-only action and a heading look the same on a page that draws no
  buttons. `ui-kit/TextButton` is the one shape a word that acts wears, and
  the note sheet's "Note" is named in `titleMedium` so the name and the
  actions are different kinds of thing. Apply it to every new action; a bare
  clickable `Text` is what made Save and Note the same word.
- The notes list reads newest first by the note's own moment, not the ayah's
  save moment, so `saved.db` has a `note_at` column and `DATABASE_VERSION` is
  2. A schema change means the migration and its test in the same session, the
  way the saved-ayah database has always worked.
- A quiet alpha is not a legibility strategy. `onBackground.copy(alpha = 0.45f)`
  measured 2.44:1 on the sepia ground and 2.82:1 on paper: the alpha looked
  muted on the screen the author was looking at and failed the design
  document's own 4.5:1 rule for muted text on two of the four themes. A tone
  with meaning takes the theme's own secondary token (`onSurfaceVariant`),
  which is defined per theme and measures 6.1:1 and up on all four grounds.
  D-084 moved the study reading's three quiet tones and left two named:
  the card's tafsir range (0.7 alpha, 3.3:1) and its note hint (0.5, 2.2:1).
  Measure before choosing a tone, and say "alpha" only when the alpha is
  against a ground that was actually checked.
- A hardware bitmap is a texture, and a texture has a size limit. The share
  card is read off a Compose graphics layer with `toImageBitmap`, which
  makes a **hardware** bitmap on API 28 and up; a tall card passes the
  driver's limit (4,096 px on much of the mid range hardware this app runs
  on) and loses its tail, and a software canvas refuses the bitmap outright
  (`Software rendering doesn't support hardware bitmaps`). The capture takes
  the card in 2,048 px windows and stitches software copies into one
  software bitmap, so the picture is the card's real size on every device.
  Reopen only with a measurement; the emulator here has a large limit and
  would not show the truncation (D-097).
- Testing the study reading by tapping the page's exact center is a trap: a
  translation footnote marker sits in the flow of the text, so at the center
  of Al-Fatihah 1:1 one of them is under the finger, and the tap opens the
  footnote sheet instead of the chrome. Every test that wants the chrome
  taps the page's own left margin through `tapThePaper()`, which is where a
  reader taps too. Eight tests failed together from this before it was
  found; the clean 2.1 tree shows the same flakiness, so it is a trap and
  not a regression (D-097).
- A reminder scheduled outside the app must not need a boot permission. The
  daily ayah arms through `AlarmManager` and an unexported receiver, and
  re-arms on the app's own launch: a reboot or a clock change costs at most
  one morning before the app is opened again, which is the price of not
  adding `RECEIVE_BOOT_COMPLETED` to a manifest whose permission set is
  audited (D-097).
- `getIntExtra(EXTRA, 0)` is not an absent extra. The daily reminder's first
  cut defaulted a missing ayah to 0, which coerced to ayah 1, so every plain
  launch jumped to the first ayah; the default is a sentinel that cannot be
  a place (D-097).
- The Bangla interface follows the QUL Bangla corpus for spelling: তাফসীর,
  not তাফসির. The Listening page reads অডিও ("audio"), the owner's own word
  after an earlier session read a garbled `শ্রবণ` as a spelling question.
  Sweep the whole Bangla surface whenever a word is settled, because the
  double spellings drift one string at a time (D-097).

### Housekeeping at the end of the session

At the end of the ninth session the hand-off bundle, every module build
directory, and the content working area were deleted, freeing about 3.3 GB;
the repository stands at 597 MB. What is kept is deliberate: `content/raw/`
(the owner's manual QUL and QuranEnc exports, the provenance of every pack),
`content/quran.db` and `content/packs/` (the shipped assets the catalog pins),
and `play-store/screenshots/` (the store set, captured by CI).

The eleventh session kept the content working area and the build directories,
because it ran the owner-machine gates and then built and verified the release
bundle from them. Deleting them before the next release would only mean
fetching them again; the space is worth less than the time.

| Removed | Bring it back with |
|---|---|
| `play-store/aab/*.aab` (the hand-off copy) | the `signed-bundle` artifact from the newest `build` run |
| every `build/` directory | any `./gradlew` build |
| `content/work/verify` (extracted sources, 288 MB) | `./gradlew :tools:run --args="verify"` |
| `content/work/fonts-v2`, `qpc-v2-font` (the font packs) | `./gradlew :tools:run --args="fetch"` |
| `content/work/db`, `json`, `qpc-v2.db` (built component files) | `./gradlew :tools:run --args="build"` |
| `content/work/audio-dev` (the development sample) | `./gradlew :tools:run --args="audio sample"` |

## Next session: the remaining queue, in order

0. **The instrumented suites, the store set, and the 2.5 release: done in
   the thirty-third session.** The data suite is 29/29, the app suite is
   45/45, all five owner gates are green, and 2.5 was submitted with its
   hand-off closed. The queue starts at item 1.

1. **The segmented controls' touch targets: done in the thirty-seventh
   session.** The text size steps and the playback pace drew 38 dp and 46 dp
   cells, under the design document's own 48 dp rule, and D-087 named the fix
   as structural: an outer 48 dp touch box with the mark inside it. Both rows
   are one `SegmentedRow` now, every step is 48 dp to touch, and the name of
   the thing sits above the steps because five 48 dp boxes are 250 dp wide
   (D-116). The Go to Ayah grid's cells were brought to 48 dp in the
   thirty-first session, so that class is now closed.
2. **The exact-alarm door: opened in the thirty-seventh session.** The owner
   took `SCHEDULE_EXACT_ALARM` (rule 2 above), so the reminder is exact where
   the reader has granted it and the platform's own batched alarm where they
   have not, with the Daily page naming what the phone is refusing. The one
   thing left on it is the owner's own hand at release: the Play Console
   declaration for the permission, with the reminder as the justification.
3. **Mushaf text size, if the owner wants it.** The owner declined zoom and
   asked what other apps do; the answer is in D-087. The honest larger-text
   path is a re-layout of the pre-justified page at a bigger em, built as a
   pure tested `PageLayout` in `core` first, and only then on the page. The
   flowing Unicode alternative is also described there. No work is queued
   unless the owner asks.
4. **Measure on real hardware.** The numbers in D-037 come from a software
   rendered emulator, the slowest Android this app will run on. A
   Macrobenchmark module for startup and page turns, run on a phone, is the
   only way to hold the budget the design document sets (under 300 ms to the
   first painted page on a warm start) and to prove the baseline profile is
   pulling its weight.
5. **The second language.** The strings are ready for one; the reader is not.
   A translation pass needs a translator for the interface, a `values-xx`
   folder, and the same care the Arabic content gets: a real review, not a
   machine.
6. **Content backlog.** More translations and tafsirs as packs (each one is a
   dataset entry in `PackSources`, a pack definition, and a Release), and a
   second mushaf script if a font and layout are chosen.
7. **A recording of the tour's study frames.** The screenshot set covers the
   surface; a short screen recording of a page turn, a mode switch, and the
   ayah card would catch the motion a still cannot, and the pipeline already
   has an emulator to do it on.
8. **A reading plan, if the owner wants one.** A khatm is a commitment, not a
   guess: an explicit plan (a daily portion, a finish date) is the only
   honest way to keep a linear reading place apart from casual lookups, and
   it builds on the portion the glossary already describes. The eighth
   session shipped no heuristic for it (D-045).
9. **Instant launch, second step.** A pre-rendered bitmap of the *next* page
   in the direction the reader was reading, so the first swipe after a launch
   is also a texture draw.
10. **Robustness, second step.** A pack that fails verification at download
    time should say which check failed (size, hash, or unpack) rather than one
    line for all three, and the content self check should offer the removal it
    recommends.
11. **Trust work, second step.** The owner removed export and import in the
    tenth session (D-051), so this item is gone unless they ask for a different
    way to carry saved work. If they do, the shape to build is a merge preview
    and a way to move one note rather than the whole document, never the same
    JSON door again.

## Traps worth remembering

* QUL downloads need an account, so their datasets are placed by hand into
  `content/raw/qul/` and pinned by `tools verify`.
* Pack downloads are content addressed: `pack-<id>-<hash8>`, and the content
  database lives at `content-db-<hash8>`. Never replace an existing tag; the
  catalog and old build reports depend on it.
* `content/packs/` and `content/quran.db` are generated, never committed.
* A debug build carries every pack in `assets/packs/` for offline work; a
  release build carries only the core pack, and that is enforced by variant,
  not by a condition.
* A still frame cannot show which way a page turned, and the order of the
  words on a line and the order of the pages are two different bugs. The
  lines were fixed in the fourteenth session while the pager still ran left
  to right on the screen; `MushafTurnTest` now pins the direction on every
  form factor.


## Where the project stands (end of the thirty-eighth session)

**3.0 (versionCode 31) is prepared for submission at the owner's word
(D-118, D-119, D-120, D-121).** The hand-over is the bundle and the store
set in one message, before the submission; the hand-off copy of the bundle is
deleted once the owner confirms Play has it.

**The tree answers the owner's three reports of this session, and 2.9
(versionCode 30) is still the release in Google Play.**

- **Repeat the surah, and one answer for the end of the audio.** The end of
  the recitation is one value, `core.EndOfAudio`, not three switches: repeat
  the ayah, repeat the surah, or carry on to the next surah. Turning one on
  turns the other two off, because a surah that repeats never ends and the
  continuation would then never come. The surah repeat is the app's own,
  because the playlist grows and `REPEAT_MODE_ALL` would loop everything
  loaded; it seeks to the surah's first ayah on the device from the two
  events a surah's end arrives through, and a deliberate Next or Previous is
  never intercepted. The store keeps its three keys and settles an older
  install's impossible pair by keeping the narrower promise.
- **The pill's words keep a measure, or they take a line of their own.** Four
  48 dp targets left about 91 dp of text on a 369 dp phone, which is fourteen
  characters: a surah name and its ayah always broke, and the pace and repeat
  were ellipsized. The pill is now two rows wherever the words cannot keep
  300 dp, and the one row it has always been on a tablet or a landscape
  phone. The phone pill grows from about 55 dp to about 98 dp, which is the
  price the owner chose over a one-line pill where the tap between the reciter
  and the place is a guess. `PlaybackUi.kt` (768 lines) is four files now.
- **A settings row's own mark is the last thing in it.** A row with a switch
  ends with the switch, a row without ends with its chevron on the same line,
  and a row with both is untouched. Six hub rows lost 64 dp of empty right
  margin and their values gained the room, which is what the wrapped "Arabic
  30, translation 17" needed. This reverses the other half of D-111.

**The suite.** `:core:test` (91, ten new), `:data:testDebugUnitTest` (33),
`:app:testDebugUnitTest`, `:app:lintDebug` and `:app:assembleDebug` are green
on this machine, and `:app:compileDebugAndroidTestKotlin` compiles. All five
owner gates ran green. There are no AVDs here, so `PlaybackPillTest`,
`ListeningSettingsTest` and `SettingsRowAlignmentTest` are unrun: none of the
three is among the six classes CI names, and the next session with an
emulator, or the next release's full app run, is where they answer.

**The 3.0 push.** Build run 36563923564 attempt 1 green: the five gates, the
data instrumented suite, and the signed bundle (`quran-3.0-vc31.aab`,
148,740,218 bytes, SHA-256 `d15ca873...d1bf6154`, `jar verified`, core pack
only). Capture run 36563923631 attempt 1 was red on the 10 inch leg alone
with the launcher ANR over `05-search` (class 4 of
`docs/screenshot-failures.md`, the guard refusing the frame); one rerun, and
attempt 2 is green on all three legs. The store set is refreshed: 24 frames
`cmp`'d against their artifacts, the four reading frames byte-identical to
2.9, the search, Browse and card frames the status bar clock alone, and the
settings frames carrying the new tail with Version 3.0 in the foot.

## Where the project stands (end of the thirty-seventh session)

**The tree answers three of the owner's reports, and 2.9 (versionCode 30) is
submitted to Google Play for review (D-114, D-115, D-116, D-117).** The hand-
off is closed: the bundle went with the screenshots and the notes in one
message, and the hand-over copy was deleted the same session, so the artifact
stays in build run 36542803366 and in Play and `play-store/aab/` keeps only
its own note.

- **The daily ayah keeps the minute it was given (D-114).** The alarm is
  exact where the reader has granted the app the phone's own exact time
  (`SCHEDULE_EXACT_ALARM`, the one new permission, the owner's word in rule
  2), and the platform's own batched alarm where they have not, with the
  Daily page naming what the phone is refusing and one button for it. A
  windowed alarm was tried first and measured out: the platform's floor for
  a window is ten minutes on Android 15, so the app's one minute ask comes
  back as ten. Two ways the reminder could vanish entirely are closed: the
  next alarm is armed before the ayah is read, and a launch no longer
  replaces an alarm that is due inside its window. The channel-level block is
  read and named, and the way out is that channel's own page.
- **The app follows the phone's own day and night (D-115).** The activity no
  longer claims `uiMode` in `configChanges`, which was a promise to the
  platform the app could not keep, and the platform now rebuilds the window
  on a theme change the way it does for every other app.
- **The settings sheet reads as a list (D-116).** The name of a row holds
  its room whatever the value says, a row says a state rather than an
  explanation, the hub is in three quiet groups, the value column wears the
  same type as a switch row's status line, and every step of a text size or a
  pace is a full 48 dp target (D-087's named item, now done).

**The suite.** The JVM suite (core 81, data 33), `:app:lintDebug`,
`:app:assembleDebug` and `:app:compileDebugAndroidTestKotlin` are green on
this machine. The instrumented work was done on the phone emulator:
`SystemThemeTest` 2/2, red on the old manifest and green on the new one
(both halves: the installed activity claimed `CONFIG_UI_MODE`, and with the
phone's night mode on the app reported light and drew a light page);
`DailyReminderTest` 2/2 in both permission states, with the platform's own
alarm record read back (`window=0` for the exact alarm); `SettingsRowAlignmentTest`
3/3; `ListeningSettingsTest` and `SettingsVisibilityTest` green. The emulator
then vanished three times mid-run, the documented class on this machine, so
`SettingsMergeTest`, `DailyAyahToggleTest` and the screenshot tour are CI's
to answer on the push.

**Two tests were red for nobody to see, and are fixed.** The switch column
test compared the switch's left edge in dp against the chevron's right edge in
pixels, and `ListeningSettingsTest` looked for a switch in the merged tree
where a row that opens a page reads as one control. Both are named in
`AGENTS.md` now: CI runs six named app classes, so a session that touches
the settings runs the whole app suite on the emulator.

**The 2.9 push.** Build run 36542803366 green: the five content gates, the
data instrumented suite, and the signed bundle. The capture run 36542803357
came back with all three legs' eight frames, the phone and the 10 inch green
and the 7 inch red on a Compose runtime crash in a test class that ran after
the tour had written its frames. Twelve frames are byte-identical to the 2.8
set; the twelve that differ are the search, settings, Browse and ayah card on
each form, and only the settings frames carry content: the other three differ
in the status bar clock alone, with the 10 inch search frame's keyboard
chrome as well. The bundle is `quran-2.9-vc30.aab`, 148,734,182 bytes, SHA-256
`6bc3b5a7...ae14756`, `jar verified`, signed with the shared upload key.

**A mistake this session made, kept in the record.** A five-chip Browse frame
was "found" in a green artifact and a workflow change was pushed on it. The
python on this machine is a Windows build, so a `/tmp` path in a script is
the C: drive's temp while the same path at the bash prompt is the MSYS temp,
and every measurement and crop had been made against a folder an earlier
session left behind. The real artifacts match the code. The workflow change
was undone in the same session, the hardening that is worth keeping (an
uninstall before the tour) stayed, and the lesson is in
`docs/screenshot-failures.md`: copy a frame into the repository before
anything looks at it, and when a number and a picture disagree, take a third
measurement.

## Where the project stands (end of the thirty-sixth session)

**The tree carries the owner's two reports: the settings tail puts every
switch last, and the icon and the feature graphic are redrawn as the center
of a mushaf cover (D-111, D-112). 2.8 (versionCode 29) is submitted to Google Play
for review (D-113), its hand-off closed.**

- **The settings tail is two columns, switch last.** On a row that carries
both a switch and a chevron, the chevron stands one column before the
switch, and every row keeps both columns, so all switches end at the
sheet's edge and all chevrons at one place before them.
`SettingsRowAlignmentTest` pins the columns, the order, and the page row's
compact height.
- **The icon and the feature graphic are the center of a mushaf cover.** The mark is a twelve petal shamsa in flat champagne gold on an ink navy field, a finial in every notch, a ray and bead ring, and the word القرآن in the app's Amiri at the center, with two pendants on the vertical axis (D-112). The store icon wears the pendants; the adaptive foreground is the medallion alone inside the safe zone; the shared card carries a miniature of the same medallion. The feature graphic is the medallion centered inside two thin rules at 1024x500, 24-bit with no alpha. The palette is the owner's correction: ink navy and champagne gold, flat, no metallic ramps.

**The suite and the gates.** The JVM suite, `:app:lintDebug`,
`:app:assembleDebug`, and `:app:compileDebugAndroidTestKotlin` are green on
the zn machine. All five owner gates ran green: `verify` 29 datasets,
`audit` 0 unexplained differences, `fonts` all coverage checks, `search`,
and `checkdb`. There are no AVDs on this machine, so the instrumented
suites are CI's: the build run's data leg and the capture run's three legs
passed.

**The 2.8 push.** Build run 36448152402 attempt 1 green: the gates, the data
instrumented suite, and the signed bundle (`quran-2.8-vc29.aab`,
148,728,995 bytes, SHA-256 `e13f1eca...b03d2f`, `jar verified`). Capture run
36448152056 attempt 1 went red on the tablet10 leg alone (the launcher ANR
over the keyboard-heavy search frame, class 4 of
`docs/screenshot-failures.md`, and the guard refused the frame); one rerun,
attempt 2 green on all three legs. The store set is refreshed: 24 frames
`cmp`'d against their artifacts, the settings frames carry the new tail and
Version 2.8, and the rest is the status bar clock, the tablet10 keyboard
chrome, and subpixel antialiasing.

## Where the project stands (end of the thirty-fifth session)

**The tree answers the owner's work on 2.6, and 2.7 (versionCode 28) is
submitted to Google Play for review (D-108, D-109, D-110).**

- **Search leads with a surah name, then a reference.** The only query that
yields both is a bare number, and the surah card is the better landing there
than 2:1; a full reference still has no surah hit to displace it.
- **Bangla wording pass.** `settings_keep_awake_subtitle`, the fuller
  `settings_daily_note`, `settings_daily_blocked`, `settings_translations_note`,
  `settings_speed_note`, `last_read_empty_title`, and `share_unavailable`
  were corrected; the owner kept `action_clear`, `action_play`,
  `playback_reciter_ready`, and `action_browse`.
- **The daily reminder's text is centered.** The collapsed and big texts
  carry an `AlignmentSpan`; if the system ignores paragraph spans the
  reminder is exactly what it was.
- **Content stays current quietly.** `PackUpdater` replaces a pack the
  reader already has when the app carries a newer version, only on an
  unmetered connection, only after the first page is readable. The manual
  Check installed content shows Updating and repairs what it finds, ending
  in "Everything installed is intact." or a positive "Update available".
  D-109 amends the offline rule; D-023 is amended for the second time.

**The suite and the gates.** `:core:test`, `:data:testDebugUnitTest`,
`:app:lintDebug`, and `:app:assembleDebug` are green. The data instrumented
suite is 33/33 (29 plus the new `PackVerifierTest`, whose stale/current/
absent/shipped detection is pinned). The app instrumented suite was attempted
twice on the phone emulator and the device vanished mid-run both times
(23 of 48, then 38 of 48, with `ComposeTimeoutException`, a missing node as a
sheet never drew, and an empty failure body), the documented loaded-emulator
class on this machine; the capture workflow ran its six named classes green
on all three legs, and CI is the instrumented authority. All five owner gates
ran green on this machine: `verify` 29 datasets, `audit` 0 unexplained
differences, `fonts` all coverage checks, `search`, and `checkdb`.

**The 2.7 push.** Build run 36395366975 attempt 1 green: the gates, the data
instrumented suite, and the signed bundle. Capture run 36395366904 attempt 1,
all three legs green first try. The store set was refreshed, 24 frames
compared with their artifacts by `cmp` and every changed frame read; the
settings frames carry Version 2.7, the tablet10 search difference is the
keyboard's chrome alone, and the rest is the status bar clock and subpixel
antialiasing.

**The bundle.** `quran-2.7-vc28.aab`, 148,102,394 bytes, SHA-256
`bc3e382ce703fa166183e4033da3c52a71cbf2f853f3b58a55f12c19d3c51381`,
`jar verified`, signed with the shared upload key
(`53:7D:09:D2:...:0D:9D:E5:21`). The owner submitted 2.7 to Google Play for
review and confirmed it, so the hand-off copy was deleted the same session:
the artifact stays in build run 36395366975 and in Play, and `play-store/aab/`
keeps only its own note. The tree is clean.

## Where the project stands (end of the thirty-fourth session)

**The tree answers the owner's report on 2.5 (D-105), and 2.6
(versionCode 27) is prepared for submission at the owner's word.**

- **The settings rows are compact again.** D-103's 48 dp `ChevronSlot`
  square was taller than the line beside it, so the plain hub rows grew
  from 51 dp to 76 dp in 2.5 and the hub read as one uniform ladder. The
  slot is a width now wherever the chevron is not a control of its own;
  the horizontal column of switches and chevrons is untouched, and
  `SettingsRowAlignmentTest` pins the compact page row.
- **The switch lives in one place.** The hub row owns it; the Translations,
  Tafsirs, and Daily pages drop the second copy, with a quiet line when the
  reading is hiding what the page lists.
- **Continue to the next surah.** Off by default, under Repeat the ayah on
  the Listening page and in the pill's listening menu. On, the end of a
  surah fetches the next package with the reciter being heard and plays on,
  with the size, the progress, and a cancel on the pill; off, the Continue
  offer stands as before, where it is the only in-player way to the next
  surah and the only size disclosure. D-023 is amended and
  `docs/privacy.md` says so.

**The suite and the gates.** The JVM suite, `:app:lintDebug`,
`:app:assembleDebug`, and `:app:compileDebugAndroidTestKotlin` are green on
the zn machine, which now carries a real JDK 17 (`C:\Users\zn\jdks\jdk-17`)
and a `local.properties` with its SDK path; both facts are recorded under
"Session setup". All five owner gates ran green: `verify` 29 datasets,
`audit` 0 unexplained differences, `fonts` coverage passed, `search`, and
`checkdb` on the new database (`380e0442...`). The content took QUL's
2:238 typo fix for the English Ibn Kathir tafsir (D-106).

**The 2.6 push.** Build run 36341047053 attempt 1 green: the gates, the
data instrumented suite, and the signed bundle (`quran-2.6-vc27.aab`,
148,097,166 bytes, SHA-256 `316557d1...98ef`, `jar verified`). Capture run
36341047052 attempt 1, all three legs green first try: 24 frames compared
with their artifacts by `cmp` and every changed frame read. The settings
frames are the release's: compact rows, one switch and chevron column, and
the About row reads Version 2.6. The owner submitted 2.6 to Google Play for
review and confirmed it, so the hand-off copy was deleted the same session;
the artifact stays in build run 36341047053.

## Where the project stands (end of the thirty-third session)

**The tree answers the reader's report on 2.4 (D-103), and 2.5
(versionCode 26) is submitted to Google Play for review (D-104).**

- **The shared text of an ayah reads left to right.** `core/ShareText` is
the one builder of the words, and its first character is U+200E, so a
message that opens with Arabic no longer turns the translation and its
closing period around; the image card is untouched, and
`core/ShareTextTest` pins the shape and the `java.text.Bidi` flip.
- **The ayah grid's header draws the surah in the Browse row's own type.**
The name, the place, and the Arabic name are one pair of composables in
`feature-browse/NumberedRows`, used by both surfaces.
- **The settings tail is one 48 dp `ChevronSlot` on every row.** The
chevron aligns across rows, and a row that carries only a switch keeps the
chevron's room empty, which lines every switch up.

**The suite and the gates.** The JVM suite, lint, and `assembleDebug` are
green; the data instrumented suite is 29/29 and the app instrumented suite
is 45/45 on the phone emulator. All five owner gates ran green: `verify`
29 datasets, `audit` 0 unexplained differences, `fonts` coverage passed on
22,985,677 reading codepoints, `checkdb` (128,966,656 bytes, SHA-256
`5c5988fa...`), and `search` 465 readable excerpts. CI: build run
36306853778 green with the signed bundle; capture run 36306854024 attempt
2, after one rerun of two environment-class legs (tablet10's launcher ANR
over 05-search, the phone's loaded-emulator timeout in
`SettingsVisibilityTest`), all three green. The store set is refreshed
from attempt 2, 24 frames `cmp`'d, and the settings frames read. The
bundle is `quran-2.5-vc26.aab`, 148,091,370 bytes, SHA-256
`140423ca...c9b5`, `jar verified` with the shared upload key; the owner
confirmed the submission, the hand-off copy is deleted, and the artifact
stays in build run 36306853778.

## Where the project stands (end of the thirty-second session)

**The tree answers the owner's simplification report (D-101), and 2.4
(versionCode 25) is submitted to Google Play for review.** The
session folded Go to Ayah into the surah list, folded Notes into Saved,
kept the search word meanings (with D-099's numbers restated), and moved
the settings chevron after the switch.

- **A surah row is the door to its own ayahs.** Browse has four tabs now
  (Surahs, Juz, Last Read, Saved). Tapping a surah opens its 48 dp number
  grid as a page inside the sheet, with a back header carrying the surah's
  name, place, length, and Arabic name. The grid marks the current ayah when
  that surah is the one the reader is in, and otherwise the newest history
  place in that surah; the first ayah of an unread surah still opens the
  surah's opening in study mode.
- **A note is written on a kept ayah.** Saved is the one list: every kept
  ayah, note previews under the places that have them, ordered by the most
  recent of the two moments. The pill keeps its Note action; writing a note
  saves the ayah with it. Removing a save that carries a note asks first in
  a small sheet that shows the note. `saved.db` is version 5, and the
  migration brings note-only rows into Saved so nothing is lost.
- **The word meanings stay in search.** The "Words" chip already lets a
  reader turn the source off; the matched-word refinement from 2.3 is
  unchanged.
- **The chevron is the row's last mark** on the three combined rows (Show
  translation, Show tafsir, the daily reminder), after the switch.

**The suite.** `:core:test`, `:data:testDebugUnitTest`, `:app:testDebugUnitTest`,
`:app:lintDebug`, and `:app:assembleDebug` are green. The data instrumented
suite is 29/29, run twice on the phone emulator. The app suite ran 39 tests
with 37 passing; the two failures are the documented emulator classes
(`searchIsFastWhenWarm` on the loaded emulator, and an empty failure body as
the device vanished during `SurahAyahsScrollTest`), and the five tests after
that one never ran. The tour and `SavedNotesTest` passed. One reboot was
spent and the device died again, so local connected runs stopped. CI is the
instrumented authority, and it ran green for the release: build 36299617649,
and the capture on run 36299617648 attempt 2 after one rerun of the phone
leg, whose first attempt met the loaded-emulator class in `SavedNotesTest`.
The store set is refreshed for 2.4, with the Browse and settings changes
read.

## The 2.4 release

**Version:** 2.4, versionCode 25, with the version line in
`play-store/listing.md` and the notes beside it (398 characters, counted).

**The bundle:** `quran-2.4-vc25.aab` from the green build run 36299617649:
148,092,639 bytes, SHA-256
`7bc5da4ba7f69cc94ad0626e4f5e937b9fd82aae12c64bde8bf824065e539970`,
`jar verified`, signed with the shared upload key. The submission is
confirmed and the hand-off copy was deleted; the artifact stays in build run
36299617649 and in Play, and `play-store/aab/` keeps only its own note.

**The store set:** run 36299617648 attempt 2 (the phone leg's first attempt
met the loaded-emulator class in `SavedNotesTest` and the rerun passed;
tablet7 and tablet10 were green first try), 24 frames compared with their
artifacts by `cmp` and every changed frame read. Browse shows four chips,
and settings shows the chevron after the switch with Version 2.4 in the
foot.

**The five owner gates ran green** before the release (D-102).

## Where the project stands (end of the thirty-first session)

**The tree answers the reader's tenth report, and 2.3 (versionCode 24) is
prepared for submission at the owner's word** (D-099). No `versionCode` has
moved in this list beyond the release commit below; the release itself is
recorded under "The 2.3 release".

- **A switch is a switch and the row is a door.** `ToggleRow` splits them:
only the switch toggles, the rest of the row opens its page, and the two list
pages carry their own switch at the head so a page behind a switch is not a
dead end.
- **The reading builds from the Arabic outward:** word meanings above Show
translation, translation above Show tafsir.
- **The daily reminder is on with the app** and its moment lives on a Daily
page behind its hub row, chosen on the platform's clock with typed input
beside it, so every minute of the day is reachable. The stored value is a
minute of the day (`dailyAyahMinute`), the old hour key is still read, the
phone's permission is asked at the two acts of setting and nowhere else, and
the page names the phone's own notification settings when the phone has
turned the reminder off. The channel's words are refreshed on installs that
already have it.
- **Go to Ayah says what it is standing for:** the card carries the surah's
Arabic name and its length, the ayah cells are 48 dp, the surah step opens on
the reader's own surah, and both steps wear one filled mark for "the place
you are standing". The picker has its own file, and `BrowseSheet.kt` is back
under 400 lines.
- **Search keeps its word meanings and stops printing Arabic that matched
nothing:** the Arabic is drawn when it is the match, when the row has no
other evidence, and otherwise the row points at the matched word itself. The
rule is `arabicLineFor` in `data`, pure, tested in the JVM suite and against
the shipped database, with its measured reasoning in the KDoc.
- **Bangla:** প ্ৰত িদ িন ের আয ় everywhere, channel included, and every new
line was composed from words the app already ships rather than typed fresh.

**The suite.** `:core:test`, `:data:testDebugUnitTest`, `:app:testDebugUnitTest`,
`:app:lintDebug` (no errors, two informational KTX suggestions) and
`:app:assembleDebug` are green, and `:app:compileDebugAndroidTestKotlin`
compiles. The instrumented suites carry the new shapes: `DailyAyahToggleTest`
pins the switch-versus-door split and the picker, `DailyAyahTest` pins minutes
and both ends of the day, `SettingsVisibilityTest` taps the switch tag, and
the search instrumented test proves a meaning row names the word that carried
it from the ayah's own text.

**Owed:** the hand-over to Play, and the instrumented suites are CI's
authority this release: two local emulator runs met the documented
environment classes (a cold first test timing out before the study reading
drew on the first run after install, and the study list's prefetch scheduler
on a loaded emulator, `The current thread must have a looper`), and both
classes pass in isolation on the same build. CI ran the app suite on all
three capture legs and the data suite on `build`, all green (runs 36263904415
and 36263904407).

## The 2.3 release

**Version:** 2.3, versionCode 24, bumped from 2.2 in the release commit, with
the version line in `play-store/listing.md` and the notes beside it (444
characters, counted).

**The bundle:** from the newest green build on `main`, run 36263904415:
`quran-2.3-vc24.aab`, 148,097,627 bytes, SHA-256
`59d82244bf3858b0668cb643e56afee59cf34d4ddca6dfed73cbc2fc768af026` (matching
the artifact's own `_temp` checksum), `jar verified`, signed with the shared
upload key.

**The store set:** from run 36263904407, all three legs green first try, 24
frames compared with their artifacts by `cmp` and the changed frames read.
The changed frames are the same four on every form factor, all subpixel: a
tablet band comparison flagged them and the pixel measurement classed them
(worst average delta 0.04 of 255, worst single pixel 139), which is the
antialiasing class the runbook names. The settings frame's foot reads
`Version 2.3`, read back from the installed frame itself.

**The five owner gates ran green on this machine** before the release:
`verify` 29 datasets, `audit` 0 unexplained differences, `fonts` coverage
passed, `checkdb` (the committed database and 10 pack files verified), and
`search` (63 Arabic round trips, 82 non-ASCII codepoints, 465 excerpts).

## Where the project stands (end of the thirtieth session)

**2.2 (versionCode 23) is submitted to Google Play for review** (D-097,
D-098). The owner asked for eight things against 2.1 and then said "go for
play release"; the listening page reads অডিও, the owner's own word, after
the first read of a garbled request. The hand-off is closed: the bundle was
deleted once the submission was confirmed, and `play-store/aab/` keeps only
its own note.

What the release carries:

- **Go to Ayah lands the reader's ayah mid-grid.** The picker scrolled the
  place to the viewport's top edge, where it read as any other row; it now
  centers it, and the scroll is keyed on the place as well as the surah.
  `GoToAyahScrollTest` fails on the old scroll with the measured numbers.
- **Search never says the same match twice.** The word meaning block is
  drawn only when it is the row's only evidence of the match; a translation
  highlight is never repeated under it. The rule is `shouldShowWordMeaning`
  in `data`, pure and tested in the JVM suite and against the shipped
  database.
- **The reading switches and the packs are one row each.** Show translation
  and Show tafsir carry the chevron that opens their own list, with the
  chosen pack's name under them and their own spoken names; the separate hub
  rows are gone. Word meanings is untouched.
- **The share card is captured whole on every device.** `toImageBitmap`
  makes a hardware bitmap, capped by the GPU's texture size and refused by a
  software canvas; the capture now stitches 2,048 px software windows, and
  the sheet's preview has its height cap on the viewport rather than on the
  card. Two tests pin both halves.
- **The theme swatch says what is drawing.** With auto-night on and the
  phone dark, Night is filled and the day page is named beside it.
- **One Bangla spelling, and the listening word.** তাফসীর everywhere (the
  QUL corpus's own), and the Listening page reads অডিও.
- **The daily reminder.** One ayah a day at the reader's own hour, the
  translation when they read with one, and a tap that opens the ayah in the
  study reading. Offline, no new permission, no new dependency, off until
  asked for. `core/DailyAyah` is the pure rotation and `DailyAyahTest`
  pins it.

**The suite.** `core:test`, `data:testDebugUnitTest`,
`app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug` are
green; the data instrumented suite is 31/31 and the app instrumented suite
is 39/39 on the phone emulator. All five owner gates ran green (D-098). CI:
build 36164556479 green with the signed bundle, screenshot run 36164556475
all three legs green on attempt 2 (the phone leg's first attempt met a
launcher ANR over 05-search; the guard refused the frame, which is the
environment class D-078 names).

**The store set is current** (run 36164556475 attempt 2, 24 frames cmp'd,
all changed frames read). **The bundle was handed over and is in Play**:
`quran-2.2-vc23.aab`, 147,867,353 bytes, SHA-256
`ba4b9c1e4060a8fa16ef5424319ea2f8b17c9f7121fb83aff3961b4b5ec67c7d`, `jar
verified`, signed with the shared upload key, core pack only. The hand-off
copy is deleted; the artifact stays in build run 36164556479.

## Where the project stands (end of the twenty-ninth session)

**2.1 (versionCode 22) is submitted to Google Play for review**, and the
hand-off is closed: the bundle was deleted once the submission was confirmed,
and `play-store/aab/` keeps only its own note. It carries the reader's
eighth report (D-094, D-095, D-096):

- **Tafsir Arabic wraps from the right.** The cause was Compose's own
default: `TextDirection.Unspecified` resolves against the interface's layout
direction, not the text's first strong character. `RichBlocks` states
`TextDirection.Content` and `ArabicBody` states `Rtl`; `TafsirDirectionTest`
draws the real block and reads the last line's pixels.
- **Notes preview what you wrote.** Two lines at most, in the reading face,
clamped on every row; Saved is untouched, so the two lists differ at a
glance. `NotesPreviewTest` covers it.
- **Settings draws what the reading shows.** Show translation, Show tafsir,
and Show word meanings sit in the hub under Font size, translation and tafsir
on by default; the word by word switch moved there from the Translations
page. Word by word stays off by default because turning it on is the door
that fetches the list. `SettingsVisibilityTest` covers the reading response.
- **Go to Ayah is the sixth Browse tab,** with the chips always visible and
the surah choice as the one step with a head and a back.
- **Search names its word meanings,** and the shared ayah card no longer
prints the translation's name.

**The capture's window guard was widened.** The first 2.1 capture was green
with a focus-less "Pixel Launcher isn't responding" dialog over two phone
frames; the guard now reads visible error dialogs out of their own window
blocks (only `mHasSurface=true` counts) and the tour waits for Browse to
close after the back key. `docs/screenshot-failures.md` catalogues the mode.
The store set is from run 36125481691 (24 of 24 `cmp`'d), and the bundle
`quran-2.1-vc22.aab` came from the newest green build, run 36125481634:
147,841,916 bytes, SHA-256
`a81cad7ef254409cdd24c5681f3541d9b2453709b88a17453f1bef8a51a1d6dd`, `jar
verified`, core pack only.

**The suite.** All five owner gates ran green on this machine (`verify` 29
datasets, `audit` 0 unexplained differences, `fonts` coverage passed,
`checkdb`, `search`), plus the JVM suite, lint, `assembleDebug`, the data
instrumented suite 31/31, and the app instrumented suite 23/23 on the phone
emulator. CI: build 36125481634 and screenshot run 36125481691, all green.

**The fast path.** The release runbook now records the measured budget
(local suite about six minutes, build and capture in parallel, numeric frame
verification) and names the next speedup: a booted emulator snapshot in the
per-form-factor AVD cache, with the target of a capture leg under three
minutes.

## Where the project stands (end of the twenty-eighth session)

**2.0 (versionCode 21) is submitted to Google Play for review**, and the
hand-off is closed: the bundle was deleted once the submission was confirmed,
and `play-store/aab/` keeps only its own note. It carries the reader's
seventh report (D-090, D-092, D-093):

- **The playback pill never touches the glass.** Both states keep a 16 dp
gutter outside the shadow, and the label gives before the size or the doors
do. `PlaybackPillTest` measures the real bounds.
- **Share shows the card before you send it,** then sends the picture alone
(the default) or the words alone. The caption is gone, which had made every
receiver post the ayah twice, and the card is one centered column ending in
**Quran: The Noble Book**.
- **The tafsir's mixed paragraphs no longer breathe as Arabic paragraphs:**
1.9 down to 1.35, past the 1.32 em the corpus reaches at its worst, with the
line room measured out of the sources' own glyph ink and the three cases
classed by a pure `core` function.
- **Search results run from the verse outward,** each kind in Mushaf order.
The order is a pure function with its own JVM tests.
- **Go to Ayah** is capitalized and its picker is a designed page.
- **The pill carries the pace and repeat,** the same two values the Listening
page owns: one value, two doors.
- **Arabic as a third interface language is deferred** on the owner's word,
with its design recorded in D-090.

**The suite.** All five owner gates ran green on this machine (`verify` 29
datasets, `audit` 0 unexplained differences, `fonts` coverage passed,
`checkdb`, `search`), plus the JVM suite, lint, both assembles, the data
instrumented suite 31/31, and the app instrumented suite 19/19 on the phone
emulator. CI: build 36008294664 (gates, data tests, signed bundle) and
screenshot run 36008295034, all three legs green.

**A race the capture found, and the rule it wrote into the runbook.** The
phone's ayah-card frame came back missing its Translation block: the card's
window settles before its translations are read off the database. The card
now says when it is whole (`ayah-card` after `ayah-card-loading`) and the
tour waits on that. The second finding is the bundle's own: the hand-off must
come from the **newest** green build, because the card fix landed after the
version bump and the first download predated it (D-093, and the runbook step
now says so).

**The store set is current.** Run 36008295034, all three form factors, every
frame `cmp`'d (24 of 24) and every changed frame read. The changes are the
release's own: search leads with a translation match instead of a tafsir row,
settings reads Version 2.0, and Browse carries the capitalized Go to Ayah
chip.

**The bundle.** `quran-2.0-vc21.aab`, 147837180 bytes, SHA-256
`523d22e233a5db9c7d40a74ce5f3804968712b9e665b9f17f78218e6baa9a66d`,
`jar verified`, signed with the shared upload key
(`53:7D:09:D2:...:0D:9D:E5:21`), carrying only the core pack. Handed over
with the screenshots in one message, before the submission.

## Where the project stands (end of the twenty-seventh session)

**1.9 (versionCode 20) is submitted to Google Play for review.** It
carries the craftsmanship pass. Every quiet text tone was measured against
the design document's own 4.5:1 rule and raised, including the five the log
had never named (Browse numbers, the search hint and source, the footnotes,
the shared card's reference), and the ornament gold, which draws the page
number and the surah names, went from 3.2:1 to 4.6:1 on paper. The color
scheme now names every role Material draws from, so no sheet picks up a
default tone or a blue tint. The study reading and the settings sheet hold a
readable measure on a wide screen; on the ten inch tablet a translation had
been running the full 2560 px, over two hundred characters a line. A control
that floats over the reading wears a floating tone and a soft lift. The
study surah opening is built to the printed page's model, and the study
block answers the same TalkBack action the Mushaf does. Settings gained a
**Listening** page: the recitation pace (0.5x to 1.5x, pitch kept) and an
ayah repeat, both remembered and both said on the playback pill when they
are not the ordinary ones. The app now honors the system's reduce-motion
setting.

**The screenshot workflow was repaired, and its failures catalogued.** The
emulator runner executes each `script:` line in its own shell, so the old
`set +e` / `gradle_status=$?` shape never shared a shell: the runner stopped
the script at the failed Gradle line and **no red leg ever kept its frames**,
which is why `store-screenshots-tablet10` was missing from the failed 1.9
capture. The exit status is now saved on the Gradle line itself, every
collection line is guarded, and the window guard reads the focused window's
owner instead of searching the dump for a phrase a stale record can carry.
`docs/screenshot-failures.md` catalogues all five failure classes with their
evidence and fixes; a red leg is now a lookup, and all three legs went green
first try on the fixed workflow (run 35980906679).

**The suite.** All five owner gates ran green on the machine this session:
`verify` 29 datasets, `audit` 0 unexplained differences, `fonts` coverage
passed (628169 study codepoints, 604 pages, 22985677 reading codepoints),
`checkdb` (the committed database unchanged, 128966656 bytes), and `search`.
The JVM suite, lint, and `assembleDebug` are green. CI is green on the push:
build 35980906492 with the data instrumented suite and the `signed-bundle`
job, and the screenshot run 35980906679 on all three legs.

**The store set is current.** Run 35980906679, all three form factors, every
frame compared with its artifact by `cmp` (24 matches) and every changed
frame read before it shipped. The changed frames are the settings frames,
which now read Version 1.9 and carry the Listening row, plus the status bar
clock and the cursor's blink; nothing else moved, because the pass was
verified in the previous capture.

## Where the project stands (end of the twenty-sixth session)

**1.8 (versionCode 19) is submitted to Google Play for review.** The owner
answered five UI
questions about the 1.7 surfaces, and the answers are in the tree (D-084).
Go to ayah moved from a row over
the surah list into a chip in Browse's tab row, one tap from any tab, a
door and not a state: the picker swaps in and the reader's own tab waits
when it closes. The word by word aid stays under the ayah as its gloss (a
verse is read and recited as one line, and the playing word's wash needs
the line's layout), with its Arabic stepped one tone down, its meanings on
the theme's secondary tone, and `Space.Line` above it instead of
`Space.Block`. The ayah reference stays at the foot of its block, where it
closes what the reader just read, and it now takes `onSurfaceVariant`
(instead of 0.45 alpha, which measured 2.44:1 on sepia). The study pill's
deep door reads **Tafsir** with a new scroll glyph, because in the study
reading the card holds only tafsirs; from the Mushaf it stays **More**,
where the card holds the meanings, the translation, and the tafsirs. The
long-press pill itself stays, with its reasoning written in D-084. A new
`AyahActionsTest` pins the two names.

**The suite.** JVM, lint, and `assembleDebug` green. All five owner gates
ran green on the restored sources this time: `verify` 29 datasets, `audit`
0 unexplained differences, `fonts` coverage passed, `checkdb` and `search`.
The phone emulator ran the full app instrumented suite 16/16 (the new
`AyahActionsTest` and the screenshot tour among them) and the data suite
31/31. The store set is from run 35883297322 attempt 2 (the tablet7 leg on
its second attempt after a truncated system-image download on the runner,
the class now written into "If a leg fails"): all three form factors,
every frame `cmp`'d (24 matches, 0 mismatches), every changed frame read.
The settings frame reads Version 1.8, Browse shows the Go to ayah chip in
its tab row, and the study frames show the aid as a gloss with a legible
reference.

**The hand-off.** The bundle is `quran-1.8-vc19.aab`, 147803132 bytes,
SHA-256 `7a2fe75a33790fe17b90d90767908940c5e59af769b6cf3f19deae91d5f29205`,
`jar verified`, signed with the shared upload key
(`53:7D:09:D2:...:0D:9D:E5:21`), carrying only the core pack. The bundle
and the screenshots were handed over together, before the submission, and
the hand-off copy was deleted once the submission was confirmed.

## Where the project stands (end of the twenty-fifth session)

**1.7 (versionCode 18) is submitted to Google Play for review.** It
carries four changes from the owner's session: Browse
gained a Go to ayah picker over the surah list, with the reader's own surah
already chosen and their ayah marked; Search reads references by surah name
("baqara 255") beside "2:255", and the empty-search prompt names both;
Saved rows say when they were saved, from a `saved_at` column in `saved.db`
version 4; and the word-meaning aid is "Word meanings" everywhere, with
the ayah card's door naming only the list's language.

**The owner gates all ran green this time.** The raw QUL and QuranEnc
sources were restored to `content/raw` (24 QUL downloads, two QuranEnc
files, and the Tanzil XML with `marks`, `sajdah`, `alef`, and `tatweel`
set), closing the three gates 1.6 left owed: `verify` 29 datasets,
`audit` 0 unexplained differences, `fonts` coverage passed, plus `checkdb`
and `search`.

**The suite.** JVM, lint, and `assembleDebug` green. Data instrumented
31/31. The four affected app classes pass on the phone emulator; the full
local app run died twice with the emulator (`device 'emulator-5554' not
found`, empty failure bodies), so the tour's authority is the CI capture,
which is green on all three legs after one rerun of the 10 inch leg (the
boot-time dialog over 05-search, D-078's known case).

**The hand-off.** The bundle is `quran-1.7-vc18.aab`, 147,801,933 bytes,
SHA-256 `be0174e036add42fbf84c62f1914da228a600da801ea18287e2092a7458cd195`,
`jar verified`, signed with the shared upload key
(`53:7D:09:D2:...:0D:9D:E5:21`). The screenshots come from run 35841954697,
all three form factors, every frame compared with its artifact by `cmp` and
every changed frame read before it shipped (the Go to ayah row, the Word
meanings door, Version 1.7; the Mushaf and search differences are the
subpixel antialiasing and the cursor's blink). The bundle and the
screenshots were handed over together, before the submission. The hand-off
copy of the bundle was deleted once the submission was confirmed.

## Where the project stands (end of the twenty-fourth session)

**1.6 (versionCode 17) is submitted to Google Play for review.** It answers
the reader's twelfth report (D-080), eight things from the owner: About this surah wears the same lapis wash a chosen ayah wears;
QUL was checked and carries no Bangla surah
introduction, so that item stays content backlog; Browse's surah numbers
keep their last digit and follow the interface's digits; Save and Note are
separate marks in `saved.db` version 3; the Saved, Notes, and Last Read
lists are place lists, with Remove on the first two and Forget in Last Read
speaking মুছুন like every other removal; a note opened from Browse washes
its ayah. The JVM suite, lint, and `assembleDebug` are green; the
data instrumented tests pass (28) and the app instrumented tests pass (13)
on the phone emulator, the new `BrowseNumbersTest` among them. What was
handed over: 147,736,755 bytes, SHA-256
`5de7f297eca1cc2f7ef3598137b88e0c25fb7debb79e47a19858f280c8c32ced`, signed
with the shared upload certificate, and the screenshots from run 35823592289
with every frame compared by `cmp`. The three owner gates (`verify`,
`audit`, `fonts`) are owed this release: the manual exports and the verify
extractions are gone from the machine, and the owner chose to ship with them
named, the way 1.4 did (D-081). The hand-off copy of the bundle was deleted
once the submission was confirmed.

## Where the project stands (end of the twenty-third session)

**1.5 (versionCode 16) is submitted to Google Play for review.** It carries
two fixes: About this surah keeps its headings on their own lines
(`RichText.paragraphs`, drawn by the study view and audited by
`tools search`), and opened from the Mushaf, the ayah card reads word by
word first, then the translation, then the tafsir (D-079). What was handed
over: 147,736,943 bytes, SHA-256
`afa01b1d7953f51fb9c4c0fe4d6a99b80b742ba899592d904ca3b942d9571d5e`, signed
with the shared upload certificate, carrying only the core pack. The
screenshots came from run 35771681366, all three form factors, every frame
compared with its artifact by `cmp`, and both were handed over together.
The hand-off copy was deleted once the submission was confirmed.

**The Bangla names.** The session researched a full Bangla surah-name list
(Wikipedia base, adapted per the Arabic and English columns) and the owner
decided the release ships without it; a future release can pick it up, and
D-079 records where the work stopped.

**Verification.** JVM suite, lint, and `assembleDebug` green. All five
owner-machine content gates ran green locally this time (`verify`, `audit`,
`search`, `fonts`, `checkdb`), closing the three D-078 left owed. The
instrumented tests and the screenshot tour ran in CI (build run 35771681314,
screenshots run 35771681366, both green); the emulator was left out of this
session at the owner's word.

## Where the project stands (end of the twenty-second session)

**1.4 (versionCode 15) is submitted to Google Play for review, carrying
the reader's eleventh report (D-077); 1.3 remains in review.** The bundle
is `quran-1.4-vc15.aab`, 147,738,638 bytes, SHA-256
`5dce62ed7086efac2cfee6857c1073991d13f81bbb069b5730cb2cd09f711f4b`, verified
as signed by the shared upload certificate; the hand-off copy was deleted
once the owner confirmed the submission, and `play-store/aab/` keeps only its
own note. The screenshots came from the same push (run
35702564098), all three form factors, every frame `cmp`'d against its
artifact and every sheet frame checked for its content, and both were handed
over together before the submission (D-078).

**What the eleventh report answered:** Browse's numbers align on one right edge so every name starts at one place;
every language but English is named in its own script with the English name
beside it (`বাংলা (Bangla)`); text keeps 12 dp from every trailing button
app-wide; the ayah card labels its Word by word block and the door under the
label says "Meanings" with the list's language; the pull gate now wears every
scrolling sheet, so a scroll back to the top never closes the card or the
settings pages; the pill reads Play, Note, Save, Share, More; the note sheet
says Take a note or Edit note; and Share sends a picture of the ayah (the
Hafs ayah, the first enabled translation, the reference, the app's mark and
name at the foot, the plain text riding as the caption, and the text share as
the fallback when anything fails).

**Verification.** JVM suite, lint, assembleDebug green; data instrumented
22/22; app instrumented 12/12 in two full runs, and CI green twice more
(build and all three screenshot legs). Walked on the emulator: the
pill's order, the welcome's language naming, the settings hub, the study
card, the note sheet's "Take a note", the share end to end (the PNG and the
system chooser's "Sharing image"), and the scroll gate keeping the card open
through repeated scroll-up gestures. After the emulator crashed this
session its input injection went unreliable in the screen's top band, so the
Browse numbers, the Language page, and the Mushaf card's label were read
from the CI set instead (run 35696938701, all three form factors, every
frame `cmp`'d against its artifact and every sheet frame checked for its
content); the one cold-boot crash and one snapshot-observer failure are
named in D-077 as environment findings, confirmed clean by two green CI
runs.

**The release's gates, plainly.** JVM, lint, both assembles, both
instrumented suites, `fetch`, `checkdb`, and `search` are green here and in
CI; `verify`, `audit`, and `fonts` did not run on this machine because the
manual QUL and QuranEnc exports in `content/raw` are gone from it (content
is byte-identical to the release that ran all five green), and D-078 names
that as owed, not passed.

## Where the project stands (end of the twenty-first session)

**1.3 (versionCode 14) is submitted to Google Play for review.** It answers
the reader's tenth report (D-074), and the screenshot workflow was made
deterministic (D-075). What was handed over: 147,700,916 bytes, SHA-256
`66dbc990261f41bdc31d42715a4a9d09ca3bb3acfcc2f60503a44777feffb1b1`, signed
with the shared upload certificate, carrying only the core pack. The bundle
was built from `93efe44`, which is byte-for-byte the app source in the tree:
the commits after it touch docs, the screenshots, and the workflow only. The
screenshots came from run 35630768597, all three form factors, every frame
compared with its artifact by `cmp`, and both were handed over together,
before the submission. The hand-off copy was deleted once the submission was
confirmed; `play-store/aab/` keeps only its own note (D-076).

**The reader's tenth report (D-074).** Seven fixes, each on a surface the
ninth report had just reshaped. The Language page now survives the Activity
recreation a language choice causes, so the reader stays where they chose.
Every toggle row keeps 12 dp between its words and its switch. Font sizes run
0.65 through 1.2. About this surah no longer truncates when open, and its
press mark is the ayah's own wash with the ayah's own inset. The ayah card's
horizontal rules are gone, replaced by "Translation" and "Tafsir" labels.
`ui-kit/TextButton` gives every text-only action the app's button shape, so
Save and Note are never mistaken for each other. Browse gained a Notes tab
that lists the ayahs a note was written on, newest note first, and tapping a
row opens the note over its ayah; `saved.db` went to version 2 with a
`note_at` column.

**Verification.** JVM suite, lint, and assembleDebug green; data instrumented
tests 22/22; app instrumented tests 12/12 on the phone profile. All five
owner-machine content gates ran (`verify`, `audit`, `fonts`, `checkdb`,
`search`), and every changed surface was walked on the emulator. The store set
was captured from CI (run 35630768597, the deterministic capture), all three
form factors, every frame compared with its artifact by `cmp` before it was
installed. The frame D-073 left open is closed, and the capture now completes
in under five minutes on all three legs.

## Where the project stands (end of the twentieth session)

**1.2 (versionCode 13) is submitted to Google Play for review.** This session
answered the reader's ninth report (D-072) and closed the hand-off (D-073).

**The reader's ninth report (D-072).** The language-change crash was found at
last, and it was two defects hiding each other. `openLibrary` loaded the pack
catalog unmarked and marked only a throwaway copy, so the view model believed
every pack missing; with word by word on, a language switch re-fetched a word
list already on the device and reopened the library, which closed the old
database under any in-flight query and threw on a worker. The catalog is now
marked once, in `openLibrary` and `reopenLibrary`. `ContentDatabase` hands each
query a read ticket from its start to its cursor's close, and close waits for
the last ticket, with the fresh library published before the old is retired on
a worker. The note moved off the ayah card into the long-press pill, which
gained a Note action and lost the surah reference; More now shows only what
the reading behind it does not (Mushaf: translation, word by word, tafsir;
study: tafsir only). The phone's back button closes the pill before the app.
Settings lists read alphabetically, and word meanings sits above the
translations. Bangla surah names stay content backlog with their two candidate
sources named in D-072.

**Verification.** JVM suite, lint, assembleDebug green; data instrumented tests
19/19 with a new `PackCatalogTest`; search and the Mushaf turn pass. The app
tour's leg died with the emulator (device not found), an environment failure.
The screenshot set is from run 35595201651; the tour's ayah-card frame was
adjusted after it and is named open in D-073.

## Where the project stands (end of the nineteenth session)

**1.1 (versionCode 12) is submitted to Google Play for review.** This
session answered the reader's eighth report, and the release bundle and
screenshots were handed over together, before the submission, per the
runbook (D-069 answers the report, D-070 rebuilds the screenshot workflow,
D-071 closes the hand-off).

**The reader's eighth report (D-069).** The owner read 1.0 on a phone and
reported sixteen things. One was fatal: turning Mushaf pages quickly killed
the app, because the launch picture's writer raced its own cache and threw
`NoSuchFileException` on a worker. `PageCache.save` is now serialized and
guarded whole, and `PageRenderer.rememberStartupPage` copies the bitmap before
the cache can recycle it, so the second race the first had hidden (`Can't
compress a recycled bitmap`) is closed too. The reciter chooser is now the
pill exactly, with no shadow and no border, because the pill has neither. The
search results list wears the same drag gate Browse has, so a scroll back to
the top no longer closes the sheet. Settings are tidier: word by word under
Translations, Follow the reciter with the Reciters, Keep the screen awake in
the hub, and the Reading page deleted. Bangla wording was corrected: Last Read
is সর্বশেষ পঠিত, Theme is থিম, Font size is ফন্ট সাইজ, the Arabic size row is
কুরআনের আয়াত, the theme names are পেপার/সেপিয়া/নাইট/ব্ল্যাক, word by word is
শব্দে শব্দে অনুবাদ, Remove is মুছুন, and Sepia is সেপিয়া. Bangla surah names and
Bangla pack names were researched and left as content backlog (D-069 records
what exists). Two reports (a first-launch language crash and a Browse crash
after switching to Bangla) did not reproduce on this machine and are named as
open, not assumed clean.

**The reader's seventh report (D-067).** The owner read 0.10 on a phone and
asked for five things. The top bar is one row again, with a single door that
shows the reading the reader is not in and takes them there, replacing the
two-choice switch that made the bar two rows. The reciter chooser in the play
offer wears the pill's own surface and a rounded shape, with the chosen
reciter checked. Word by word is one switch in the settings hub, and turning
it on fetches the word list that speaks the translation's language, with the
size on the row before the tap. A first-launch screen offers English or
Bangla, each named in its own script, and the choice sets the interface, the
translation, the tafsir, and the word meanings together; a Language row at
the top of Settings changes it later. The locale is applied in
`MainActivity.attachBaseContext` from a synchronous mirror, so every sheet,
dialog, and popup speaks the chosen language, and the release bundle
disables language splitting. The version convention is now written in the
runbook: 0.1 through 0.9, then 1.0, never 0.10.

**0.10 (versionCode 10) is submitted to Google Play for review.** What 0.10
handed over: 147,691,199 bytes, SHA-256
`320f7b8de7ce0bcbff01eb97c185215df000efb1bb6dcf2ee6984b12b618b0af`, signed
with the owner's upload key, carrying only the core pack. The hand-off copy was
deleted once the submission was confirmed; `play-store/aab/` keeps its own
note. The screenshots came from the same pipeline that built the bundle, all
three form factors, every frame compared with its artifact by `cmp`, and both
were handed over together, before the submission (D-065 answers the report,
D-066 is the hand-off).

**The reader's sixth report (D-065).** The owner read 0.9 on a phone and
reported six things. The top bar is two rows on every size now: the controls
first, with Browse leading, the reading modes centered on the screen, and
Search and Settings at the end, then the surah and juz centered as one unit
under them, so no name of the 114 is shortened on any phone. The pressed
surah paragraph answers in the same rounded shape as an ayah, and the word
being recited in the study reading wears the Mushaf's rounded wash instead
of a rectangle. The end of a surah offers the next one as a card with its
name and an arrow. The word list language stayed what it was, the reader's
translation's, with no selector and no stored choice, and Last Read keeps its
twenty places.

**The reader's fifth report (D-063).** The owner read 0.8 on a phone and
reported seven things. The font size page previews Al-Ikhlas 112:1 instead of
the reader's own place, so a long ayah cannot push the steps off the screen.
Removing the selected reciter now writes a fallback: the remaining reciter
with the most audio on the device, and Husary when nothing is downloaded
anywhere. The Reciters page no longer downloads or explains the word timings;
they arrive with the first surah played, the offer's reciter name carries a
chevron because the dropdown had been there since 0.2 and could not be found,
and the downloaded surah rows keep a compact 40 dp Remove target. The two
reading modes are one centered switch in the top bar. Arabic inside a Latin
tafsir stands at 1.4 to 1, the proportion the study translation already gives
its inline Arabic. The ayah card no longer repeats Save and Share, and About
this surah is pressed only where it opens.

**The reader's fourth report (D-061).** The owner read 0.7 on a phone and
reported seven things. The ayah actions bar is a floating pill now, the top
bar reads a little smaller, and the Mushaf pager turns right to left, with
`MushafTurnTest` pinning it because a still frame cannot show a direction.
The search chips kept their shape and gained the check they were missing, and
an all-off filter row says so instead of "No matches." Back from a settings
page returns to the hub, the downloaded surahs door sits closer under its
reciter with the arrow beside a name that never changes, and the Browse pull
is fixed for the case that got past the last fix: an interrupted fling never
reports its end, so the gate now watches the finger landing and lets any
gesture that finds the list scrolled own its leftovers.

**The reader's third report (D-059).** The owner read 0.6 on a phone and
reported ten things, the first of them a defect that had shipped since the
first release: every Mushaf line was drawn left to right, so its words read
backwards. The renderer
now starts at each line's right edge and walks left. The open book drawn in
D-057 read as an inverse book, so the Mushaf icon was redrawn as pages rising
from the fold with the spine dipping at the foot, beside the study page. A
footnote now takes the size of the text it belongs to. About this surah
dismisses with a tap anywhere outside it. Opening a surah from Browse lands on
the reader's last place in it, or at its top when there is none; the Juz rows
now open the ayah they name. Browse lost its title and Last Read rows lost
their redundant Open action. A scroll back up in Browse can no longer pull the
sheet closed, because a new pure policy in `core` judges the pull by where the
gesture began; pull to close from the top is unchanged. A reciter's downloaded
surahs are now a door with an arrow, indented under the reciter's name, with
tighter rows, and the page says plainly that the small download is the word
timings, not the audio.

**The reader's second report (D-057).** The owner read 0.5 on a phone and reported
thirteen things. The Mushaf icon is an open book now, so the two reading
modes never read as lookalikes. Last Read names a place and only a place: the
ayah's Arabic and translation are gone from those rows. The search filters
wrap under a "Search in" label instead of scrolling sideways, so no filter is
hidden. The ayah card opens a translator's note from its marker, like the
study reading, and its disclosure arrows are 20 dp at full contrast with the
state spoken. The word by word aid is set at the ayah's own ratio to its
translation (about 24.7 sp for a 14 sp meaning), centered over each meaning,
and drawn by one shared component in both surfaces. The reading gained a named
spacing scale (`ui-kit/theme/Space.kt`), and the settings pages gained the same
room. "Text" became "Font size" and "The page" became "Theme". A reciter's
row now says that its 1.6 MB download is the word timings, with the audio added
one surah at a time. A download offer leaves with the surah it belongs to, a
finished surah is no longer left marked as playing, the continuation offer has
a close button, and the ayah card no longer repeats the Play action.

**The reader's first report (D-053).** The owner read 0.3 on a phone and
reported twelve things, two of them the same crash: a duplicate string name
shared by two modules (`pack_downloading`, with different format arguments)
threw on the main thread from `stringResource`, so tapping Add on a
translation, tafsir, or word list crashed the app, and behind that crash sat a
pack row whose tap was wrapped in `if (pack.installed)`, so a pack the reader
did not have could only be reached through the button that crashed. The turn
no longer lifts the page or casts a shadow; the study chrome steps aside while
scrolling; Appearance gained automatic night mode; Settings has one pack row
for every list, where tapping a name or a mark installs or turns on what it
names; a downloaded surah's Remove removes; and the app says Bangla wherever
it names the language.

**Signing moved into CI and stays private (D-054).** A `signed-bundle` job in
`build.yml` signs on every push to `main` from the four family secrets
(`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), never
on a pull request, and leaves the bundle and its checksum in the run's own
artifacts: private, one `gh run download` away, and deleted by GitHub after
two weeks. It fails when the secrets are absent rather than producing an
unsigned artifact, and it reads `jarsigner`'s words rather than its exit
code, because an unsigned file exits 0 and says so only in prose.
`release.yml` was deleted: it was a second signing path whose four secret
names never existed, so a tag would have attached an unsigned bundle to a
draft release.

**A real bug the screenshots found (D-055).** Collecting the store set is what
surfaced it: the tablet search frame showed the reader `</p><h2>` inside a
tafsir result, because a tafsir is stored as a small HTML subset for its own
panels and search cut its excerpt from that stored form for a view with no
parser. `RichText.plain` is now the readable form of anything, and
`Search.excerpt` matches on that same form, so the highlight still lands; the
surah introduction had the same defect and the same fix. `tools search` now
audits every excerpt it can produce (465 of them) and all 114 introductions
for markup and for a highlight that lands, in CI's content gates.

**A screenshot leg can pass while holding a photograph of Android.** The
first 0.5 capture came back with "Pixel Launcher isn't responding" in all
sixteen 10-inch frames, the tour on the wrong screens after it because the
dialog had swallowed the taps, and the workflow still green. The capture now
checks the window list before keeping a frame and fails rather than write a
dialog into the store listing, the workflow clears a dialog before the tour
starts, and a leg must now produce every frame the tour captures (eight,
since the thirteenth session).

The suite is green locally and in CI: core tests (57), data unit tests (9),
the data instrumented tests (16), the app instrumented tests (12, the
screenshot tour and the Mushaf turn test included), lint with no issues, and
all five content gates (`verify`, `audit`, `fonts`, `checkdb`, `search`),
which this machine ran against the raw QUL and QuranEnc exports in
`content/raw`, closing the caveat the fifteenth session left open.
