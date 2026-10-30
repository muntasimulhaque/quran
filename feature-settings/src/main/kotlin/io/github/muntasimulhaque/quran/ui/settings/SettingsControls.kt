package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.StepRow
import io.github.muntasimulhaque.quran.ui.theme.NightBackground
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.NightText
import io.github.muntasimulhaque.quran.ui.theme.PaperBackground
import io.github.muntasimulhaque.quran.ui.theme.PaperInk
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(Radii.Card)
                    .selectable(selected = theme == selected, role = Role.RadioButton) {
                        onSelect(theme)
                    }
                    .padding(horizontal = 6.dp, vertical = Space.Line),
            ) {
                ThemeSwatch(theme = theme, chosen = theme == selected)
                Text(
                    text = theme.name(),
                    style = MaterialTheme.typography.labelSmall,
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
                    style = amiriDisplay(18f),
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
 * The size of one kind of text: the row names what it sizes, the steps grow
 * from the smallest to the largest, and the chosen one is filled. The sample
 * above the rows is drawn in the script of the text that is being sized, so
 * Arabic is judged as Arabic.
 *
 * The row names nothing else. It used to print "3 of 5" beside every name,
 * which is the place of the step in a ladder the reader can already see: the
 * five cells grow from the smallest to the largest and the chosen one is
 * filled, so the number said the same thing twice, and four of them stacked
 * turned the page into arithmetic (owner report). A reader who cannot see
 * the fill still hears the place, because each cell says it (owner decision,
 * this session).
 */
@Composable
fun SizeRow(role: TypeRole, step: Float, onChange: (Float) -> Unit) {
    val label = stringResource(
        when (role) {
            TypeRole.Arabic -> R.string.settings_size_arabic
            TypeRole.Translation -> R.string.settings_size_translation
            TypeRole.Tafsir -> R.string.settings_size_tafsir
            TypeRole.Words -> R.string.settings_size_words
        },
    )
    SegmentedRow(
        label = label,
        value = step,
        steps = TextSize.STEPS,
        onSelect = onChange,
        // The letters run from the smallest step to the largest, so the row
        // reads as one scale.
        cell = { index, active ->
            val size = (11 + index * 2).sp
            Text(
                text = if (role == TypeRole.Arabic) "\u0627" else "A",
                // The Arabic cell is drawn in the Quran's own face, the way
                // the sample above it is. It wore the interface face, so the
                // letter the reader was sizing was not the letter they read
                // (owner decision, the theme and consistency session).
                style = if (role == TypeRole.Arabic) {
                    TextStyle(fontFamily = rememberHafs(), fontSize = size)
                } else {
                    MaterialTheme.typography.titleMedium.copy(fontSize = size)
                },
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
    ) { chosen ->
        val index = TextSize.STEPS.indexOf(chosen)
        stringResource(
            R.string.settings_text_size_option,
            index + 1,
            TextSize.STEPS.size,
            label,
        )
    }
}

/**
 * One choice out of a few, drawn the same way everywhere it appears: the name
 * of the thing above, the choices in one pill under it, the chosen one filled.
 *
 * The name is above rather than beside because of the 48 dp rule. Every step
 * carries its own touch box, so a row of five is 250 dp wide, and beside a
 * name that would leave the name 60-odd dp on a small phone: "Playback speed"
 * would break in the middle and the Bengali names would break anywhere. Above,
 * the name has the row to itself and the choices have the row's middle, and
 * the pair reads as one block (these cells were 38 and 46 dp once, named
 * and not done until later).
 *
 * The rhythm is what says which name belongs to which pill: the name stands
 * 4 dp above its own control, and the next row opens 16 dp under it. The
 * page used to keep 4 dp above and below every row and 8 dp between a name
 * and its pill, so the gap inside a row and the gap between rows measured
 * 11 dp and 12 dp on the glass: four controls floated in one column of
 * equal gaps, and nothing grouped (owner report).
 */
@Composable
private fun SegmentedRow(
    label: String,
    value: Float,
    steps: List<Float>,
    onSelect: (Float) -> Unit,
    /** What one step draws, given its place in the scale and whether it is chosen. */
    cell: @Composable (index: Int, active: Boolean) -> Unit,
    /** What a screen reader says for one step, place and meaning together. */
    description: @Composable (Float) -> String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Space.Gutter, end = Space.Gutter, top = Space.Block),
    ) {
        Text(
            text = label,
            // A control's name wears the same words every other row's name
            // wears, so the sheet reads as one voice (owner decision, the
            // theme and consistency session).
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        StepRow(
            steps = steps,
            value = value,
            onSelect = onSelect,
            cell = cell,
            description = description,
            modifier = Modifier
                .padding(top = Space.Tight)
                .align(Alignment.CenterHorizontally),
        )
    }
}

