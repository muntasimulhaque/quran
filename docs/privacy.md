# Privacy

Quran: The Noble Book collects nothing. There are no accounts, no ads, no
trackers, no analytics, and no crash reporting.

## What leaves the device

Almost nothing, and only to the project's own GitHub Releases page
(`github.com/muntasimulhaque/quran`). No other host is ever contacted.
Every request carries nothing but the request itself: there is no account,
no identifier, and no usage data. GitHub, like every web server, sees the
connecting IP address; the app sends nothing else, and the project stores
nothing.

Three reads touch the network, all verified against a SHA-256 recorded at
build time:

- A content pack the reader asks for (a translation, a tafsir, or a word
  list), after they have seen its size and tapped Add.
- A recitation package for one surah, after the reader taps Play, sees the
  surah's name and size, and taps Download; or after they have turned on
  Continue to the next surah in Settings or on the player itself. That
  switch is their word, given once, for the packages that follow the surah
  being heard, and the player still shows each package's name and size while
  it downloads, with a cancel.
- A quiet refresh of a pack the reader already has, when the app itself has
  carried a newer version of it (a corrected tafsir, say). The refresh runs
  only on an unmetered connection and only for installed packs, so a reader
  on cellular data never pays for it; it waits until the first page is
  readable and never blocks the reading. A pack that fails its checksum is
  discarded and retried later.

Nothing else is fetched at launch, and the app works fully offline with no
connection at all.

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

Nothing else is declared. The player's library also merges in
`ACCESS_NETWORK_STATE` and `WAKE_LOCK`, which the app does not declare.
`ACCESS_NETWORK_STATE` is what the quiet refresh reads to stay off metered
connections; it collects nothing.
