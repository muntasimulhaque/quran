package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.net.Uri
import java.io.File

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
    private val assetCache = HashMap<String, Boolean>()

    /** The playable location of one ayah's audio, or null when it is absent. */
    fun uri(audioPath: String): Uri? {
        val file = File(imported, audioPath)
        if (file.isFile) return Uri.fromFile(file)
        for (prefix in assetPrefixes) {
            val path = prefix + audioPath
            if (assetExists(path)) return Uri.parse("asset:///$path")
        }
        return null
    }

    /** The ayah files of one surah for one reciter folder, for sizes and removal. */
    fun filesForSurah(folder: String, surah: Int): List<File> {
        val directory = File(imported, folder)
        val prefix = "%03d".format(surah)
        return directory.listFiles { file ->
            file.isFile && file.name.startsWith(prefix) && file.name.endsWith(".mp3")
        }?.toList().orEmpty()
    }

    fun bytesForSurah(folder: String, surah: Int): Long =
        filesForSurah(folder, surah).sumOf { it.length() }

    fun removeSurah(folder: String, surah: Int): Int {
        val files = filesForSurah(folder, surah)
        files.forEach { it.delete() }
        return files.size
    }

    /** Removes every downloaded surah of one reciter, for removing a reciter pack. */
    fun removeAll(folder: String): Int {
        val directory = File(imported, folder)
        val files = directory.listFiles()?.size ?: 0
        directory.deleteRecursively()
        return files
    }

    private fun assetExists(path: String): Boolean = assetCache.getOrPut(path) {
        runCatching { context.assets.open(path).close() }.isSuccess
    }
}
