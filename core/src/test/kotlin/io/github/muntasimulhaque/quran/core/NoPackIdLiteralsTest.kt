package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A pack id is content, and it is written in one layer only: the content
 * vocabulary in `data` (the core constant, the convention builders, the wire
 * names of a type) and the tools registry that builds the catalog. Every
 * other shipping file reads a pack by its type and language through the
 * catalog, so a translation, a tafsir, a reciter, or a word list is added,
 * replaced, or retired without a line of app code. This gate is what keeps
 * that true once this session's hand-written tables are gone
 * (owner decision, 20.2).
 *
 * A test tag is not a pack: `tafsir-panel` and `words-audio-line` name a
 * composable on the glass, so the line that carries one is exempt. A tag is
 * UI vocabulary, and renaming one to satisfy this gate would be the tail
 * wagging the dog.
 */
class NoPackIdLiteralsTest {

    @Test
    fun noPackIdIsWrittenOutsideTheContentVocabulary() {
        val root = repositoryRoot()
        val offenders = mutableListOf<String>()
        for (module in SHIPPING_MODULES) {
            val source = File(root, "$module/src/main")
            assertTrue(
                "$module/src/main was not found under $root, so this gate would scan nothing",
                source.isDirectory,
            )
            source.walkTopDown()
                .onEnter { directory -> directory.name != "build" }
                .filter { file -> file.isFile && file.extension == "kt" }
                .forEach { file ->
                    val path = file.relativeTo(root).invariantSeparatorsPath
                    if (path in VOCABULARY) return@forEach
                    file.readLines().forEachIndexed { index, line ->
                        if (line.contains("testTag(")) return@forEachIndexed
                        val hit = PACK_ID.find(line) ?: return@forEachIndexed
                        offenders += "$path:${index + 1}: ${hit.value}"
                    }
                }
        }
        assertTrue(
            "a pack id is written outside the content vocabulary in: " +
                offenders.joinToString(", "),
            offenders.isEmpty(),
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
        /**
         * The shapes an id takes: one pack is named `core`, and the rest are
         * a type's word, a dash, and the thing it speaks for. The type's own
         * wire name (`word-audio`) is not here, because a bare type is
         * vocabulary and not an id.
         */
        val PACK_ID = Regex(
            "\"(core|translation-[a-z0-9_-]+|tafsir-[a-z0-9_-]+|reciter-[a-z0-9_-]+|" +
                "words-[a-z0-9_-]+|word-root|word-pronunciation-[a-z0-9_-]+)\"",
        )

        /** The modules whose `main` sources ship in the app. */
        val SHIPPING_MODULES = listOf(
            "app",
            "core",
            "data",
            "ui-kit",
            "feature-reading",
            "feature-search",
            "feature-browse",
            "feature-playback",
            "feature-settings",
            "content-assets",
        )

        /**
         * The content vocabulary: the three files that spell what an id is.
         * The core pack's id and the app's own preferences key, the reciter
         * and word-list conventions with the two literal ids they name, and
         * the wire names a pack type round-trips through. Anywhere else, a
         * pack is found by its type and language, never by a written id.
         */
        val VOCABULARY = setOf(
            "data/src/main/kotlin/io/github/muntasimulhaque/quran/data/PackStore.kt",
            "data/src/main/kotlin/io/github/muntasimulhaque/quran/data/ContentDatabase.kt",
            "data/src/main/kotlin/io/github/muntasimulhaque/quran/data/Models.kt",
        )
    }
}
