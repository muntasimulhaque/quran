package io.github.muntasimulhaque.quran.ui.mushaf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import io.github.muntasimulhaque.quran.content.R
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PageFontStore
import io.github.muntasimulhaque.quran.ui.theme.PagePalette
import io.github.muntasimulhaque.quran.data.PageWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * A rendered Mushaf page: the paper, the glyphs, and the geometry of every
 * word. The bitmap is what the reader sees; the boxes are what make the page
 * touchable, so a tap lands on the ayah under the finger and a recitation can
 * wash the word being recited.
 */
class RenderedPage(
    val bitmap: Bitmap,
    val words: List<WordBox>,
    private val ayahBoxes: Map<Int, List<RectF>>,
    private val ayahs: Map<Int, Ayah>,
) {
    val widthPx: Int get() = bitmap.width
    val heightPx: Int get() = bitmap.height

    /** The ayah under a touch, with a little slack around each word. */
    fun ayahAt(x: Float, y: Float, slop: Float): Ayah? {
        val number = ayahNumberAt(x, y, slop) ?: return null
        return ayahs[number]
    }

    private fun ayahNumberAt(x: Float, y: Float, slop: Float): Int? {
        var best: Int? = null
        var bestDistance = Float.MAX_VALUE
        for ((ayah, boxes) in ayahBoxes) {
            for (box in boxes) {
                if (x >= box.left - slop && x <= box.right + slop &&
                    y >= box.top - slop && y <= box.bottom + slop
                ) {
                    return ayah
                }
                val dx = maxOf(box.left - x, 0f, x - box.right)
                val dy = maxOf(box.top - y, 0f, y - box.bottom)
                val distance = dx * dx + dy * dy
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = ayah
                }
            }
        }
        val limit = slop * 3f
        return if (bestDistance <= limit * limit) best else null
    }

    /** The word box for one word of one ayah, for the recitation wash. */
    fun wordBox(ayah: Int, position: Int): WordBox? =
        words.firstOrNull { !it.marker && it.ayah == ayah && it.position == position }

    /** One rectangle per line of an ayah, for the selection wash. */
    fun ayahLineBoxes(ayah: Int): List<RectF> {
        val boxes = ayahBoxes[ayah] ?: return emptyList()
        val lines = ArrayList<RectF>()
        for (box in boxes.sortedBy { it.top }) {
            val last = lines.lastOrNull()
            if (last != null && kotlin.math.abs(last.centerY() - box.centerY()) < box.height() * 0.5f) {
                last.union(box)
            } else {
                lines += RectF(box)
            }
        }
        return lines
    }

    fun ayahBox(ayah: Int): RectF? {
        val boxes = ayahBoxes[ayah] ?: return null
        val box = RectF()
        for (line in boxes) box.union(line)
        return box
    }
}

