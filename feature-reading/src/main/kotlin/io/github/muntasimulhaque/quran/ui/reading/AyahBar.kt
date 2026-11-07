package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.kit.floatingLift
import io.github.muntasimulhaque.quran.ui.theme.Radii

/**
 * The height of a verse's head row, and of the bar that stands in it. The row
 * is this tall whether the bar is up or not, so raising the bar costs no
 * layout at all: nothing moves, the paper does not re-measure, and the change
 * is a fade in one already-measured line (owner decision, this build).
 */
internal val AyahBarHeight = 48.dp

/** The least a cell may be: the finger's floor, as the old bar had it. */
private val MinCell = 48.dp

/** The air inside the capsule, left and right of its cells. */
private val BarPad = 6.dp

/** The air between two cells. */
private val CellGap = 2.dp

/** The air a cell keeps above its mark and under its word. */
private val CellPad = 3.dp

/**
 * What one verse's bar can do. The reading places the bar; the app owns the
 * acts, so the same five calls answer the bar's cells and the five-row list
 * the large-text reader gets (owner decision, this build).
 *
 * [tafsir] is false when the reader has no tafsir on the device. The bar then
 * carries four cells, and its width is measured on those four, so a reader
 * without a commentary never sees a door with nothing behind it.
 */
class AyahBarActions(
    val tafsir: Boolean = true,
    val play: (Ayah) -> Unit = {},
    val tafsirOn: (Ayah) -> Unit = {},
    val save: (Ayah) -> Unit = {},
    val note: (Ayah) -> Unit = {},
    val share: (Ayah) -> Unit = {},
)

/**
 * The verse's own actions, as the pill the reading used to raise: one capsule,
 * a mark above its word, every cell the width of the widest word so five
 * labels press in five identical cells instead of five ovals of their own
 * making.
 *
 * The construction is the old bar's, kept because it is the one that fits.
 * Measured in the app's faces at the default text, it is 290 dp in English and
 * 290 in Bangla, inside the 304 dp a 360 dp phone gives its words; a capsule of
 * words alone is 265 and 329, so it breaks in Bangla, and a capsule of marks
 * beside their words is 325 and 386, so it breaks in English. The mark above
 * the word is what keeps the cell as wide as the wider of the two and not as
 * wide as both (owner decision, this build).
 *
 * It never scrolls. The same measurement that sizes the cells decides whether
 * the bar is drawn at all: past the width, or past the default text scale, the
 * reader gets the five-row list the app already has, and no action is ever
 * hidden behind a sideways drag on a page that scrolls down.
 *
 * [onAct] closes the bar first and then does the work, the pill's own rule:
 * one panel asks one question and the tap answers it.
 */
@Composable
internal fun AyahBar(
    actions: AyahBarActions,
    /** The verse is already kept, so its cell says what the tap will undo. */
    saved: Boolean,
    /** The reader wrote a note on this verse, so its cell says so. */
    noted: Boolean,
    onAct: ((Ayah) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = barLabels(actions)
    val cell = rememberAyahBarCell(actions)
    val ink = MaterialTheme.colorScheme.primary
    val on = stringResource(R.string.reading_action_state_on)
    val off = stringResource(R.string.reading_action_state_off)
    Row(
        modifier = modifier
            .height(AyahBarHeight)
            .testTag("ayah-bar")
            .clip(Radii.Pill)
            .floatingLift(Radii.Pill)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = BarPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarCell(Icon.Play, labels.play, cell, ink) { onAct(actions.play) }
        if (actions.tafsir) {
            BarCell(Icon.Tafsir, labels.tafsir, cell, ink) { onAct(actions.tafsirOn) }
            // The verse's own acts end here; what the reader does with the
            // verse follows. One hairline, one meaning.
            Box(
                Modifier
                    .padding(horizontal = CellGap)
                    .width(1.dp)
                    .height(18.dp)
                    .background(MaterialTheme.colorScheme.outline),
            )
        }
        BarCell(
            icon = if (saved) Icon.BookmarkFilled else Icon.Bookmark,
            label = labels.save,
            width = cell,
            ink = ink,
            state = if (saved) on else off,
        ) { onAct(actions.save) }
        BarCell(
            icon = Icon.Note,
            label = labels.note,
            width = cell,
            ink = ink,
            state = if (noted) on else off,
        ) { onAct(actions.note) }
        BarCell(Icon.Share, labels.share, cell, ink) { onAct(actions.share) }
    }
}

/**
 * The one width every cell of a bar shares: the widest word of the five plus
 * the cell's own padding, never under the finger's floor. Measured in the
 * reader's language and at the reader's text size, so a Bangla bar measures
 * Bangla words and a grown text measures grown ones.
 */
@Composable
internal fun rememberAyahBarCell(actions: AyahBarActions): Dp {
    val labels = barLabels(actions)
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelMedium
    val density = LocalDensity.current
    return remember(labels, style, density) {
        val widest = labels.all.maxOfOrNull { measurer.measure(it, style).size.width } ?: 0
        with(density) {
            (maxOf(widest, GlyphSize.Action.roundToPx()).toDp() + 12.dp)
                .coerceAtLeast(MinCell)
        }
    }
}

/** The width a bar will take on the reading's line, for the fit check. */
@Composable
internal fun rememberAyahBarWidth(actions: AyahBarActions): Dp =
    barWidth(rememberAyahBarCell(actions), if (actions.tafsir) 5 else 4, actions.tafsir)

internal fun barWidth(cell: Dp, cells: Int, divider: Boolean): Dp =
    cell * cells + (if (divider) CellGap * 2 else 0.dp) + BarPad * 2

/**
 * Whether the bar is the right shape at this size.
 *
 * Two answers, both arithmetic: the bar has to fit the line the reading gives
 * its words, and its labels have to stand at the default text size. The second
 * is the head row's own guard: the row is one fixed height so opening costs no
 * layout, and a label grown past the row would have to grow it. A reader at a
 * larger text size gets the list, which is the surface that grows (owner
 * decision, this build).
 */
internal fun barFits(width: Dp, line: Dp, fontScale: Float): Boolean =
    width <= line && fontScale <= 1.1f

@Composable
private fun BarCell(
    icon: Icon,
    label: String,
    width: Dp,
    ink: Color,
    /** The mark is a color; this is its sentence, on the marked cells. */
    state: String? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .clip(Radii.Pill)
            .clickable(onClick = onClick)
            .semantics {
                if (state != null) stateDescription = state
            }
            .padding(vertical = CellPad),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconGlyph(icon, ink, Modifier.size(GlyphSize.Action))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ink,
            maxLines = 1,
        )
    }
}

/** The five words of one bar, read once so the measure and the drawing agree. */
private class BarLabels(
    val play: String,
    val tafsir: String,
    val save: String,
    val note: String,
    val share: String,
) {
    val all: List<String> get() = listOf(play, tafsir, save, note, share)
}

@Composable
private fun barLabels(actions: AyahBarActions): BarLabels = BarLabels(
    play = stringResource(R.string.reading_bar_play),
    tafsir = stringResource(R.string.reading_bar_tafsir),
    save = stringResource(R.string.reading_bar_save),
    note = stringResource(R.string.reading_bar_note),
    share = stringResource(R.string.reading_bar_share),
)
