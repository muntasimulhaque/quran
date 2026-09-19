package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Two modules that declare the same string name, with different format
 * arguments, is a crash the compiler cannot see: resource merging keeps one
 * of the two, and `stringResource` then throws on the arguments it was never
 * given. That is exactly what happened to `pack_downloading` (the app's copy
 * took two arguments, the settings module's took one, and tapping Add on a
 * translation threw on the main thread).
 *
 * The merged table is not visible from a test, but the names are: every
 * module's `strings.xml` is parsed here and a name two modules both declare
 * fails the build. The one exception is a name in one module that forwards to
 * a string it does not own, which is not how this app writes resources.
 */
class StringNameTest {

    private val moduleDirectories = listOf(
        "app",
        "feature-browse",
        "feature-mushaf",
        "feature-playback",
        "feature-search",
        "feature-settings",
        "feature-study",
        "ui-kit",
    )

    @Test
    fun noStringNameIsDeclaredByTwoModules() {
        val owners = HashMap<String, MutableList<String>>()
        for (module in moduleDirectories) {
            val strings = File(repositoryRoot(), "$module/src/main/res/values/strings.xml")
            if (!strings.isFile) continue
            val text = strings.readText()
            for (name in NAME.findAll(text)) {
                owners.getOrPut(name.groupValues[1]) { mutableListOf() }.add(module)
            }
        }
        val clashes = owners.filterValues { it.size > 1 }
        assertTrue(
            "these string names are declared by more than one module, and the merged " +
                "resource will keep only one of them: " +
                clashes.entries.joinToString(", ") { "${it.key} in ${it.value.joinToString(" and ")}" },
            clashes.isEmpty(),
        )
    }

    /** The module this test runs in sits one level under the repository root. */
    private fun repositoryRoot(): File {
        val working = File(System.getProperty("user.dir") ?: ".")
        val root = if (working.name == "core") working.parentFile ?: working else working
        assertTrue("the repository root was not found from $working", root.isDirectory)
        return root
    }

    private companion object {
        val NAME = Regex("""<(?:string|plurals|string-array)\s+name="([^"]+)"""")
    }
}
