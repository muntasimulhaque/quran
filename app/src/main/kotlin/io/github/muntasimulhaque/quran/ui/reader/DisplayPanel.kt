package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.feature.settings.R as SettingsR
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.kit.AppPopup
import io.github.muntasimulhaque.quran.ui.kit.MenuActionRow
import io.github.muntasimulhaque.quran.ui.kit.MenuSwitchRow
import io.github.muntasimulhaque.quran.ui.kit.PopupAlignment
import io.github.muntasimulhaque.quran.ui.settings.SizeRow

/**
 * The reading's display, in the app's one popup, hung from the sliders mark in
 * the summoned top bar.
 *
 * A reader who wants the page larger used to leave the page: the bar, the
 * Settings sheet, the Text page, a step, and back again. The sizes and the
 * reading's three switches now stand where the page is, in the same popup the
 * pill opens, and Settings stays the home of everything a reader changes
 * rarely (owner decision, this session).
 *
 * The panel is a second hand on values that already have a home, never a
 * second setting: the Quran's and the translation's ladders are the Text
 * page's own rows, drawn by [SizeRow], and the three switches are the hub's
 * own words. The word list's size and the tafsir's stay in Settings, because
 * the aid is a setup choice and the tafsir is on the ayah card and not on the
 * page; a fourth ladder here would make the panel a sheet.
 *
 * **The switches lead and the sizes follow** (owner decision, this session).
 * A switch that turns the translation on would otherwise push itself down the
 * panel by the height of the ladder it reveals, out from under the finger
 * that just tapped it. The switches hold the top, the ladders sit under them,
 * and the translation's ladder appears in place when the translation does.
 *
 * The switches are dimmed while the pack they draw has nothing on the device:
 * the panel has no pack list to send the reader to, so the row stays where it
 * is and All settings at the foot is the way to add what is missing.
 */
@Composable
internal fun DisplayPanel(
    /** The sliders mark's own place on the glass, measured where it is drawn. */
    anchor: IntRect,
    settings: AppSettings,
    hasTranslation: Boolean,
    hasWords: Boolean,
    onTypeSize: (TypeRole, Float) -> Unit,
    onShowTranslation: (Boolean) -> Unit,
    onWordByWord: (Boolean) -> Unit,
    onTajweed: (Boolean) -> Unit,
    onAllSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppPopup(
        anchor = anchor,
        onDismiss = onDismiss,
        alignment = PopupAlignment.Center,
        name = stringResource(R.string.display_title),
    ) {
        MenuSwitchRow(
            label = stringResource(SettingsR.string.settings_show_translation_title),
            checked = settings.showTranslation && hasTranslation,
            onCheckedChange = onShowTranslation,
            enabled = hasTranslation,
            tag = "panel-translation",
        )
        MenuSwitchRow(
            label = stringResource(SettingsR.string.settings_words_title),
            checked = settings.wordByWord,
            onCheckedChange = onWordByWord,
            enabled = hasWords,
            tag = "panel-words",
        )
        MenuSwitchRow(
            label = stringResource(SettingsR.string.settings_tajweed_title),
            checked = settings.tajweed,
            onCheckedChange = onTajweed,
            tag = "panel-tajweed",
        )
        SizeRow(
            role = TypeRole.Arabic,
            step = settings.arabicSize,
            onChange = { onTypeSize(TypeRole.Arabic, it) },
            gutter = SizeGutter,
        )
        if (hasTranslation && settings.showTranslation) {
            SizeRow(
                role = TypeRole.Translation,
                step = settings.translationSize,
                onChange = { onTypeSize(TypeRole.Translation, it) },
                gutter = SizeGutter,
            )
        }
        MenuActionRow(
            icon = Icon.Settings,
            label = stringResource(R.string.display_all_settings),
            onClick = onAllSettings,
            modifier = Modifier.testTag("display-all-settings"),
        )
    }
}

/**
 * The five steps' own room: the popup's 288 less the 250 the five 48 dp cells
 * and their pill take. Nineteen a side is the only width that lets the app's
 * one step row stand inside the app's one popup without a cell giving up the
 * finger's target (owner decision, this session).
 */
private val SizeGutter = 19.dp
