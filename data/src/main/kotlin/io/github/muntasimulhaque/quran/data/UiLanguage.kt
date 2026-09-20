package io.github.muntasimulhaque.quran.data

/**
 * The languages the interface is written in, and the content each one reads.
 *
 * The choice is one: a reader who picks Bangla gets a Bangla interface, the
 * Bangla translation, the Bangla tafsir, and the Bangla word list. The
 * interface strings live in `values-bn`; this enum is the code side of the
 * same decision, and it is the only place that maps a language to its packs,
 * so no screen has to know which translation belongs to which language.
 *
 * The tag is the BCP 47 tag Android uses for a resource folder and a locale,
 * and it is what is stored in the reader's settings.
 */
enum class UiLanguage(val tag: String) {
    English("en"),
    Bangla("bn");

    /** The packs this language reads by default. */
    val content: LanguageContent
        get() = when (this) {
            English -> LanguageContent(
                translation = TRANSLATION_EN,
                tafsir = TAFSIR_EN,
                words = ContentDatabase.wordsPackId("en"),
            )
            Bangla -> LanguageContent(
                translation = TRANSLATION_BN,
                tafsir = TAFSIR_BN,
                words = ContentDatabase.wordsPackId("bn"),
            )
        }

    companion object {
        /** The language the system asks for, or English when it is not offered. */
        fun suggested(systemTag: String?): UiLanguage = of(systemTag) ?: English

        /** The language a stored tag names, or null when it names none. */
        fun of(tag: String?): UiLanguage? = entries.firstOrNull { it.tag == tag }

        /** The id of the Saheeh International translation pack. */
        const val TRANSLATION_EN = "translation-saheeh-en"

        /** The id of the Taisirul Quran translation pack. */
        const val TRANSLATION_BN = "translation-taisirul-quran-bn"

        /** The id of the English Ibn Kathir pack. */
        const val TAFSIR_EN = "tafsir-ibn-kathir-en"

        /** The id of the Bangla Ibn Kathir pack. */
        const val TAFSIR_BN = "tafsir-ibn-kathir-bn"
    }
}

/** The three packs one interface language reads by default. */
data class LanguageContent(
    val translation: String,
    val tafsir: String,
    val words: String,
)

/**
 * The settings after a language choice: the tag is stored, and the language's
 * translation and tafsir replace the other offered language's own defaults,
 * so a reader who moves from Bangla to English never keeps two defaults
 * fighting. A pack the reader added by hand, like As-Sa'di, is never removed.
 */
fun AppSettings.withLanguage(language: UiLanguage): AppSettings {
    val content = language.content
    val previous = UiLanguage.of(uiLanguage)
    return copy(
        uiLanguage = language.tag,
        translationPacks = translationPacks
            .minus(previous?.content?.translation.orEmpty())
            .plus(content.translation),
        tafsirPacks = tafsirPacks
            .minus(previous?.content?.tafsir.orEmpty())
            .plus(content.tafsir),
    )
}

