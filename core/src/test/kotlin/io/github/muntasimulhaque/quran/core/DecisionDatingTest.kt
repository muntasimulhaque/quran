package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The house rule that a settled decision is dated by its version, not by the
 * session that made it.
 *
 * A session name is a fact about the person who wrote the line and nothing a
 * later reader can act on: once the session has ended, "this session" is a
 * word that points at nothing, and the fifth session named in a codebase is
 * not findable by anyone who was not there. A version is. `owner decision,
 * 19.6` is a release, and `git log` finds the commit that shipped it, the
 * notes that describe it, and the build that carries it.
 *
 * The rule had been written down and then broken in a hundred and thirty
 * places, so it is a gate now. It reads the source files and the rules file,
 * never a release note: a note may quote the old wording while explaining
 * it, and a note that is history must stay as it was written. The file that
 * draws the line is skipped for the same reason, since its own prose has to
 * name the practice it forbids.
 *
 * The pattern is a tag whose own parenthetical reaches the word `session`
 * before its closing paren, and which carries no version on the way: a line
 * that already reads `owner decision, 19.6` and happens to mention a session
 * further along in the sentence is not an offender. The tag's own kind may
 * sit on the next line, which is how the editor wrapped a dozen of them.
 *
 * The attributes file is read too, because it carries decisions of its own.
 * The line that pins a checked-out file's bytes was added with a decision and
 * was still dated by its session when the sweep passed, since the sweep read
 * Kotlin and XML and this file is neither.
 *
 * An audit is the same fault wearing a more official word. Eight decisions
 * from the 11.7 hardening were dated "security audit", which names a
 * conversation and not a release, and a later reader cannot find the release
 * that made the call. They name 11.7 now, and the gate refuses the phrase the
 * way it refuses a session.
 */
class DecisionDatingTest {

    @Test
    fun `no settled decision is dated by a session or an audit`() {
        val root = repositoryRoot()
        val sessions = mutableListOf<String>()
        val audits = mutableListOf<String>()
        root.walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRECTORIES }
            .filter { it.isFile && it.name !in SKIPPED_FILES && isScanned(it) }
            .forEach { file ->
                val text = runCatching { file.readText() }.getOrNull() ?: return@forEach
                val path = file.relativeTo(root).path.replace('\\', '/')
                for (match in SESSION_TAG.findAll(text)) {
                    sessions += path + ":" + lineOf(text, match)
                }
                for (match in AUDIT_TAG.findAll(text)) {
                    audits += path + ":" + lineOf(text, match)
                }
            }
        assertTrue(
            "a decision is dated by a session instead of its version: " +
                sessions.joinToString(", "),
            sessions.isEmpty(),
        )
        assertTrue(
            "a decision is dated by an audit instead of its version: " +
                audits.joinToString(", "),
            audits.isEmpty(),
        )
    }

    /** The line a tag starts on, for a failure that can be opened. */
    private fun lineOf(text: String, match: MatchResult): Int =
        text.substring(0, match.range.first).count { it == '\n' } + 1

    /**
     * The source files the rule governs, and the two prose files whose tags a
     * later reader is owed a version for. A release note is not scanned.
     */
    private fun isScanned(file: File): Boolean =
        file.extension.lowercase() in SOURCE_EXTENSIONS ||
            file.name == "AGENTS.md" ||
            file.name == ".gitattributes"

    /** The module this test runs in sits one level under the repository root. */
    private fun repositoryRoot(): File {
        val working = File(System.getProperty("user.dir") ?: ".")
        val root = if (working.name == "core") working.parentFile ?: working else working
        assertTrue("the repository root was not found from $working", root.isDirectory)
        return root
    }

    private companion object {
        /**
         * A tag that names a session: `owner decision,` or `owner report,`
         * followed by a phrase with no version in it and then the word
         * `session`, all before the parenthetical closes. The `owner` and its
         * kind may sit on two lines, and the session word may sit on a third,
         * which is how most of the tags were written.
         */
        val SESSION_TAG = Regex(
            """owner\s+(?:\* |// )?\s*(decision|report),((?:(?!\d+\.\d)[^)]){0,90}?)session""",
        )

        /**
         * A tag that names an audit instead of a release: the same shape as
         * [SESSION_TAG], reaching `security audit` before the parenthetical
         * closes and carrying no version on the way. The eight that named
         * this one were the 11.7 hardening's, and a later reader owed the
         * release, not the meeting.
         */
        val AUDIT_TAG = Regex(
            """owner\s+(?:\* |// )?\s*(decision|report),((?:(?!\d+\.\d)[^)]){0,90}?)[Ss]ecurity audit""",
        )

        val SOURCE_EXTENSIONS = setOf("kt", "kts", "xml")

        val SKIPPED_FILES = setOf("DecisionDatingTest.kt")

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
