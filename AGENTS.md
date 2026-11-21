# Quran: working rules

**The first task of every session, before reading this file: `git fetch`, then
`git pull origin main`.** The owner works from more than one machine, and a
session that has not pulled has not started.

A free, offline Android Quran reader, in English and Bangla.

Store title: `Quran: The Noble Book`. App title and launcher name: `Quran`.
Package: `io.github.muntasimulhaque.quran`, permanent.

## Why this file is short

The code is the source of truth. Every settled decision lives in the KDoc
beside the thing it decided, tagged `owner decision`, so
`git grep "owner decision"` is the record of why the app is the way it is: the
pill's one measure, the five text sizes, the chevron rule, the ring on the
theme swatch, the pace on the reciter's line. The code that draws a decision
and the test that measures it keep it honest, and a decision that moves is
edited where it lives.

Prose that restates the code is a second story that drifts. Once it disagrees
with the code, an agent has two answers and no way to pick. So this file holds
only what no file of code can say: the boundaries the owner keeps, the
vocabulary, the traps the tests taught us, the procedure of a release, and a
map thin enough to make navigation faster. It is not the final word. If a
change you believe in contradicts a rule, do not drop it silently and do not
build against the rule either: name the conflict, make the case, and let the
owner decide. An overridden rule is edited here in the same change, never left
as a dead letter.

## Every session

1. Pull first, as above. Never in the same block as a read of this file, which
   races the copy you are about to update.
2. Do the work, then ask one question: anything else? The build waits for the
   owner's word, and no `versionCode` moves until the session is done.
