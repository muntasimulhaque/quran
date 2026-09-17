package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.util.LruCache

/**
 * Resolves the font for a Mushaf page from the app's own assets.
 *
 * The 604 page fonts ship in the base app, so a release APK is a complete
 * Quran: it installs once, works offline, and needs no Play delivery
 * library and no extra permissions. A small LRU keeps the typefaces of the
 * current page and its neighbours warm.
 */
class PageFontStore(private val context: Context) {

    private val cache = LruCache<Int, Typeface>(6)

    fun typeface(page: Int): Typeface? {
        cache.get(page)?.let { return it }
        val loaded = runCatching {
            Typeface.createFromAsset(context.assets, "fonts/pages/p$page.ttf")
        }.onFailure { Log.w(TAG, "font p$page could not be read", it) }
            .getOrNull() ?: return null
        cache.put(page, loaded)
        return loaded
    }

    private companion object {
        const val TAG = "PageFontStore"
    }
}
