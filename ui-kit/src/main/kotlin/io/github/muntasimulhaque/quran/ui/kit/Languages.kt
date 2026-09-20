package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/**
 * The name of a content language, as a reader says it in the interface's own
 * language: English shows "Bangla", and a Bangla interface shows "বাংলা".
 * The platform's own locale data carries every name, so no list of languages
 * is written by hand here and a new pack names itself.
 */
@Composable
fun languageName(code: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(code, locale) { Locale.forLanguageTag(code).getDisplayName(locale) }
}

/**
 * A language's name as the language itself writes it, whatever the interface
 * says. The page that asks a reader to choose a language cannot assume the
 * reader understands its own question, so each choice on it is named in the
 * script it belongs to: English, বাংলা.
 */
fun nativeLanguageName(code: String): String {
    val locale = Locale.forLanguageTag(code)
    return locale.getDisplayName(locale)
}

