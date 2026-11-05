package io.github.muntasimulhaque.quran.tools

import java.io.File
import java.io.IOException

/**
 * The one gate between a name in a manifest or a ZIP and a file on disk.
 *
 * The content tools run on the owner's machine and in CI, where a write
 * outside `content/work` is not a bad download but a changed build. A name
 * that leaves its root is refused rather than repaired (owner decision,
 * security audit).
 */
object SafeFiles {

    private val AUDIO = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}/[0-9]{6}\\.mp3")
    private val SEGMENT = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")

    fun resolveUnder(root: File, relative: String): File? {
        if (relative.isBlank() || relative.startsWith("/") || relative.startsWith("\\") ||
            relative.contains('\\') || relative.contains(':') || relative.contains('\u0000')
        ) {
            return null
        }
        if (relative.split('/').any { it.isEmpty() || it == "." || it == ".." }) return null
        return runCatching { File(root, relative).canonicalFile }
            .getOrNull()
            ?.takeIf { under(root, it) }
    }

    fun childUnder(base: File, name: String): File? {
        if (!SEGMENT.matches(name)) return null
        return runCatching { File(base, name).canonicalFile }
            .getOrNull()
            ?.takeIf { under(base, it) }
    }

    fun audioFile(base: File, path: String): File? {
        if (!AUDIO.matches(path) || path.split('/').any { it == "." || it == ".." || it.contains("..") }) {
            return null
        }
        return runCatching { File(base, path).canonicalFile }
            .getOrNull()
            ?.takeIf { under(base, it) }
    }

    fun under(base: File, child: File): Boolean = try {
        val root = base.canonicalPath
        val target = child.canonicalPath
        target == root || target.startsWith(root + File.separator)
    } catch (_: IOException) {
        false
    }
}
