package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
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
import io.github.muntasimulhaque.quran.ui.theme.controlEdge
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
 * The edge is the app's control boundary, not a hairline: it clears 3:1 on
 * the page and on the sheet, so a pale page on a pale sheet and a dark page
 * on a dark sheet both keep their circle (owner report; measured again in
 * the consistency audit).
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
                    color = controlEdge(),
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
 * The size of one kind of text: its name on one line, and its scale under it,
 * with the ends showing the smallest and the largest the kind can be and the
 * chosen stop filled. The sample above the rows is the reader's own ayah, so a
 * change is judged on the page it is about to change.
 *
 * **The name stands over its rail** (owner decision, this session). It used to
 * keep a fixed 120 dp column beside the rail, and the two end marks a fixed
 * slot each: at 200 percent on a phone the column took half the row, the name
 * ellipsized, and on a narrow glass the marks were dropped to keep the rail
 * alive, which took away the one thing that says which way the scale runs.
 * Over the rail, the name has the page's whole width and the rail has what is
 * left after its two marks at every text size. The pace in the listening menu
 * already draws its label over the same [ScaleRow] (owner decision, the
 * forty-seventh session), so this is one control learned once in both places,
 * not a second shape.
 *
 * **The rails are one length.** Each one starts on the gutter plus its mark and
 * ends on the mirror, whatever its name says, so two controls on one page read
 * as one measure. The name is a row's name and wears the same voice as every
 * other row's name; it is silent to a screen reader, because the rail already
 * speaks it, and the drawn text would otherwise be read twice.
 */
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
        },
    )
    val scale = if (role == TypeRole.Arabic) 0.75f else 1f
    val markSlot = SizeMarkSlot * LocalDensity.current.fontScale
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = gutter, end = gutter, top = Space.Block),
    ) {
        Text(
            text = label,
            // A control's name wears the same words every other row's name
            // wears, so the sheet reads as one voice. The rail under it speaks
            // the same name, so the drawn line is silent to a screen reader.
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { testTag = "size-label" },
            maxLines = 1,
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
                .fillMaxWidth()
                .padding(top = Space.Line),
        )
    }
}

/**
 * One end of a size scale: the letter at the size that end offers, in the
 * script it is read in. The Arabic is drawn at three quarters, so the largest
 * still fits the row; the others at their own size, as the sample ayah above
 * is drawn. [modifier] is the fixed mark slot, and the letter stands on its
 * middle so two marks of different scripts read on one line.
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
