package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext

data class WordAudioPackage(
    val surah: Int,
    val url: String,
    val bytes: Long,
    val sha256: String,
    val words: Int,
)

/**
 * The published word pronunciation packages, one ZIP per surah.
 *
 * The app trusts this manifest and nothing else. A package is unpacked only
 * after its SHA-256 matches, and every file it carries is one drawn word's
 * own Opus clip. The manifest travels in the app's assets beside the catalog;
 * the audio itself is downloaded only when the reader has turned the word aid
 * on and asked for the surah.
 */
class WordAudioManifest private constructor(
    private val packages: Map<Int, WordAudioPackage>,
) {

    fun packageFor(surah: Int): WordAudioPackage? = packages[surah]

    fun surahs(): Set<Int> = packages.keys

    /** What the whole Book would weigh, for the page's one size line. */
    fun totalBytes(): Long = packages.values.sumOf { it.bytes }

    companion object {
        fun load(context: Context): WordAudioManifest {
            val json = runCatching {
                context.assets.open("word-audio/manifest.json").bufferedReader().use { it.readText() }
            }.getOrNull() ?: return WordAudioManifest(emptyMap())
            return parse(json)
        }

        fun parse(json: String): WordAudioManifest {
            val out = HashMap<Int, WordAudioPackage>()
            val root = runCatching { JSONObject(json) }.getOrNull()
                ?: return WordAudioManifest(emptyMap())
            val baseUrl = root.optString("baseUrl")
            val tag = root.optString("tag")
            if (!baseUrl.startsWith("https://") || !SafePaths.validTag(tag)) {
                return WordAudioManifest(emptyMap())
            }
            val packs = root.optJSONArray("packs") ?: JSONArray()
            for (index in 0 until packs.length()) {
                val pack = packs.optJSONObject(index) ?: continue
                val surah = pack.optInt("surah")
                val asset = pack.optString("asset")
                val bytes = pack.optLong("bytes")
                val sha = pack.optString("sha256")
                val words = pack.optInt("words")
                if (!SafePaths.validZip(asset) || surah !in 1..114 ||
                    bytes <= 0 || !SafePaths.validHash(sha)
                ) {
                    continue
                }
                out[surah] = WordAudioPackage(
                    surah = surah,
                    url = "$baseUrl/$tag/$asset",
                    bytes = bytes,
                    sha256 = sha,
                    words = words,
                )
            }
            return WordAudioManifest(out)
        }
    }
}

/**
 * Finds a word's own Opus file without touching the network.
 *
 * Two places are searched, in order: the surah folder the reader downloaded,
 * then the app's bundled Al-Fatiha. A file is named `sss_aaa_www.opus`, the
 * same surah, ayah, and word position the content database uses, so the store
 * answers by position and never by order.
 */
class WordAudioStore(private val context: Context) {

    private val imported = File(context.filesDir, "word-audio")
    private val positionsCache = ConcurrentHashMap<Int, Map<Int, Set<Int>>>()
    private val assetCache = ConcurrentHashMap<String, Boolean>()
    private val assetListCache = ConcurrentHashMap<Int, Set<String>>()

    fun uri(surah: Int, ayah: Int, position: Int): Uri? {
        if (surah !in 1..114 || ayah < 1 || position < 1) return null
        val name = fileName(surah, ayah, position)
        val folder = folder(surah)
        val file = SafePaths.childUnder(File(imported, folder), name)
        if (file?.isFile == true) return Uri.fromFile(file)
        val asset = "word-audio/$folder/$name"
        if (assetExists(asset)) return Uri.parse("asset:///$asset")
        return null
    }

    /**
     * The word positions that have a file, per ayah of this surah, downloaded
     * or bundled. The reading asks this once per surah and keeps the answer
     * in its rows, so a word whose clip the aligner could not place draws no
     * speaker rather than a dead one.
     *
     * The answer is keyed by the ayah inside the surah, not by the running
     * ayah number, because that is what the file name carries and what the
     * sheet knows when it opens.
     */
    fun positions(surah: Int): Map<Int, Set<Int>> {
        if (surah !in 1..114) return emptyMap()
        positionsCache[surah]?.let { return it }
        val folder = folder(surah)
        val positions = HashMap<Int, MutableSet<Int>>()
        fun add(name: String) {
            val match = WORD_FILE.matchEntire(name) ?: return
            if (match.groupValues[1] != folder) return
            val ayah = match.groupValues[2].toIntOrNull() ?: return
            val position = match.groupValues[3].toIntOrNull() ?: return
            if (ayah < 1 || position < 1) return
            positions.getOrPut(ayah) { LinkedHashSet() } += position
        }
        File(imported, folder).listFiles { file -> file.isFile }?.forEach { add(it.name) }
        assetNames(folder).forEach { add(it) }
        val answer = positions.mapValues { it.value.toSet() }
        positionsCache[surah] = answer
        return answer
    }

