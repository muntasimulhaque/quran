# Privacy

Quran: The Noble Book collects nothing. There are no accounts, no ads, no
trackers, no analytics, and no crash reporting.

## What leaves the device

Almost nothing, and only to the project's own GitHub Releases page
(`github.com/muntasimulhaque/quran`). The app asks no other host; GitHub's
own download service may redirect the request to its release storage.
Every request carries nothing but the request itself: there is no account,
no identifier, and no usage data. GitHub, like every web server, sees the
connecting IP address; the app sends nothing else, and the project stores
nothing.

The app reads the network only for content from the project's own Releases,
and every download is verified against a SHA-256 recorded at build time:

- The reading of the language the reader chose: the translation arrives as
  part of that choice, and the tafsir follows on the same choice, waiting for
  an unmetered connection when the phone is on cellular. A word list arrives
  when the reader turns word by word on, where the row names its size first.
- A content pack the reader asks for in Settings, after the row names its
  size.
- A recitation package for one surah, after the reader taps Play, sees the
  surah's name and size, and taps Download; or after they have turned on
  Continue to the next surah in the player's own listening menu. That
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

- `INTERNET`: content from the project's own Releases, as described above.
- `POST_NOTIFICATIONS`: the media notification while recitation plays, and
  the daily ayah reminder.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: keeping
  recitation playing with the screen off.
- `SCHEDULE_EXACT_ALARM`: the daily ayah reminder, and nothing else. Before
  Android 12 exact alarms were never gated, so on those releases the app uses
  them and there is nothing for the reader to grant. From Android 12 the
  phone's special access decides: on many phones, and from Android 14 on most
  new installs, it starts out withheld, and the app asks for it in the
  phone's own screen at the two moments the reader sets the reminder
  (turning it on, or moving its time). The Daily page then says where it
  stands, with the one door the phone offers. Nothing about this permission
  leaves the phone: it is a switch for when this app's own reminder is due, and
  no other app's alarms are touched.
- `RECEIVE_BOOT_COMPLETED`: putting the daily reminder back after a restart,
  an app update, or a change of the phone's clock or time zone. It is used
  only to re-arm the reminder the reader already set.

The app declares those six permissions. The player's library also merges in
`ACCESS_NETWORK_STATE`, `WAKE_LOCK`, and
`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which the app does not declare.
`ACCESS_NETWORK_STATE` is what the quiet refresh reads to stay off metered
connections; `WAKE_LOCK` keeps the recitation alive with the screen off; the
receiver permission is the AndroidX library's own internal guard. None of
them collects anything.
