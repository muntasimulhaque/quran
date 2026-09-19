# Release runbook

The bundle is built and signed by CI on every push to `main`, and the signed
artifact is downloaded from the run. The owner's machine can still build one
from the vault, and the same signature check runs either way. Step 3 has both.

## 1. Prove the content first

```bash
./gradlew :core:test
./gradlew :tools:run --args="verify"     # every source hash, structural checks
./gradlew :tools:run --args="build"      # the database and the packs
./gradlew :tools:run --args="checkdb"    # the committed database and the catalog
./gradlew :tools:run --args="search"     # Arabic, Bangla, and Latin search
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

The bundle is built in two places, and they sign with the same key:

**On CI, from a push to `main`.** The `build` workflow's `signed-bundle` job
takes the upload key from the four repository secrets, runs the core tests
and the two content gates a runner can run, builds the bundle, refuses to go
on if `jarsigner` does not say `jar verified`, prints the signing
certificate's SHA-256, and uploads the bundle to the run's own artifacts:

```bash
gh run list --workflow=build --limit 1            # find the newest run
gh run download <run-id> -n quran-signed-aab -D play-store/aab
```

The artifact is private: only someone signed in to this repository can
download it, and GitHub deletes it after two weeks. Nothing signed is ever
posted to a public Release, so a stale signed build cannot sit around
waiting to be uploaded by mistake.

**On the owner's machine, from the vault.** Gradle probes each drive and each
layout the `Google Play Signing Key` folder has worn. When it is absent (a
fresh clone) the build degrades to unsigned rather than failing, which is
why the signature is proved in the next step rather than assumed. A build can
point at another properties file with `-Pquran.keystore=<file>`, which is how
CI signs from secrets.

The release bundle carries **only the core pack**: the Quran text and its
page layout. Translations, tafsirs, word lists, and reciters are downloaded
from the project's own Releases when a reader asks for them.

Expected sizes: bundle about 147 MB, APK about 144 MB, of which the 604 Mushaf
page fonts are the bulk, because the app must render the Book with no network.

## 3b. Prove the signature, whichever way it was built

`jarsigner -verify` exits 0 on a file that is not signed at all, and says so
only in its words. That is why the command that decides is the words:

```bash
report="$(jarsigner -verify app/build/outputs/bundle/release/app-release.aab 2>&1)"
printf '%s\n' "$report" | tail -2
printf '%s' "$report" | grep -q "jar verified" || { echo "refusing: unsigned"; exit 1; }
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab | grep -A2 SHA256:
```

The certificate must be
`53:7D:09:D2:03:00:12:9E:97:3B:79:45:31:6B:FE:24:CF:AD:CF:BC:77:EE:C5:22:9C:BF:30:17:0D:9D:E5:21`
(the shared upload key, D-017). The same check runs inside the CI job, so a
run that uploaded an artifact has already passed it.

## 3c. The secrets CI signs with

The four repository secrets, in Settings, Secrets and variables, Actions.
They are write-only: GitHub never shows a saved value again, which is why
they live in the vault first and are copied from there.

| Secret | Where its value comes from |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 signing.keystore` from the vault folder |
| `KEYSTORE_PASSWORD` | `storePassword` in the vault's `keystore.properties` |
| `KEY_ALIAS` | `keyAlias` in the same file |
| `KEY_PASSWORD` | `keyPassword` in the same file |

A repository being public does not expose a secret: the value is never
readable, only usable by a workflow in this repository. Signing runs only on
a push to `main` and never on a pull request, so a fork can never reach the
key, and the job's actions are pinned by commit rather than by tag.

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
