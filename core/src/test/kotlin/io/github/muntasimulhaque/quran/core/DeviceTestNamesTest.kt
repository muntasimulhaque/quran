package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The house rule that a device test's name is an identifier.
 *
 * A JVM suite may name a test as a sentence with spaces, because a JVM report
 * has room for one and the sentence is what the reader of a failure needs. The
 * Android runtime cannot hold a space in a method name, and an instrumented
 * test runs on Android: a backticked name there compiles, installs, and then
 * the runner cannot call it, which reads as a defect in the app rather than in
 * the test. The device suites kept the identifier by habit alone, so the rule
 * is a gate now (owner decision, 19.6).
 *
 * The JVM suites are not read here: both spellings are lawful there, chosen by
 * whether the sentence needs a comma or a possessive.
 */
class DeviceTestNamesTest {

    @Test
    fun noDeviceTestIsNamedWithABacktick() {
        val offenders = mutableListOf<String>()
        deviceTestFiles().forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                if (SPACED_NAME.containsMatchIn(line)) {
                    offenders += file.relativeTo(repositoryRoot()).path + ":" + (index + 1)
                }
            }
        }
        assertTrue(
            "a device test named with a backtick cannot be run on Android: " +
                offenders.joinToString(", "),
            offenders.isEmpty(),
        )
    }

    @Test
    fun theDeviceSuitesAreFound() {
        val files = deviceTestFiles()
        assertTrue(
            "no device test file was found under the repository root",
            files.size > 10,
        )
    }

    private fun deviceTestFiles(): List<File> =
        repositoryRoot().walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRECTORIES }
            .filter { it.isFile && it.extension == "kt" && isDeviceTest(it) }
            .toList()

    private fun isDeviceTest(file: File): Boolean {
        val path = file.path.replace('\\', '/')
        return path.contains("/src/androidTest/") && !path.contains("/build/")
    }

    /** The module this test runs in sits one level under the repository root. */
    private fun repositoryRoot(): File {
        val working = File(System.getProperty("user.dir") ?: ".")
        val root = if (working.name == "core") working.parentFile ?: working else working
        assertTrue("the repository root was not found from $working", root.isDirectory)
        return root
    }

    private companion object {
        /** A function whose name is written inside backticks. */
        val SPACED_NAME = Regex("\\bfun\\s+`")

        val SKIPPED_DIRECTORIES = setOf(".git", ".gradle", ".kotlin", "build", "node_modules")
    }
}
