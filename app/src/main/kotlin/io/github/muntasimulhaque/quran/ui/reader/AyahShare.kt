package io.github.muntasimulhaque.quran.ui.reader

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
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
internal data class ShareCard(
    val arabic: String,
    val translation: List<TextRun>,
    val translationName: String?,
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
): ShareCard = withContext(Dispatchers.IO) {
    val line = viewModel.studyRow(ayah.number)?.translations?.firstOrNull()
    val runs = line?.text?.text
        ?.takeIf { it.isNotBlank() }
        ?.let { text -> RichText.footnotes(text).filter { it.marker == null } }
    val surah = viewModel.surahOf(ayah.number)
    val name = surah?.nameSimple
        ?: context.getString(R.string.surah_fallback_name, ayah.surah)
    ShareCard(
        arabic = ayah.text,
        translation = runs.orEmpty(),
        translationName = line?.packName?.takeIf { runs != null },
        reference = "$name ${ayah.surah}:${ayah.ayah}",
        text = viewModel.ayahShareText(ayah),
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
        ) {
            Text(
                text = card.arabic,
                style = TextStyle(
                    fontFamily = hafs,
                    fontSize = 26.sp,
                    lineHeight = 48.sp,
                    textAlign = TextAlign.Right,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (card.translation.isNotEmpty()) {
                TranslationBody(
                    runs = card.translation,
                    modifier = Modifier.padding(top = Space.Block),
                    sizeSp = 15f,
                    lineSp = 23f,
                    arabicSp = 21f,
                )
            }
            Text(
                text = card.reference,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier
                    .padding(top = if (card.translation.isEmpty()) Space.Block else Space.Line)
                    .align(Alignment.End),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Section),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppMark(mark = mark, ground = lapis, modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.padding(start = Space.Line),
                )
                Spacer(Modifier.weight(1f))
                card.translationName?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
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
 * The card's one appearance: it composes at the foot of the reader's own
 * screen, is recorded into a graphics layer that is never drawn back, and
 * [onImage] receives the pixels two frames later. Nothing of it reaches the
 * eye: the layer has no draw call, the node clears its semantics so TalkBack
 * never meets a card the reader cannot see, and the whole thing leaves the
 * composition the moment it has been read.
 *
 * The child is measured taller than the screen allows, because a long ayah
 * makes a tall card and a picture must never be cut: the node reports the
 * screen's own height, the child stands outside those bounds (nothing here
 * clips), and the layer records the child's full height.
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
    val height = remember { CardHeight() }
    val density = LocalDensity.current
    Layout(
        content = {
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
                AyahShareCardContent(card, Modifier.fillMaxWidth())
            }
        },
        modifier = modifier
            .width(CardWidth)
            .clearAndSetSemantics { }
            .drawWithContent {
                layer.record(size = IntSize(size.width.toInt(), height.px)) {
                    this@drawWithContent.drawContent()
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
        height.px = child.height
        layout(constraints.maxWidth, child.height.coerceAtMost(constraints.maxHeight)) {
            child.place(0, 0)
        }
    }
    LaunchedEffect(layer, card) {
        // Two frames: one for the composition to be measured and recorded,
        // one to be sure the record has finished before it is read back.
        withFrameNanos { }
        withFrameNanos { }
        val image = runCatching { layer.toImageBitmap() }.getOrNull()
        if (image == null || image.width < 2 || image.height < 2) {
            onFailed()
        } else {
            onImage(image)
        }
    }
}

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
 * The chooser with the card as its image and the ayah's text as the caption
 * every receiving app can use. The URI is read-granted to whoever the reader
 * picks, the provider exposes only this one folder, and false (rather than a
 * thrown error) is the answer when anything refuses, because the caller has
 * the plain text ready and a share must never end in nothing.
 */
internal fun startShareCard(context: Context, file: File, text: String): Boolean = runCatching {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, "ayah", uri)
    }
    context.startActivity(Intent.createChooser(intent, null))
    true
}.getOrElse { false }
