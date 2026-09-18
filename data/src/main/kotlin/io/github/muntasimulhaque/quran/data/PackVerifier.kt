package io.github.muntasimulhaque.quran.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Checks the packs on the device against the hashes the catalog carries.
 *
 * Every download is verified once, when it arrives, and this is the second
 * look: a file that a storage problem, a half-restore, or an interrupted
 * copy has damaged is found here, named, and offered for removal instead of
 * surfacing later as a query that returns nothing.
 *
 * The shipped core pack is not hashed on every check: it comes from the app's
 * own signed assets and is re-copied whenever its length does not match.
 */
class PackVerifier(private val context: Context) {

    /** The names of the installed packs whose bytes no longer match the catalog. */
    suspend fun damaged(packs: List<ContentPack>): List<String> = withContext(Dispatchers.IO) {
        packs.filter { it.installed && !it.shipped }
            .filter { !intact(it) }
            .map { it.name }
    }

    private fun intact(pack: ContentPack): Boolean {
        val file = PackStore(context).fileFor(pack.id)
        if (!file.isFile) return false
        if (pack.sha256.isEmpty()) return true
        if (pack.bytes > 0 && file.length() != pack.bytes) return false
        return sha256(file).equals(pack.sha256, ignoreCase = true)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
