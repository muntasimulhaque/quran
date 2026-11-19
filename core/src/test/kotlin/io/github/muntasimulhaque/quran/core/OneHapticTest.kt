package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The app answers one touch with a buzz, and it is the one the reader asked a
 * question with.
 *
 * A word's sheet arrives under the finger on a long press, and that arrival is
 * the only touch the app does not draw: there is no sheet edge yet when the
 * buzz lands, so the hand is told the word took before the paper moves. Every
 * other touch is silent on purpose. The page is read for an hour, a button is
 * a door and not an event, and a buzz on each tap would be a buzz in the
 * reading, which is the noise this app is built not to make
 * (owner decision, 20.5).
 *
 * The rule was an asymmetry nothing could see: one haptic in a tree of
 * hundreds of controls, and no line saying whether that was the choice or the
 * leftovers of one. It is the choice, so it is a gate now, in the family of
 * `UnusedResourcesTest`: a second buzz anywhere in shipped code fails here
 * and the decision has to be made again out loud.
 */
class OneHapticTest {

    @Test
    fun `the app answers one touch with a buzz, and it is the word's long press`() {
        val root = repositoryRoot()
        val sites = mutableListOf<String>()
        root.walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRECTORIES }
            .filter { it.isFile && it.extension == "kt" && isShipped(it) }
            .forEach { file ->
                val text = runCatching { file.readText() }.getOrNull() ?: return@forEach
                val count = text.windowed(HAPTIC.length).count { it == HAPTIC }
                repeat(count) { sites += file.name }
            }
        assertEquals(
            "the app has a second haptic: " + sites.joinToString(", "),
            1,
            sites.size,
        )
        assertTrue(
            "the one haptic left the word's long press: " + sites.first(),
            sites.first() == "AyahBlock.kt",
        )
    }

    /** True for a shipped Kotlin source, never for a test APK's own. */
    private fun isShipped(file: File): Boolean {
        val path = file.path.replace('\\', '/')
        return !path.contains("/src/test/") && !path.contains("/src/androidTest/")
    }

    /** The module this test runs in sits one level under the repository root. */
    private fun repositoryRoot(): File {
        val working = File(System.getProperty("user.dir") ?: ".")
        val root = if (working.name == "core") working.parentFile ?: working else working
        assertTrue("the repository root was not found from $working", root.isDirectory)
        return root
    }

    private companion object {
        const val HAPTIC = "performHapticFeedback("

        val SKIPPED_DIRECTORIES = setOf(
            ".git",
            ".gradle",
            ".kotlin",
            "build",
            "node_modules",
            "raw",
            "work",
            "packs",
            "screenshots",
        )
    }
}
