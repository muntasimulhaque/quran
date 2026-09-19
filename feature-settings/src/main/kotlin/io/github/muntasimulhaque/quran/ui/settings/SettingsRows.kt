package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.BlackBackground
import io.github.muntasimulhaque.quran.ui.theme.BlackText
import io.github.muntasimulhaque.quran.ui.theme.NightBackground
import io.github.muntasimulhaque.quran.ui.theme.NightText
import io.github.muntasimulhaque.quran.ui.theme.PaperBackground
import io.github.muntasimulhaque.quran.ui.theme.PaperInk
import io.github.muntasimulhaque.quran.ui.theme.SepiaBackground
import io.github.muntasimulhaque.quran.ui.theme.SepiaInk

/** A quiet heading over a group of rows. */
@Composable
fun Group(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 4.dp),
    )
}

/**
 * One category in the settings hub: what it is called, where it stands right
 * now, and the chevron that says it opens something.
 */
@Composable
fun PageRow(title: String, summary: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(start = 22.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (!summary.isNullOrBlank()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        IconGlyph(
            icon = Icon.Chevron,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(18.dp)
                // The mark says where a tap goes: a row opens a page, so the
                // chevron points the way the page slides in, to the right.
                .graphicsLayer(rotationZ = -90f),
        )
    }
}

/** The head of a page the reader opened: one way back, one name. */
@Composable
fun PageHeader(title: String, onBack: () -> Unit) {
    val back = stringResource(R.string.settings_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 22.dp, top = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onBack)
                .semantics {
                    contentDescription = back
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            IconGlyph(
                icon = Icon.Chevron,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(22.dp)
                    // The chevron opens a page pointing down, so going back
                    // is the same mark turned to point the way it came.
                    .graphicsLayer(rotationZ = 90f),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(start = 22.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // The whole row is the control, so the switch is only a picture of
        // its state and TalkBack reads one labelled node.
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** One choice among several: the reader takes one, and the mark says which. */
@Composable
fun ChoiceRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    SettingRow(
        title = title,
        subtitle = subtitle,
        selected = selected,
        role = Role.RadioButton,
        onClick = onClick,
        trailing = trailing,
    ) { Mark(selected = selected, radio = true) }
}

/** A row that can be on or off among many, with a check for its state. */
@Composable
fun MarkRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    SettingRow(
        title = title,
        subtitle = subtitle,
        selected = selected,
        role = Role.Checkbox,
        onClick = onClick,
        trailing = trailing,
    ) { Mark(selected = selected, radio = false) }
}

/**
 * One pack in a list of choices: a mark for its state, its name, and the one
 * action it needs. The mark and the name are one control, so a reader who
 * taps the name of a pack they do not have yet is asking for it, with the
 * size already on the row in front of them; a reader who taps a name they do
 * have turns it on or off. The action at the right is the same door for a
 * reader who looks for a button instead of a row.
 *
 * Radios for a reciter, checks for translations, tafsirs, and word lists:
 * one reciter is heard at a time, while more than one reading may be on.
 */
@Composable
fun PackChoiceRow(
    pack: ContentPack,
    subtitle: String,
    selected: Boolean,
    radio: Boolean,
    setup: PackSetupState?,
    onActivate: () -> Unit,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
) {
    val busy = setup?.packId == pack.id && setup.failed.not()
    SettingRow(
        title = pack.name,
        subtitle = subtitle,
        selected = selected,
        role = if (radio) Role.RadioButton else Role.Checkbox,
        // A pack that is not here yet cannot be turned on: its row is the
        // door that brings it here instead, and nothing looks selectable
        // while it is on its way in.
        onClick = if (pack.installed && !busy) onActivate else onInstall,
        trailing = { PackTrailing(pack, setup, onInstall, onRemove) },
    ) { Mark(selected = selected, radio = radio) }
}

/**
 * A row with a mark at its left, the way a list of choices is read: the mark
 * comes before the name, so the eye lands on the state first and the name
 * reads as the thing it belongs to. The whole row is one tappable control.
 *
 * The mark and the name keep a clear gap between them: a radio set against
 * the word it selects reads as one crowded glyph, and the reader has to look
 * twice to see which mark belongs to which name.
 */
@Composable
private fun SettingRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    role: Role,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
    mark: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .selectable(selected = selected, role = role, onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        mark()
        Column(
            Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        Box(Modifier.padding(start = 12.dp)) { trailing() }
    }
}

/** The app's own mark: a drawn ring and dot, or a check, never a stock icon. */
@Composable
private fun Mark(selected: Boolean, radio: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier.size(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            if (radio) {
                drawCircle(
                    color = if (selected) accent else accent.copy(alpha = 0.35f),
                    radius = w * 0.42f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.09f),
                )
                if (selected) {
                    drawCircle(color = accent, radius = w * 0.2f)
                }
            } else if (selected) {
                drawLine(
                    color = accent,
                    start = androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.52f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.78f),
                    strokeWidth = w * 0.13f,
                )
                drawLine(
                    color = accent,
                    start = androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.78f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.84f, h * 0.22f),
                    strokeWidth = w * 0.13f,
                )
            } else {
                drawCircle(
                    color = accent.copy(alpha = 0.35f),
                    radius = w * 0.42f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.09f),
                )
            }
        }
    }
}

/** A row of two lines that opens a door. */
@Composable
fun TextRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A line the reader cannot change: a version, a size, what stands where. */
@Composable
fun ValueRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f),
        )
    }
}

/**
 * The action one pack row carries: Add when it is not here, Retry when a
 * download failed, its progress while it is on its way, Remove once it is. A
 * row already in use shows nothing else, because the mark at the left already
 * says what state it is in.
 */
@Composable
private fun PackTrailing(
    pack: ContentPack,
    setup: PackSetupState?,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
) {
    when {
        setup?.packId == pack.id && setup.failed ->
            PackActionText(stringResource(R.string.pack_action_retry), onInstall)

        setup?.packId == pack.id -> Text(
            text = if (setup.progress == null) {
                stringResource(R.string.settings_pack_preparing)
            } else {
                stringResource(R.string.settings_pack_downloading, (setup.progress * 100).toInt())
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        !pack.installed -> PackActionText(stringResource(R.string.pack_action_add), onInstall)

        pack.shipped -> Text(
            text = stringResource(R.string.pack_included),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        else -> Text(
            text = stringResource(R.string.pack_action_remove),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onRemove)
                .padding(horizontal = 10.dp),
        )
    }
}

/**
 * One quiet action on a pack row. The touch target is the smallest a finger
 * needs, so a tap that lands near the word lands on it.
 */
@Composable
internal fun PackActionText(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** The four grounds, as swatches: each one is the page it will paint. */
@Composable
fun ThemeRow(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        AppTheme.entries.forEach { theme ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .selectable(selected = theme == selected, role = Role.RadioButton) {
                        onSelect(theme)
                    }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            ) {
                val (ground, ink) = theme.swatch()
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(ground)
                        .border(
                            width = if (theme == selected) 2.dp else 1.dp,
                            color = if (theme == selected) {
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
                    color = if (theme == selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 5.dp),
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
 */@Composable
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
            .padding(horizontal = 22.dp, vertical = 4.dp),
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
