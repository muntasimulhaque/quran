package io.github.muntasimulhaque.quran.data

/**
 * The languages the interface is written in.
 *
 * The choice is one: a reader who picks Bangla gets a Bangla interface, the
 * Bangla translation, the Bangla tafsir, and the Bangla word list. The
 * interface strings live in `values-bn`; which packs a language reads is the
 * catalog's `defaults` block, so a new translation in any language is a
 * content operation and a second English never touches this file
 * (owner decision, 20.2).
 *
 * The tag is the BCP 47 tag Android uses for a resource folder and a locale,
 * and it is what is stored in the reader's settings.
 */
enum class UiLanguage(val tag: String) {
    English("en"),
    Bangla("bn");

    companion object {
        /** The language a stored tag names, or null when it names none. */
        fun of(tag: String?): UiLanguage? = entries.firstOrNull { it.tag == tag }

        /**
         * The language this phone speaks, when it speaks one the app carries,
         * and English otherwise. A fresh install takes this answer without a
         * question: English is the fallback, not a screen
         * (owner decision, 15.5).
         */
        fun ofSystem(languageTag: String? = java.util.Locale.getDefault().language): UiLanguage =
            of(languageTag) ?: English
    }
}

/** The three packs one interface language reads by default. */
data class LanguageContent(
    val translation: String,
    val tafsir: String,
    val words: String,
) {
    /**
     * The packs a language choice asks the download queue for: its
     * translation at once, and its tafsir when the connection is unmetered.
     * The word list is not here, because it is the reader's own switch and
     * not a language's default (owner decision, 14.8).
     */
    val asked: Set<String> get() = setOf(translation, tafsir).filterTo(LinkedHashSet()) { it.isNotBlank() }
}

/**
 * The catalog as the language rules read it: every pack it carries, in the
 * catalog's own order, and the `defaults` block that names what each
 * interface language reads.
 *
 * It is a value, not a lookup, so every rule built on it is a pure function
 * that a test can pin without a device, and no screen has to know which
 * pack belongs to which language (owner decision, 20.2).
 */
data class CatalogContent(
    val packs: List<ContentPack> = emptyList(),
    val defaults: Map<String, LanguageContent> = emptyMap(),
) {
    /** What [language] reads. */
    fun of(language: UiLanguage): LanguageContent =
        resolveLanguageContent(language, packs, defaults)
}

/**
 * What one interface language reads, from the catalog's `defaults` block.
 *
 * The rule (owner decision, 20.2). A slot the block names for this language
 * is the answer when the catalog carries that pack. Otherwise the language
 * reads the first pack of the slot's type in its language, a bundled one
 * before a downloaded one and the catalog's own order inside each group, so
 * a language whose default nobody wrote still reads something real rather
 * than nothing. Otherwise the slot is empty, and the language reads no pack
 * of that type; a translation that arrives later in that language is then a
 * content update and a language default away, never a code edit.
 *
 * A named id the catalog does not carry is ignored and the same resolution
 * continues, so one typo in the catalog can never select a pack that does
 * not exist and can never take anything down.
 */
fun resolveLanguageContent(
    language: UiLanguage,
    packs: List<ContentPack>,
    defaults: Map<String, LanguageContent>,
): LanguageContent {
    val named = defaults[language.tag] ?: LanguageContent("", "", "")

    fun resolve(id: String, type: PackType): String {
        if (id.isNotBlank() && packs.any { it.id == id }) return id
        return packs
            .filter { it.type == type && it.language == language.tag }
            .minByOrNull { if (it.shipped) 0 else 1 }
            ?.id
            .orEmpty()
    }

    return LanguageContent(
        translation = resolve(named.translation, PackType.Translation),
        tafsir = resolve(named.tafsir, PackType.Tafsir),
        words = resolve(named.words, PackType.Words),
    )
}

/**
 * The page a fresh install opens on: the phone's language, its translation
 * selected, the reminder off and still unanswered, and the place at
 * Al-Fatiha. English is the fallback when the phone speaks neither language
 * the app carries. This is the first decision the app makes on the reader's
 * behalf, and it makes only the ones the phone already answered (owner
 * decision, 15.5).
 */
fun AppSettings.bootstrapFor(language: UiLanguage, content: CatalogContent): AppSettings =
    withLanguage(language, content).copy(
        ayah = 1,
        // The tafsir is a download the reader asks for, not a pack the first
        // open selects behind their back. The Tafsir action offers it when
        // they want it; the translation is the only content the first page
        // needs (owner decision, 15.5).
        tafsirPacks = emptySet(),
        dailyAyah = false,
        reminderAnswered = false,
    )