    fun isInstalled(surah: Int): Boolean = positions(surah).isNotEmpty()

    /** True when the app carries at least one of this surah's words in its assets. */
    fun isBundled(surah: Int): Boolean =
        surah in 1..114 && assetNames(folder(surah)).isNotEmpty()

    /** The downloaded bytes of one surah, for the page's own line. */
    fun bytes(surah: Int): Long {
        if (surah !in 1..114) return 0
        val folder = File(imported, folder(surah))
        return folder.listFiles { file -> file.isFile }?.sumOf { it.length() } ?: 0
    }

    fun invalidate(surah: Int) {
        positionsCache.remove(surah)
        assetListCache.remove(surah)
    }

    fun remove(surah: Int): Int {
        if (surah !in 1..114) return 0
        val folder = SafePaths.childUnder(imported, folder(surah)) ?: return 0
        val files = folder.listFiles()?.size ?: 0
        folder.deleteRecursively()
        invalidate(surah)
        return files
    }

    /** Removes every downloaded word, keeping the bundled Al-Fatiha assets. */
    fun clear(): Int {
        val files = imported.listFiles()?.size ?: 0
        imported.deleteRecursively()
        positionsCache.clear()
        return files
    }

    private fun fileName(surah: Int, ayah: Int, position: Int): String =
        String.format(Locale.ROOT, "%03d_%03d_%03d.opus", surah, ayah, position)

    private fun folder(surah: Int): String = String.format(Locale.ROOT, "%03d", surah)

    private fun assetExists(path: String): Boolean = assetCache.getOrPut(path) {
        runCatching { context.assets.open(path).close() }.isSuccess
    }

    private fun assetNames(folder: String): Set<String> = assetListCache.getOrPut(
        folder.toIntOrNull() ?: 0,
    ) {
        runCatching { context.assets.list("word-audio/$folder")?.toSet() }.getOrNull().orEmpty()
    }

    private companion object {
        val WORD_FILE = Regex("""^([0-9]{3})_([0-9]{3})_([0-9]{3})\.opus\z""")
    }
}

/**
 * Downloads one surah's word pronunciation package and unpacks it whole.
 *
 * The network and commit rules are the recitation package's, because they are
 * the rules this app has already paid for: the hash is checked before a file
 * is unpacked, the set lands in a staging folder, and one directory rename
 * makes it visible. A cancel stops the transfer itself and leaves nothing
 * behind.
 */
class WordAudioDownloader(private val context: Context) {

