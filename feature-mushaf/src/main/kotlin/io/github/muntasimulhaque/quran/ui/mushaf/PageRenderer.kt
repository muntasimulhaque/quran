package io.github.muntasimulhaque.quran.ui.mushaf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import io.github.muntasimulhaque.quran.content.R
import io.github.muntasimulhaque.quran.core.PageFrame
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PageFontStore
import io.github.muntasimulhaque.quran.ui.theme.PagePalette
import io.github.muntasimulhaque.quran.data.PageWord
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

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
    val palette: PagePalette,
) {
    val widthPx: Int get() = bitmap.width
    val heightPx: Int get() = bitmap.height

    /** The page's ayahs in Mushaf order: reading order, for both readers. */
    val ayahOrder: List<Ayah> = ayahs.values.sortedBy { it.number }

    val ayahNumbers: Set<Int> get() = ayahBoxes.keys

    /** The ayah under a touch, with a little slack around each word. */
    fun ayahAt(x: Float, y: Float, slop: Float): Ayah? {
        val number = ayahNumberAt(x, y, slop) ?: return null
        return ayahs[number]
    }

    private fun ayahNumberAt(x: Float, y: Float, slop: Float): Int? {
        var best: Int? = null
        var bestDistance = Float.MAX_VALUE
        for (ayah in ayahOrder) {
            when (val hit = under(ayah.number, x, y, slop)) {
                is Hit.Exact -> return ayah.number
                is Hit.Near -> if (hit.distance < bestDistance) {
                    bestDistance = hit.distance
                    best = ayah.number
                }
                is Hit.Far -> Unit
            }
        }
        val limit = slop * 3f
        return if (bestDistance <= limit * limit) best else null
    }

    private sealed interface Hit {
        data object Exact : Hit
        data class Near(val distance: Float) : Hit
        data object Far : Hit
    }

    private fun under(ayah: Int, x: Float, y: Float, slop: Float): Hit {
        val boxes = ayahBoxes[ayah] ?: return Hit.Far
        var best = Float.MAX_VALUE
        for (box in boxes) {
            if (x >= box.left - slop && x <= box.right + slop &&
                y >= box.top - slop && y <= box.bottom + slop
            ) {
                return Hit.Exact
            }
            val dx = maxOf(box.left - x, 0f, x - box.right)
            val dy = maxOf(box.top - y, 0f, y - box.bottom)
            val distance = dx * dx + dy * dy
            if (distance < best) best = distance
        }
        return Hit.Near(best)
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
    private val inFlight = ConcurrentHashMap<PageKey, CompletableDeferred<RenderedPage?>>()
    private val lastPage = PageCache(context)
    private var hafs: Typeface? = null
    private var amiri: Typeface? = null

    fun peek(key: PageKey): RenderedPage? = synchronized(cache) { cache.get(key) }

    /** The picture of the page the reader left, for the first frame of a launch. */
    suspend fun loadStartupPage(): StartupPage? = withContext(Dispatchers.IO) { lastPage.load() }

    /**
     * Writes the page the reader has settled on, so the next launch can paint
     * it before anything else is ready. A newer settle cancels the write
     * before it starts, so swiping through the Quran never touches the disk.
     *
     * The bitmap is copied while the cache lock is held, because swiping on
     * can evict and recycle the page's own bitmap out from under the write.
     * Compressing a recycled bitmap throws, and a launch picture that only
     * sometimes lands is worse than one that always does.
     */
    suspend fun rememberStartupPage(key: PageKey) {
        val copy = withContext(Dispatchers.Default) {
            synchronized(cache) {
                val source = cache.get(key)?.bitmap ?: return@withContext null
                if (source.isRecycled) {
                    null
                } else {
                    runCatching { source.copy(Bitmap.Config.ARGB_8888, false) }.getOrNull()
                }
            }
        } ?: return
        withContext(Dispatchers.IO) {
            try {
                lastPage.save(key.page, key.widthPx, key.theme, copy)
            } finally {
                copy.recycle()
            }
        }
    }

    /**
     * One render per page, shared by everyone who asks: a swipe and the
     * prefetch behind it wait on the same bitmap instead of racing to draw it
     * twice, and every waiter is woken when it lands.
     */
    suspend fun get(
        key: PageKey,
        content: ContentDatabase,
        fonts: PageFontStore,
        palette: PagePalette,
    ): RenderedPage? {
        peek(key)?.let { return it }
        val gate = CompletableDeferred<RenderedPage?>()
        val claimed = inFlight.putIfAbsent(key, gate) == null
        if (!claimed) return inFlight[key]?.await()
        return try {
            val rendered = withContext(Dispatchers.Default) { render(content, fonts, key, palette) }
            if (rendered != null) synchronized(cache) { cache.put(key, rendered) }
            gate.complete(rendered)
            rendered
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            gate.complete(null)
            throw cancelled
        } catch (error: Throwable) {
            // A page that cannot be drawn shows its paper rather than taking
            // the whole reading surface down with it.
            android.util.Log.w(TAG, "page ${key.page} could not be rendered", error)
            gate.complete(null)
            null
        } finally {
            inFlight.remove(key, gate)
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
        val textWidth = widthPx * PageFrame.TEXT_WIDTH_RATIO
        val fontPx = textWidth / PageFrame.EM_PER_LINE
        val lineHeight = fontPx * PageFrame.LINE_HEIGHT_RATIO
        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = fontPx
            color = palette.ink.toArgb()
        }
        // The rule is ruled against the text's own ink, so the frame is
        // measured from the font and not from the slots: these faces carry
        // their marks above the letters, and a frame ruled off the slots
        // stands on them.
        val ink = glyphPaint.fontMetrics.let { it.descent - it.ascent } / fontPx
        val frame = PageFrame.of(widthPx, ink)
        val bitmap = Bitmap.createBitmap(widthPx, frame.heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.paper.toArgb())

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

        // The page's own furniture: the rule the text sits within, the juz at
        // the left of the foot rule, and the page number in its roundel, as
        // the printed page carries them. A manuscript page is *within* gold
        // and black rules, and that rule is also what gives the page an edge
        // on a wide ground: without it the page and the app's background are
        // the same colour and the sheet of paper stops existing off a phone.
        drawPageRule(canvas, frame, rulePaint)
        drawFooter(canvas, key.page, frame, widthPx, fontPx, ornamentPaint, rulePaint, content)

        for (line in lines) {
            val slotTop = frame.slotTop + (line.line - 1) * lineHeight
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
                    // The Quran is read right to left: the line's first word
                    // sits at its right edge and every word after it lays out
                    // to the left. Drawing them left to right is what showed
                    // the reader each line in reverse.
                    var x = if (line.centered) (widthPx + total) / 2f else (widthPx + textWidth) / 2f
                    val metrics = glyphPaint.fontMetrics
                    val baseline = slotTop + (lineHeight - (metrics.descent - metrics.ascent)) / 2f - metrics.ascent
                    for (id in line.firstWordId..line.lastWordId) {
                        val word = wordsById[id] ?: continue
                        val advance = glyphPaint.measureText(word.glyph)
                        x -= advance
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
                    }
                }
            }
        }
        return RenderedPage(bitmap, words, ayahBoxes, ayahs, palette)
    }

    /**
     * The page's own rule: a hairline frame around the text, in the ornament
     * tone a shade quieter than the foot rule that carries the page number.
     *
     * A bound mushaf is ruled: the text stands within gold and black rules
     * that run the whole page, and that is the drawing that says "page" more
     * quietly than any ornament. It is a hairline, because it is a printed
     * page and the paper is the subject.
     *
     * Where the rule stands is [PageFrame]'s one number, and it stands the
     * same distance inside the page on all four sides, with the same air
     * between itself and the text. The frame used to be four numbers: a share
     * of the page's width at the sides, the first slot at the head, and a
     * whole band at the foot. That left the rule about 4 dp above the first
     * line's marks and 5 dp from the last glyph of every line, with its own
     * foot a band away, so one rectangle read as a wire pressed against the
     * text at the top and the left and as a page at the foot (owner report,
     * the forty-fourth session).
     */
    private fun drawPageRule(canvas: Canvas, frame: PageFrame, rule: Paint) {
        val quiet = Paint(rule)
        quiet.alpha = (rule.alpha * 0.5f).toInt().coerceIn(1, 255)
        canvas.drawLine(frame.left, frame.head, frame.left, frame.foot, quiet)
        canvas.drawLine(frame.right, frame.head, frame.right, frame.foot, quiet)
        canvas.drawLine(frame.left, frame.head, frame.right, frame.head, quiet)
    }

    private fun drawFooter(
        canvas: Canvas,
        page: Int,
        frame: PageFrame,
        widthPx: Int,
        fontPx: Float,
        paint: Paint,
        rule: Paint,
        content: ContentDatabase,
    ) {
        val top = frame.foot
        canvas.drawLine(frame.left, top, frame.right, top, rule)
        val radius = fontPx * PageFrame.ROUNDEL_EM
        // The roundel stands in the middle of the room the foot's rule leaves,
        // so it is as far from the text above it as from the page's own edge.
        val centerY = top + (frame.heightPx - top) / 2f
        // The page number stands in a hairline roundel, the way a printed
        // page rules it, rather than in a filled disc: a disc of gold behind
        // gold is a heavier mark than the page's own furniture is.
        val roundel = Paint(rule).apply {
            style = Paint.Style.STROKE
            strokeWidth = maxOf(1f, widthPx / 900f)
            alpha = (rule.alpha * 1.5f).toInt().coerceIn(1, 255)
        }
        canvas.drawCircle(widthPx / 2f, centerY, radius, roundel)
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
            frame.left,
            baseline,
            paint,
        )
        paint.textAlign = Paint.Align.CENTER
    }

    /**
     * The rectangle a page's own furniture is ruled in is `PageFrame`'s, and
     * it is one number on all four sides rather than one per side: the rule
     * stands off the text by the air the glyphs carry above their own letters,
     * and the margin left outside it is the same margin at the head.
     */
    private fun hafsTypeface(): Typeface? = hafs ?: runCatching {
        Typeface.createFromAsset(context.assets, "fonts/UthmanicHafs_V22.ttf")
    }.getOrNull()?.also { hafs = it }

    private fun amiriTypeface(): Typeface =
        amiri ?: ResourcesCompat.getFont(context, R.font.amiri_quran)
            ?.also { amiri = it }
            ?: Typeface.SERIF

    private companion object {
        const val TAG = "PageRenderer"
        const val CACHE_PAGES = 6

        /** The juz's own size at the left end of the foot's rule, in ems. */
        const val HEADER_RATIO = 0.34f

        /** The surah name's own size between two rules, in ems. */
        const val SURAH_NAME_RATIO = 0.62f

        /** The basmallah's own size, which is a smaller hand than the page. */
        const val BASMALLA_RATIO = 0.8f

        /** The page number's own size inside its roundel, in ems. */
        const val PAGE_NUMBER_RATIO = 0.4f
        const val ARABIC_JUZ = "\u0627\u0644\u062C\u0632\u0621"
    }
}

/** Arabic-Indic digits, as the printed page uses. */
fun arabicDigits(number: Int): String =
    number.toString().map { if (it in '0'..'9') '\u0660' + (it - '0') else it }.joinToString("")
