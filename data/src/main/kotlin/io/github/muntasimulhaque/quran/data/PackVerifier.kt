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
 * surfacing later as a query that returns nothing. A pack whose catalog
 * entry has no 64-hex SHA-256 is never intact: a missing checksum is a
 * refusal, not a permission (owner decision, security audit).
 *
 * The shipped core pack is not hashed here: it comes from the app's own
 * signed assets and [PackStore.coreFile] checks it on every open.
 */
class PackVerifier(private val context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences("pack-integrity", Context.MODE_PRIVATE)

    /** The installed packs whose bytes no longer match the catalog. */
    suspend fun needsRefresh(packs: List<ContentPack>): List<ContentPack> =
        withContext(Dispatchers.IO) {
            packs.filter { it.installed && !it.shipped }.filter { !intact(it) }
        }

    /**
     * Whether a pack may be attached to the reading.
     *
     * The first open after an install hashes the whole file once and keeps a
     * marker of its length, moment, and catalog hash. A later open whose
     * marker still matches does not read the file again; the daily integrity
     * pass hashes every pack and refreshes the marker, so a file that moved
     * without changing its size or moment is still caught once a day. A
     * missing or malformed hash is always refused.
     */
    fun trustedForOpen(pack: ContentPack): Boolean {
        if (!SafePaths.validId(pack.id) || !SafePaths.validHash(pack.sha256)) return false
        val file = PackStore(context).fileFor(pack.id)
        if (!file.isFile) return false
        if (pack.bytes > 0 && file.length() != pack.bytes) return false
        if (preferences.getString(pack.id, null) == fingerprint(pack, file)) return true
        return verify(pack, file)
    }

    /**
     * Whether one pack's bytes match the catalog: its length when the
     * catalog carries one, and its whole SHA-256. It is the honest check and
     * it is the expensive one, so [PackUpdater.dailyIntegrityRefresh] asks
     * it once a day.
     */
    fun intact(pack: ContentPack): Boolean {
        if (!SafePaths.validId(pack.id) || !SafePaths.validHash(pack.sha256)) return false
        val file = PackStore(context).fileFor(pack.id)
        if (!file.isFile) return false
        if (pack.bytes > 0 && file.length() != pack.bytes) return false
        return verify(pack, file)
    }

    /** Records a file that the caller has already hashed and verified. */
    fun remember(pack: ContentPack, file: File) {
        if (!SafePaths.validId(pack.id) || !SafePaths.validHash(pack.sha256) || !file.isFile) return
        preferences.edit().putString(pack.id, fingerprint(pack, file)).apply()
    }

    /** Drops the marker for a pack that was removed or replaced. */
    fun forget(id: String) {
        if (preferences.contains(id)) preferences.edit().remove(id).apply()
    }

    private fun verify(pack: ContentPack, file: File): Boolean {
        val matches = sha256(file).equals(pack.sha256, ignoreCase = true)
        if (matches) {
            preferences.edit().putString(pack.id, fingerprint(pack, file)).apply()
        } else {
            preferences.edit().remove(pack.id).apply()
        }
        return matches
    }

    private fun fingerprint(pack: ContentPack, file: File): String =
        "${file.length()}:${file.lastModified()}:${pack.sha256}"

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
