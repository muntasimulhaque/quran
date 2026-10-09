package io.github.muntasimulhaque.quran.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The startup profile, walked once on a device and written into the app.
 *
 * A profile is a list of the code paths the reader walks on every launch, so
 * the runtime can compile them ahead of time instead of on first use.
 *
 * The app already ships a hand-written seed profile
 * (app/src/main/baseline-prof.txt, twenty-five rules naming the launch path by
 * hand). What this module is for is replacing that seed with a measured one: a
 * generated profile covers the code the reader actually walked, and the
 * startup benchmark is the only way to know what either one is worth.
 *
 * The journey is the launch and the reading, because that is what every launch
 * does: open the reader where it was left, and scroll the reading. The result is
 * written to app/src/main/baseline-prof.txt and committed, because a profile
 * that is not in the tree is a profile nobody ships.
 *
 * Generating it needs a connected device, so it runs by hand:
 *
 *     ./gradlew :benchmark:connectedBenchmarkAndroidTest
 *
 * and the profile the run prints is copied to
 * app/src/main/baseline-prof.txt. Android Gradle Plugin 9 no longer offers a
 * `generateReleaseBaselineProfile` task; the merge side is automatic, so once
 * the file is in app/src/main/ the release build packs it with no further
 * wiring. The merge tasks that do it are `mergeReleaseStartupProfile` and
 * `mergeReleaseArtProfile`.
 *
 * This module is development only. It is not in the bundle, and nothing in the
 * app depends on it. What adding it cost the bundle is the
 * `profileinstaller` library, measured at 1,886 bytes on the 3.0 release; a
 * generated profile is a few hundred kilobytes of compressed text, against a
 * 148 MB bundle.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.desc("Reading")), 10_000)
            // the reader's own work: read, then scroll and come back
            device.swipe(500, 1500, 500, 500, 20)
            device.waitForIdle()
        }
    }

    private companion object {
        const val PACKAGE = "io.github.muntasimulhaque.quran"
    }
}
