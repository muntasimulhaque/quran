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
import io.github.muntasimulhaque.quran.data.DownloadError
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.contentPackName
import io.github.muntasimulhaque.quran.ui.kit.formatBytes
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.uikit.R as UiKitR

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
 * device is not counted, so this line never promises a translation the
 * reading cannot draw. The switch above it follows the choice instead: the
 * value says what is here, and the switch says what the reader asked for
 * (owner decision, this session).
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
 * Whether the reading has a translation it can draw, which is also whether
 * the switch can be used. It is not the switch's state: that is the reader's
 * own answer, and a chosen pack that is not on the device leaves the switch
 * on and disabled rather than off, so the row never reports a choice the
 * reader did not make (owner decision, this session: the switch used to read
 * on only when a translation could be drawn).
 */
internal fun hasInstalledTranslation(settings: AppSettings, packs: List<ContentPack>): Boolean =
    packs.any { it.id in settings.translationPacks && it.installed }

/**
 * Whether the reader has chosen a translation at all, here or not. The
 * switch's state is this and the reader's own answer; its usability is
 * [hasInstalledTranslation]. The two questions are drawn apart on purpose: a
 * chosen pack that is not on the device yet is still the reader's choice, and
 * the switch says so while it waits (owner decision, this session).
 */
internal fun hasChosenTranslation(settings: AppSettings, packs: List<ContentPack>): Boolean =
    packs.any { it.id in settings.translationPacks }

/**
 * The word list the reading speaks: the one that matches the reader's own
 * language, and no other. There is no fallback to English: a Bangla reader
 * whose list has not landed yet is asked to add the Bangla list, never
 * handed the English one (owner decision, this session). Null when the
 * catalog carries no list for the language, so the row can say nothing
 * rather than promise a list that does not exist.
 */
internal fun wantedWordsPack(settings: AppSettings, packs: List<ContentPack>): ContentPack? {
    val language = settings.readingLanguage
    return packs.firstOrNull { it.id == ContentDatabase.wordsPackId(language) }
}

/**
 * One pack's own line on a list page: what it costs to have, and nothing else.
 * The language it speaks is the heading the row stands under, so a row that
 * said it again under its own name spent a line repeating the group it is in
 * (owner report, this session).
 */
@Composable
internal fun packSizeLine(pack: ContentPack): String =
    if (pack.shipped) {
        stringResource(R.string.pack_included)
    } else {
        formatBytes(pack.bytes)
    }

/**
 * What a pack on its way is doing, in one line, or null when nothing is.
 *
 * The hub and the pack's own page ask the same question of the same state,
 * so they are answered here and cannot drift: a download names its percent,
 * a preparing pack says so, and a failure says what the reader can do about
 * it. The page under the words switch reads it under the switch itself, so
 * the reader's tap is answered where the finger is rather than only at the
 * pack row below (owner decision, this session).
 */
@Composable
internal fun packSetupLine(setup: PackSetupState?): String? = when {
    setup == null -> null
    setup.failed -> stringResource(
        if (setup.failure == DownloadError.Storage) {
            UiKitR.string.failure_storage_words
        } else {
            UiKitR.string.failure_download
        },
    )
    setup.progress != null -> stringResource(
        R.string.settings_pack_downloading,
        (setup.progress * 100).toInt(),
    )
    else -> stringResource(R.string.settings_pack_preparing)
}

/**
 * The packs of one kind, grouped by the language they speak, and alphabetical
 * inside each group. When [leadingLanguage] names a group, that group stands
 * first and the rest follow in the alphabetical order of their names: the
 * Translations page puts the language the reader is reading above the other
 * one, the same way the reading itself does (owner decision, this session).
 *
 * The grouping is what the list grows into. A heading costs its room once per
 * language, while a language printed under every name costs a line per pack,
 * so two English translations and two Bangla ones would say "English" and
 * "Bangla" twice each and still give the eye nothing to group. The heading is
 * the same one the settings pages use, so it is one style, one rhythm, and one
 * TalkBack jump whichever group a reader is in (owner decision, this session).
 *
 * The list spent 12.5 with no headings and the language in each row's own line
 * instead, which read as one flat list of names; with more than one pack per
 * language it stops being readable at all (owner report).
 */
@Composable
internal fun LanguageGroups(
    packs: List<ContentPack>,
    type: PackType,
    leadingLanguage: String? = null,
    row: @Composable (ContentPack) -> Unit,
) {
    val groups = packs.filter { it.type == type }.groupBy { it.language }
    val names = HashMap<String, String>()
    for (language in groups.keys) names[language] = languageName(language)
    groups.entries
        .sortedWith(
            compareBy(
                { if (leadingLanguage != null && it.key == leadingLanguage) 0 else 1 },
                { (names[it.key] ?: it.key).lowercase() },
            ),
        )
        .forEach { (language, group) ->
            Group(names[language] ?: language)
            // The names inside a group are read by the name the row draws, so
            // the order is the reader's own alphabet and not the catalog's
            // English one.
            group
                .map { pack -> pack to contentPackName(pack.id, pack.name) }
                .sortedBy { (_, name) -> name.lowercase() }
                .forEach { (pack, _) -> row(pack) }
        }
}
