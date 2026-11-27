package io.github.muntasimulhaque.quran.benchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The numbers the app's own promises are judged by: how long a cold start
 * takes to reach readable text, and how smoothly the reading scrolls in each
 * of its two directions.
 *
 * Neither has a threshold here on purpose. A threshold on a shared machine is
 * a flake, and the reading is judged on a device: the numbers are read
 * against the previous run, the way the store set is compared frame by frame,
 * and a regression is a conversation rather than a red leg.
 *
 * Both scroll tests make the same gesture, one swipe of the same distance and
 * speed, and the same count of them; the only difference is which way the page
 * moves. Without that pair the app had no number for the upward scroll at
 * all, and the upward scroll is the one that is cold: a lazy list keeps only
 * the row ahead of the reader and drops the row behind them, so a backward
 * gesture rebuilds every block on the frame it re-enters.
 *
 * Each scroll test reads two things. The frame timeline is the reader's own
 * experience of a frame, and it is what a real phone is judged by. The
 * choreographer's frame is the main thread's share of that frame, composition,
 * measurement, layout, and the draw it records, without the GPU's time; on a
 * machine whose GPU is the bottleneck, and on an emulator it always is, the
 * frame timeline hides the app's own work behind the bus, and this is the
 * number that still answers whether the app did less. The two are read
 * together and neither is a threshold.
 *
 * A fresh install opens the one screen before the reading (the language, then
 * the reminder card), so the first launch onboards the app and every measured
 * launch after it opens the reading the reader left. The scroll test then
 * opens Al-Baqarah 2:255 through the app's own deep link, so it has a long
 * surah to move through rather than the seven ayat of Al-Fatiha.
 *
 * Run it from a machine with a device or an emulator attached:
 *
 *     ./gradlew :benchmark:connectedBenchmarkAndroidTest
 *
 * This module is development only. It never ships and nothing in the app
 * depends on it.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
// The choreographer's own frame section is read through the benchmark library's
// experimental metric. It is development only, it never reaches the bundle, and
// it is the one experimental call in this module.
@OptIn(ExperimentalMetricApi::class)
class ReadingBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 10,
        startupMode = StartupMode.COLD,
        setupBlock = { onboard() },
    ) {
        startActivityAndWait()
        // The first page is the promise: the clock stops when the reading is
        // on screen, not when the window is.
        device.wait(Until.hasObject(By.desc("Reading")), 10_000)
    }

    @Test
    fun readingScroll() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(
            FrameTimingMetric(),
            TraceSectionMetric("Choreographer#doFrame%", TraceSectionMetric.Mode.Average),
        ),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            onboard()
            openAyah()
        },
    ) {
        repeat(4) {
            device.swipe(500, 1500, 500, 500, 30)
            device.waitForIdle()
        }
    }

    /**
     * The same gesture as [readingScroll], pointing back up the page.
     *
     * The walk down the page is the setup and not the measurement: it leaves
     * the reader a page and a half into the surah, with blocks above the
     * glass whose copy the list is free to drop. The measured frames are the
     * return, so this number and [readingScroll] read as one measurement of
     * one gesture in two directions.
     */
    @Test
    fun readingScrollBack() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(
            FrameTimingMetric(),
            TraceSectionMetric("Choreographer#doFrame%", TraceSectionMetric.Mode.Average),
        ),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            onboard()
            openAyah()
            repeat(6) {
                device.swipe(500, 1500, 500, 500, 30)
                device.waitForIdle()
            }
        },
    ) {
        // Four swipes, the count the forward leg measures, so the two numbers
        // describe one gesture in two directions and nothing else.
        repeat(4) {
            device.swipe(500, 500, 500, 1500, 30)
            device.waitForIdle()
        }
    }

    /**
     * The reading is the only screen a first run opens on: the language
     * card this once answered is gone, so the setup only waits for the
     * page it measures.
     */
    private fun MacrobenchmarkScope.onboard() {
        startActivityAndWait()
        device.wait(Until.hasObject(By.desc("Reading")), 10_000)
        pressHome()
    }

    /** The long surah the scroll test moves through, by the reminder's own extra. */
    private fun MacrobenchmarkScope.openAyah() {
        startActivityAndWait(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(PACKAGE)
                putExtra("io.github.muntasimulhaque.quran.extra.AYAH", 262)
            },
        )
        device.wait(Until.hasObject(By.desc("Reading")), 10_000)
    }

    private companion object {
        const val PACKAGE = "io.github.muntasimulhaque.quran"
    }
}
