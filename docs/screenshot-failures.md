# Store screenshots: every way a leg can fail

This file exists because the same workflow kept going red for reasons that
were each diagnosed from scratch, one red run at a time. That is the wrong
shape. Every failure mode this workflow has ever shown is written here, with
the evidence, the class, and the fix, so a red leg is a lookup and never an
investigation. When a new mode appears, it is added here in the same session
that fixes it, before the next capture.

Read this with `gh run list --workflow=screenshots.yml` in front of you, and
with `gh run view --log --job <id>` for the log. **A red run is one of the
classes below, always.** If it is not, the class is missing from this file
and the file is the thing to fix.

## The five classes

| # | Class | Where it dies | What the log says | The fix |
|---|---|---|---|---|
| 1 | **Runner infrastructure** | before the capture script, in the emulator action's own setup | `Install Android SDK` fails, `Error on ZipFile unknown archive`, or the action's own `emu kill` answers `Connection refused` | Rerun the failed leg once. A second failure in the same pre-script step is a runner outage to report, not to debug. |
| 2 | **Device drop under load** | mid-run | `adb: device offline`, `device 'emulator-5554' not found` | The capture is too heavy for that profile. Make it lighter; never just rerun. |
| 3 | **The tour's anchors** | in the test | `ComposeTimeoutException: Condition still not satisfied`, `could not find any node that satisfies: (ContentDescription = ...)` | Real finding: the UI moved or a label changed under the tour. Fix the anchor to a tag, in the session. |
| 4 | **A window that is not ours** | in a sheet capture | `a system window (...) stayed over <frame>` | The system put something over the app. See "The window guard" below; the guard is now the fix and the message names the window. |
| 5 | **Our own script** | after Gradle, before the upload | no `Collected:` line, then `No files were found`, or `[command]/usr/bin/sh` on a line whose predecessor failed | A bug in `screenshots.yml`. See "The script bug that hid every red leg" below. |

Class 5 is the one that kept costing the most, because it destroys the
evidence for every other class.

## The script bug that hid every red leg

**What it looked like.** The 10 inch leg failed the tour, and the artifact
`store-screenshots-tablet10` did not exist at all. The runbook's promise is
that a red leg keeps its frames so the failure can be read. The promise was
false.

**Why.** The `reactivecircus/android-emulator-runner` action does not run the
`script:` block as one shell. It feeds each line to its own `/usr/bin/sh -c`.
The script was written as:

```
set +e
./gradlew :app:connectedDebugAndroidTest ...
gradle_status=$?
set -e
mkdir -p store-shots
find ... -exec cp {} store-shots/ ';'
...
exit $gradle_status
```

Each of those lines ran in a separate shell, so `set +e` never applied to the
Gradle line, `gradle_status=$?` read the exit status of nothing, and the
runner stopped the whole block at the moment Gradle returned 1. The `mkdir`,
the `cp`, and the `exit` never ran. The upload step then had an empty
`store-shots` and warned `No files were found with the provided path`.

**The fix, in the tree.** The exit status is saved from the Gradle line
itself, on the same line, so it does not need a shell variable to survive:

```
./gradlew ... ; echo $? > /tmp/gradle_status
```

and every collection line is guarded (`2>/dev/null || true`), so a missing
output directory cannot abort the script before the frames are gathered. The
final line still returns the saved Gradle status, so a red tour is still red
while its frames are safe in the artifact.

**How to see it is fixed.** A red leg's log contains `Collected:` and
`frames: N`, and its artifact exists. If a leg is red and its artifact is
missing, this file is out of date.

## The window guard

`captureScreen` keeps a frame only when the focused window belongs to the
app, and it dismisses what does not. The guard used to search the whole
`dumpsys window windows` output for `isn't responding` or `Application Not
Responding`. That is the wrong question: the dump lists windows that are
gone or suppressed as well as the one in front, so on the 10 inch profile a
stale launcher record made the search true forever. The tour then retried a
screen that had no dialog on it, eight times, and failed the leg.

The guard now reads `mCurrentFocus` / `mFocusedWindow` and takes the package
that owns it. If that package is not ours, the window is a real intruder and
its name goes into the failure message, so the next red leg says
`a system window (com.google.android.apps.nexuslauncher) stayed over
05-search` instead of hunting.

## Every mode, with its evidence

The list below is the full history of this workflow's reds, newest first.

**Run 35974207613 (1.9 work), tablet10, attempt 1 and attempt 2.**
`a system dialog stayed over 05-search` (class 4, the stale-record bug), and
the artifact was missing (class 5, the script bug). Both are fixed in the
tree as described above. This is the run that made this file necessary.

**Run 35596449680, all three legs.** `adb: device offline` then
`Failed to inject touch input ... could not find any node that satisfies:
(ContentDescription = 'Switch to the Mushaf page')` (classes 2 and 3). The
tour's first press looked for a mode door the app did not have up. Real
finding: the anchor was wrong. Fixed by revealing the chrome first.

**Run 35569779756, all three legs.** `adb: device offline` and
`ComposeTimeoutException: Condition still not satisfied after 15000 ms`
(classes 2 and 3). An anchor never appeared. Fixed in the tour.

**Run 35526311219 and 35524134742, phone.** `adb: device offline` (class 2,
the capture's load). Fixed by making the capture lighter: the lean
`-Pquran.devPacks=screenshot` build and the one-invocation test run.

**Run 35463781932, phone and tablet7.** `adb: device offline`, then no
artifact, `No files were found` (classes 2 and 5). The script bug.

**Run 35436773080, tablet7.** `adb: device offline` then
`ComposeTimeoutException` (classes 2 and 3).

**Run 35377607496, all three legs.** `adb: device offline`, no artifact
(classes 2 and 5).

**The 0.5-era failure that started the workflow hardening.** "Pixel Launcher
isn't responding" sat over every 10 inch frame while the leg was green. The
workflow now suppresses ANR dialogs at the device level (`hide_error_dialogs
1`, `anr_show_background 0`) and the tour checks the window list, so a
dialog is never silently shipped.

## The rules this file implies

1. **A red leg always leaves its frames.** If it does not, the script bug is
   back; check that the Gradle line still carries `; echo $? > /tmp/gradle_status`
   on the same line and that no collection line can fail hard.
2. **Never rerun class 3 or class 4.** A node assertion and a named intruder
   window are findings about the app or the tour. They are fixed in the
   session. Rerun only class 1 (once) and, with a lighter capture, class 2.
3. **A green leg is not proof of a good frame.** Every frame still gets
   `cmp`'d against its artifact and read for its content before it ships.
4. **A new failure mode is added here before the next capture**, not
   remembered in a session note.
