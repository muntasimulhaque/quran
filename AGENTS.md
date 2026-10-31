# Quran: working rules

**The first task of every session, before reading this file: `git fetch`, then
`git pull origin main`.** The owner works from more than one machine, and a
session that has not pulled has not started.

A free, offline Android Quran reader. One reading with the QPC Hafs text,
color-coded tajweed, Saheeh International, word-by-word, and Tafsir Ibn Kathir,
with recitation by Al-Minshawi and Al-Husary. No ads, no trackers, no accounts,
and no network beyond the project's own Releases. Code is MIT; each content
dataset keeps its own source, version, and license.

Store title: `Quran: The Noble Book`. App title and launcher name: `Quran`.
Package: `io.github.muntasimulhaque.quran`, permanent.

## Why this file is short

The code is the source of truth. Every settled decision lives in the KDoc beside
the thing it decided, tagged `owner decision`, so `git grep "owner decision"` is
the record of why the app is the way it is: the pill's one measure, the five text
sizes, the chevron rule, the ring on the theme swatch, the pace on the reciter's
line. The code that draws a decision and the test that measures it keep it
honest, and a decision that moves is edited where it lives.

Prose that restates the code is a second story that drifts. Once it disagrees
with the code, an agent has two answers and no way to pick.

So this file holds only what no file of code can say: the boundaries the owner
keeps, the vocabulary, and a map thin enough to make navigation faster. It is not
the final word. If a change you believe in contradicts a rule, do not drop it
silently and do not build against the rule either: name the conflict, make the
case, and let the owner decide. An overridden rule is edited here in the same
change, never left as a dead letter.

## Every session

1. Pull first, as above. Never in the same block as a read of this file, which
   races the copy you are about to update.
2. Do the work, then ask one question: anything else? The build waits for the
   owner's word, and no `versionCode` moves until the session is done.
