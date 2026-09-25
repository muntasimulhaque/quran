package io.github.muntasimulhaque.quran.ui.reader

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.core.TextRun
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.ui.ReaderViewModel
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import io.github.muntasimulhaque.quran.ui.theme.Lapis
import io.github.muntasimulhaque.quran.ui.theme.PaperBackground
import io.github.muntasimulhaque.quran.ui.theme.PaperHairline
import io.github.muntasimulhaque.quran.ui.theme.PaperInk
import io.github.muntasimulhaque.quran.ui.theme.PaperMuted
import io.github.muntasimulhaque.quran.ui.theme.PaperSurface
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.rememberHafs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** The card's own width: one column of reading, the size a phone shows well. */
private val CardWidth = 320.dp

/**
 * One ayah, ready to become a picture: the Arabic, the first translation the
 * reader has on, the reference, and the plain text that rides beside the
 * image for the receiving app to use.
 *
 * The translation is the first enabled pack, the same one search and the
 * plain share text read, so a card and its caption can never disagree. Its
 * footnote markers are dropped from the card: a superscript that cannot be
 * tapped is a number with no door behind it.
 */
/**
 * A share in progress: the card, and whether the reader has asked for the
 * picture. The words-only door never composes a bitmap, and the capture
 * never runs before the choice (D-090).
 */
internal data class ShareRequest(val card: ShareCard, val capture: Boolean = false)

internal data class ShareCard(
    val arabic: String,
    val translation: List<TextRun>,
    val reference: String,
    val text: String,
)

/**
 * Reads everything the card draws, off the main thread. The reference and
 * the plain text are built the same way the share text builds them, so the
 * image and the words say the same ayah.
 */
internal suspend fun loadShareCard(
    context: Context,
    viewModel: ReaderViewModel,
    ayah: Ayah,
): ShareRequest = withContext(Dispatchers.IO) {
    val line = viewModel.studyRow(ayah.number)?.translations?.firstOrNull()
    val runs = line?.text?.text
        ?.takeIf { it.isNotBlank() }
        ?.let { text -> RichText.footnotes(text).filter { it.marker == null } }
    val surah = viewModel.surahOf(ayah.number)
    val name = surah?.nameSimple
        ?: context.getString(R.string.surah_fallback_name, ayah.surah)
    ShareRequest(
        ShareCard(
            arabic = ayah.text,
            translation = runs.orEmpty(),
            reference = "$name ${ayah.surah}:${ayah.ayah}",
            text = viewModel.ayahShareText(ayah),
        ),
    )
}

/**
 * The card as an image: the ayah in the reading's own Arabic face, its
 * translation under it, the reference under that, and the app's mark at the
 * foot.
 *
 * The card wears the manuscript paper whatever theme the reader is reading
 * in: this picture leaves the app and lands in a chat or a feed, and the
 * app's own face is what the people there should see. It is drawn at one
 * fixed text scale for the same reason: a picture is one artifact shared
 * with everyone, while the reading itself stays as large as the reader asked
 * for. The colors are the theme's own (D-010), reached through a
 * MaterialTheme of the paper, so the shared card and the app are one hand.
 */
