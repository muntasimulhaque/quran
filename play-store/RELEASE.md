# Release runbook

One command builds, one upload publishes. Everything here has been run.

## 1. Prove the content first

```bash
./gradlew :core:test
./gradlew :tools:run --args="verify"     # every source hash, structural checks
./gradlew :tools:run --args="build"      # the database and the packs
./gradlew :tools:run --args="checkdb"    # the committed database and the catalog
./gradlew :tools:run --args="search"     # Arabic, Bengali, and Latin search
./gradlew :tools:run --args="packs"      # pack files and the catalog
./gradlew :app:lintDebug
```

`verify`, `build`, and `packs` need the raw sources and write new artifacts.
`checkdb` and `search` are the gates a fresh clone and CI can run, and CI runs
them on every push.

## 2. Publish what the app downloads

```bash
./gradlew :tools:run --args="packs publish"     # content packs, content addressed
gh release upload content-db-<hash8> content/quran.db --clobber
```

Tags are derived from each pack's own SHA-256, so a pack version is never
overwritten and an old catalog entry keeps resolving.

## 3. Build the bundle

```bash
./gradlew :app:bundleRelease
```

The keystore lives in the owner's vault, outside every repository. When it is
absent (a fresh clone, CI) the build degrades to unsigned instead of failing.
The release bundle carries **only the core pack**: the Quran text and its page
layout. Translations, tafsirs, word lists, and reciters are downloaded from
the project's own Releases when a reader asks for them.

Expected sizes: bundle about 147 MB, APK about 144 MB, of which the 604 Mushaf
page fonts are the bulk, because the app must render the Book with no network.

## 4. Upload

1. Play Console, **Internal testing**, create a release.
2. Upload `app/build/outputs/bundle/release/app-release.aab`.
3. Play will ask about the app's size and the pack download. Answer: content
   packs are downloaded by the reader's choice over HTTPS from the project's
   own GitHub Releases; nothing is downloaded at launch.

## 5. Store listing

Everything the listing needs is in this folder:

| Field | Value |
|---|---|
| App name | `Quran: The Noble Book` |
| Short description | see `listing.md` |
| Full description | see `listing.md` |
| Category | Books & Reference |
| Content rating | Everyone (no ads, no purchases, no user content) |
| App icon | `icon-512.png` |
| Feature graphic | `feature-graphic-1024x500.png` |
| Phone screenshots | `screenshots/` |
| Data safety | nothing collected or shared; see `listing.md` and `docs/privacy.md` |
| Privacy policy | `https://github.com/muntasimulhaque/quran/blob/main/docs/privacy.md` |

## 6. After the upload

* Keep the internal track for a few days, then the closed track, then
  production when the store page reads well.
* The `INTERNET` permission exists for one use only: a pack or a surah the
  reader asked for, from the project's own Releases. Play's data safety form
  and the privacy policy say exactly that.
