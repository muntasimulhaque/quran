package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.ScaleRow
import io.github.muntasimulhaque.quran.ui.kit.focusRing
import io.github.muntasimulhaque.quran.ui.theme.NightBackground
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.NightText
import io.github.muntasimulhaque.quran.ui.theme.PaperBackground
import io.github.muntasimulhaque.quran.ui.theme.PaperInk
import io.github.muntasimulhaque.quran.ui.theme.TypeLadder
import io.github.muntasimulhaque.quran.ui.theme.amiriDisplay
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.rememberHafs

/**
 * The three grounds, as swatches: Light, Dark, and System, which follows the
 * phone's own day and night. The ring stands on the stored choice, the same
 * as every other single choice in the sheet: tapping System rings System,
 * and Light and Dark carry no ring while it is chosen (owner decision). The
 * phone following underneath is used only where the colors are picked, never
 * for any mark or label here.
 */
@Composable
fun ThemeRow(
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.Block, vertical = Space.Tight),
    ) {
        AppTheme.entries.forEach { theme ->
            val press = remember(theme) { MutableInteractionSource() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(Radii.Card)
                    .focusRing(press, Radii.Card)
                    .selectable(
                        selected = theme == selected,
                        interactionSource = press,
                        role = Role.RadioButton,
                    ) {
                        onSelect(theme)
                    }
                    .padding(horizontal = 6.dp, vertical = Space.Line),
            ) {
                ThemeSwatch(theme = theme, chosen = theme == selected)
                Text(
                    text = theme.name(),
                    // A swatch's name is a label, not a row's value: it wears
                    // the label's Medium at 12, where the value under a row
                    // wears the caption's Regular (owner decision, this
                    // session).
                    style = MaterialTheme.typography.labelMedium,
                    color = if (theme == selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = Space.Line),
                )
            }
        }
    }
}

/**
 * One ground as a swatch. Light and Dark are the page they will paint, with
 * the ayah's own letter standing on it; System is the two halves of the
 * phone's own day and night, split down the middle, the mark the whole
 * industry uses for "follow the system" and the one thing a single filled
 * circle cannot say (owner decision, this session).
 *
 * The chosen swatch wears its halo outside the fill, with a gap: a ring on
 * the fill's own edge read as a border and ate the page's color where the eye
 * looks for it, and a ring around the fill reads as the choice (owner report).
 * The edge is the page's own shade, not the theme's hairline, so a pale page
 * on a pale sheet and a dark page on a dark sheet both keep their circle
 * (owner report).
 */
@Composable
private fun ThemeSwatch(theme: AppTheme, chosen: Boolean) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .then(
                if (chosen) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(46.dp)) {
                val radius = size.minDimension / 2f
                when (theme) {
                    AppTheme.Light -> drawCircle(PaperBackground, radius = radius)
                    AppTheme.Dark -> drawCircle(NightBackground, radius = radius)
                    // The left half is the day page and the right half the
                    // night, so the swatch says "the phone decides" without
                    // a word. Angles run from three o'clock, clockwise: 90 to
                    // 270 is the left half, 270 to 90 the right.
                    AppTheme.System -> {
                        drawArc(PaperBackground, startAngle = 90f, sweepAngle = 180f, useCenter = true)
                        drawArc(NightBackground, startAngle = 270f, sweepAngle = 180f, useCenter = true)
                    }
                }
            }
            // The letter stands on a single ground, so it is drawn only where
            // there is one: on System the split is the mark and a letter
            // straddling both halves would read as neither.
            if (theme != AppTheme.System) {
                // The letter is drawn in the Quran's display hand, never in
                // the phone's default Arabic face: a swatch that names the
                // page must wear the page's own letter (owner decision, this
                // session).
                Text(
                    text = "\u0627",
                    style = amiriDisplay(TypeLadder.SWATCH_SP),
                    color = if (theme == AppTheme.Dark) NightText else PaperInk,
                )
            }
        }
    }
}

@Composable
fun AppTheme.name(): String = stringResource(
    when (this) {
        AppTheme.Light -> R.string.settings_theme_light
        AppTheme.Dark -> R.string.settings_theme_dark
        AppTheme.System -> R.string.settings_theme_system
    },
)