/** One glyph group on the page, in page pixels. */
data class WordBox(
    val id: Int,
    val ayah: Int,
    val position: Int,
    val marker: Boolean,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class PageKey(val page: Int, val widthPx: Int, val theme: String)

/**
 * Renders page bitmaps on worker threads, keeps a few of them, and never
 * renders the same page twice at once. A page turn is a texture draw because
 * the neighbouring pages are already there.
 */
class PageRenderer(private val context: Context) {

    private val cache = object : LruCache<PageKey, RenderedPage>(CACHE_PAGES) {
        override fun entryRemoved(evicted: Boolean, key: PageKey, oldValue: RenderedPage, newValue: RenderedPage?) {
            if (evicted) oldValue.bitmap.recycle()
        }
    }
    private val inFlight = ConcurrentHashMap<PageKey, Boolean>()
    private var hafs: Typeface? = null
    private var amiri: Typeface? = null

    fun peek(key: PageKey): RenderedPage? = synchronized(cache) { cache.get(key) }

    suspend fun get(
        key: PageKey,
        content: ContentDatabase,
        fonts: PageFontStore,
        palette: PagePalette,
    ): RenderedPage? {
        peek(key)?.let { return it }
        // One render per page: a swipe and a prefetch ask the same question.
        val claimed = inFlight.putIfAbsent(key, true) == null
        if (!claimed) {
            while (inFlight.containsKey(key)) {
                kotlinx.coroutines.delay(16)
                peek(key)?.let { return it }
            }
            return peek(key)
        }
        return try {
            val rendered = withContext(Dispatchers.Default) {
                render(content, fonts, key, palette)
            }
            if (rendered != null) synchronized(cache) { cache.put(key, rendered) }
            rendered
        } finally {
            inFlight.remove(key)
        }
    }

    private fun render(
        content: ContentDatabase,
        fonts: PageFontStore,
        key: PageKey,
        palette: PagePalette,
    ): RenderedPage? {
        val typeface = fonts.typeface(key.page) ?: return null
        val widthPx = key.widthPx
        val textWidth = widthPx * TEXT_WIDTH_RATIO
        val fontPx = textWidth / EM_PER_LINE
        val lineHeight = fontPx * LINE_HEIGHT_RATIO
        val band = lineHeight * BAND_RATIO
        val height = (band * 2 + lineHeight * LINES).roundToInt()
        val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.paper.toArgb())

        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = fontPx
            color = palette.ink.toArgb()
        }
        val hafsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = hafsTypeface() ?: typeface
            textSize = fontPx * BASMALLA_RATIO
            color = palette.ink.toArgb()
            textAlign = Paint.Align.CENTER
        }
        val ornamentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = amiriTypeface()
            textAlign = Paint.Align.CENTER
            color = palette.ornament.toArgb()
        }
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.ornament.toArgb()
            strokeWidth = maxOf(1f, widthPx / 720f)
            alpha = 130
        }

        val pageWords = content.pageWords(key.page)
        val wordsById = HashMap<Int, PageWord>(pageWords.size)
        for (word in pageWords) wordsById[word.id] = word
        val lines = content.pageLines(key.page)
        val ayahs = content.ayahsForPage(key.page).associateBy { it.number }
        val words = ArrayList<WordBox>(pageWords.size)
        val ayahBoxes = HashMap<Int, MutableList<RectF>>()
        val basmallahGlyphs = content.basmallahGlyphs()

        // The page's own furniture: the juz at the left of the foot rule and
        // the page number in its medallion, as the printed page carries them.
        // The surah names come from the layout itself, on the ornamental line
        // the page already has, so they are never written twice.
        drawFooter(canvas, key.page, widthPx, height, band, textWidth, fontPx, ornamentPaint, rulePaint, content)

        for (line in lines) {
            val slotTop = band + (line.line - 1) * lineHeight
            when (line.type) {
                "surah_name" -> {
                    val name = content.surah(line.surah)?.nameArabic ?: ""
                    val size = fontPx * SURAH_NAME_RATIO
                    ornamentPaint.textSize = size
                    val metrics = ornamentPaint.fontMetrics
                    val baseline = slotTop + (lineHeight - (metrics.descent - metrics.ascent)) / 2f - metrics.ascent
                    canvas.drawText(name, widthPx / 2f, baseline, ornamentPaint)
                    canvas.drawLine(
                        widthPx / 2f - textWidth * 0.22f,
                        slotTop + lineHeight * 0.86f,
                        widthPx / 2f + textWidth * 0.22f,
                        slotTop + lineHeight * 0.86f,
                        rulePaint,
                    )
                }
                "basmallah" -> {
                    if (basmallahGlyphs.isEmpty()) continue
                    val metrics = hafsPaint.fontMetrics
                    val baseline = slotTop + (lineHeight - (metrics.descent - metrics.ascent)) / 2f - metrics.ascent
                    canvas.drawText(basmallahGlyphs, widthPx / 2f, baseline, hafsPaint)
                }
                else -> {
                    var total = 0f
                    for (id in line.firstWordId..line.lastWordId) {
                        val word = wordsById[id] ?: continue
                        total += glyphPaint.measureText(word.glyph)
                    }
                    var x = if (line.centered) (widthPx - total) / 2f else widthPx - (widthPx - textWidth) / 2f - total
                    val metrics = glyphPaint.fontMetrics
                    val baseline = slotTop + (lineHeight - (metrics.descent - metrics.ascent)) / 2f - metrics.ascent
                    for (id in line.firstWordId..line.lastWordId) {
                        val word = wordsById[id] ?: continue
                        val advance = glyphPaint.measureText(word.glyph)
                        canvas.drawText(word.glyph, x, baseline, glyphPaint)
                        val box = RectF(
                            x,
                            baseline + metrics.ascent,
                            x + advance,
                            baseline + metrics.descent,
                        )
                        words += WordBox(
                            id = word.id,
                            ayah = word.ayah,
                            position = word.position,
                            marker = word.marker,
                            left = box.left,
                            top = box.top,
                            right = box.right,
                            bottom = box.bottom,
                        )
                        if (!word.marker) {
                            ayahBoxes.getOrPut(word.ayah) { mutableListOf() }.add(box)
                        }
                        x += advance
                    }
                }
            }
        }
        return RenderedPage(bitmap, words, ayahBoxes, ayahs)
    }

    private fun drawHeader(
        canvas: Canvas,
        content: ContentDatabase,
        page: Int,
        widthPx: Int,
        band: Float,
        textWidth: Float,
        fontPx: Float,
        paint: Paint,
    ) {
        val ayahs = content.ayahsForPage(page)
        val first = ayahs.firstOrNull() ?: return
        val last = ayahs.lastOrNull()
        val position = content.pagePosition(page)
        val name = buildString {
            append(content.surah(first.surah)?.nameArabic ?: "")
            if (last != null && last.surah != first.surah) {
                append("  \u00B7  ")
                append(content.surah(last.surah)?.nameArabic ?: "")
            }
        }
        paint.textSize = fontPx * HEADER_RATIO
        val metrics = paint.fontMetrics
        val baseline = band - (band - (metrics.descent - metrics.ascent)) / 2f - metrics.descent
        canvas.drawText(name, widthPx / 2f, baseline, paint)
        val juz = position?.juz ?: 1
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("${ARABIC_JUZ} ${arabicDigits(juz)}", (widthPx - textWidth) / 2f, baseline, paint)
        paint.textAlign = Paint.Align.CENTER
    }

    private fun drawFooter(
        canvas: Canvas,
        page: Int,
        widthPx: Int,
        height: Int,
        band: Float,
        textWidth: Float,
        fontPx: Float,
        paint: Paint,
        rule: Paint,
        content: ContentDatabase,
    ) {
        val top = height - band
        canvas.drawLine(
            (widthPx - textWidth) / 2f,
            top,
            (widthPx + textWidth) / 2f,
            top,
            rule,
        )
        val radius = fontPx * MEDALLION_RATIO
        val centerY = top + band / 2f
        canvas.drawCircle(widthPx / 2f, centerY, radius, rule)
        paint.textSize = fontPx * PAGE_NUMBER_RATIO
        val metrics = paint.fontMetrics
        val baseline = centerY + (metrics.descent - metrics.ascent) / 2f - metrics.descent
        canvas.drawText(arabicDigits(page), widthPx / 2f, baseline, paint)
        // The juz sits at the left end of the rule, in the same quiet gold.
        val juz = content.pagePosition(page)?.juz ?: return
        paint.textSize = fontPx * HEADER_RATIO
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            "${ARABIC_JUZ} ${arabicDigits(juz)}",
            (widthPx - textWidth) / 2f,
            baseline,
            paint,
        )
        paint.textAlign = Paint.Align.CENTER
    }

    private fun hafsTypeface(): Typeface? = hafs ?: runCatching {
        Typeface.createFromAsset(context.assets, "fonts/UthmanicHafs_V22.ttf")
    }.getOrNull()?.also { hafs = it }

    private fun amiriTypeface(): Typeface =
        amiri ?: ResourcesCompat.getFont(context, R.font.amiri_quran)
            ?.also { amiri = it }
            ?: Typeface.SERIF

    private companion object {
        const val CACHE_PAGES = 6

        /** A full line of glyphs sums to this many em. */
        const val EM_PER_LINE = 15.6f
        const val LINE_HEIGHT_RATIO = 1.644f
        const val LINES = 15
        const val TEXT_WIDTH_RATIO = 0.90f

        /** The head and foot bands that carry the page's own furniture. */
        const val BAND_RATIO = 0.86f
        const val HEADER_RATIO = 0.34f
        const val SURAH_NAME_RATIO = 0.62f
        const val BASMALLA_RATIO = 0.8f
        const val MEDALLION_RATIO = 0.34f
        const val PAGE_NUMBER_RATIO = 0.4f
        const val ARABIC_JUZ = "\u0627\u0644\u062C\u0632\u0621"
    }
}

/** Arabic-Indic digits, as the printed page uses. */
fun arabicDigits(number: Int): String =
    number.toString().map { if (it in '0'..'9') '\u0660' + (it - '0') else it }.joinToString("")
