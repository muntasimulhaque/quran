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
 * How the app names a language wherever the reader chooses one: the
 * language's own script first, so a reader who cannot read the current
 * interface still finds their own, and the English name beside it in
 * parentheses, so a reader who knows only one of the two names still knows
 * which row is which. English carries no second name: English is its own
 * answer, and "English (English)" would say nothing twice. The platform's
 * locale data writes both names, so no list of languages lives in this file.
 */
fun languageChoiceName(code: String): String {
    val locale = Locale.forLanguageTag(code)
    val own = locale.getDisplayName(locale)
    if (locale.language == Locale.ENGLISH.language) return own
    return "$own (${locale.getDisplayName(Locale.ENGLISH)})"
}

