# Quran: The Noble Book

A free, open source Quran reader for Android. It opens where you left off
and keeps the Quran text itself at the center: no ads, no
trackers, no accounts, nothing collected, ever.

**Status:** 6.4 (versionCode 66) is ready to hand over to the Play Console. The bundle is `play-store/aab/quran-6.4-vc66.aab` (11,676,658 bytes, SHA-256 `59b32f90d3da9a1edcb3702cc0351845364c7a65c3086a4ae2e041e3892b7be9`, `jar verified`, signed with the shared upload key, certificate `53:7D:09:D2:03:00:12:9E:97:3B:79:45:31:6B:FE:24:CF:AD:CF:BC:77:EE:C5:22:9C:BF:30:17:0D:9D:E5:21`), built by CI run 38287741638 on main from 03eb25c and taken from that run's quran-signed-aab artifact. The gates ran green in that run, and the owner-machine content gates ran green here: verify, audit, fonts, search, build, packs, and checkdb. The app suite ran whole on this box's emulator: the JVM suite, lint, and assemble green locally, and the full app instrumented suite green at 79 of 79. The core pack carries a new index on the word table, so it was published to its own hash-addressed release and the catalog moved with it. The store set was captured again on all three legs from run 38287741629 and compared frame by frame: one frame of the twenty-four genuinely changed, the tablet settings sheet's About row, which now names 6.4 where the installed set still named 6.2. The rest differ only in the emulator's clock, the soft keyboard, and the text cursor, none of which the app draws, so the new set is installed whole.

**In 6.3.** The juz number beside the surah follows the reading, and a jump outlives the process: open an ayah from Browse, Search, or the daily reminder and the next launch returns to it.
