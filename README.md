# Quran: The Noble Book

A free, open source Quran reader for Android. It opens where you left off
and keeps the Quran text itself at the center: no ads, no
trackers, no accounts, nothing collected, ever.

**Status:** 6.3 (versionCode 65) is submitted to the Play Console and waiting for review. The bundle is `play-store/aab/quran-6.3-vc65.aab` (11,668,093 bytes, SHA-256 `3549895ab20976a7088c9d0e44c2ea493c7e25c04fe80cde738c3209b4bd63b8`, `jar verified`, signed with the shared upload key, certificate `53:7D:09:D2:03:00:12:9E:97:3B:79:45:31:6B:FE:24:CF:AD:CF:BC:77:EE:C5:22:9C:BF:30:17:0D:9D:E5:21`), built by CI run 38263219144 on main from df083c1 and taken from that run's quran-signed-aab artifact. The gates ran green in that run, and the owner-machine content gates ran green here: verify, audit, fonts, search, and checkdb. The app suite ran whole on this box's emulator: the JVM suite, lint, and assemble green locally, and the full app instrumented suite green at 79 of 79, including the new JuzLabelScrollTest. The store set was captured again on all three legs from run 38263219160: all twenty-four frames are pixel-identical to the installed set, so the set stands unchanged. The hand-off copy is deleted now that Play has it.

**In 6.2.** The Arabic tafsir reads correctly again: commas and punctuation that showed as dotted marks are proper Arabic punctuation, in the Amiri face. The core text pack is smaller, and the leftover page machinery is gone.
