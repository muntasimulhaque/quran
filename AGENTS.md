# Quran: working rules

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

1. Read this file.
2. `git fetch` and pull `main` before any other command: the owner works
   from more than one machine, and no session builds on a stale head.
3. Do the work, then ask one question: anything else? The build waits for
   the owner's word. No `versionCode` moves until the session is done and
   the owner says so.
4. Session end: append the session's entries to `docs/decisions.md`, bring
   the README status and the release notes current, and leave the tree
   clean.

## Hard constraints (non-negotiable)

1. **Offline except one thing.** The app holds `INTERNET` for exactly one
   use, approved by the owner and recorded in D-023: downloading a
   recitation package for one surah, from the project's own GitHub
   Releases, only after the reader taps Play and then approves the shown
   size. Nothing is fetched at launch, nothing is fetched automatically,
   no other host is ever contacted, and there is no analytics or telemetry
   of any kind. Everything else in the app works with no connection at
   all. No WebView.
2. **Permissions: media, notifications, and that one network use.** The
   self-declared permissions are exactly `INTERNET` (the download above),
   `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, and
   `FOREGROUND_SERVICE_MEDIA_PLAYBACK`. Library-merged permissions are
   documented, not fought. A new permission needs the owner's sign-off
   written here first.
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
8. **English UI, Arabic content.** Arabic is data, not a locale; RTL
   support stays on. No localization infrastructure until the owner asks.
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
11. **No AI attribution anywhere.** No `Co-Authored-By` trailers, no
    "generated with" footers, no name in contributors, commits, or code.
12. **Owner's law.** The build waits for the owner's word; one build
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
- MSYS rewrites `/sdcard/...` style arguments into Windows paths. Prefix
  `adb shell`, `adb push`, and `adb pull` with `MSYS_NO_PATHCONV=1`, and the
  same for `gh api` (and drop its leading slash).
- The Store alias intercepts `python3`, but a real `python` 3.12 exists on
  this machine; `node -e` is still the small-edit tool used here. Use it for
  small file edits instead of writing a script file.

## Release hand-off

The bundle and the screenshots are one delivery. They are collected, checked,
and handed over together, before the owner submits anything, because a
screenshot refreshed after the submission has nothing left to be used for:
the store already has the old set.

1. Raise `versionCode` by 1 and `versionName` by 0.1 in the app build
   file, and update the version line in `play-store/listing.md`.
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
sitting around.
7. **Hand over the bundle and the screenshots in the same message**, with the
   size, the checksum, what changed in the set (or that nothing did), and the
   notes pasted verbatim. Delete the hand-off copy once the owner confirms the
   Play submission. Hand the notes over as a bare paragraph: no blockquote, no
   fence, no wrapping quotes, nothing on the same line as the text.

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
- Last Read is a Browse tab beside Surahs, Juz, and Saved (D-051).
- Text sizes are 0.75, 0.85, 1, 1.2, 1.4 (D-051).
- Saheeh International is the only translation (D-004).
- Ibn Kathir and As-Sa'di are the tafsirs (D-005).
- Minshawi and Husary are the only reciters (D-007).
- No streaks, no gamification (D-009).
- The design direction is the manuscript language described in D-010.
- Content is sourced from QUL and QuranEnc under the owner decision in
  D-003, with all existing licenses honored and a takedown path in About.

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
  the old first paint.
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
- A screenshot leg can pass while holding a photograph of Android. A loaded
  software-rendered emulator raises "Pixel Launcher isn't responding", the
  dialog sits over every frame after it, and it swallows the taps the tour is
  making, so the tour ends up on the wrong screens: the 10-inch set once came
  back with that dialog in all sixteen frames and the workflow still green.
  The capture checks the window list before keeping a frame and fails the run
  rather than write a dialog into the store listing, and the leg requires
  every frame the tour captures (eight, since the thirteenth session) instead
  of one. Even so, install a set only after comparing every frame with the
  artifact: the guard catches the dialog, and `cmp` is what catches everything
  else.
- The bundle and the screenshots are one delivery, and the screenshots come
  first. A set refreshed after the owner has submitted has nothing left to be
  used for: the store already holds the old one, so the work is wasted and the
  session has spent CI minutes and an emulator run for nobody. Collect and
  verify both, then hand both over in the same message, before the submission.
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

1. **Measure on real hardware.** The numbers in D-037 come from a software
   rendered emulator, the slowest Android this app will run on. A
   Macrobenchmark module for startup and page turns, run on a phone, is the
   only way to hold the budget the design document sets (under 300 ms to the
   first painted page on a warm start) and to prove the baseline profile is
   pulling its weight.
2. **The second language.** The strings are ready for one; the reader is not.
   A translation pass needs a translator for the interface, a `values-xx`
   folder, and the same care the Arabic content gets: a real review, not a
   machine.
3. **Content backlog.** More translations and tafsirs as packs (each one is a
   dataset entry in `PackSources`, a pack definition, and a Release), and a
   second mushaf script if a font and layout are chosen.
4. **Instant launch, second step.** A pre-rendered bitmap of the *next* page
   in the direction the reader was reading, so the first swipe after a launch
   is also a texture draw.
5. **Trust work, second step.** The owner removed export and import in the
   tenth session (D-051), so this item is gone unless they ask for a different
   way to carry saved work. If they do, the shape to build is a merge preview
   and a way to move one note rather than the whole document, never the same
   JSON door again.
6. **Robustness, second step.** A pack that fails verification at download
   time should say which check failed (size, hash, or unpack) rather than one
   line for all three, and the content self check should offer the removal it
   recommends.
7. **A khatm plan, if the owner wants one.** A khatm is a commitment, not a
   guess: an explicit plan (a daily portion, a finish date) is the only honest
   way to keep a linear reading place apart from casual lookups, and it builds
   on the portion the glossary already describes. The eighth session shipped no
   heuristic for it (D-045).
8. **A recording of the tour's study frames.** The screenshot set covers the
   surface; a short screen recording of a page turn, a mode switch, and the
   ayah card would catch the motion a still cannot, and the pipeline already
   has an emulator to do it on.

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

## Where the project stands (end of the fifteenth session)

**0.8 (versionCode 8) is handed over for Google Play.** What 0.8 handed over:
147,681,534 bytes, SHA-256
`b7cbe82f1c66ab66fed4ce8a7466f78956e3d74fc1886dec39b5bbd3a7bdce92`, signed
with the owner's upload key, carrying only the core pack. The hand-off copy
lives in `play-store/aab/` until the submission is confirmed, then it is
deleted. The screenshots come from the same pipeline that built the bundle,
all three form factors, every frame compared with its artifact by `cmp`, and
both were handed over together, before the submission (D-061 answers the
report, D-062 is the hand-off).

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
screenshot tour and the new Mushaf turn test included), lint with no issues,
and the gates that run (`checkdb`, `search`). `verify`, `audit`, and `fonts`
need `content/raw`, the owner's manual QUL and QuranEnc exports, which the
fifteenth session's machine did not carry; run them on the machine that owns
them before the hand-over is closed.
