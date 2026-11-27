package io.github.muntasimulhaque.quran.data

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * One transfer the download service can carry.
 *
 * The three kinds are the three things the app ever fetches: a content pack,
 * one surah's recitation, and one surah's word pronunciation. They used to
 * live in three different call sites with three different lifetimes. The
 * reader's tap started a coroutine in the window, and a reader who left the
 * app during a 186 MB surah could lose the whole transfer. The job is the
 * same three things written down as one request, so the service can finish it
 * after the window is gone (owner decision, 23.1).
 *
 * [bytes] is the catalog's own size, never a guess. It is the transfer's
 * ceiling, the progress line's denominator, and the free-space check's number.
 */
sealed class DownloadJob {

    abstract val bytes: Long

    /** The line the download notification and the service's title read. */
    abstract fun title(context: Context, language: String): String

    /**
     * The part file a resumable transfer appends to, or null when the job's
     * downloader already owns its part. A pack is the one case with its own
     * part file, and its downloader has kept it since 9.3.
     */
    open fun partFile(context: Context): File? = null

    /** One content pack, addressed by the catalog and verified by its hash. */
    data class Pack(val pack: ContentPack) : DownloadJob() {
        override val bytes: Long get() = pack.bytes

        override fun title(context: Context, language: String): String =
            pack.displayName(language)
    }

    /** One surah of one reciter, written into that reciter's own folder. */
    data class Recitation(
        val packageToFetch: RecitationPackage,
        val folder: String,
    ) : DownloadJob() {
        override val bytes: Long get() = packageToFetch.bytes

        override fun title(context: Context, language: String): String =
            context.getString(R.string.download_recitation_title, packageToFetch.surah)

        override fun partFile(context: Context): File? = DownloadParts.part(
            context,
            "recitation-${packageToFetch.recitation}-${packageToFetch.surah}.zip",
        )
    }

    /** One surah's word pronunciation package. */
    data class WordAudio(val packageToFetch: WordAudioPackage) : DownloadJob() {
        override val bytes: Long get() = packageToFetch.bytes

        override fun title(context: Context, language: String): String =
            context.getString(R.string.download_word_title, packageToFetch.surah)

        override fun partFile(context: Context): File? = DownloadParts.part(
            context,
            "word-${packageToFetch.surah}.zip",
        )
    }
}

/**
 * The part files of transfers that can still be resumed.
 *
 * They live in the app's own storage, not the cache: Android may clear the
 * cache at any moment, and a cache-cleared 150 MB part is the same as no part
 * at all. A completed transfer deletes its part, and a failed one keeps it for
 * the next tap (owner decision, 23.1).
 */
object DownloadParts {

    fun root(context: Context): File = File(context.filesDir, "downloads")

    fun part(context: Context, name: String): File? {
        val root = root(context).apply { mkdirs() }
        return SafePaths.childUnder(root, name)
    }
}

/**
 * The job as a small JSON record, so a service the platform restarts after a
 * process death can pick the same transfer up from its part file.
 *
 * Nothing here carries a URL, a hash, or a size: those come from the catalog
 * and the two manifests inside the app, which are the only authorities on
 * what a transfer is. The record is an address, not a second catalog.
 */
internal object DownloadSpec {

    fun write(context: Context, job: DownloadJob) {
        val json = when (job) {
            is DownloadJob.Pack -> JSONObject()
                .put("kind", "pack")
                .put("id", job.pack.id)
            is DownloadJob.Recitation -> JSONObject()
                .put("kind", "recitation")
                .put("recitation", job.packageToFetch.recitation)
                .put("surah", job.packageToFetch.surah)
                .put("folder", job.folder)
            is DownloadJob.WordAudio -> JSONObject()
                .put("kind", "word")
                .put("surah", job.packageToFetch.surah)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, json.toString())
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .apply()
    }

    /** The job the last process was carrying, resolved against today's content. */
    fun read(context: Context): DownloadJob? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?: return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        return when (json.optString("kind")) {
            "pack" -> {
                val id = json.optString("id")
                if (!SafePaths.validId(id)) return null
                PackCatalog.load(context).get(id)?.let(DownloadJob::Pack)
            }
            "recitation" -> {
                val recitation = json.optString("recitation")
                val surah = json.optInt("surah")
                val folder = json.optString("folder")
                if (!SafePaths.validId(recitation) ||
                    surah !in 1..114 ||
                    !SafePaths.validFolder(folder)
                ) {
                    return null
                }
                RecitationManifest.load(context).packageFor(recitation, surah)?.let {
                    DownloadJob.Recitation(it, folder)
                }
            }
            "word" -> {
                val surah = json.optInt("surah")
                if (surah !in 1..114) return null
                WordAudioManifest.load(context).packageFor(surah)?.let(DownloadJob::WordAudio)
            }
            else -> null
        }
    }

    private const val PREFS = "download-service"
    private const val KEY = "active"
}

