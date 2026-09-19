# Architecture: small modules, one direction

The app is a set of small modules with one rule: **dependencies point one
way, and no module knows about the app.** A feature cannot reach the reader's
state, the app cannot be imported by anything below it, and Gradle refuses to
build a cycle, so the rule enforces itself.

## The modules

```
:core            pure Kotlin: the Arabic normalizer, the rich text parser,
                 and the search primitives. No Android, no UI.
:data            content and memory: the pack catalog and store, the content
                 database (the core pack plus attached packs), the settings,
                 the saved ayahs and notes, the last-read history, the
                 recitation files and downloads. No UI.
:content-assets  no code: the fonts the app is drawn with (Inter, Literata,
                 Amiri Quran). The generated assets (page fonts, study font,
                 core pack, catalog, recitation manifest) are wired in by the
                 app module, which owns the APK.
:ui-kit          the shared look: theme and palettes, the hand-drawn icons,
                 the rich text views, and the small formatters. Every feature
                 draws with these and nothing else.
:feature-mushaf  the page renderer and the page: glyph geometry, theme-aware
                 bitmaps, the cache, and the washes.
:feature-study   the per-surah reading and the ayah card.
:feature-search  the search sheet.
:feature-browse  the surahs, the juz, the last-read places, and the saved list.
:feature-playback the player: controller, session service, and the bar.
:feature-settings the settings sheet, as pure data in and callbacks out.
:app             the wiring: the activity, the view model that holds the
                 reader's state, the screen that composes the features, and
                 the manifest where the app's permissions live.
:tools           the content pipeline, on the JVM, with no Android at all.
```

`app` depends on every feature. Features depend only on `ui-kit`, `data`,
`content-assets`, and `core`. Features never depend on `app` or on each
other.

## The rules a feature follows

1. **Plain data in, callbacks out.** A feature composable takes values and
   lambdas. It never takes a view model, never touches a database, and never
   looks up state for itself. `SettingsSheet` receives `AppSettings`, the
   pack list, and a `SettingsActions` record of callbacks; `StudyList`
   receives the surah, its settings, and a `loadRows` function that answers
   with a whole surah. That is why a feature can be read top to bottom,
   tested, and previewed with made-up data.
2. **One file, one job.** No file in a feature passes a few hundred lines
   without being asked why. The settings feature is a hub, its pages,
   its rows, and the credits sheet; the reader screen keeps the shell
   and hands the pager to a function of its own. `ContentDatabase` is the one
   honest exception, because every query in the app lives in one auditable
   place, and splitting it by table would scatter the transaction rules.
3. **No Android framework above the data line.** `core` is pure Kotlin, so
   its tests run in milliseconds without a device. `tools` is a plain JVM
   program, so the whole content pipeline runs on a laptop or in CI.
4. **Nothing is downloaded by a feature.** Downloads live in `data`
   (`PackDownloader`, `RecitationDownloader`) and happen only when the app
   asks on the reader's behalf, after the size has been shown.
5. **Resources stay where they are read.** The fonts live in
   `content-assets`, and every sentence the reader can see lives in a
   `strings.xml` in the module that draws it: `app` for the shell, each
   feature for its own surface, `ui-kit` for the two mode names. Nothing in
   Kotlin carries user facing English. Assets are read at runtime through
   `context.assets`, which the APK merges from every module.

## Why this shape

* **It makes the reading experience legible.** The Mushaf renderer, the
  study reading, the search, the browse lists, the player, and the settings
  are separate pieces with separate reasons to change. A change to the player
  cannot break the page, and a reviewer can read one feature without holding
  the whole app in their head.
* **It makes the content modular, not just the code.** `data` speaks packs
  and nothing else: the core pack the app ships, and translations, tafsirs,
  word lists, and reciters fetched from the project's own Releases.
* **It is honest about the one place where everything meets.** All the
  reader's state lives in `ReaderViewModel` in `app`. That is deliberate: one
  owner of the reading position, the mode, the packs, and the player, and no
  shadow copies anywhere else.
* **It keeps the build fast and the tests cheap.** Pure Kotlin tests for
  `core`, JVM tests for `tools`, instrumented tests for the data layer, and
  one screenshot tour for the whole app.

## What is measured, not assumed

* `:core:test` runs the normalizer, parser, search primitives, and the scan
  that refuses an em dash anywhere in the repository.
* `:data:testDebugUnitTest` runs the pack id contract, and
  `:data:connectedDebugAndroidTest` runs the saved store, its shape, the
  last-read history, and the recitation manifest.
* `:app:connectedDebugAndroidTest` runs the search suite against the shipped
  content, the word by word aid in both languages, and the screenshot tour
  that walks every surface, waiting for the screen to settle before it keeps
  a frame.
* CI runs all of it, plus lint (clean, with nothing suppressed), plus the
  content gates that can run without the raw sources, and uploads the
  screenshots as build artifacts.
* The performance numbers are measured, not assumed: a launch in a minified
  release build on a software rendered emulator reaches its first frame at
  about 780 ms warm, and the page picture is on disk before the content
  database opens.
