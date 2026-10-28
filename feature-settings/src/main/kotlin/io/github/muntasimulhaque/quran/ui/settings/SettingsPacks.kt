package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.contentPackName
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * What a list of packs says, in one place.
 *
 * The hub names the pack a row is on in a few words, and the list pages name
 * every pack in a group by the language it speaks. Both are the same question,
 * asked of a pack catalog, and they are answered here so the hub and the
 * pages cannot drift into saying different things about the same pack.
 */

/**
 * The translations the reading draws with, in one line: the pack's own name
 * when there is one, a count when there are several, and an honest nothing
 * when the reader has added none. A pack that is chosen but not yet on the
 * device is not counted, so the row never promises a translation the reading
 * cannot draw.
 */
@Composable
internal fun translationName(settings: AppSettings, packs: List<ContentPack>): String {
    val chosen = packs.filter { it.id in settings.translationPacks && it.installed }
    return when {
        chosen.isEmpty() -> stringResource(R.string.settings_none_yet)
        chosen.size == 1 -> contentPackName(chosen.first().id, chosen.first().name)
        else -> stringResource(R.string.settings_chosen_count, chosen.size)
    }
}

/** The tafsirs the reading draws with, in the same one line as the translations. */
@Composable
internal fun tafsirSummary(settings: AppSettings, packs: List<ContentPack>): String {
    val chosen = packs.filter { it.type == PackType.Tafsir && it.installed && it.id in settings.tafsirPacks }
    return when {
        chosen.isEmpty() -> stringResource(R.string.settings_none_yet)
        chosen.size == 1 -> contentPackName(chosen.first().id, chosen.first().name)
        else -> stringResource(R.string.settings_chosen_count, chosen.size)
    }
}

/**
 * Whether the reading has a translation it can draw. The switch reads on only
 * when this is true, so a row that says "Not added yet" never reads on above
 * its own words (owner decision, this session).
 */
internal fun hasInstalledTranslation(settings: AppSettings, packs: List<ContentPack>): Boolean =
    packs.any { it.id in settings.translationPacks && it.installed }

/**
 * The word list the reading speaks: the one that matches the reader's own
 * language, and no other. There is no fallback to English: a Bangla reader
 * whose list has not landed yet is asked to add the Bangla list, never
 * handed the English one (owner decision, this session). Null when the
 * catalog carries no list for the language, so the row can say nothing
 * rather than promise a list that does not exist.
 */
internal fun wantedWordsPack(settings: AppSettings, packs: List<ContentPack>): ContentPack? {
    val language = UiLanguage.of(settings.uiLanguage)?.tag ?: UiLanguage.English.tag
    return packs.firstOrNull { it.id == ContentDatabase.wordsPackId(language) }
}

/**
 * One pack's own line on a list page: the language it speaks and whether it
 * came with the app, so a reader never wonders what a size beside a pack
 * means.
 */
@Composable
internal fun translationSubtitle(pack: ContentPack): String {
    val detail = if (pack.shipped) {
        stringResource(R.string.pack_included_suffix)
    } else {
        formatBytes(pack.bytes)
    }
    return stringResource(R.string.pack_installed, languageName(pack.language), detail)
}

/**
 * The packs of one kind, read in one list: the language each one speaks, then
 * the name each row draws. A list of choices is read, not searched, so the
 * packs are in one order everywhere in the app.
 *
 * The list used to stand under a heading per language, and every row under
 * the heading named the language again in its own line ("Bangla", then
 * "Bangla · 53 MB"). With one or two packs per language the heading cost a
 * block of air to say what the first row already said, so the language stayed
 * in the row and the heading went (owner report).
 */
@Composable
internal fun LanguageGroups(
    packs: List<ContentPack>,
    type: PackType,
    row: @Composable (ContentPack) -> Unit,
) {
    packs.filter { it.type == type }
        .map { pack -> Triple(pack, languageName(pack.language), contentPackName(pack.id, pack.name)) }
        .sortedWith(compareBy({ (_, language, _) -> language.lowercase() }, { (_, _, name) -> name.lowercase() }))
        .forEach { (pack, _, _) -> row(pack) }
}
