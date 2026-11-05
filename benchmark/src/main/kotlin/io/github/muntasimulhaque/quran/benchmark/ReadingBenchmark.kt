package io.github.muntasimulhaque.quran.benchmark

import android.content.Intent
import android.net.Uri
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The two numbers the app's own promises are judged by: how long a cold start
 * takes to reach readable text, and how smoothly the reading scrolls.
 *
 * Neither has a threshold here on purpose. A threshold on a shared machine is
 * a flake, and the reading is judged on a device: the numbers are read
 * against the previous run, the way the store set is compared frame by frame,
 * and a regression is a conversation rather than a red leg.
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
        metrics = listOf(FrameTimingMetric()),
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
     * The one screen before the reading, answered once. It is inside the
     * setup, never the measurement: a first run that stopped there would
     * time a language card, which is not the app the reader opens every day.
     */
    private fun MacrobenchmarkScope.onboard() {
        startActivityAndWait()
        if (device.wait(Until.hasObject(By.desc("Reading")), 3_000)) {
            pressHome()
            return
        }
        device.wait(Until.findObject(By.text("English")), 5_000)?.click()
        device.wait(Until.findObject(By.text("Not now")), 10_000)?.click()
        device.wait(Until.hasObject(By.desc("Reading")), 10_000)
        pressHome()
    }

    /** The long surah the scroll test moves through, by the app's own link. */
    private fun MacrobenchmarkScope.openAyah() {
        startActivityAndWait(
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("quran://ayah/2:255")
                setPackage(PACKAGE)
            },
        )
        device.wait(Until.hasObject(By.desc("Reading")), 10_000)
    }

    private companion object {
        const val PACKAGE = "io.github.muntasimulhaque.quran"
    }
}
