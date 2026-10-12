# Store kit

Version: 6.2 (versionCode 64)

## Release notes (6.2, 345 characters)

Tafsir Arabic now reads correctly. Commas and punctuation that showed as dotted marks are proper Arabic punctuation again. Underneath, the app is simpler: one reading, no leftover page machinery, and a smaller download for the core text. The reading, recitations, search, Browse, and daily ayah are as they were. No ads, no trackers, no account.

## Before each release

1. Raise `versionCode` by 1 and `versionName` by 0.1 in
   `app/build.gradle.kts` and update the version line at the top of this
   file, in the same commit that ends the session.
2. Run the owner-machine gates: `./gradlew :tools:run --args="verify"`,
   `audit`, `fonts`, `checkdb`, `search`, and the instrumented tests on an
   emulator.
3. Commit and push. The `build` workflow's `signed-bundle` job signs on every
   push to `main`, checks the content, and leaves the bundle in the run's own
   artifacts. Pull it down into `play-store/aab/`:

   ```bash
   gh run list --workflow=build --limit 1
   gh run download <run-id> -n quran-signed-aab -D play-store/aab
   ```

   The artifact is private and expires after two weeks, so nothing signed is
   ever public and nothing signed lingers.
4. Upload the bundle to the internal track, check the size report, then
   promote. Delete the copy from `play-store/aab/` once Play has it, so a
   stale bundle can never be uploaded twice.

The four secrets signing needs (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`) live in the repository's secrets, each value
from the upload key in the owner's vault. The workflow refuses to publish an
unsigned bundle: if the secrets are missing it fails instead of handing over
something Play would reject.

## Assets in this folder

| File | Size | Use |
|---|---|---|
| `icon-512.png` | 512 x 512 | store icon |
| `feature-graphic-1024x500.png` | 1024 x 500 | feature graphic |
| `screenshots/phone/*.png` | 1080 x 1920 | phone screenshots (8) |
| `screenshots/tablet7/*.png` | 800 x 1280 | 7 inch tablet screenshots (8) |
| `screenshots/tablet10/*.png` | 2560 x 1800 | 10 inch tablet screenshots (8) |



## What the app carries, and what a reader adds

The app ships the Quran text only: about eight
megabytes, a complete offline reading that needs no network and no account. Everything
else is added by the reader, from the project's own GitHub Releases, with the
size shown before a byte moves and a SHA-256 check before it is used:

| Language | Translation | Tafsir | Word by word |
|---|---|---|---|
| English | Saheeh International (2.2 MB) | Ibn Kathir (23 MB) | 4.6 MB |
| Bangla | Taisirul Quran (5.1 MB) | Ibn Kathir (47 MB) | 6.5 MB |

Recitations: Minshawi and Husary, one surah at a time (0.2 to 122 MB each),
plus 1.7 MB of timing data per reciter.

Content updates arrive on their own: when a pack you already have is
superseded, the app replaces it quietly on Wi-Fi, so the tafsir and
translation stay current without a tap.

## Store answers to have ready

* **Is the app free?** Yes, and open source (MIT).
* **Does it show ads?** No, and it has no analytics and no accounts.
* **Why does it need the internet permission?** To download a content pack
  or a surah's recitation the reader asks for, from the project's own
  releases, and to quietly refresh an installed content pack when the app
  carries a newer version, on Wi-Fi only. No other host is ever contacted.
* **Data safety**: no data collected, no data shared, nothing stored off the
  device. Notes and bookmarks stay in the app's private storage.
