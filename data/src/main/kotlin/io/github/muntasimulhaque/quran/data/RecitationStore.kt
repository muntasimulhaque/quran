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

    /** True when the first ayah of a recitation can be played. */
    fun isAvailable(audioPath: String?): Boolean =
        !audioPath.isNullOrBlank() && uri(audioPath) != null

    /** Where an import should place its files, mirroring the audio paths. */
    fun importDirectory(): File = imported.apply { mkdirs() }

    private fun assetExists(path: String): Boolean = assetCache.getOrPut(path) {
        runCatching { context.assets.open(path).close() }.isSuccess
    }
}