/**
 * The size of one kind of text: its name and its scale on one line, the ends
 * showing the smallest and the largest the kind can be, and the chosen stop
 * filled. The sample above the rows is the reader's own ayah, so a change is
 * judged on the page it is about to change.
 *
 * The row used to be a name with a 250 dp pill of five 48 dp cells under it, a
 * line of its own for every kind of text. The cells were the targets the
 * accessibility rule asked for, and the name moved above them for it; the rail
 * is one target that fits beside the name, so four kinds of text come down by
 * a line each and the page still offers every size (owner decision, this
 * session). The smallest and largest marks at the ends say which way the scale
 * runs, and the chosen stop's halo says where the reader is.
 *
 * **The four rails are one length** (owner report, this session). The label
 * and the two end marks used to take their own intrinsic width, so "Quran
 * text" left a longer rail than "Word meanings" and the page read as four
 * different controls. The label now keeps a fixed column and each mark a
 * fixed slot, both scaled with the system font scale, so the rail between
 * them is the same on every row of a given page. On a tablet the page is
 * wider and every rail is longer together; the equality is what the eye
 * reads, not one dp value. The label's single line ellipsizes if a translation
 * or a larger font outgrows the column, and the marks stay centred in their
 * slots so a wide Arabic alef and a narrow Latin A start the rail on one
 * line.
 */
private val SizeLabel = 120.dp

/** The slot one end mark keeps, so the rail's two ends never move. */
private val SizeMarkSlot = 24.dp

@Composable
fun SizeRow(
    role: TypeRole,
    step: Float,
    /** The room the block keeps from its surface's edges, the app's one gutter. */
    gutter: Dp = Space.Gutter,
    onChange: (Float) -> Unit,
) {
    val label = stringResource(
        when (role) {
            TypeRole.Arabic -> R.string.settings_size_arabic
            TypeRole.Translation -> R.string.settings_size_translation
            TypeRole.Tafsir -> R.string.settings_size_tafsir
            TypeRole.Words -> R.string.settings_size_words
        },
    )
    val scale = if (role == TypeRole.Arabic || role == TypeRole.Words) 0.75f else 1f
    val fontScale = LocalDensity.current.fontScale
    val labelSlot = SizeLabel * fontScale
    val markSlot = SizeMarkSlot * fontScale
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = gutter, end = gutter, top = Space.Block)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            // A control's name wears the same words every other row's name
            // wears, so the sheet reads as one voice (owner decision, the
            // theme and consistency session). The fixed column is the rail's
            // leading line: the four names may be different lengths, the
            // four rails may not start on four different ones.
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(labelSlot),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        ScaleRow(
            label = label,
            steps = TextSize.STEPS,
            value = step,
            onSelect = onChange,
            startMark = {
                SizeMark(role, TextSize.STEPS.first(), scale, Modifier.width(markSlot))
            },
            endMark = {
                SizeMark(role, TextSize.STEPS.last(), scale, Modifier.width(markSlot))
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = Space.Line),
        )
    }
}

/**
 * One end of a size scale: the letter at the size that end offers, in the
 * script it is read in. The Arabic and the aid's word are drawn at three
 * quarters, so the largest still fits the row; the others at their own size,
 * as the sample ayah above is drawn. [modifier] is the fixed mark slot, and
 * the letter stands on its middle so two marks of different scripts read on
 * one line.
 */
@Composable
private fun SizeMark(
    role: TypeRole,
    step: Float,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val size = (TextSize.sp(role, step) * scale).sp
    Text(
        text = if (role == TypeRole.Arabic) "ا" else "A",
        // The Arabic mark is drawn in the Quran's own face, the way the sample
        // above it is: the letter the reader is sizing is the letter they
        // read (owner decision, the theme and consistency session).
        style = if (role == TypeRole.Arabic) {
            TextStyle(fontFamily = rememberHafs(), fontSize = size)
        } else {
            MaterialTheme.typography.titleMedium.copy(
                fontSize = size,
                lineHeight = (size.value * 1.2f).sp,
            )
        },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
