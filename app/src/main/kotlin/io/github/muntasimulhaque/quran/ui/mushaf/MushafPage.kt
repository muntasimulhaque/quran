package io.github.muntasimulhaque.quran.ui.mushaf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.Word
import io.github.muntasimulhaque.quran.ui.theme.Gold
import io.github.muntasimulhaque.quran.ui.theme.PaperBackground
import io.github.muntasimulhaque.quran.ui.theme.PaperInk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// The page fonts are pre-justified: a full line's glyphs sum to 15.6 em, and
// the line height follows the font's own vertical metrics.
private const val EM_PER_LINE = 15.6f
private const val LINE_HEIGHT_RATIO = 1.644f
private const val BASMALA = "\u0628\u0650\u0633\u0652\u0645\u0650 \u0671\u0644\u0644\u0651\u064e\u0647\u0650 " +
    "\u0671\u0644\u0631\u0651\u064e\u062d\u0652\u0645\u064e\u0670\u0646\u0650 \u0671\u0644\u0631\u0651\u064e\u062d\u0650\u064a\u0645\u0650"

/**
 * A small LRU of rendered pages: the current page and its neighbours, so a
 * page turn is a texture draw instead of a render.
 */
private object PageBitmaps {
    private val cache = LruCache<Int, Bitmap>(5)

    fun get(page: Int): Bitmap? = cache.get(page)
    fun put(page: Int, bitmap: Bitmap) = cache.put(page, bitmap)
}

@Composable
fun MushafPage(content: ContentDatabase, page: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 14.dp, end = 14.dp, top = 52.dp, bottom = 66.dp),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }.roundToInt()
        val bitmap by produceState(initialValue = PageBitmaps.get(page), page, widthPx) {
            val cached = PageBitmaps.get(page)
            if (cached != null) {
                value = cached
                return@produceState
            }
            val rendered = withContext(Dispatchers.Default) {
                renderPage(context, content, page, widthPx)
            }
            PageBitmaps.put(page, rendered)
            value = rendered
        }
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            )
        }
    }
}

private fun renderPage(context: Context, content: ContentDatabase, page: Int, widthPx: Int): Bitmap {
    val textWidth = widthPx * 0.94f
    val fontPx = textWidth / EM_PER_LINE
    val lineHeight = fontPx * LINE_HEIGHT_RATIO
    val height = (lineHeight * 15f).roundToInt()
    val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(PaperBackground.toArgb())

    val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.createFromAsset(context.assets, "fonts/pages/p$page.ttf")
        textSize = fontPx
        color = PaperInk.toArgb()
    }
    val studyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.createFromAsset(context.assets, "fonts/UthmanicHafs_V22.ttf")
        color = PaperInk.toArgb()
        textAlign = Paint.Align.CENTER
    }
    val ornamentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ResourcesCompat.getFont(context, R.font.amiri_quran)
        textSize = fontPx * 0.9f
        color = Gold.toArgb()
        textAlign = Paint.Align.CENTER
    }
    val rulePaint = Paint().apply {
        color = Gold.toArgb()
        strokeWidth = 1.5f
        alpha = 140
    }

    val lines = content.pageLines(page)
    val ayahLines = lines.filter { it.type == "ayah" }
    val wordsById = HashMap<Int, Word>()
    if (ayahLines.isNotEmpty()) {
        val first = ayahLines.minOf { it.firstWordId }
        val last = ayahLines.maxOf { it.lastWordId }
        for (word in content.words(first, last)) wordsById[word.id] = word
    }
    val side = (widthPx - textWidth) / 2f

    for (line in lines) {
        val slotTop = (line.line - 1) * lineHeight
        when (line.type) {
            "surah_name" -> {
                val name = content.surah(line.surah)?.nameArabic ?: ""
                val metrics = ornamentPaint.fontMetrics
                val baseline = slotTop + (lineHeight - (metrics.descent - metrics.ascent)) / 2f - metrics.ascent
                canvas.drawText(name, widthPx / 2f, baseline, ornamentPaint)
                canvas.drawLine(
                    side + textWidth * 0.28f,
                    slotTop + lineHeight * 0.84f,
                    side + textWidth * 0.72f,
                    slotTop + lineHeight * 0.84f,
                    rulePaint,
                )
            }
            "basmallah" -> {
                studyPaint.textSize = fontPx * 0.8f
                val metrics = studyPaint.fontMetrics
                val baseline = slotTop + (lineHeight - (metrics.descent - metrics.ascent)) / 2f - metrics.ascent
                canvas.drawText(BASMALA, widthPx / 2f, baseline, studyPaint)
            }
            else -> {
                val words = buildString {
                    for (id in line.firstWordId..line.lastWordId) {
                        wordsById[id]?.let { append(it.glyph) }
                    }
                }
                pagePaint.textAlign = if (line.centered) Paint.Align.CENTER else Paint.Align.RIGHT
                val metrics = pagePaint.fontMetrics
                val baseline = slotTop + (lineHeight - (metrics.descent - metrics.ascent)) / 2f - metrics.ascent
                val x = if (line.centered) widthPx / 2f else side + textWidth
                canvas.drawText(words, x, baseline, pagePaint)
            }
        }
    }
    return bitmap
}
