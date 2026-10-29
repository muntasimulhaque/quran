package io.github.muntasimulhaque.quran

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/**
 * The directory the build collects a frame from.
 *
 * The runner hands a test an additional output directory as an instrumentation
 * argument, and the pipeline pulls the PNGs out of it; a plain run without one
 * falls back to the app's own external files. A frame written anywhere else is
 * a frame the pipeline silently drops, which is what the variant frames did
 * on their first run: they wrote to the fallback while the tour wrote to the
 * argument, and the leg came up two frames short. One function now, so a
 * second capture class cannot answer the question differently.
 */
internal fun screenshotOutputDirectory(): File {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val given = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
    return if (given.isNullOrBlank()) {
        File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots")
    } else {
        File(given)
    }
}
