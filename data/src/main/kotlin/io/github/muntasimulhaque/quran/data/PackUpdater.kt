package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

/**
 * Keeps the packs a reader already has current, without a tap.
 *
 * An app update can carry a newer catalog than the files already on the
 * device: a corrected tafsir, a revised translation. The reader chose those
 * packs and already trusts the project's own release page, so the app quietly
 * replaces a stale copy with the catalog's current one. It is the same host,
 * the same whole-file SHA-256 check, and the same private folder as a
 * reader-initiated download; nothing else is ever contacted.
 *
 * Two guards keep it kind to the reader. It runs only on an unmetered
 * connection, so a reader on cellular never pays for a refresh in the
 * background; `ACCESS_NETWORK_STATE` is already in the merged manifest
 * through Media3, so this adds no permission. And it checks a catalog once,
 * not on every launch: the catalog's own fingerprints are remembered, so a
 * reader who is offline or metered pays nothing and the check is simply
 * retried on a later launch. The check runs when the app opens, against the
 * catalog this build carries: the catalog is a file inside the app itself, so
 * it cannot name anything new between two launches of the same build, and a
 * pass that could not finish is simply retried at the next open, because the
 * fingerprint is remembered only when every stale pack caught up. There is no
 * manual check, and none could find anything the open had not already named.
 *
 * The refresh is the reader's own word, given when they chose the pack
 * (owner decision, 21.5). It touches only packs already on this device, so a
 * reader who never added a pack never pays a byte for it, and it never runs
 * on a metered connection: automatic never means on any connection. A newer
 * version of a pack is named by the catalog that ships inside the app, so it
 * can only arrive with a new release of the app, and the reader's own update
 * is what offers it. A newer version is not baked into the app package: that
 * would make every reader carry bytes for content they never asked for.
 *
 * A stale pack keeps working while the refresh is in flight. Nothing here
 * runs on the main thread, a replaced file is swapped by rename, and a mutex
 * keeps two passes from ever downloading the same pack at once.
 */
class PackUpdater(private val context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences("pack-refresh", Context.MODE_PRIVATE)

    private val mutex = Mutex()

    /** True when a refresh would run now: a connected, unmetered network. */
    fun canRefresh(): Boolean = unmetered()

    /**
     * Replaces every installed pack whose bytes are behind the catalog, and
     * returns the ids that changed. A no-op when the connection is metered or
     * absent, or when the catalog was already checked.
     * Never throws: a failed download is left for the next attempt.
     */
    suspend fun refresh(catalog: PackCatalog): List<String> =
        mutex.withLock {
            if (!unmetered()) return@withLock emptyList()
            val fingerprint = fingerprint(catalog)
            if (preferences.getString(KEY, null) == fingerprint) {
                return@withLock emptyList()
            }
            val stale = runCatching { PackVerifier(context).needsRefresh(catalog.all()) }
                .getOrNull() ?: return@withLock emptyList()
            if (stale.isEmpty()) {
                preferences.edit().putString(KEY, fingerprint).apply()
                return@withLock emptyList()
            }
            val downloader = PackDownloader(context)
            val replaced = ArrayList<String>(stale.size)
            for (pack in stale) {
                val result = runCatching { downloader.download(pack) { _, _ -> } }.getOrNull()
                if (result?.isSuccess == true) replaced += pack.id
            }
            // The fingerprint is remembered only when every stale pack caught
            // up, so a dropped connection is retried on the next attempt
            // rather than being marked done.
            if (replaced.size == stale.size) {
                preferences.edit().putString(KEY, fingerprint).apply()
            }
            replaced
        }

    /** True only on a connected, unmetered network. */
    private fun unmetered(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val active = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(active) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /**
     * The catalog's expected content, one entry per pack: the ids and the
     * hashes they must have. A catalog whose hashes are unchanged is not
     * re-examined, which is what keeps the check off every launch.
     */
    private fun fingerprint(catalog: PackCatalog): String {
        val digest = MessageDigest.getInstance("SHA-256")
        for (pack in catalog.all().sortedBy { it.id }) {
            digest.update(pack.id.toByteArray(Charsets.UTF_8))
            digest.update(' '.code.toByte())
            digest.update(pack.sha256.toByteArray(Charsets.UTF_8))
            digest.update('\n'.code.toByte())
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val KEY = "catalog"
    }
}
