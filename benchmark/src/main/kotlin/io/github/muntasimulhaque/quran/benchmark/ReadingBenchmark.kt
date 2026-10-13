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
            setWordByWord(false)
            openAyah()
        },
    ) {
        repeat(4) {
            device.swipe(500, 1500, 500, 500, 30)
            device.waitForIdle()
        }
    }

    /**
     * The same scroll with the word by word aid on, which is the heaviest
     * reading the app draws: every word is a pair of text nodes under the
     * Arabic, and the frame timing of this reading is the one the smooth
     * scroll promise is really about. The plain leg above turns the aid back
     * off in its own setup, so neither leg can silently measure the other's
     * reading (owner decision, this session).
     */
    @Test
    fun readingScrollWordByWord() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            onboard()
            setWordByWord(true)
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

    /**
     * Turns the reading's word meanings on or off through the app's own
     * settings, so each leg measures the reading it names. The word list is
     * in the debug build's own assets, so no network is involved, and the
     * switch is read back before it is left: a leg that failed to set its
     * state fails here rather than reporting a number under the wrong name.
     */
    private fun MacrobenchmarkScope.setWordByWord(enabled: Boolean) {
        startActivityAndWait()
        // The reading's quiet margin brings the chrome; its centre may hold
        // an ayah under the finger.
        device.click(6, device.displayHeight / 2)
        val settings = device.wait(Until.findObject(By.desc("Settings")), 10_000)
            ?: error("the chrome did not come up to open Settings")
        settings.click()
        val label = device.wait(Until.findObject(By.text("Show word meanings")), 10_000)
            ?: error("the word meanings row was not found")
        // The label is the row's own text; the toggleable node is its parent,
        // and that is where the state is read.
        val on = runCatching { label.parent?.isChecked }.getOrNull() == true
        if (on != enabled) label.click()
        device.pressBack()
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE = "io.github.muntasimulhaque.quran"
    }
}
