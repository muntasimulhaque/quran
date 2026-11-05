package io.github.muntasimulhaque.quran.data

import java.io.File
import java.io.IOException

/**
 * The one gate between names that arrive in content and names on the disk.
 *
 * A pack id, a reciter id, a reciter folder, and an audio path all travel
 * through the catalog, the recitation manifest, or a downloaded database.
 * None of them is a file name until it has passed this gate. A name that
 * does not is not repaired into a different name: it is refused, so a
 * content mistake can never become a write or a delete outside the folder
 * that content owns (owner decision, security audit).
 */
object SafePaths {

    /** A catalog pack id or a recitation id: lowercase, digits, hyphen. */
    private val ID = Regex("^[a-z0-9][a-z0-9-]{0,63}$")

    /** One reciter folder, as the QUL exports name it. */
    private val FOLDER = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")

    /** One flat ayah file under its reciter folder. */
    private val AUDIO = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}/[0-9]{6}\\.mp3$")

    /** A file name that may be used beside other files. */
    private val SEGMENT = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")

    /** A release tag or an asset name. */
    private val ASSET = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")

    /** A release asset that a recitation package may name. */
    private val ZIP = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}\\.zip$")

    private val HASH = Regex("^[0-9a-fA-F]{64}$")

    private val LANGUAGE = Regex("^[a-z]{2,8}$")

    fun validId(id: String): Boolean = ID.matches(id)

    fun validFolder(folder: String): Boolean =
        FOLDER.matches(folder) && !folder.contains("..")

    fun validAudioPath(path: String): Boolean =
        AUDIO.matches(path) &&
            path.split('/').all { it != "." && it != ".." && !it.contains("..") }

    fun validHash(hash: String): Boolean = HASH.matches(hash)

    fun validTag(tag: String): Boolean = ASSET.matches(tag) && !tag.contains("..")

    fun validZip(asset: String): Boolean = ZIP.matches(asset) && !asset.contains("..")

    fun validLanguage(language: String): Boolean = LANGUAGE.matches(language)

    /** The reciter folder an audio path belongs to, or null when it is not one. */
    fun folderOf(audioPath: String): String? =
        if (!validAudioPath(audioPath)) null else audioPath.substringBefore('/')

    /**
     * The file a relative audio path names under [base], or null when the
     * path is not a flat reciter path or it resolves outside [base].
     */
    fun audioFile(base: File, audioPath: String): File? {
        if (!validAudioPath(audioPath)) return null
        return runCatching { File(base, audioPath).canonicalFile }
            .getOrNull()
            ?.takeIf { under(base, it) }
    }

    /** One direct child of [base], or null when the name escapes it. */
    fun childUnder(base: File, name: String): File? {
        if (!SEGMENT.matches(name)) return null
        return runCatching { File(base, name).canonicalFile }
            .getOrNull()
            ?.takeIf { under(base, it) }
    }

    /**
     * The identifier a pack's schema is quoted under. A valid id is quoted;
     * an invalid one becomes a schema that is never attached, so a bad name
     * is a miss and not a way into the SQL text.
     */
    fun sqlIdentifier(id: String): String =
        if (validId(id)) "\"$id\"" else "\"__invalid_pack__\""

    private fun under(base: File, child: File): Boolean = try {
        val root = base.canonicalPath
        val target = child.canonicalPath
        target == root || target.startsWith(root + File.separator)
    } catch (_: IOException) {
        false
    }
}
