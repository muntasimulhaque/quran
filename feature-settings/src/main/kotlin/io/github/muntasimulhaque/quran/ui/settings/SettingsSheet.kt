package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.Recitation
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.shortReciterName
import io.github.muntasimulhaque.quran.ui.theme.BlackBackground
import io.github.muntasimulhaque.quran.ui.theme.BlackText
import io.github.muntasimulhaque.quran.ui.theme.NightBackground
import io.github.muntasimulhaque.quran.ui.theme.NightText
import io.github.muntasimulhaque.quran.ui.theme.PaperBackground
import io.github.muntasimulhaque.quran.ui.theme.PaperInk
import io.github.muntasimulhaque.quran.ui.theme.SepiaBackground
import io.github.muntasimulhaque.quran.ui.theme.SepiaInk
import kotlinx.coroutines.launch

/**
 * What the settings sheet can ask the app to do. The sheet itself owns no
 * state beyond what is open; every change is a call, so the feature can be
 * read, tested, and previewed without a view model.
 */
data class SettingsActions(
    val onTheme: (AppTheme) -> Unit = {},
    val onTextSize: (TextSize) -> Unit = {},
    val onKeepAwake: (Boolean) -> Unit = {},
    val onFollowReciter: (Boolean) -> Unit = {},
    val onShowFootnotes: (Boolean) -> Unit = {},
    val onWordByWord: (Boolean) -> Unit = {},
    val onDimLevel: (Int) -> Unit = {},
    val onSelectRecitation: (String) -> Unit = {},
    val onTranslationPack: (String) -> Unit = {},
    val onToggleTafsir: (String) -> Unit = {},
    val onInstallPack: (String) -> Unit = {},
    val onRemovePack: (String) -> Unit = {},
    val onPackSetupCancel: () -> Unit = {},
    val onRemoveDownloads: suspend (String, Int) -> Unit = { _, _ -> },
)

/** A pack the reader asked for, while it downloads. */
data class PackSetupState(
    val packId: String,
    val name: String,
    val bytes: Long,
    val progress: Float?,
    val failed: Boolean,
)

