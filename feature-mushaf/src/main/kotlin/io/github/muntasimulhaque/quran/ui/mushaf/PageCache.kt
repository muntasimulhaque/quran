package io.github.muntasimulhaque.quran.ui.mushaf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * The last page the reader saw, kept on disk as a picture.
 *
 * Opening the app must land on the page the reader left, and the shortest
 * road to a page is the picture of it taken while they were reading. One
 * bitmap, keyed by the width and the theme it was drawn for, written after
 * the reader rests on a page and read back before the content even opens.
 * It is a cache in the strict sense: if it is gone or does not match, the
 * app simply renders the page as it always does.
 */
class PageCache(context: Context) {

    private val directory = File(context.cacheDir, "last-page").apply { mkdirs() }
    private val image = File(directory, "page.webp")
    private val marker = File(directory, "page.key")

    /** The page picture for this width and theme, or null when there is none. */
    fun load(): StartupPage? {
        if (!image.isFile || !marker.isFile) return null
        val parts = runCatching { marker.readText().trim().split(' ') }.getOrNull() ?: return null
        if (parts.size != 3) return null
        val page = parts[0].toIntOrNull() ?: return null
        val width = parts[1].toIntOrNull() ?: return null
        val theme = parts[2]
        if (page !in 1..604 || width <= 0) return null
        val bitmap = runCatching {
            BitmapFactory.decodeFile(image.path)
        }.getOrNull() ?: return null
        return StartupPage(page = page, widthPx = width, theme = theme, bitmap = bitmap)
    }

    /**
     * Replaces the page picture. One write runs at a time, and every step
     * that can fail is inside one guard.
     *
     * Two settles can race here (a swipe and the flick after it), and the
     * losing writer used to reach `copyTo` on a temporary file the winning
     * one had already renamed away, which threw on a worker and killed the
     * process. The lock serializes the writers, the temporary's name is the
     * same for both because the lock is, and the whole write is guarded so
     * no filesystem surprise can escape.
     */
    @Synchronized
    fun save(page: Int, widthPx: Int, theme: String, bitmap: Bitmap) {
        runCatching {
            val temporary = File(directory, "page.webp.part")
            val written = temporary.outputStream().buffered().use { output ->
                bitmap.compress(Bitmap.CompressFormat.WEBP, QUALITY, output)
            }
            if (!written) {
                temporary.delete()
                return
            }
            if (!temporary.renameTo(image)) {
                temporary.copyTo(image, overwrite = true)
                temporary.delete()
            }
            marker.writeText("$page $widthPx $theme")
        }.onFailure {
            // A picture that could not be written is not worth a crash: the
            // next launch simply renders the page instead of painting it.
            android.util.Log.w(TAG, "the launch picture could not be written", it)
        }
    }

    /** Forgets the picture, for when a page can no longer be trusted. */
    fun clear() {
        image.delete()
        marker.delete()
    }

    private companion object {
        const val TAG = "PageCache"

        /** Text tolerates no blur; a page is a few hundred kilobytes at this quality. */
        const val QUALITY = 92
    }
}

/** One page kept on disk, ready for the next launch. */
data class StartupPage(
    val page: Int,
    val widthPx: Int,
    val theme: String,
    val bitmap: Bitmap,
)
