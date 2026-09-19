package io.github.muntasimulhaque.quran.ui.kit

/**
 * The name of a content language, as a reader says it. The pack catalog
 * carries ISO codes; every surface that shows one shows it through this, so
 * the same language is never named two ways.
 */
fun languageName(code: String): String = when (code) {
    "ar" -> "Arabic"
    "bn" -> "Bangla"
    "de" -> "German"
    "en" -> "English"
    "es" -> "Spanish"
    "fa" -> "Persian"
    "fr" -> "French"
    "ha" -> "Hausa"
    "hi" -> "Hindi"
    "id" -> "Indonesian"
    "ml" -> "Malayalam"
    "ms" -> "Malay"
    "ps" -> "Pashto"
    "ru" -> "Russian"
    "so" -> "Somali"
    "sw" -> "Swahili"
    "ta" -> "Tamil"
    "tr" -> "Turkish"
    "ur" -> "Urdu"
    "zh" -> "Chinese"
    else -> code.uppercase()
}

/**
 * The key languages are sorted by: the lowercased name the reader reads, so a
 * list of languages is alphabetical in the interface's own alphabet rather
 * than in ISO codes, which would scatter the list for no reason a reader can
 * see.
 */
fun languageSortKey(code: String): String = languageName(code).lowercase()
