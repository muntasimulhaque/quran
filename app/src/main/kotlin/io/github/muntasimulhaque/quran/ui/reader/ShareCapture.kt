package io.github.muntasimulhaque.quran.ui.reader

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.core.content.FileProvider
import java.io.File

/** The measured height of the card, which can stand taller than the screen. */
private class CardHeight(var px: Int = 0)

/**
 * The tallest slice the capture asks the GPU to hold at once.
 *
 * `GraphicsLayer.toImageBitmap` produces a *hardware* bitmap on API 28 and
 * up, and a hardware bitmap is a texture: the driver refuses or clips
 * anything taller than its own texture limit, which is 4,096 px on much of
 * the mid range hardware this app runs on and 8,192 or 16,384 px on the rest.
 * A long ayah at a large text scale passes 4,096 px easily, so the capture
 * takes the card in slices this tall and stitches them into one software
 * bitmap. The limit is deliberately under the smallest known texture size,
 * and one slice covers every ordinary card, so the common case pays nothing
 * for the guarantee (owner report).
 */
private const val CaptureSlicePx = 2048

/**
 * The card's one appearance: it composes at the foot of the reader's own
 * screen, is recorded into a graphics layer that is never drawn back, and
 * [onImage] receives the whole card. Nothing of it reaches the eye: the
 * layer has no draw call, the node clears its semantics so TalkBack never
 * meets a card the reader cannot see, and the whole thing leaves the
 * composition the moment it has been read.
 *
 * The child is measured taller than the screen allows, because a long ayah
 * makes a tall card and a picture must never be cut: the node reports the
 * screen's own height, the child stands outside those bounds (nothing here
 * clips), and the layer records the card slice by slice. Each slice moves
 * the card up by its own height and records one window of it, and the
 * windows are stitched into a single software bitmap, so the output is the
 * card's real size on every device, whatever the GPU's texture limit is.
 *
 * If any of it fails, [onFailed] is called and the share falls back to the
 * plain text, so the reader never meets a Share that did nothing.
 */
@Composable
internal fun ShareCardCapture(
    card: ShareCard,
    onImage: (ImageBitmap) -> Unit,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current
    val widthPx = with(density) { ShareCardWidth.roundToPx() }
    // The card's full height, reported by the measuring child, and the slice
    // the capture is photographing right now. Both are Compose state: the
    // effect below must re-run when the card has been measured, and must
    // walk a new slice on every pass.
    var cardHeight by remember { mutableIntStateOf(0) }
    var slice by remember { mutableStateOf(Slice(top = 0, height = 0)) }
    Layout(
        content = {
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
                AyahShareCardContent(
                    card,
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size -> cardHeight = size.height },
                )
            }
        },
        modifier = modifier
            .width(ShareCardWidth)
            .clearAndSetSemantics { }
            .drawWithContent {
                val tall = slice.height.takeIf { it > 0 } ?: cardHeight
                if (tall > 0) {
                    layer.record(size = IntSize(size.width.toInt(), tall)) {
                        this@drawWithContent.drawContent()
                    }
                }
            },
    ) { measurables, constraints ->
        val child = measurables[0].measure(
            Constraints(
                minWidth = 0,
                maxWidth = constraints.maxWidth,
                minHeight = 0,
                maxHeight = Constraints.Infinity,
            ),
        )
        val tall = slice.height.takeIf { it > 0 } ?: child.height
        layout(constraints.maxWidth, tall.coerceAtMost(constraints.maxHeight)) {
            child.place(0, -slice.top)
        }
    }
    LaunchedEffect(layer, card, widthPx, cardHeight) {
        val total = cardHeight
        if (total <= 0 || widthPx <= 0) return@LaunchedEffect
        // A software bitmap has no texture limit, so the stitched card is the
        // card, whatever the driver would have held.
        val stitched = runCatching {
            Bitmap.createBitmap(widthPx, total, Bitmap.Config.ARGB_8888)
        }.getOrNull()
        if (stitched == null) {
            onFailed()
            return@LaunchedEffect
        }
        val canvas = android.graphics.Canvas(stitched)
        var top = 0
        while (top < total) {
            val tall = minOf(CaptureSlicePx, total - top)
            slice = Slice(top = top, height = tall)
            // Two frames: one for the slice to be placed and recorded, one to
            // be sure the record has finished before it is read back.
            withFrameNanos { }
            withFrameNanos { }
            val image = runCatching { layer.toImageBitmap() }.getOrNull()
            // The width of a bitmap Android has already released throws
            // rather than answering, so the read is guarded as well as the
            // capture: a share card that cannot be drawn is a card the reader
            // is not sent, and a crash is not how the app says so.
            val pixels = runCatching { image?.let { it.width to it.height } }.getOrNull()
            if (image == null || pixels == null || pixels.first < 1 || pixels.second < 1) {
                stitched.recycle()
                onFailed()
                return@LaunchedEffect
            }
            // The read-back is a hardware bitmap on API 28 and up, and a
            // software canvas refuses one ("Software rendering doesn't
            // support hardware bitmaps"). The slice is copied to a software
            // bitmap first: the slice is under the texture limit by
            // construction, so the copy is the whole window, and the
            // stitched result is software, which the PNG writer can encode
            // directly (owner report). Below API 26 there is no
            // hardware config at all, so the copy is skipped.
            val source = image.asAndroidBitmap()
            val piece = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                source.config == Bitmap.Config.HARDWARE
            ) {
                source.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                source
            }
            if (piece == null) {
                stitched.recycle()
                onFailed()
                return@LaunchedEffect
            }
            canvas.drawBitmap(piece, 0f, top.toFloat(), null)
            if (piece !== source) piece.recycle()
            top += tall
        }
        onImage(stitched.asImageBitmap())
    }
}

/** One window of the card: where it starts, and how tall it is. */
private data class Slice(val top: Int, val height: Int)

/**
 * Writes the card where the content provider can hand it out: one PNG in the
 * app's own cache, overwritten each time, never anywhere else. Null when the
 * bytes cannot be written (a full cache); the caller then shares the text.
 */
internal fun writeShareCard(context: Context, image: ImageBitmap): File? = runCatching {
    val directory = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(directory, "ayah.png")
    file.outputStream().buffered().use { stream ->
        val written = image.asAndroidBitmap()
            .compress(Bitmap.CompressFormat.PNG, 100, stream)
        check(written) { "the share card could not be encoded" }
    }
    file
}.getOrNull()

/**
 * The chooser with the card as its image. The image goes alone: the plain
 * text used to ride along as a caption, which put the ayah in a chat twice,
 * once printed into the picture and once as type (owner report). A
 * reader who wants the words has their own door for them, `shareAyahText`.
 * The URI is read-granted to whoever the reader picks, the provider exposes
 * only this one folder, and false (rather than a thrown error) is the answer
 * when anything refuses, because the caller has the plain text ready and a
 * share must never end in nothing.
 */
internal fun startShareCard(context: Context, file: File): Boolean = runCatching {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, "ayah", uri)
    }
    context.startActivity(Intent.createChooser(intent, null))
    true
}.getOrElse { false }

/**
 * The same ayah as words, with no picture: the plain text alone, for a reader
 * who is writing a message rather than posting a card. It is the second door
 * of the share sheet, never the default (owner decision, 28).
 */
internal fun shareAyahText(context: Context, text: String): Boolean = runCatching {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
    true
}.getOrElse { false }