3. Session end: the README status and the release notes current, the map checked
   against `settings.gradle.kts`, the tree clean. What may go: `build/`
   directories, `content/work/`, the hand-off AAB. What must stay: `content/raw/`
   (the owner's manual exports, the provenance of every pack), `content/quran.db`,
   `content/packs/`, and `play-store/screenshots/`.

## The law

1. **Offline except one thing.** `INTERNET` serves exactly one purpose: content
   from this project's own GitHub Releases, and only on the reader's word. The
   chosen language's translation arrives at once and its tafsir after it
   (unmetered only when the phone is on cellular); the word list arrives with
   the reading toggle that turns it on; a recitation package arrives for one
   surah after Play and the shown size (or after a standing Continue to the next
   surah); and a held pack refreshes quietly only when the app is newer, only
   unmetered, only after the first page is readable. No other host is ever
   contacted, and there is no analytics or telemetry. No WebView.
2. **Permissions: exactly `INTERNET`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`,
   `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `SCHEDULE_EXACT_ALARM`,
   `RECEIVE_BOOT_COMPLETED`.** `USE_EXACT_ALARM` is never declared: Play accepts
   it for an alarm clock or a calendar only, and rejected 3.9 over it. The app
   asks for the notification once, at the reminder card on the first screen, and
   for the exact time at the two acts that set the reminder, on the phones that
   withhold it, never where the notification was refused. Library-merged
   permissions are documented, not fought. A new permission needs the owner's
   sign-off written here first.
3. **No ads, trackers, analytics, or accounts.** AndroidX, Kotlin, Media3, and
   DataStore only, with kotlinx.serialization in `tools/` alone; the
   accessibility test's framework is test-only and never ships. The reader's own
   work never leaves the device: there is no backup, no export, and no import.
   A new dependency is proposed here first.
4. **The sacred text is untouchable.** Not one byte of Quran text, translation,
   or tafsir is edited by hand. Content changes only through `tools/`, and every
   dataset carries its source, version, checksum, and license in
   `content/manifest.json`.
5. **The app must not crash.** No `!!`, no unchecked casts, no swallowed
   exceptions. State survives rotation, backgrounding, and process death. A
   crash is a stop-the-line event.
6. **Nothing may feel like AI slop.** No placeholder copy, dead buttons, lorem
   text, stock iconography, or generic Material defaults. Every string is a real
   sentence, every color is chosen, every icon is drawn for this app.
7. **Accessibility is a rule, not a feature.** TalkBack works end to end, the
   reading honors the system font scale at its own five steps, and contrast
   targets live in the theme and are measured by a test.
8. **Two UI languages, one content language each.** English and Bangla ship
   together, the strings in each module's `values-bn`; the reader's choice sets
   the translation, the tafsir, and the word meanings too. Arabic is data, not a
   locale, so RTL stays on. A new language needs a real translation pass and a
   `values-xx` folder. The frozen names (`app_name`, `first_paint_title`,
   `first_paint_subtitle`, the store title) are never translated.
9. **Reader first.** The app opens where the reader left off. There is no home
   screen, no dashboard, and no permanent tab bar; index, search, library, and
   settings are sheets raised from a slim bar.
10. **Simple to the bone, fast to the point of invisible, quiet.** No wizard
    beyond the optional first screen, no dialog before the text, no feature that
    asks a choice before the reading. Cold start lands on readable text with no
    spinner, the reading is a scroll through rows already measured, and nothing
    blocks the main thread. No streaks, badges, social, or guilt.
11. **No AI attribution anywhere.** No `Co-Authored-By` trailers, no "generated
    with" footers, no name in contributors, commits, or code.
12. **The owner's law.** The build waits for the owner's word, and one build
    carries the whole session. `versionCode` by 1; `versionName` up one tenth
    within the major line, 0.9 then 1.0 (never 0.10, never two digits).

## Style

- **No em dashes**, anywhere. `NoEmDashTest` scans the repo and fails the build.
- **US English**, everywhere: color, gray, center, license, behavior.
- **User-facing strings** live in the `strings.xml` of the module that draws
  them, nowhere in Kotlin. **Colors** live in the theme, nowhere else.
- **Small pieces, plain names.** Split early, and a name that says the idea beats
  a name that says the screen.
- **Small edits** are `node -e` replacements, and a CRLF file needs `\r?\n` in the
  pattern or the replace silently no-ops. Grep afterwards to confirm.

## Map

| Path | What is there |
| --- | --- |
| `core/` | pure JVM Kotlin, no `android.*`: references, search normalization, rich text, models |
| `data/` | read-only content access, the saved-ayah and last-read databases, preferences |
| `app/` | the shell: activity, view model, the screen that composes the features |
| `feature-reading/` | the reading surface |
| `feature-search/` | the search sheet |
| `feature-browse/` | Surahs, Juz, Last Read, Saved |
| `feature-playback/` | the pill, the listening menu, the player |
| `feature-settings/` | the settings hub and its pages |
| `ui-kit/` | the theme and palettes, the hand-drawn icons, the shared rows and shapes, the small formatters |
| `content-assets/` | the faces and the core pack the app ships |
| `tools/` | the offline pipeline (`--args="help"` lists it); `tools/icons/forge.py` draws every icon and the feature graphic |
| `content/` | `quran.db`, `packs/`, `manifest.json`, `recitation-manifest.json`; `raw/` is local and gitignored |
| `play-store/` | the listing, the screenshots per form factor, the hand-off AAB |
| `benchmark/` | the startup profile's generator, development only, never in the bundle |
| `docs/` | the Pages site and the privacy policy Play links to |
| `.github/workflows/` | `build.yml` (gates, data tests, signed bundle), `screenshots.yml` (the store set) |

## Where the rest lives

| Thing | Home |
| --- | --- |
| Status, version, release notes | `README.md` |
| Store copy, the version line, the eight frames | `play-store/listing.md` |
| Every dataset's source, version, checksum, license | `content/manifest.json` |
| Every settled design decision and the road not taken | the KDoc tagged `owner decision` |
| A decision since overridden, and the reasoning that led here | `git log` |

## Glossary

- **the reading**: the ayah-by-ayah reader with translation, word-by-word, and
  tafsir. There is one reading and it has no mode name.
- **the reference**: an ayah key in `surah:ayah` form, for example 2:255.
- **the deck**: the day's ayah is one card of a shuffled 6,236, dealt by the day
  number and reshuffled every pass, so nothing about it is stored.
- **the ayah card**: the sheet one ayah opens. The ayah, its translation, and its
  meanings are already on the reading, so the card does not repeat them.
- **the text button**: `ui-kit`'s one shape for a word that acts. A heading, a
  name, or a label is bare type; anything that answers a touch is a rounded
  shape, except a numeric index, which is bare type with a target and one
  filled circle for the reader's place.
- **the pack**: one downloadable set of content or recitation, addressed by its
  own hash. The app reads a pack and never hotlinks one.
- **the content database**: the read-only SQLite built by `tools/`, shipped as an
  asset, replaced wholesale on update.
- **the manifest**: `content/manifest.json`, the one place a dataset's source,
  version, license, and checksum are recorded.

## Traps

- **A popup's place on the glass cannot be read by a compose test.** Every node
  in a popup reports coordinates from that window's own corner. Pin the rule
  that places it as arithmetic in a JVM test, and the anchor in the compose test.
- **A gate that reads content by table name is reading a contract, not a
  schema.** Change the build's shape and run every `:tools` gate.
- **A red leg is a fact to baseline, not a verdict.** `git worktree add` the
  parent commit and run the class there before believing a change of yours did
  it. A retry is worth one when it names the failure; a re-wait is worth
  nothing.
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
  the app to verify UI**: JVM suite and lint, push, then read the capture run's
  frames. Every UI claim is backed by a frame or a compose run.

## Verify

```bash
./gradlew :core:test :tools:test :data:testDebugUnitTest :ui-kit:testDebugUnitTest :feature-playback:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :data:connectedDebugAndroidTest   # content, storage, recitation manifest
./gradlew :app:connectedDebugAndroidTest    # the app surfaces and the screenshot tour
./gradlew :tools:run --args="help"          # the content pipeline, every step
```

A local `content/quran.db` rebuild must reproduce its committed SHA-256 exactly;
if it does not, stop and find out why before committing. Gates that read
`content/raw` run on the owner's machine only, so `ls content/raw` before a
release starts.

## Release

The bundle, the store note, and the screenshots are one delivery, handed over
together before the owner submits anything. The note stands in the hand-over
message itself as bare paragraphs under 500 characters, never only in a file. The
bundle comes from the newest green `build` run on `main`, not the first one after
the version bump, and signing is proved by `jarsigner`'s words and its certificate
SHA-256, never by exit code alone. Screenshots are collected whole from one run
whenever visible UI changed, all three form factors, eight frames each, every
frame read; without a visible change the hand-over says so and nothing is
captured. Delete the hand-off copy in `play-store/aab/` once Play has it.

## This machine

- Every Gradle call begins with
  `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`. It is the
  only JDK here. `local.properties` holds `sdk.dir` and is gitignored.
- The emulator runs here on three display-on AVDs (`quran_phone`,
  `quran_tablet7`, `quran_tablet10`) with the host GPU (`-gpu host`) and its
  window, so the instrumented suites have a home that is not CI and leave the
  CPU to the suite. A software renderer is never used here; the CI runners keep
  their own flags. `gh` is authenticated.
- `python3` and `python` are the Store alias; the real interpreter is under
  `C:/Users/zn/AppData/Local/hermes/tools/`. Nothing in the build needs it.
- Two tool calls in one block run in parallel. Never edit a file and read it in
  the same block.
