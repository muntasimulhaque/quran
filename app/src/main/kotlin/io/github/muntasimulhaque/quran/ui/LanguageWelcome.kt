package io.github.muntasimulhaque.quran.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import java.util.Locale

/**
 * The one screen before the reading, shown once: which language the app
 * speaks. The choice is not only the interface: the translation, the tafsir,
 * and the word meanings all follow it, so a reader who reads in Bangla is
 * never handed an English column and asked to change it later.
 *
 * The screen itself speaks the system's language when the system speaks one
 * of the offered two, and each language is named in its own script, so the
 * page is understood before it is answered. It is shown until the choice is
 * made, and the choice is remembered; the language can be changed at any
 * time from Settings.
 */
@Composable
fun LanguageWelcome(
    suggested: UiLanguage,
    onChoose: (UiLanguage) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.first_paint_title),
                style = TextStyle(
                    fontFamily = Amiri,
                    fontSize = 54.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                ),
            )
            Text(
                text = stringResource(R.string.first_paint_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 40.dp),
            )
            Text(
                text = stringResource(R.string.welcome_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
            Column(
                modifier = Modifier.padding(top = 26.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                UiLanguage.entries.forEach { language ->
                    LanguageCard(
                        language = language,
                        suggested = language == suggested,
                        onClick = { onChoose(language) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.welcome_later),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

/** One language, named in its own script, with a quiet mark when the phone speaks it. */
@Composable
private fun LanguageCard(
    language: UiLanguage,
    suggested: Boolean,
    onClick: () -> Unit,
) {
    val name = remember(language) { nativeName(language) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 22.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (suggested) {
                Text(
                    text = stringResource(R.string.welcome_suggested),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        IconGlyph(
            icon = Icon.Chevron,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .rotate(-90f),
        )
    }
}

/**
 * A language's name as the language itself writes it: English is English and
 * Bangla is বাংলা whatever the rest of the interface says, because the page
 * that asks the question cannot assume its own answer.
 */
private fun nativeName(language: UiLanguage): String {
    val locale = Locale.forLanguageTag(language.tag)
    return locale.getDisplayName(locale)
}

/** The language the phone itself asks for, resolved to one of the offered two. */
@Composable
fun systemLanguage(): UiLanguage =
    UiLanguage.suggested(LocalConfiguration.current.locales[0].language)

