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
        /** The language a stored tag names, or null when it names none. */
        fun of(tag: String?): UiLanguage? = entries.firstOrNull { it.tag == tag }

        /** The id of the Saheeh International translation pack. */
        const val TRANSLATION_EN = "translation-saheeh-en"

        /** The id of the Al-Bayan Simple Translation pack. */
        const val TRANSLATION_BN = "translation-al-bayan-bn"

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
 * fighting. A pack that is not a language default is left exactly where it
 * was.
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

/**
 * The translations a reader keeps after a pack leaves the build.
 *
 * A pack removed from the catalog is still named in the reader's stored set.
 * The unknown ids are dropped. If any of them was a translation the reader
 * had chosen, the language's own default takes its place, so a Bangla reader
 * who had the retired Bangla pack keeps a Bangla translation instead of a
 * silent gap. A reader who removed that default themselves has no unknown id
 * and is left alone.
 */
fun translationSelectionAfterRemoval(
    language: String?,
    selected: Set<String>,
    offered: Set<String>,
): Set<String> {
    val kept = selected.filterTo(LinkedHashSet()) { it in offered }
    val removed = selected.any { it !in offered }
    if (!removed) return kept
    val fallback = UiLanguage.of(language)?.content?.translation
    if (fallback != null && fallback in offered) kept += fallback
    return kept
}

/**
 * The packs that speak [language]: what search reads when the reader's chosen
 * language decides. A reader who reads in Bangla searches Bangla, even when a
 * pack of another language was added by hand, because one field over two
 * languages answers in two voices (owner decision, this session).
 */
fun List<ContentPack>.forLanguage(language: String): List<ContentPack> =
    filter { it.language == language }

