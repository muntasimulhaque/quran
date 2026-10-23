package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.TimeZone

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
 * retried on a later launch. A reader who asks for the check themselves can
 * [force] it, which the manual check uses to repair what it finds.
 *
 * A stale pack keeps working while the refresh is in flight. Nothing here
 * runs on the main thread, a replaced file is swapped by rename, and a mutex
 * keeps the quiet pass and the manual check from ever downloading the same
 * pack at once.
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
     * absent, or when the catalog was already checked and [force] is false.
     * Never throws: a failed download is left for the next attempt.
     */
    suspend fun refresh(catalog: PackCatalog, force: Boolean = false): List<String> =
        mutex.withLock {
            if (!unmetered()) return@withLock emptyList()
            val fingerprint = fingerprint(catalog)
            if (!force && preferences.getString(KEY, null) == fingerprint) {
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

    /**
     * A quiet integrity pass, at most once a day, that reads every installed
     * pack whose bytes have changed since the last pass and compares it with
     * the catalog's hash, repairing what a storage problem or an interrupted
     * copy has damaged. A pack whose size and moment are unchanged since it
     * was verified is not read again: the hash is the honest check, and this
     * keeps it off a reader's battery when nothing has moved (owner
     * decision, this session). [refresh] only
     * re-examines the catalog when its fingerprints change, which is an app
     * update; this is the second look that runs on the reader's behalf,
     * without a tap, so a reader never has to know the word for it (owner
     * decision, this session: the check a reader would not understand is gone
     * from the surface and kept here instead).
     *
     * It is kind to the reader the way [refresh] is: only on an unmetered
     * connection, only after the reading is already open, and the day it last
     * ran is remembered so the pass is once a day and not once a launch. A
     * reader who is offline or metered pays nothing and the pass is retried on
     * a later launch. A stale pack keeps working while a replacement is
     * fetched. Returns the ids that changed, so the caller can reopen the
     * library over them; a failure is left for the next day rather than shown.
     */
    suspend fun dailyIntegrityRefresh(catalog: PackCatalog): List<String> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (!unmetered()) return@withLock emptyList()
                val today = localDay(System.currentTimeMillis())
                if (preferences.getInt(KEY_INTEGRITY_DAY, -1) == today) {
                    return@withLock emptyList()
                }
                // The day is remembered whether or not work was found: the
                // SHA-256 read is the cost, and it is paid once a day rather
                // than held open until something is actually wrong.
                preferences.edit().putInt(KEY_INTEGRITY_DAY, today).apply()
                // The verifier reads a pack only when its bytes have changed
                // since the last pass that verified them. The hash is the
                // honest check, but it reads the whole pack, and the tafsir
                // is tens of megabytes: a file whose size and moment are
                // unchanged is the file that was verified, so the read is
                // skipped and the answer is the same (owner decision, this
                // session).
                val verifier = PackVerifier(context)
                val store = PackStore(context)
                val stale = runCatching {
                    catalog.all()
                        .filter { it.installed && !it.shipped }
                        .filter { pack -> !rememberedIntact(pack, verifier, store) }
                }.getOrNull() ?: return@withLock emptyList()
                if (stale.isEmpty()) return@withLock emptyList()
                val downloader = PackDownloader(context)
                val replaced = ArrayList<String>(stale.size)
                for (pack in stale) {
                    val result = runCatching { downloader.download(pack) { _, _ -> } }.getOrNull()
                    if (result?.isSuccess == true) replaced += pack.id
                }
                replaced
            }
        }

    /**
     * Whether one pack is intact, by its whole SHA-256. The old fast path
     * trusted a file whose size and modification time were unchanged; a bit
     * that flips without touching either would be trusted forever. The pass
     * now hashes every installed pack once a day on a worker, which is the
     * cost of the answer being true.
     */
    private fun rememberedIntact(
        pack: ContentPack,
        verifier: PackVerifier,
        store: PackStore,
    ): Boolean {
        val file = store.fileFor(pack.id)
        if (!file.isFile) return false
        if (pack.bytes > 0 && file.length() != pack.bytes) return false
        if (pack.sha256.isEmpty()) return true
        return verifier.intact(pack)
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

    /** The reader's own day number, so the integrity pass is once a day. */
    private fun localDay(millis: Long): Int =
        Math.floorDiv(
            millis + TimeZone.getDefault().getOffset(millis),
            DAY_MILLIS,
        ).toInt()

    private companion object {
        const val KEY = "catalog"
        const val KEY_INTEGRITY_DAY = "integrity_day"
        const val DAY_MILLIS = 86_400_000L
    }
}