@Composable
internal fun AyahShareCardContent(card: ShareCard, modifier: Modifier = Modifier) {
    val hafs = rememberHafs()
    // The launcher's star, drawn from the same resource the home screen
    // draws, so the mark on the card is the mark of the app and not a
    // second icon drawn from memory.
    val mark = painterResource(R.mipmap.ic_launcher_fg)
    val lapis = colorResource(R.color.icon_background)
    val paper = lightColorScheme(
        primary = Lapis,
        background = PaperBackground,
        onBackground = PaperInk,
        surface = PaperSurface,
        onSurface = PaperInk,
        onSurfaceVariant = PaperMuted,
        outline = PaperHairline,
    )
    MaterialTheme(colorScheme = paper) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(start = 26.dp, end = 26.dp, top = 30.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The card is one plaque, and everything on it is centered: the
            // reference that names the ayah, the ayah, its translation, and
            // the app's own foot. Before this, four different alignments met
            // on one narrow card (owner report, D-090): the Arabic right,
            // the translation left, the reference right, the name left.
            Text(
                text = card.reference,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = card.arabic,
                style = TextStyle(
                    fontFamily = hafs,
                    fontSize = 26.sp,
                    lineHeight = 48.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Block),
            )
            if (card.translation.isNotEmpty()) {
                TranslationBody(
                    runs = card.translation,
                    modifier = Modifier.padding(top = Space.Block),
                    sizeSp = 15f,
                    lineSp = 23f,
                    arabicSp = 21f,
                    centered = true,
                )
            }
            Box(
                Modifier
                    .padding(top = Space.Section)
                    .fillMaxWidth(0.22f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            )
            Row(
                modifier = Modifier.padding(top = Space.Block),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppMark(mark = mark, ground = lapis, modifier = Modifier.size(20.dp))
                Text(
                    // The card carries the store's own name, not the
                    // launcher's: this picture leaves the app and lands
                    // where nobody knows what the icon on the home screen
                    // says (owner decision, 28).
                    text = stringResource(R.string.share_card_name),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = Space.Line),
                )
            }
        }
    }
}

/** The app's mark at a foot's size: the drawn star on its lapis square. */
@Composable
private fun AppMark(mark: Painter, ground: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 22))
            .background(ground),
    ) {
        Image(
            painter = mark,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

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
 * for the guarantee (owner report, D-097).
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
    val widthPx = with(density) { CardWidth.roundToPx() }
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
            .width(CardWidth)
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
            if (image == null || image.width < 1 || image.height < 1) {
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
            // directly (owner report, D-097). Below API 26 there is no
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
 * once printed into the picture and once as type (owner report, D-090). A
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

/**
 * The share sheet: the card as the reader will send it, and the two doors.
 *
 * The card used to leave the app sight unseen, with its text welded to the
 * picture as a caption; a reader could not know what they were sending or
 * choose the plain words instead (owner report, D-090). Here the real card
 * is drawn at its own scale as a preview, and the doors are named: **Share
 * image** (the default) and **Share text**. The preview is the same
 * composable the capture draws, so what the reader sees is what the picture
 * will be, not a second drawing that can drift from it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AyahShareSheet(
    card: ShareCard,
    onShareImage: () -> Unit,
    onShareText: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
                .testTag("share-sheet"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = card.reference,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, bottom = Space.Block),
            )
            // The preview keeps the card's own measure and corners, so the
            // reader looks at the artifact rather than at a description of
            // it. It scrolls with the sheet if an ayah makes it tall.
            ShareCardPreviewBox(card)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = Space.Section),
                horizontalArrangement = Arrangement.spacedBy(Space.Line),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    label = stringResource(R.string.share_image),
                    onClick = onShareImage,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    label = stringResource(R.string.share_text),
                    onClick = onShareText,
                    quiet = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** The card, drawn as it will be sent: the preview's own measure, no capture. */
@Composable
internal fun ShareCardPreview(card: ShareCard) {
    Box(
        modifier = Modifier
            .width(CardWidth)
            .clip(RoundedCornerShape(18.dp)),
    ) {
        AyahShareCardContent(card)
    }
}

/**
 * The preview as the share sheet shows it: the card's own measure behind a
 * height cap, with the cap on the viewport rather than on the card.
 *
 * Modifier order is the whole of it. `heightIn` before `verticalScroll`
 * caps the viewport, so a taller card still measures at its real height and
 * the reader scrolls to the foot of it; the other order caps the card
 * itself, and the clip cut a long ayah off at the cap with no scroll to
 * reach the rest (owner report, D-097). The corner clip stays outside the
 * cap so the sheet's own shape is cut once.
 */
@Composable
internal fun ShareCardPreviewBox(card: ShareCard) {
    Box(
        modifier = Modifier
            .padding(horizontal = 22.dp)
            .clip(RoundedCornerShape(18.dp))
            .heightIn(max = 340.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        ShareCardPreview(card)
    }
}
