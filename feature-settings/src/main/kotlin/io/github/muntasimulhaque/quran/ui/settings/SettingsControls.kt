package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.SpeedSteps
import io.github.muntasimulhaque.quran.ui.kit.speedText
import io.github.muntasimulhaque.quran.ui.theme.BlackBackground
import io.github.muntasimulhaque.quran.ui.theme.BlackText
import io.github.muntasimulhaque.quran.ui.theme.NightBackground
import io.github.muntasimulhaque.quran.ui.theme.NightText
import io.github.muntasimulhaque.quran.ui.theme.PaperBackground
import io.github.muntasimulhaque.quran.ui.theme.PaperInk
import io.github.muntasimulhaque.quran.ui.theme.SepiaBackground
import io.github.muntasimulhaque.quran.ui.theme.SepiaInk
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The four grounds, as swatches: each one is the page it will paint. The
 * swatch that is filled is the page the reader is actually reading on, not
 * the stored day choice: with automatic night mode on and the phone in dark
 * mode, Night draws the app, so Night is what the row says (owner report).
 * The day choice is named in the note under the switch, and a tap
 * still sets the day page.
 */
@Composable
fun ThemeRow(
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
    /** The page drawn right now, when the system has a say. */
    shown: AppTheme = selected,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = Space.Tight),
    ) {
        AppTheme.entries.forEach { theme ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .selectable(selected = theme == shown, role = Role.RadioButton) {
                        onSelect(theme)
                    }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
            ) {
                val (ground, ink) = theme.swatch()
                // The chosen page wears its halo outside the swatch, with a
                // gap. A ring on the fill's own edge read as a border and
                // ate the page's color where the eye looks for it; a ring
                // around the fill reads as the choice (owner report).
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .then(
                            if (theme == shown) {
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
                    // The edge is the page's own shade, not the theme's
                    // hairline: a pale page on a pale sheet and a dark page
                    // on a dark sheet both keep their circle without either
                    // wearing a chrome outline (owner report).
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(ground, CircleShape)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "\u0627",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            color = ink,
                        )
                    }
                }
                Text(
                    text = theme.name(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (theme == shown) {
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

fun AppTheme.swatch(): Pair<Color, Color> = when (this) {
    AppTheme.Paper -> PaperBackground to PaperInk
    AppTheme.Sepia -> SepiaBackground to SepiaInk
    AppTheme.Night -> NightBackground to NightText
    AppTheme.Black -> BlackBackground to BlackText
}

@Composable
fun AppTheme.name(): String = stringResource(
    when (this) {
        AppTheme.Paper -> R.string.settings_theme_paper
        AppTheme.Sepia -> R.string.settings_theme_sepia
        AppTheme.Night -> R.string.settings_theme_night
        AppTheme.Black -> R.string.settings_theme_black
    },
)

/**
 * The size of one kind of text: the row names what it sizes, the steps grow
 * from the smallest to the largest, and the chosen one is filled. The sample
 * above the rows is drawn in the script of the text that is being sized, so
 * Arabic is judged as Arabic.
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
            Text(
                text = if (role == TypeRole.Arabic) "\u0627" else "A",
                style = MaterialTheme.typography.titleMedium
                    .copy(fontSize = (11 + index * 2).sp),
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
 * The pace of the recitation, in the same segmented shape the text sizes
 * use: one control, learned once. The pace belongs to hearing the way the
 * size belongs to reading, so a reader who learned one has learned the other.
 * The value is in numbers, because "slow" and "fast" are not the same for
 * every reader.
 */
@Composable
fun SpeedRow(value: Float, onChange: (Float) -> Unit) {
    val label = stringResource(R.string.settings_speed_label)
    SegmentedRow(
        label = label,
        value = value,
        steps = SpeedSteps,
        onSelect = onChange,
        cell = { index, active ->
            Text(
                text = speedText(SpeedSteps[index]),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
    ) { chosen ->
        stringResource(R.string.settings_speed_option, speedText(chosen), label)
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
            .padding(start = 22.dp, end = 22.dp, top = Space.Tight, bottom = Space.Tight),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .padding(top = Space.Line)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            steps.forEachIndexed { index, step ->
                val active = kotlin.math.abs(step - value) < 0.01f
                val said = description(step)
                Box(
                    modifier = Modifier
                        // The 48 dp is the whole reason this control has a
                        // shape of its own: the mark inside is smaller, and the
                        // target a finger gets is not.
                        .size(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (active) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .selectable(selected = active, role = Role.RadioButton) { onSelect(step) }
                        .semantics { contentDescription = said },
                    contentAlignment = Alignment.Center,
                ) {
                    cell(index, active)
                }
            }
        }
    }
}

