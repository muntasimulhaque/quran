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
import io.github.muntasimulhaque.quran.core.LayoutLine
import io.github.muntasimulhaque.quran.core.LayoutWord
import io.github.muntasimulhaque.quran.core.PageFrame
import io.github.muntasimulhaque.quran.core.PageTextLayout
import io.github.muntasimulhaque.quran.core.SlotKind
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PageWord
import io.github.muntasimulhaque.quran.ui.theme.PagePalette
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * A rendered Mushaf page: the paper, the words, and the geometry of every
 * one of them. The bitmap is what the reader sees; the boxes are what make
 * the page touchable, so a tap lands on the ayah under the finger and a
 * recitation can wash the word being recited.
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

/** One word or ayah number on the page, in page pixels. */
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

/**
 * What one rendered page is: its number, the pixel width it was drawn at,
 * the theme it was drawn for, and the reader's text size it was drawn with.
 * A page is never redrawn for a new size: it is a new page with the same
 * number.
 */
data class PageKey(val page: Int, val widthPx: Int, val theme: String, val scale: Float)

/**
 * Renders page bitmaps on worker threads, keeps a few of them, and never
 * renders the same page twice at once. A page turn is a texture draw because
 * the neighbouring pages are already there.
 *
 * The page is drawn from the Book's own text with the one Arabic face the
 * app ships, at the reader's own text size: the words are laid out by
 * [PageTextLayout], which keeps the printed page's lines wherever its words
 * still fit the reader's measure and re-sets them where they no longer do.
 * The page's furniture (the rule, the surah's band, the basmallah, the foot's
 * roundel and juz) is the page's own, in the measure's em, so it keeps its
 * proportions at every text size.
 */
class PageRenderer(private val context: Context) {