/**
 * One sheet for everything the reader can choose: how the page looks, how it
 * behaves, who recites, and which content packs are on. Nothing here is
 * hidden behind a second level: the sheet is grouped, and the groups are
 * short enough to read at a glance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    packs: List<ContentPack>,
    packSetup: PackSetupState?,
    recitations: List<Recitation>,
    surahs: List<Surah>,
    downloadedSurahs: suspend (String) -> List<DownloadedSurah>,
    actions: SettingsActions,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var downloadsOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }

    if (aboutOpen) {
        AboutSheet(
            packs = packs,
            version = "0.1",
            onDismiss = { aboutOpen = false },
        )
        return
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 10.dp),
            )

            Group("Appearance")
            ThemeRow(settings.theme) { actions.onTheme(it) }
            TextSizeRow(settings.textSize) { actions.onTextSize(it) }
            DimRow(settings.dimLevel) { actions.onDimLevel(it) }

            Group("Reading")
            ToggleRow(
                title = "Keep the screen awake",
                subtitle = "The page stays lit while you read",
                checked = settings.keepAwake,
            ) { actions.onKeepAwake(it) }
            ToggleRow(
                title = "Follow the reciter",
                subtitle = "The page moves with the recitation",
                checked = settings.followReciter,
            ) { actions.onFollowReciter(it) }
            ToggleRow(
                title = "Show word meanings",
                subtitle = "Each word with its meaning, under the ayah",
                checked = settings.wordByWord,
            ) { actions.onWordByWord(it) }
            ToggleRow(
                title = "Show footnotes",
                subtitle = "Translator notes under each ayah",
                checked = settings.showFootnotes,
            ) { actions.onShowFootnotes(it) }

            Group("Recitation")
            packs.filter { it.type == PackType.Recitation }.forEach { pack ->
                PackRow(
                    pack = pack,
                    selected = pack.id == ContentDatabase.reciterPack(settings.recitation),
                    setup = packSetup?.takeIf { it.packId == pack.id },
                    onInstall = { actions.onInstallPack(pack.id) },
                    onRemove = { actions.onRemovePack(pack.id) },
                    onSelect = { actions.onSelectRecitation(pack.id.removePrefix(ContentDatabase.RECITER_PREFIX)) },
                )
            }
            TextRow(
                title = if (downloadsOpen) "Hide downloaded surahs" else "Manage downloaded surahs",
            ) { downloadsOpen = !downloadsOpen }
            if (downloadsOpen) {
                val downloaded by androidx.compose.runtime.produceState(
                    initialValue = emptyList<DownloadedSurah>(),
                    settings.recitation,
                ) {
                    value = downloadedSurahs(settings.recitation).sortedBy { it.surah }
                }
                if (downloaded.isEmpty()) {
                    Text(
                        text = "No surahs downloaded for this reciter yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 10.dp),
                    )
                }
                downloaded.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = formatBytes(row.bytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Remove",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    scope.launch { actions.onRemoveDownloads(settings.recitation, row.surah) }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Group("Content")
            Text(
                text = "The Quran text and its page layout are in the app. Everything else " +
                    "is added when you want it, and can be removed at any time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 8.dp),
            )
            packs
                .filter { it.type == PackType.Translation || it.type == PackType.Tafsir || it.type == PackType.Words }
                .groupBy { it.language }
                .entries
                .sortedBy { (language, _) -> languageOrder(language) }
                .forEach { (language, group) ->
                    Text(
                        text = languageName(language),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 2.dp),
                    )
                    group.forEach { pack ->
                        PackRow(
                            pack = pack,
                            selected = when (pack.type) {
                                PackType.Translation -> pack.id == settings.translationPack
                                PackType.Tafsir -> pack.id in settings.tafsirPacks
                                else -> true
                            },
                            setup = packSetup?.takeIf { it.packId == pack.id },
                            onInstall = { actions.onInstallPack(pack.id) },
                            onRemove = { actions.onRemovePack(pack.id) },
                            onSelect = {
                                when (pack.type) {
                                    PackType.Translation -> actions.onTranslationPack(pack.id)
                                    PackType.Tafsir -> actions.onToggleTafsir(pack.id)
                                    else -> Unit
                                }
                            },
                        )
                    }
                }

            Group("About")
            AboutRow("Version", "0.1")
            AboutRow("Ads and trackers", "None, and none ever")
            AboutRow("Source code", "github.com/muntasimulhaque/quran")
            AboutRow("Privacy", "Nothing leaves this device unless you ask it to")
            TextRow("Credits and licenses") { aboutOpen = true }
        }
    }
}

/** One pack: what it is, what it costs, and the one action it needs. */
@Composable
private fun PackRow(
    pack: ContentPack,
    selected: Boolean,
    setup: PackSetupState?,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
    onSelect: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = pack.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = buildString {
                        append(
                            when (pack.type) {
                                PackType.Translation -> "Translation"
                                PackType.Tafsir -> "Tafsir"
                                PackType.Words -> "Word by word"
                                PackType.Recitation -> "Recitation"
                                PackType.Script -> "Script"
                            },
                        )
                        if (pack.shipped) {
                            append("  \u00B7  included")
                        } else {
                            append("  \u00B7  ${formatBytes(pack.bytes)}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                setup?.failed == true -> Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onInstall)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                setup != null -> Text(
                    text = if (setup.progress == null) {
                        "Preparing..."
                    } else {
                        "Downloading ${(setup.progress * 100).toInt()}%"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                !pack.installed -> Text(
                    text = "Add",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onInstall)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                pack.shipped -> Text(
                    text = "Included",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selected) {
                        Text(
                            text = if (pack.type == PackType.Tafsir) "On" else "Selected",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else if (pack.type == PackType.Translation || pack.type == PackType.Tafsir) {
                        Text(
                            text = "Use",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onSelect)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                    Text(
                        text = "Remove",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onRemove)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Group(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 6.dp),
    )
}

@Composable
private fun ThemeRow(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppTheme.entries.forEach { theme ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(theme) }
                    .padding(6.dp),
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
                    text = theme.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (theme == selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

private fun AppTheme.swatch(): Pair<Color, Color> = when (this) {
    AppTheme.Paper -> PaperBackground to PaperInk
    AppTheme.Sepia -> SepiaBackground to SepiaInk
    AppTheme.Night -> NightBackground to NightText
    AppTheme.Black -> BlackBackground to BlackText
}

@Composable
private fun TextSizeRow(selected: TextSize, onSelect: (TextSize) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Aa",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TextSize.entries.forEach { size ->
                val active = size == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
                        )
                        .clickable { onSelect(size) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "\u0627",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = (12 + size.ordinal * 2).sp),
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

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
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
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 22.dp, vertical = 10.dp),
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
        if (selected) {
            Text(
                text = "Selected",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TextRow(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    )
}

@Composable
private fun AboutRow(title: String, value: String) {
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

private fun languageName(code: String): String = when (code) {
    "ar" -> "Arabic"
    "bn" -> "Bengali"
    "de" -> "German"
    "en" -> "English"
    "es" -> "Spanish"
    "fa" -> "Persian"
    "fr" -> "French"
    "hi" -> "Hindi"
    "id" -> "Indonesian"
    "ml" -> "Malayalam"
    "ru" -> "Russian"
    "sw" -> "Swahili"
    "ta" -> "Tamil"
    "tr" -> "Turkish"
    "ur" -> "Urdu"
    else -> code.uppercase()
}

/** The dim choices, named the way a reader thinks of them. */
@Composable
private fun DimRow(level: Int, onLevel: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Screen dim",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Softens the page for night reading",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(3.dp),
        ) {
            listOf(0 to "Off", 1 to "Dim", 2 to "Darker").forEach { (value, label) ->
                val active = value == level
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
                        )
                        .clickable { onLevel(value) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
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
 * Credits and licenses: who made the text, the translations, the tafsirs, the
 * fonts, and the recitations this app carries, and under what terms. Every
 * pack in the catalog appears with its own credit and license, so the page
 * cannot drift from what is installed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSheet(
    packs: List<ContentPack>,
    version: String,
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 34.dp),
        ) {
            Text(
                text = "Quran: The Noble Book",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp),
            )
            Text(
                text = "Version $version  \u00B7  no ads, no trackers, no accounts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 4.dp),
            )

            Group("Content")
            packs.forEach { pack ->
                Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 12.dp)) {
                    Text(
                        text = "${pack.name}  \u00B7  ${languageName(pack.language)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = pack.credit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = pack.license,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                }
            }

            Group("Fonts")
            Credit("QPC V2 page fonts", "King Fahd Glorious Quran Printing Complex, unmodified", "Study and Mushaf text")
            Credit("Amiri Quran", "Khaled Hosny, SIL Open Font License 1.1", "Ornaments and Arabic outside the Mushaf")
            Credit("Literata", "TypeTogether, SIL Open Font License 1.1", "Translations and tafsir")
            Credit("Inter", "Rasmus Andersson, SIL Open Font License 1.1", "The interface")

            Group("Recitations")
            Credit("Minshawi", "Muhammad Siddiq Al-Minshawi, via the Quranic Universal Library", "Murattal")
            Credit("Husary", "Mahmoud Khalil Al-Husary, via the Quranic Universal Library", "Murattal")

            Group("The app")
            AboutRow("Source code", "github.com/muntasimulhaque/quran")
            AboutRow("License", "MIT")
            AboutRow("Privacy", "Nothing leaves this device unless you ask it to")
            AboutRow("Network", "Only to fetch a pack or a surah you asked for")
        }
    }
}

@Composable
private fun Credit(title: String, credit: String, use: String) {
    Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = credit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = use,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
    }
}

/**
 * The order languages appear in: the interface's own language first, then the
 * language of the Quran's revelation, then the rest by name.
 */
private fun languageOrder(language: String): String = when (language) {
    "en" -> "0"
    "ar" -> "1"
    else -> "2" + languageName(language)
}
