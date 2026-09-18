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

    /** Replaces the page picture. Written whole, so a half-written one is never read. */
    fun save(page: Int, widthPx: Int, theme: String, bitmap: Bitmap) {
        val temporary = File(directory, "page.webp.part")
        val written = runCatching {
            temporary.outputStream().buffered().use { output ->
                bitmap.compress(Bitmap.CompressFormat.WEBP, QUALITY, output)
            }
        }.getOrDefault(false)
        if (!written) {
            temporary.delete()
            return
        }
        if (!temporary.renameTo(image)) {
            temporary.copyTo(image, overwrite = true)
            temporary.delete()
        }
        runCatching { marker.writeText("$page $widthPx $theme") }
    }

    /** Forgets the picture, for when a page can no longer be trusted. */
    fun clear() {
        image.delete()
        marker.delete()
    }

    private companion object {
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
