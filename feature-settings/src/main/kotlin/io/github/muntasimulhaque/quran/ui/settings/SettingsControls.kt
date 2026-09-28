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
 * mode, Night draws the app, so Night is what the row says (owner report,
 * D-097). The day choice is named in the note under the switch, and a tap
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
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(ground)
                        .border(
                            width = if (theme == shown) 2.dp else 1.dp,
                            color = if (theme == shown) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
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
 * The size of one kind of text: the row names what it sizes, the steps grow,
 * and the value on the right says exactly what it comes to. The sample is
 * drawn in the script of the text it sizes, so Arabic is judged as Arabic.
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            TextSize.STEPS.forEachIndexed { index, value ->
                val active = value == step
                val description = stringResource(
                    R.string.settings_text_size_option,
                    index + 1,
                    TextSize.STEPS.size,
                    label,
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (active) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .selectable(selected = active, role = Role.RadioButton) { onChange(value) }
                        .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        // The letters run from the smallest step to the
                        // largest, so the row reads as one scale.
                        text = if (role == TypeRole.Arabic) "\u0627" else "A",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (11 + index * 2).sp,
                        ),
                        color = if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

/**
 * The pace of the recitation, in the same segmented shape the text sizes use:
 * one row, the choices growing left to right, the chosen one filled. The pace
 * belongs to hearing the way the size belongs to reading, so the two are the
 * same control, and a reader who learned one has learned the other. The value
 * is in numbers, because "slow" and "fast" are not the same for every reader.
 */
@Composable
fun SpeedRow(value: Float, onChange: (Float) -> Unit) {
    val label = stringResource(R.string.settings_speed_label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            SpeedSteps.forEach { speed ->
                val active = kotlin.math.abs(speed - value) < 0.01f
                val description = stringResource(
                    R.string.settings_speed_option,
                    speedText(speed),
                    label,
                )
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (active) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .selectable(selected = active, role = Role.RadioButton) { onChange(speed) }
                        .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = speedText(speed),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                        color = if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