    suspend fun download(
        packageToFetch: WordAudioPackage,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (packageToFetch.surah !in 1..114 ||
            !SafePaths.validHash(packageToFetch.sha256) ||
            packageToFetch.bytes <= 0
        ) {
            return@withContext Result.failure(
                IllegalStateException("the word audio package has no valid identity"),
            )
        }
        val temporary = runCatching {
            File.createTempFile("word-audio-${packageToFetch.surah}-", ".part", context.cacheDir)
        }.getOrNull() ?: return@withContext Result.failure(
            IllegalStateException("the word audio part could not be created"),
        )
        var connection: java.net.HttpURLConnection? = null
        try {
            connection = ReleaseHttp.open(packageToFetch.url)
            if (connection.responseCode !in 200..299) {
                return@withContext Result.failure(IllegalStateException("HTTP ${connection.responseCode}"))
            }
            val type = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
            if (!type.isNullOrEmpty() && type !in ALLOWED_CONTENT_TYPES) {
                return@withContext Result.failure(IllegalStateException("the word audio is not a zip"))
            }
            val contentLength = connection.contentLengthLong
            if (contentLength >= 0 && contentLength > packageToFetch.bytes) {
                return@withContext Result.failure(
                    IllegalStateException("the word audio is larger than the manifest says"),
                )
            }
            val total = packageToFetch.bytes
            val digest = MessageDigest.getInstance("SHA-256")
            val deadline = System.nanoTime() + MAX_DOWNLOAD_MILLIS * 1_000_000L
            var read = 0L
            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        coroutineContext.ensureActive()
                        if (System.nanoTime() > deadline) {
                            return@withContext Result.failure(
                                IllegalStateException("the word audio took too long"),
                            )
                        }
                        val count = input.read(buffer)
                        if (count < 0) break
                        read += count
                        if (read > total) {
                            return@withContext Result.failure(
                                IllegalStateException("the word audio is larger than the manifest says"),
                            )
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                        onProgress(read, total)
                    }
                }
            }
            connection.disconnect()
            connection = null
            if (read != packageToFetch.bytes) {
                return@withContext Result.failure(
                    IllegalStateException("the word audio ended at $read bytes"),
                )
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(packageToFetch.sha256, ignoreCase = true)) {
                return@withContext Result.failure(IllegalStateException("checksum mismatch"))
            }
            coroutineContext.ensureActive()
            unpack(temporary, packageToFetch.surah)
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            connection?.disconnect()
            temporary.delete()
        }
    }

    private suspend fun unpack(zip: File, surah: Int) = unpackGate.withLock {
        if (surah !in 1..114) throw IllegalStateException("the surah number is not safe")
        val folder = String.format(Locale.ROOT, "%03d", surah)
        val imported = File(context.filesDir, "word-audio")
        val target = SafePaths.childUnder(imported, folder)
            ?: throw IllegalStateException("the surah folder is not a safe name")
        val staging = File(imported, STAGING)
        staging.mkdirs()
        staging.listFiles()?.forEach { it.deleteRecursively() }
        val unpacking = File(staging, "$folder.new").apply { mkdirs() }
        var entries = 0
        var total = 0L
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                coroutineContext.ensureActive()
                val entry = input.nextEntry ?: break
                entries++
                if (entries > MAX_ENTRIES) {
                    input.closeEntry()
                    throw IllegalStateException("the word audio holds too many files")
                }
                val name = entry.name
                val match = WORD_FILE.matchEntire(name)
                if (entry.isDirectory || match == null || match.groupValues[1] != folder) {
                    input.closeEntry()
                    continue
                }
                val file = SafePaths.childUnder(unpacking, name)
                    ?: throw IllegalStateException("the package names an unsafe file")
                var fileBytes = 0L
                file.outputStream().buffered().use { output ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        fileBytes += count
                        total += count
                        if (fileBytes > MAX_FILE_BYTES || total > MAX_UNCOMPRESSED_BYTES) {
                            throw IllegalStateException("the word audio is larger than a surah")
                        }
                        output.write(buffer, 0, count)
                    }
                }
                input.closeEntry()
            }
        }
        coroutineContext.ensureActive()
        if (unpacking.listFiles().orEmpty().isEmpty()) {
            throw IllegalStateException("the package held no word files")
        }
        val landing = SafePaths.childUnder(imported, "$folder.new")
            ?: throw IllegalStateException("the surah landing is not a safe name")
        val backup = SafePaths.childUnder(imported, "$folder.old")
            ?: throw IllegalStateException("the surah backup is not a safe name")
        if (landing.exists()) landing.deleteRecursively()
        if (!target.exists() && backup.exists()) backup.renameTo(target)
        if (!unpacking.renameTo(landing)) {
            throw IllegalStateException("the surah folder could not be completed")
        }
        if (target.exists()) {
            if (backup.exists()) backup.deleteRecursively()
            if (!target.renameTo(backup)) {
                landing.renameTo(unpacking)
                throw IllegalStateException("the previous surah folder could not be moved aside")
            }
        }
        if (!landing.renameTo(target)) {
            if (backup.exists()) backup.renameTo(target)
            throw IllegalStateException("the surah folder could not be committed")
        }
        backup.deleteRecursively()
        unpacking.deleteRecursively()
        staging.delete()
        Unit
    }

    private companion object {
        const val STAGING = ".staging"
        val unpackGate = Mutex()
        val WORD_FILE = Regex("""^([0-9]{3})_([0-9]{3})_([0-9]{3})\.opus\z""")
        const val MAX_UNCOMPRESSED_BYTES = 64L * 1024 * 1024
        const val MAX_FILE_BYTES = 1024L * 1024
        const val MAX_ENTRIES = 10_000
        const val MAX_DOWNLOAD_MILLIS = 10L * 60 * 1000
        val ALLOWED_CONTENT_TYPES = setOf(
            "application/octet-stream",
            "binary/octet-stream",
            "application/zip",
            "application/x-zip-compressed",
        )
    }
}

/**
 * What the word meanings page says about the pronunciation pack: the current
 * surah's size and state, and the whole Book's size for the one line.
 */
data class WordAudioState(
    val surah: Int = 0,
    val bytes: Long = 0,
    val read: Long = 0,
    val totalBytes: Long = 0,
    val installed: Boolean = false,
    val bundled: Boolean = false,
    val failed: Boolean = false,
    val waiting: Boolean = false,
)
