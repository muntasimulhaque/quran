package io.github.muntasimulhaque.quran.ui.kit

/**
 * The name of a content language, as a reader says it. The pack catalog
 * carries ISO codes; every surface that shows one shows it through this, so
 * the same language is never named two ways.
 */
fun languageName(code: String): String = when (code) {
    "ar" -> "Arabic"
    "bn" -> "Bengali"
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
