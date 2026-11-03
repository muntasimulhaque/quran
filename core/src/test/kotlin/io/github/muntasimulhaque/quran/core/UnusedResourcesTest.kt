package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The app's set is only a set while every piece of it is drawn somewhere.
 *
 * Two marks and one sentence had outlived their surfaces: a "more" glyph and
 * a clock that no screen asked for, and a playback speed line nothing drew.
 * Each was still in the enum, still tested, still explained by a comment, so
 * nothing failed and nothing was noticed. A mark with no surface is not
 * neutral: it tells the next reader the app has a menu it does not have.
 *
 * Both rules are read off the source, so they hold for a mark or a sentence
 * added anywhere, in any module.
 */
class UnusedResourcesTest {

    @Test
    fun everyIconIsDrawnByASurface() {
        val root = repositoryRoot()
        val glyphs = File(root, UI_KIT_GLYPHS)
        val declared = readText(glyphs).substringAfter("enum class Icon {")
            .substringBefore("\n}")
            .lineSequence()
            .map { it.trim() }
            .mapNotNull { entry -> entry.takeIf { it.matches(ENTRY) }?.removeSuffix(",") }
            .map { entry -> entry.substringBefore('(').trim().substringBefore(' ') }
            .filter { it.isNotEmpty() && it !in NON_MARK_ENTRIES }
            .toList()
        assertTrue("no icon was read at all", declared.size > 10)

        val callers = ktFiles(root)
            .filterNot { it.path.contains("/ui/glyph/") }
            .joinToString("\n") { readText(it) }
        val unused = declared.filterNot { callers.contains("Icon.$it") }
        assertTrue(
            "an icon has no surface: " + unused.joinToString(", "),
            unused.isEmpty(),
        )
    }

    @Test
    fun everyStringIsDrawnSomewhere() {
        val root = repositoryRoot()
        val resources = File(root, "")
            .walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRECTORIES }
            .filter { it.isFile && it.name == "strings.xml" && it.path.contains("src${File.separator}main") }
            .toList()
        assertTrue("no strings.xml was found", resources.size > 5)

        val drawnInKotlin = ktFiles(root).joinToString("\n") { readText(it) }
        val drawnInXml = xmlFiles(root)
            .filterNot { it.name == "strings.xml" }
            .joinToString("\n") { readText(it) }

        val unused = mutableListOf<String>()
        for (file in resources) {
            val names = STRING.findValues(readText(file))
            for (name in names) {
                val inKotlin = drawnInKotlin.contains("R.string.$name")
                val inXml = drawnInXml.contains("@string/$name")
                if (!inKotlin && !inXml) unused += "${file.parentFile.name}/$name"
            }
        }
        assertTrue(
            "a string has no surface: " + unused.distinct().sorted().joinToString(", "),
            unused.isEmpty(),
        )
    }

    private fun ktFiles(root: File): List<File> = root.walkTopDown()
        .onEnter { it.name !in SKIPPED_DIRECTORIES }
        .filter { it.isFile && it.extension == "kt" && it.path.contains("src${File.separator}main") }
        .toList()

    private fun xmlFiles(root: File): List<File> = root.walkTopDown()
        .onEnter { it.name !in SKIPPED_DIRECTORIES }
        .filter { it.isFile && it.extension == "xml" && it.path.contains("src${File.separator}main") }
        .toList()

    private fun readText(file: File): String =
        runCatching { file.readText() }.getOrDefault("")

    /** The module this test runs in sits one level under the repository root. */
    private fun repositoryRoot(): File {
        val working = File(System.getProperty("user.dir") ?: ".")
        val root = if (working.name == "core") working.parentFile ?: working else working
        assertTrue("the repository root was not found from $working", root.isDirectory)
        return root
    }

    private companion object {
        const val UI_KIT_GLYPHS = "ui-kit/src/main/kotlin/io/github/muntasimulhaque/quran/ui/glyph/Icon.kt"

        /** One enum entry: a name, a comma, and nothing else on the line. */
        val ENTRY = Regex("^[A-Z][A-Za-z0-9]*,$")

        /** The enum's own head and its doc lines are not marks. */
        val NON_MARK_ENTRIES = setOf("Icon", "String")

        val STRING = Regex("""<string name="([^"]+)"""")

        val SKIPPED_DIRECTORIES = setOf(
            ".git",
            ".gradle",
            ".kotlin",
            "build",
            "node_modules",
            "content",
            "docs",
        )
    }
}

private fun Regex.findValues(text: String): List<String> =
    findAll(text).mapNotNull { it.groupValues.getOrNull(1) }.toList()
