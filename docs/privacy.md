# Privacy

Quran: The Noble Book collects nothing. There are no accounts, no ads, no
trackers, no analytics, and no crash reporting.

## What leaves the device

Almost nothing. The app has one network use, and it happens only when the
reader asks for it: downloading a recitation package for one surah from the
project's own GitHub Releases page (`github.com/muntasimulhaque/quran`).
That request carries nothing but the request itself. There is no account, no
identifier, and no usage data. GitHub, like every web server, sees the
connecting IP address; the app sends nothing else, and the project stores
nothing.

Nothing is downloaded at launch. Nothing is downloaded automatically. A
download starts only after the reader taps Play on a surah, sees the surah's
name and size, and taps Download. Each package is verified against a SHA-256
recorded at build time, and a package that fails the check is discarded.

## What stays on the device

- The reading position, the theme, the chosen reciter, bookmarks, and notes
  are stored in the app's private storage and never leave it.
- Downloaded recitation packages live in the app's private storage. Removing
  a surah from the Recitations sheet deletes its files.
- The app can be used fully offline: text, translation, tafsir, search,
  bookmarks, notes, and any surah already downloaded all work with no
  connection.

## Permissions

- `INTERNET`: the one use described above.
- `POST_NOTIFICATIONS`: the media notification while recitation plays.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: keeping
  recitation playing with the screen off.

Nothing else is declared.