3. Session end: the README status and the release notes current, the map
   checked against `settings.gradle.kts`, the tree clean. What may go:
   `build/` directories, `content/work/`, the hand-off AAB. What must stay:
   `content/raw/` (the owner's manual exports, the provenance of every pack),
   `content/quran.db`, `content/packs/`, and `play-store/screenshots/`.

## The law

1. **Offline except one thing.** `INTERNET` serves exactly one purpose: content
   from this project's own GitHub Releases, and only on the reader's word. No
   other host is ever contacted, there is no analytics or telemetry, and there
   is no WebView. Which pack may arrive, on what network, and after whose
   word is decided beside the downloader, tagged `owner decision`.
2. **Permissions: exactly `INTERNET`, `POST_NOTIFICATIONS`,
   `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`,
   `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, and never
   `USE_EXACT_ALARM`**: Play accepts that one for an alarm clock or a calendar
   only, and it rejected 3.9 over it. Where the app asks is decided beside the
   code that asks, tagged `owner decision`. Library-merged permissions are
   documented, not fought. A new permission needs the owner's sign-off
   written here first.
3. **No ads, trackers, analytics, or accounts.** AndroidX, Kotlin, Media3, and
   DataStore only, with kotlinx.serialization in `tools/` alone; the
   accessibility test's framework is test-only and never ships. The reader's
   own work never leaves the device: there is no backup, no export, and no
   import. A new dependency is proposed here first.
4. **The sacred text is untouchable.** Not one byte of Quran text,
   translation, or tafsir is edited by hand. Content changes only through
   `tools/`, and every dataset carries its source, version, checksum, and
   license in `content/manifest.json`.
5. **The app must not crash.** No `!!`, no unchecked casts, no swallowed
   exceptions. State survives rotation, backgrounding, and process death. A
   crash is a stop-the-line event.
6. **Nothing may feel like AI slop.** No placeholder copy, dead buttons, lorem
   text, stock iconography, or generic Material defaults. Every string is a
   real sentence, every color is chosen, every icon is drawn for this app.
7. **Accessibility is a rule, not a feature.** TalkBack works end to end, the
   reading honors the system font scale at its own five steps, and contrast
   targets live in the theme and are measured by a test.
8. **Two UI languages, one content language each.** English is the default and
   the fallback, and the app follows the phone's language silently: no
   language screen stands before the reading. English and Bangla ship
   together, the strings in each module's `values-bn`; a reader who wants
   another language chooses it in Settings, and that one choice sets the
   content language too. Arabic is data, not a locale, so RTL stays on. A new
   language needs a real translation pass and a `values-xx` folder. The frozen
   names (`app_name`, `first_paint_title`, `first_paint_subtitle`, the store
   title) are never translated.
9. **Reader first.** The app opens where the reader left off. There is no home
   screen, no dashboard, and no permanent tab bar; index, search, library, and
   settings are sheets raised from a slim bar.
10. **Simple to the bone, fast to the point of invisible, quiet.** No wizard,
    no dialog before the text, no feature that asks a choice before the
    reading. The reminder is offered after the first surah, never before it.
    Cold start lands on readable text with no spinner, the reading is a
    scroll through rows already measured, and nothing blocks the main thread.
    No streaks, badges, social, or guilt. The bar's choreography is decided
    beside the bar, tagged `owner decision`.
11. **No AI attribution anywhere.** No `Co-Authored-By` trailers, no
    "generated with" footers, no name in contributors, commits, or code.
12. **The owner's law.** The build waits for the owner's word, and one build
    carries the whole session. `versionCode` by 1; `versionName` up one tenth
    within the major line, 0.9 then 1.0 (never 0.10, never two digits).
13. **No workflow may gate on a hosted emulator.** A hosted emulator is a
    signal, never a stop: the release gates and the store capture run on this
    machine's own AVDs. The build workflow holds no emulator, and the
    screenshot and instrumented workflows are dispatch-only, so neither runs
    on its own or holds a bundle.

## Style

- **No em dashes**, anywhere. `NoEmDashTest` scans the repo and fails the
  build.
- **US English**, everywhere: color, gray, center, license, behavior.
- **User-facing strings** live in the `strings.xml` of the module that draws
  them, nowhere in Kotlin. **Colors** live in the theme, nowhere else. A
  sentence two modules need is written in each of them, and `CopyParityTest`
  holds the copies to one Bangla.
- **Small pieces, plain names.** Split early, and a name that says the idea
  beats a name that says the screen.
- **A test's name is a sentence.** In a JVM suite it is spaced, in backticks
  where the sentence needs a comma or a possessive, and an identifier where it
  does not. A device test's name is always an identifier: the Android runtime
  cannot hold a space in a method name, and `DeviceTestNamesTest` fails the
  build over one.
- **A settled decision is dated by its version**, not by the session that
  made it: `owner decision, 19.6` is a thing a later reader can find in
  `git log`, and "this session" names nothing once the session has ended.
- **Small edits** are `node -e` replacements, and a CRLF file needs `\r?\n` in
  the pattern or the replace silently no-ops. Grep afterwards to confirm.

## Map

| Path | What is there |
| --- | --- |
| `core/` | pure JVM Kotlin, no `android.*`: references, search normalization, rich text, models |
| `data/` | read-only content access, the downloads that fetch a pack from the project's Releases, the saved-ayah and last-read databases, preferences |
| `app/` | the shell: activity, view model, the reading's screen and chrome (the bar, the top bar, the sheets, the ayah menu, the share card), the screen that composes the features, the daily ayah's scheduler and notification |
| `feature-reading/` | the reading's rows and blocks: the ayah and its door, the word sheet, the surah frames, the footnotes |
| `feature-search/` | the search sheet |
| `feature-browse/` | Surahs, Juz, Last Read, Saved |
| `feature-playback/` | the pill, the listening menu, the player |
| `feature-settings/` | the settings hub and its pages |
| `ui-kit/` | the theme and palettes, the hand-drawn icons, the shared rows and shapes (the text button, the icon button, the confirm sheet), the rich text and tajweed views, the small formatters |
| `content-assets/` | the faces the app ships |
| `tools/` | the offline pipeline (`--args="help"` lists it), with `word-audio` cutting the pronunciation pack; `tools/icons/forge.py` draws every icon and the feature graphic |
| `content/` | `quran.db`, `packs/`, `bundled/`, `word-audio/`, `manifest.json`, `recitation-manifest.json`, `catalog.json`, `build-report.json`, `surah-names-bn.json`, `audio-sources.json`, `audio-edges.txt`, `audit-report.md`; `raw/` is local and gitignored |
| `licenses/` | the SIL Open Font License the six open faces ship under, wired into the app's assets |
| `play-store/` | the listing, the screenshots per form factor, the hand-off AAB |
| `benchmark/` | the startup profile's generator, development only, never in the bundle |
| `docs/` | the Pages site and the privacy policy Play links to |
| `.github/workflows/` | `build.yml` (gates, signed bundle), `instrumented.yml` (dispatch-only data and app tests, signal only), `screenshots.yml` (dispatch-only capture signal; the store set is local) |

## Where the rest lives

| Thing | Home |
| --- | --- |
| Status, version, and the release just shipped | `README.md` |
| Every earlier release's notes | `RELEASES.md` |
| Store copy, the version line, the eight frames | `play-store/listing.md` |
| Every dataset's source, version, checksum, license | `content/manifest.json` |
| Every settled design decision and the road not taken | the KDoc tagged `owner decision` |
| A decision since overridden, and the reasoning that led here | `git log` |

## Glossary

- **the reading**: the ayah-by-ayah reader with translation, word-by-word, and
  tafsir. There is one reading and it has no mode name.
- **the reference**: an ayah key in `surah:ayah` form, for example 2:255.
- **the deck**: the day's ayah is one card of a shuffled 6,236, dealt by the
  day number and reshuffled every pass, so nothing about it is stored.
- **the ayah card**: the sheet one ayah opens. The ayah, its translation, and
  its meanings are already on the reading, so the card does not repeat them.
- **the text button**: `ui-kit`'s one shape for a word that acts. A heading, a
  name, or a label is bare type; anything that answers a touch is a rounded
  shape, except a numeric index, which is bare type with a target and one
  filled circle for the reader's place.
- **the pack**: one downloadable set of content or recitation, addressed by its
  own hash. The app reads a pack and never hotlinks one.
- **the content database**: the read-only SQLite built by `tools/`, shipped as
  an asset, replaced wholesale on update.
- **the manifest**: `content/manifest.json`, the one place a dataset's source,
  version, license, and checksum are recorded.

## Traps

- **A popup's place on the glass cannot be read by a compose test.** Every node
  in a popup reports coordinates from that window's own corner. Pin the rule
  that places it as arithmetic in a JVM test, and the anchor in the compose
  test.
- **A sentence is never merged over.** Material's host marks the paper it draws
  with a pane title and a dismiss action, and a pane title is one of Compose's
  unmergeable semantics properties: a merging ancestor laid over `AppSnackbar`
  (a `clickable`, a `toggleable`, a list item) throws "merge function called on
  unmergeable property PaneTitle" in the first test that reads the merged tree,
  so the leg reads as a fault in the app when it is the shape of the test. A
  test that wants the page's own touch puts the page beside the sentence and
  counts it with `pointerInput`, the way `SentenceTouchTest` does (found and
  named from material3 1.5's bytecode, 19.3).
- **A gate that reads content by table name is reading a contract, not a
  schema.** Change the build's shape and run every `:tools` gate.
- **A red leg is a fact to baseline, not a verdict.** `git worktree add` the
  parent commit and run the class there before believing a change of yours did
  it. A retry is worth one when it names the failure; a re-wait is worth
  nothing.
- **A local instrumented run starts with a clean install.** Uninstall both
  packages from the target AVD before the run: the app and the test APK. A
  `ClassNotFoundException` for a test class means a stale test APK, not a code
  fault. Reinstall before believing a change of yours did it (owner decision,
  18.3).
- **A capture class borrows the tour's two rules, or it costs a run.** A frame
  goes where the runner says (`screenshotOutputDirectory()`, which reads
  `additionalTestOutputDir`), never into a directory the class spells out: a
  frame written anywhere else is dropped while every test passes, and the leg
  comes up short with no failure to read. And the class hands the app back as
  it found it, in a `finally`: every instrumented class in a run shares one
  process and one set of settings, so a text size left at the top of the
  ladder made `SheetStillnessTest` measure the footnote sheet at 1.2 and read
  a drag of 11 dp against its own 20 dp floor. Both of those cost a green run
  and looked like defects in the app.
- **One Gradle call per capture leg.** A second `connectedAndroidTest`
  invocation replaces the additional output folder, so the first call's frames
  have to be collected before it, and it pays a second install and a second
  test runner on top. A frame that wants a different orientation, a different
  theme, or a different size is a test that changes it itself.
- **Verify by exit code**, never by grepping piped output. **Do not hand-drive
  the app to verify UI**: JVM suite and lint, push, then read the capture
  run's frames. Every UI claim is backed by a frame or a compose run.

## Verify

```bash
./gradlew :core:test :tools:test :data:testDebugUnitTest :ui-kit:testDebugUnitTest :feature-browse:testDebugUnitTest :feature-playback:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :tools:run --args="checkdb"                  # the content gate build.yml runs
./gradlew :tools:run --args="search"                   # the content gate build.yml runs
./gradlew :data:connectedDebugAndroidTest   # an instrumented audit, never a release gate
./gradlew :app:connectedDebugAndroidTest    # an instrumented audit, never a release gate
./gradlew :tools:run --args="help"          # the content pipeline, every step
```

A local `content/quran.db` rebuild must reproduce its committed SHA-256 exactly;
if it does not, stop and find out why before committing. Gates that read
`content/raw` run on the owner's machine only, so `ls content/raw` before a
release starts.

## Release

A release is three steps and no more.

1. **Gates.** The `gates` job of `build.yml` is the release gate: the whole JVM
   suite, `checkdb`, `search`, lint, and `assembleDebug`, run locally with the
   commands in Verify. Nothing else gates a release.
2. **Version and record.** `versionCode` by 1, `versionName` up one tenth
   within the major line. Write the release notes and the README status, and
   move the release before this one into `RELEASES.md`; the hand-off record
   follows the run.
3. **Push and hand over.** The push to `main` lets `build.yml` produce the
   signed bundle. From the newest green run on `main`, download
   `quran-signed-aab` into `play-store/aab/` with its `.sha256`, and give the
   store note in the hand-over message itself, as bare paragraphs under 500
   characters.

When a visible surface changed, capture all three sets, phone, 7 inch, and
10 inch, with

```bash
./gradlew :app:connectedDebugAndroidTest -Pquran.devPacks=screenshot \
  -Pandroid.testInstrumentationRunnerArguments.class=io.github.muntasimulhaque.quran.ScreenshotTest,io.github.muntasimulhaque.quran.ScreenshotVariantsTest,io.github.muntasimulhaque.quran.WordMeaningTest
```

on `quran_phone`, `quran_tablet7`, and `quran_tablet10` in turn. Before each
leg, uninstall the app and its test APK from that AVD, the clean-install trap.
Collect the frames from
`app/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/<AVD>/`
before another call replaces them, read the eight of each, and commit them to
`play-store/screenshots/phone/`, `play-store/screenshots/tablet7/`, and
`play-store/screenshots/tablet10/`. The phone leg runs at 1080 x 1920, the
7 inch at 1200 x 1920 and 320 dpi, and the 10 inch at its own 2560 x 1800 (see
This machine). When no visible surface changed, the three committed sets
stand.

When the Bangla interface changed, one separate leg photographs it, on the
same phone:

```bash
./gradlew :app:connectedDebugAndroidTest -Pquran.devPacks=screenshot \
  -Pandroid.testInstrumentationRunnerArguments.class=io.github.muntasimulhaque.quran.BanglaCaptureTest
```

It writes `bn-01-reading` and `bn-02-settings` and hands the app back in the
language it found it in. Those frames are evidence and are not committed to
the store, whose set is English: Bengali's longer lines are what a settings
row held to two lines and a button held to one can break, and they are the
only way to see it.

**Not in the per-release path, ever again.** The full app instrumented suite
is an occasional audit or a manual dispatch, never a release requirement. A
width-sensitive change gets one targeted class on one tablet, not the full
suite. The store capture is all three form factors every time, and no set
stands from an earlier release when a visible surface changed. A change that
genuinely touches runtime behavior may run the affected classes on the phone,
and the hand-off says so; otherwise the instrumented work is skipped, and the
hand-off says it was skipped.

The accessibility classes are the phone's own ear, and no JVM gate can hear:
a session whose diff touches any file under `feature-reading/`,
`feature-search/`, `feature-settings/`, `ui-kit/`, or
`app/src/main/kotlin/io/github/muntasimulhaque/quran/ui/reader/` runs
`AccessibilityTest`, `LargeTextTest`, `ReadingSemanticsTest`, and
`KeyboardFocusTest` on `quran_phone` before the hand-off, with the
clean-install trap respected, and the hand-off says so. Any other session
skips them and the hand-off says that too. The trigger is a path, never a
judgment: a path is a fact a diff can be checked against, and whether a
change touches accessibility is an opinion.

The bundle comes from the newest green `build` run on `main`, not the first
one after the version bump, and signing is proved by `jarsigner`'s words and
its certificate SHA-256, never by exit code alone. Delete the hand-off copy
in `play-store/aab/` once Play has it.

## This machine

- Every Gradle call begins with
  `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`. It is the
  only JDK here. `local.properties` holds `sdk.dir` and is gitignored.
- The emulator runs here on three display-on AVDs (`quran_phone`,
  `quran_tablet7`, `quran_tablet10`) with the host GPU (`-gpu host`) and its
  window, so the instrumented suites have a home that is not CI. Every AVD
  boots from its quick-boot snapshot (the configs carry
  `fastboot.forceColdBoot = no`), so launch with `-gpu host -no-boot-anim`
  and never `-no-snapshot-save`: `adb emu kill` saves the snapshot the next
  start loads, 6 s against a cold boot's 50. A snapshot the boot refuses is
  refreshed by one start with `-no-snapshot-load` and a clean exit. A
  software renderer is never used here; the CI runners keep their own flags.
  `gh` is authenticated.
- The phone AVD is 1080 x 1920 at 420 dpi, which is the store set Play has,
  so its leg runs with no display override. A forced 440 does not reproduce
  the set: 21.0 measured a 14 percent pixel difference against the committed
  frames, where 420 matched them to 0.12 percent, the status bar's clock.
  The 7 inch AVD's display is 800 x 1280 and the store set is 1200 x 1920,
  the same page at 1.5 times the pixels, so its leg runs with
  `adb shell wm size 1200x1920` and `wm density 320`; both are reset after
  the leg. The 10 inch AVD is 2560 x 1800 at 320 dpi, and its store set is
  that same display.
- `python3` and `python` are the Store alias; the real interpreter is under
  `C:/Users/zn/AppData/Local/hermes/tools/`. Nothing in the build needs it.
- Two tool calls in one block run in parallel. Never edit a file and read it in
  the same block.