/**
 * The settings after a language choice: the tag is stored, and the language's
 * translation replaces the other offered language's own default, while its
 * tafsir replaces the other's only when the reader has chosen one at all,
 * so a reader who moves from Bangla to English never keeps two defaults
 * fighting, and a reader who never chose a tafsir is never handed one
 * (owner decision, 22.2). A pack that is not a language default is left
 * exactly where it was.
 */
fun AppSettings.withLanguage(language: UiLanguage, content: CatalogContent): AppSettings {
    val mine = content.of(language)
    val previous = UiLanguage.of(uiLanguage)
    return copy(
        uiLanguage = language.tag,
        translationPacks = translationPacks
            .minus(previous?.let { content.of(it) }?.translation.orEmpty())
            .plusPack(mine.translation),
        // The tafsir is swapped, never introduced (owner decision, 22.2): a
        // reader who has chosen no tafsir keeps none through a language
        // change, exactly as the first open leaves them none, so no
        // language change can queue a download the reader never asked for
        // (15.5). A reader who has chosen one is moved to the new language's
        // own, the way their translation is.
        tafsirPacks = if (tafsirPacks.isEmpty()) {
            tafsirPacks
        } else {
            tafsirPacks
                .minus(previous?.let { content.of(it) }?.tafsir.orEmpty())
                .plusPack(mine.tafsir)
        },
    )
}

/**
 * The set with one pack id added, and nothing added for a language that
 * reads no pack of that type: an empty slot must never put an empty id into
 * the reader's settings, where it would be a pack no catalog names
 * (owner decision, 20.2).
 */
private fun Set<String>.plusPack(id: String): Set<String> = if (id.isBlank()) this else this + id

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
    content: CatalogContent,
): Set<String> {
    val kept = selected.filterTo(LinkedHashSet()) { it in offered }
    val removed = selected.any { it !in offered }
    if (!removed) return kept
    val fallback = UiLanguage.of(language)?.let { content.of(it) }?.translation
    if (fallback != null && fallback in offered) kept += fallback
    return kept
}

/**
 * The download queue after a language choice: the packs the language the
 * reader left had asked for leave it, and the new language's packs join it.
 * A pack the reader added by hand is not a language's default and stays
 * exactly where it was. The tafsir joins the queue only when the reader's
 * own choice names it, so a language change swaps a chosen tafsir and
 * never introduces one (owner decision, 22.2).
 *
 * A queue entry used to outlive the language that asked for it, because a
 * pack that was already installed was skipped by the drain and never
 * removed. The day a content update detached that pack, the queue fetched it
 * again and the app treated the arrival as a choice, so a former language's
 * translation came back under the ayah with nothing on the glass to explain
 * it (owner report, 14.8).
 */
fun pendingAfterLanguage(
    previous: UiLanguage?,
    next: UiLanguage,
    pending: Set<String>,
    content: CatalogContent,
    chosenTafsir: Set<String>,
): Set<String> {
    val mine = content.of(next)
    // The tafsir is asked for only when the reader's choice names it,
    // the same rule withLanguage holds (owner decision, 22.2), so a
    // reader who never chose one is not handed a download behind their
    // back.
    val asked = LinkedHashSet<String>()
    if (mine.translation.isNotBlank()) asked += mine.translation
    if (mine.tafsir.isNotBlank() && mine.tafsir in chosenTafsir) asked += mine.tafsir
    return pending - previous?.let { content.of(it) }?.asked.orEmpty() + asked
}

/**
 * The packs that speak [language]: what search reads when the reader's chosen
 * language decides. A reader who reads in Bangla searches Bangla, even when a
 * pack of another language was added by hand, because one field over two
 * languages answers in two voices (owner decision, 6.5).
 */
fun List<ContentPack>.forLanguage(language: String): List<ContentPack> =
    filter { it.language == language }

/**
 * The packs in the order the reader reads them: the language the reader chose
 * first, then any other language, each group in the order the catalog gave it
 * (which is by name).
 *
 * A reader who keeps two translations on reads their own language first, and
 * the other column still follows, named. The catalog's own order is by name,
 * so "Al-Bayan Simple Translation" stood above "Saheeh International" in an
 * English reading, and the Font size sample, which draws the first line,
 * previewed Bangla under an English interface (owner report, 15.3).
 */
fun List<ContentPack>.leadingWith(language: String): List<ContentPack> =
    sortedBy { if (it.language == language) 0 else 1 }