    /**
     * The rendered pages, kept by the memory they take rather than by a
     * count: a page set large is half again the page set small, and a reader
     * who has asked for large text should hold fewer pages rather than more
     * memory.
     */
    private val cache = object : LruCache<PageKey, RenderedPage>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: PageKey, value: RenderedPage): Int = value.bitmap.allocationByteCount
        override fun entryRemoved(evicted: Boolean, key: PageKey, oldValue: RenderedPage, newValue: RenderedPage?) {
            if (evicted) oldValue.bitmap.recycle()
        }
    }
    private val inFlight = ConcurrentHashMap<PageKey, CompletableDeferred<RenderedPage?>>()
    private val lastPage = PageCache(context)
    private var textFace: Typeface? = null
    private var ornamentFace: Typeface? = null

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
                lastPage.save(key.page, key.widthPx, key.theme, key.scale, copy)
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
        palette: PagePalette,
    ): RenderedPage? {
        peek(key)?.let { return it }
        val gate = CompletableDeferred<RenderedPage?>()
        val claimed = inFlight.putIfAbsent(key, gate) == null
        if (!claimed) return inFlight[key]?.await()
        return try {
            val rendered = withContext(Dispatchers.Default) { render(content, key, palette) }
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
        key: PageKey,
        palette: PagePalette,
    ): RenderedPage? {
        val typeface = textTypeface() ?: return null
        val widthPx = key.widthPx
        val pageEm = PageFrame.em(widthPx)
        val fontPx = pageEm * key.scale

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = fontPx
            color = palette.ink.toArgb()
        }
        val metrics = textPaint.fontMetrics
        // The rule is ruled against the text's own ink, so the frame is
        // measured from the font and not from the slots: this face carries
        // its marks above the letters, and a frame ruled off the slots
        // stands on them.
        val inkEm = (metrics.descent - metrics.ascent) / fontPx

        // The page's lines: the print's own, kept where its words still fit
        // the reader's measure, re-set where they do not.
        val pageWords = content.pageWords(key.page)
        val wordsById = HashMap<Int, LayoutWord>(pageWords.size)
        for (word in pageWords) {
            wordsById[word.id] = LayoutWord(
                id = word.id,
                ayah = word.ayah,
                position = word.position,
                marker = word.marker,
                text = word.text,
            )
        }
        val spaceEm = textPaint.measureText(" ") / fontPx
        // An ayah's number stands in a roundel drawn at display time; the
        // engine only reserves its room, which is the roundel's own width
        // and the word gap that stands between it and the word before it.
        val markerEm = (2f * PageFrame.MARKER_EM) / key.scale + spaceEm
        val layout = PageTextLayout.layout(
            lines = content.pageLines(key.page).map { line ->
                LayoutLine(
                    line = line.line,
                    type = line.type,
                    centered = line.centered,
                    firstWordId = line.firstWordId,
                    lastWordId = line.lastWordId,
                    surah = line.surah,
                )
            },
            words = wordsById,
            capacityEm = PageFrame.EM_PER_LINE / key.scale,
            measure = { text -> textPaint.measureText(text) / fontPx },
            spaceEm = spaceEm,
            markerEm = markerEm,
        )

        val frame = PageFrame.of(widthPx, inkEm, lines = layout.lines, scale = key.scale)
        val bitmap = Bitmap.createBitmap(widthPx, frame.heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.paper.toArgb())

        val ornamentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = ornamentTypeface()
            textAlign = Paint.Align.CENTER
            color = palette.ornament.toArgb()
        }
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.ornament.toArgb()
            strokeWidth = maxOf(1f, widthPx / 720f)
            alpha = 130
        }
        // An ayah's number, in the ornament's digits for the same reason the
        // page number uses them: the digits are furniture-sized and tabular,
        // and they sit inside the roundel as the printed page's do.
        val roundelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = ornamentTypeface()
            textSize = pageEm * PageFrame.MARKER_DIGITS_EM
            color = palette.ink.toArgb()
            textAlign = Paint.Align.CENTER
        }
        val roundelRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = palette.ornament.toArgb()
            strokeWidth = maxOf(1f, widthPx / 900f)
            alpha = 170
        }
        val lineHeight = fontPx * PageFrame.LINE_HEIGHT_RATIO
        val textWidth = widthPx * PageFrame.TEXT_WIDTH_RATIO
        // The measure's right edge: the Book is read from it, right to left.
        val measureRight = widthPx - (widthPx - textWidth) / 2f
        val roundelHeight = 2f * PageFrame.MARKER_EM * pageEm
        val roundelPad = pageEm * ROUNDEL_PAD_EM

        // The page's own furniture: the rule the text sits within, the juz at
        // the left of the foot rule, and the page number in its roundel, as
        // the printed page carries them. A manuscript page is *within* gold
        // and black rules, and that rule is also what gives the page an edge
        // on a wide ground: without it the page and the app's background are
        // the same colour and the sheet of paper stops existing off a phone.
        drawPageRule(canvas, frame, rulePaint)
        drawFooter(canvas, key.page, frame, widthPx, pageEm, ornamentPaint, rulePaint, content)

        val ayahs = content.ayahsForPage(key.page).associateBy { it.number }
        val words = ArrayList<WordBox>(pageWords.size)
        val ayahBoxes = HashMap<Int, MutableList<RectF>>()
        val basmallah = content.basmallahText()

        layout.slots.forEachIndexed { index, slot ->
            val slotTop = frame.slotTop + index * lineHeight
            when (slot.kind) {
                SlotKind.SurahName -> {
                    val name = content.surah(slot.surah)?.nameArabic ?: ""
                    ornamentPaint.textSize = fontPx * SURAH_NAME_RATIO
                    val surahMetrics = ornamentPaint.fontMetrics
                    val baseline = slotTop +
                        (lineHeight - (surahMetrics.descent - surahMetrics.ascent)) / 2f -
                        surahMetrics.ascent
                    canvas.drawText(name, widthPx / 2f, baseline, ornamentPaint)
                    canvas.drawLine(
                        widthPx / 2f - textWidth * 0.22f,
                        slotTop + lineHeight * 0.86f,
                        widthPx / 2f + textWidth * 0.22f,
                        slotTop + lineHeight * 0.86f,
                        rulePaint,
                    )
                }
                SlotKind.Basmallah -> {
                    if (basmallah.isEmpty()) return@forEachIndexed
                    // The basmallah is the Book's own four words, drawn with
                    // the page's own face: it is text, and it sets as text.
                    val size = fontPx * BASMALLA_RATIO
                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.textSize = size
                    val basmalahMetrics = textPaint.fontMetrics
                    val baseline = slotTop +
                        (lineHeight - (basmalahMetrics.descent - basmalahMetrics.ascent)) / 2f -
                        basmalahMetrics.ascent
                    canvas.drawText(basmallah, widthPx / 2f, baseline, textPaint)
                }
                SlotKind.Text -> {
                    val baseline = slotTop +
                        (lineHeight - (metrics.descent - metrics.ascent)) / 2f -
                        metrics.ascent
                    val centerY = slotTop + lineHeight / 2f
                    textPaint.textAlign = Paint.Align.LEFT
                    for (placed in slot.words) {
                        // The Book is read right to left: the line's first
                        // word sits at its right edge and every word after
                        // it lays out to the left.
                        val right = measureRight - placed.start * fontPx
                        val word = placed.word
                        if (word.marker) {
                            val roundel = drawRoundel(
                                canvas = canvas,
                                ayah = word.ayah,
                                right = right,
                                gapPx = spaceEm * fontPx,
                                centerY = centerY,
                                height = roundelHeight,
                                pad = roundelPad,
                                paint = roundelPaint,
                                ring = roundelRing,
                            )
                            words += WordBox(
                                id = word.id,
                                ayah = word.ayah,
                                position = word.position,
                                marker = true,
                                left = roundel.left,
                                top = roundel.top,
                                right = roundel.right,
                                bottom = roundel.bottom,
                            )
                            // The ayah's number is part of its ayah: a touch
                            // on the roundel is a touch on the end of the
                            // ayah, the way the printed page's number is.
                            ayahBoxes.getOrPut(word.ayah) { mutableListOf() }.add(roundel)
                            continue
                        }
                        val left = right - placed.width * fontPx
                        canvas.drawText(word.text, left, baseline, textPaint)
                        val box = RectF(
                            left,
                            baseline + metrics.ascent,
                            right,
                            baseline + metrics.descent,
                        )
                        words += WordBox(
                            id = word.id,
                            ayah = word.ayah,
                            position = word.position,
                            marker = false,
                            left = box.left,
                            top = box.top,
                            right = box.right,
                            bottom = box.bottom,
                        )
                        ayahBoxes.getOrPut(word.ayah) { mutableListOf() }.add(box)
                    }
                }
            }
        }
        return RenderedPage(bitmap, words, ayahBoxes, ayahs, palette)
    }

    /**
     * An ayah's number in its roundel, the way the printed page ends an
     * ayah. The roundel is drawn here, at display time, rather than carried
     * in a font: its number is then the app's own text, and a wider number
     * widens its roundel instead of being squeezed into it.
     *
     * [right] is where the marker's atom stands against the previous word's
     * gap, and [gapPx] is that gap: the roundel sits inside its atom, with
     * the gap between the ayah's last word and its number.
     */
    private fun drawRoundel(
        canvas: Canvas,
        ayah: Int,
        right: Float,
        gapPx: Float,
        centerY: Float,
        height: Float,
        pad: Float,
        paint: Paint,
        ring: Paint,
    ): RectF {
        val text = arabicDigits(ayah)
        val digits = paint.measureText(text)
        val roundelRight = right - gapPx
        val left = roundelRight - maxOf(height, digits + pad)
        val top = centerY - height / 2f
        val bottom = centerY + height / 2f
        canvas.drawOval(left, top, roundelRight, bottom, ring)
        val metrics = paint.fontMetrics
        val baseline = centerY + (metrics.descent - metrics.ascent) / 2f - metrics.descent
        canvas.drawText(text, (left + roundelRight) / 2f, baseline, paint)
        return RectF(left, top, roundelRight, bottom)
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
        pageEm: Float,
        paint: Paint,
        rule: Paint,
        content: ContentDatabase,
    ) {
        val top = frame.foot
        canvas.drawLine(frame.left, top, frame.right, top, rule)
        val radius = pageEm * PageFrame.ROUNDEL_EM
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
        paint.textSize = pageEm * PAGE_NUMBER_RATIO
        val metrics = paint.fontMetrics
        val baseline = centerY + (metrics.descent - metrics.ascent) / 2f - metrics.descent
        canvas.drawText(arabicDigits(page), widthPx / 2f, baseline, paint)
        // The juz sits at the left end of the rule, in the same quiet gold.
        val juz = content.pagePosition(page)?.juz ?: return
        paint.textSize = pageEm * HEADER_RATIO
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
     * The page's own face: the one Arabic typeface the app ships, the same
     * that sets the study reading, so the page and the reading are one
     * letterform. It is loaded once and kept: a page is rendered at the
     * reader's own size, and the face carries every size.
     */
    private fun textTypeface(): Typeface? = textFace ?: runCatching {
        Typeface.createFromAsset(context.assets, "fonts/UthmanicHafs_V22.ttf")
    }.onFailure { android.util.Log.w(TAG, "the page's text font could not be read", it) }
        .getOrNull()?.also { textFace = it }

    /** The ornament face: the surah's band and the page's own furniture. */
    private fun ornamentTypeface(): Typeface = ornamentFace ?: ResourcesCompat.getFont(context, R.font.amiri_quran)
        ?.also { ornamentFace = it }
        ?: Typeface.SERIF

    private companion object {
        const val TAG = "PageRenderer"

        /**
         * The rendered pages kept in memory, by the bytes they take: a page
         * set small is about 7 MB and a page set large is half again as
         * much, so a reader with large text holds fewer pages rather than
         * more memory.
         */
        const val MAX_CACHE_BYTES = 48 * 1024 * 1024

        /** The room an ayah's roundel keeps inside itself for its number. */
        const val ROUNDEL_PAD_EM = 0.14f

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
