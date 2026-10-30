package io.github.muntasimulhaque.quran.ui.reader

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.core.TextRun
import io.github.muntasimulhaque.quran.core.localizedDigits
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.ui.ReaderViewModel
import io.github.muntasimulhaque.quran.ui.rich.TranslationBody
import io.github.muntasimulhaque.quran.ui.rich.TajweedText
import io.github.muntasimulhaque.quran.ui.theme.amiriDisplay
import io.github.muntasimulhaque.quran.ui.theme.arabicStyle
import io.github.muntasimulhaque.quran.ui.theme.PaperTajweed
import io.github.muntasimulhaque.quran.ui.theme.Gold
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

/** The card's own width: one column of reading, the size a phone shows well. */
internal val ShareCardWidth = 320.dp

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
 * never runs before the choice.
 */
internal data class ShareRequest(val card: ShareCard, val capture: Boolean = false)

internal data class ShareCard(
    val arabic: String,
    val tajweed: String = "",
    val translation: List<TextRun>,
    val reference: String,
    val text: String,
    /** The surah's own name, drawn in the ornament's hand when it is here. */
    val surahArabic: String? = null,
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
    val line = viewModel.readingRow(ayah.number)?.translations?.firstOrNull()
    val runs = line?.text?.text
        ?.takeIf { it.isNotBlank() }
        ?.let { text -> RichText.footnotes(text).filter { it.marker == null } }
    val surah = viewModel.surahOf(ayah.number)
    val name = surah?.nameSimple
        ?: context.getString(R.string.surah_fallback_name, ayah.surah)
    val locale = java.util.Locale.forLanguageTag(viewModel.readingLanguage)
    ShareRequest(
        ShareCard(
            arabic = ayah.text,
            tajweed = ayah.tajweed,
            translation = runs.orEmpty(),
            reference = localizedDigits("$name ${ayah.surah}:${ayah.ayah}", locale),
            text = viewModel.ayahShareText(ayah),
            surahArabic = surah?.nameArabic,
        ),
    )
}

/**
 * The card as an image: the surah's own name, the reference that names the
 * ayah, the ayah in the reading's own Arabic face, its translation under it,
 * and the app's mark at the foot.
 *
 * The card wears the manuscript paper whatever theme the reader is reading
 * in: this picture leaves the app and lands in a chat or a feed, and the
 * app's own face is what the people there should see. Its type is fixed and
 * never follows the phone's font size, for the same reason: a picture is one
 * artifact shared with everyone, while the reading itself stays as large as
 * the reader asked for. The colors are the theme's own, reached through a
 * MaterialTheme of the paper, so the shared card and the app are one hand.
 */
@Composable
internal fun AyahShareCardContent(card: ShareCard, modifier: Modifier = Modifier) {
    val hafs = rememberHafs()
    // The app's mark, sized for a foot: the launcher's own drawing, in the
    // theme's ink on the card's own paper (owner decision, this session: at
    // a foot's size one ink reads where the thin ornament gold did not).
    val mark = painterResource(R.drawable.ic_share_mark)
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
                .padding(start = 26.dp, end = 26.dp, top = 30.dp, bottom = Space.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The card is one plaque with two axes and one center line. The
            // labels stand on the center: the surah's own name in the
            // ornament's hand, the reference that names the ayah, and the
            // app's foot. Each body of text stands on the edge its script
            // reads from: the ayah on the right, because Arabic is read from
            // the right, and its translation at the start, because English
            // and Bangla are read from the left. Before this the whole card
            // was centered, which put a four-line translation on a ragged
            // axis with no straight edge, and before that four alignments met
            // with the labels alternating too (owner report); this is the
            // card as the reading draws it, a verse on its own edge and its
            // meaning at the page's (owner decision, this session).
            card.surahArabic?.takeIf { it.isNotBlank() }?.let { arabicName ->
                Text(
                    text = arabicName,
                    style = amiriDisplay(22f, Gold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = card.reference,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (card.surahArabic.isNullOrBlank()) 0.dp else 6.dp),
            )
            TajweedText(
                // The card is paper whatever the reader's page. The theme's
                // own palette would put the night tones on this white ground
                // (owner report, the QPC rebuild audit).
                text = AnnotatedString(card.arabic),
                code = card.tajweed,
                colors = PaperTajweed,
                enabled = true,
                style = arabicStyle(hafs, 26f, 48f, MaterialTheme.colorScheme.onSurface),
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.Block),
            )
            if (card.translation.isNotEmpty()) {
                TranslationBody(
                    runs = card.translation,
                    // The meaning sits close under the verse it renders, one
                    // row's gap, while the header above the verse keeps a
                    // block's: the gaps say what belongs with what (owner
                    // decision, this session).
                    modifier = Modifier.padding(top = Space.Row),
                    sizeSp = 15f,
                    lineSp = 23f,
                    arabicSp = 21f,
                    // The card is parchment whatever page the reader is on,
                    // so its words wear the paper ink and never the night
                    // page's softer tone.
                    color = PaperInk,
                )
            }
            Row(
                modifier = Modifier.padding(top = Space.Section),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppMark(mark = mark, modifier = Modifier.size(20.dp))
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

/**
 * The app's mark at a foot's size: the launcher's own drawing, the whole mark
 * in the theme's ink, carried by the drawable on the card's own paper, so the
 * foot is the same drawing the home screen shows, never a tint of it. The
 * same shape fills the reminder's small icon, where the shade paints it in
 * the notification color, because a small icon is read as an alpha mask.
 */
@Composable
private fun AppMark(mark: Painter, modifier: Modifier = Modifier) {
    Image(
        painter = mark,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
