package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.Locale

/**
 * Finds the audio for one ayah without ever touching the network.
 *
 * Two places are searched, in order: files the reader imported into the app's
 * own storage, then assets. Assets cover the development sample in debug
 * builds and, if the owner chooses it, an install-time Play asset pack later.
 */
class RecitationStore(private val context: Context) {

    private val imported = File(context.filesDir, "recitations")
    private val assetPrefixes = listOf("recitation/", "audio-dev/")
    private val assetCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    private val assetListCache = java.util.concurrent.ConcurrentHashMap<String, Set<String>>()

    /** The playable location of one ayah's audio, or null when it is absent. */
    fun uri(audioPath: String): Uri? {
        val file = SafePaths.audioFile(imported, audioPath) ?: return null
        if (file.isFile) return Uri.fromFile(file)
        for (prefix in assetPrefixes) {
            val path = prefix + audioPath
            if (assetExists(path)) return Uri.parse("asset:///$path")
        }
        return null
    }

    /**
     * Whether every path in one surah has a playable file, downloaded or
     * bundled.
     *
     * The whole surah is the unit: a package installs all of its ayahs and a
     * bundled surah is whole, so a partial set is a missing download, not a
     * surah to play in pieces. The check reads the imported folder only when
     * all of it is there, and otherwise reads each asset folder's name list
     * once, so a surah the app does not carry costs no exception per ayah.
     * Both `file://` and `asset:///` are covered, the two places [uri] reads
     * (owner decision, this session).
     */
    fun hasAllAudio(paths: List<String>): Boolean {
        if (paths.isEmpty()) return false
        val folder = SafePaths.folderOf(paths.first()) ?: return false
        if (paths.any { SafePaths.folderOf(it) != folder }) return false
        if (paths.all { SafePaths.audioFile(imported, it)?.isFile == true }) return true
        val names = paths.map { it.substringAfterLast('/') }
        return assetPrefixes.any { prefix ->
            val listing = assetNames(prefix + folder)
            names.all { it in listing }
        }
    }

    /**
     * The ayah files of one surah for one reciter folder, for sizes and removal.
     *
     * A surah downloaded in 15.6 is Ogg Opus. This build plays the original
     * MP3 set, so those files are removed as soon as the folder is read. The
     * package installs a whole surah folder, so a fresh download replaces them
     * anyway; the sweep is for a reader who updates and does not tap Play for a
     * while (owner decision, the recitation restore).
     */
    fun filesForSurah(folder: String, surah: Int): List<File> {
        if (surah !in 1..114) return emptyList()
        val directory = SafePaths.childUnder(imported, folder) ?: return emptyList()
        // The prefix is a file name, never a number the reader reads, so it
        // is written with the root locale: the process default follows the
        // reader's language, and a Bangla default wrote the prefix in Bangla
        // digits (০০১), which matched none of the ASCII names the packages
        // hold. A reader who downloaded a surah in one language then met a
        // download offer for it in the other (owner report, this session).
        val prefix = String.format(Locale.ROOT, "%03d", surah)
        val files = directory.listFiles { file ->
            file.isFile && file.name.startsWith(prefix) && file.name.endsWith(AYAH_SUFFIX)
        }?.toList().orEmpty()
        directory.listFiles { file ->
            file.isFile && file.name.startsWith(prefix) && file.name.endsWith(LEGACY_SUFFIX)
        }?.forEach { it.delete() }
        return files
    }

    fun bytesForSurah(folder: String, surah: Int): Long =
        filesForSurah(folder, surah).sumOf { it.length() }

    /**
     * The bytes of one surah's audio when the app carries it in its own
     * assets, or zero when it does not.
     *
     * The paths come from the recitation pack, exactly as [hasAllAudio] reads
     * them, so a surah is measured only after every one of its ayahs has been
     * found in the same asset folder. That keeps the settings page's list and
     * the player's whole-surah rule on one answer: a partial surah is not a
     * surah on the device, and it gets no row and no size.
     *
     * An asset has no file length on disk that a reader can read without
     * opening it, so a stored file's descriptor length is the size and a
     * compressed file's available bytes are. The bundled set is the fourteen
     * Al-Fatiha files, so this runs once per reciter when the settings page
     * opens, not on the reading's path.
     */
    fun bundledBytes(paths: List<String>): Long {
        if (paths.isEmpty()) return 0
        val folder = SafePaths.folderOf(paths.first()) ?: return 0
        if (paths.any { SafePaths.folderOf(it) != folder }) return 0
        val names = paths.map { it.substringAfterLast('/') }
        for (prefix in assetPrefixes) {
            val listing = assetNames(prefix + folder)
            if (!names.all { it in listing }) continue
            return names.sumOf { name -> assetBytes("$prefix$folder/$name") }
        }
        return 0
    }

    /** One bundled file's size, from its descriptor or its stream. */
    private fun assetBytes(path: String): Long {
        val declared = runCatching {
            context.assets.openFd(path).use { it.length }
        }.getOrDefault(-1L)
        if (declared >= 0) return declared
        return runCatching {
            context.assets.open(path).use { it.available().toLong() }
        }.getOrDefault(0L)
    }

    fun removeSurah(folder: String, surah: Int): Int {
        val files = filesForSurah(folder, surah)
        files.forEach { it.delete() }
        return files.size
    }

    /** Removes every downloaded surah of one reciter, for removing a reciter pack. */
    fun removeAll(folder: String): Int {
        val directory = SafePaths.childUnder(imported, folder) ?: return 0
        val files = directory.listFiles()?.size ?: 0
        directory.deleteRecursively()
        return files
    }

    private fun assetExists(path: String): Boolean = assetCache.getOrPut(path) {
        runCatching { context.assets.open(path).close() }.isSuccess
    }

    /** The names directly under one asset folder, empty when it is absent. */
    private fun assetNames(path: String): Set<String> = assetListCache.getOrPut(path) {
        runCatching { context.assets.list(path)?.toSet() }.getOrNull().orEmpty()
    }

    private companion object {
        /** The one extension a recitation package carries. */
        const val AYAH_SUFFIX = ".mp3"

        /** The extension of a surah downloaded in 15.6. */
        const val LEGACY_SUFFIX = ".opus"
    }
}
