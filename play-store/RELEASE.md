# Release runbook

One command builds, one tag publishes. Everything here has been run, except
the tag driven workflow, which is new in the seventh session and waits for
the owner's first tag.

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

`packs` rewrites `content/packs/*.db` from the committed database. It is
deterministic: after a rebuild the catalog's SHA-256 values and byte counts
must be the same as the ones before it. If they are not, stop and find out
why before uploading anything, because the catalog's tags are the Releases
readers are downloading from.

## 2. Publish what the app downloads

```bash
./gradlew :tools:run --args="packs publish"     # content packs, content addressed
gh release upload content-db-<hash8> content/quran.db --clobber
```

Tags are derived from each pack's own SHA-256, so a pack version is never
overwritten and an old catalog entry keeps resolving.

## 3. Build the bundle

The keystore lives in the owner's vault, outside every repository. When it is
absent (a fresh clone, CI) the build degrades to unsigned instead of failing.
A build can point at another properties file with
`-Pquran.keystore=<file>`, which is how the release workflow signs from
repository secrets. The release bundle carries **only the core pack**: the
Quran text and its page layout. Translations, tafsirs, word lists, and
reciters are downloaded from the project's own Releases when a reader asks for
them.

Expected sizes: bundle about 147 MB, APK about 144 MB, of which the 604 Mushaf
page fonts are the bulk, because the app must render the Book with no network.

## 3b. Or let a tag build it

```bash
git tag v0.2 && git push origin v0.2
```

The `release` workflow refuses a tag that does not name the version the app
carries, runs the core tests and the content gates, builds the signed bundle
from the repository secrets, proves the signature with `jarsigner`, and
drafts a GitHub release with the bundle and its SHA-256 attached. Without the
secrets it builds anyway and says the bundle is unsigned. Download the draft
for the Play upload, then delete the draft's assets once Play has them.

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
| Privacy policy | `https://muntasimulhaque.github.io/quran/privacy.html` |

## 6. After the upload

* Keep the internal track for a few days, then the closed track, then
  production when the store page reads well.
* The `INTERNET` permission exists for one use only: a pack or a surah the
  reader asked for, from the project's own Releases. Play's data safety form
  and the privacy policy say exactly that.
* The screenshots in `play-store/screenshots/` come from the `screenshots`
  workflow, never from a hand run. Push to `main` refreshes them whenever the
  UI, the capture test, or the app build file changes; download the artifact
  and replace the folder when the images moved.
