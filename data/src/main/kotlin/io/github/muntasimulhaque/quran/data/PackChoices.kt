package io.github.muntasimulhaque.quran.data

/**
 * What the reader's choices say about the packs on this device.
 *
 * These are the five questions the settings hub, the pack pages, and the
 * reading ask about a translation, a tafsir, and the word list, and they are
 * answered here so the app and the settings module cannot answer them two
 * ways. They were pure functions in the settings module, whose JVM suite the
 * release gates never ran; a rule nobody measures is a rule nobody hears
 * break, so the decisions moved to the module whose suite is a gate
 * (owner decision, 21.5).
 *
 * The functions are plain: a setting, a catalog, and an answer. Nothing here
 * draws, downloads, or remembers.
 */

/**
 * The translations the reading draws with: the ones the reader chose and that
 * are on the device. A pack that is chosen but not yet installed is not
 * counted, so a line that reads this never promises a translation the reading
 * cannot draw (owner decision, 14.8).
 */
fun chosenTranslations(settings: AppSettings, packs: List<ContentPack>): List<ContentPack> =
    packs.filter { it.id in settings.translationPacks && it.installed }

/** The tafsirs the reading draws with: chosen, and on the device (owner decision, 14.8). */
fun chosenTafsirs(settings: AppSettings, packs: List<ContentPack>): List<ContentPack> =
    packs.filter { it.type == PackType.Tafsir && it.installed && it.id in settings.tafsirPacks }

/**
 * Whether the reading has a translation it can draw, which is also whether
 * the switch can be used. It is not the switch's state: that is the reader's
 * own answer, and a chosen pack that is not on the device leaves the switch
 * on and disabled rather than off, so the row never reports a choice the
 * reader did not make (owner decision, 14.8: the switch used to read
 * on only when a translation could be drawn).
 */
fun hasInstalledTranslation(settings: AppSettings, packs: List<ContentPack>): Boolean =
    chosenTranslations(settings, packs).isNotEmpty()

/**
 * Whether the reader has chosen a translation at all, here or not. The
 * switch's state is this and the reader's own answer; its usability is
 * [hasInstalledTranslation]. The two questions are drawn apart on purpose: a
 * chosen pack that is not on the device yet is still the reader's choice, and
 * the switch says so while it waits (owner decision, 14.8).
 */
fun hasChosenTranslation(settings: AppSettings, packs: List<ContentPack>): Boolean =
    packs.any { it.id in settings.translationPacks }

/**
 * The word list the reading speaks: the one that matches the reader's own
 * language, and no other. There is no fallback to English: a Bangla reader
 * whose list has not landed yet is asked to add the Bangla list, never
 * handed the English one (owner decision, 6.6). Null when the
 * catalog carries no list for the language, so the row can say nothing
 * rather than promise a list that does not exist.
 */
fun wantedWordsPack(settings: AppSettings, packs: List<ContentPack>): ContentPack? {
    val language = settings.readingLanguage
    return packs.firstOrNull { it.id == ContentDatabase.wordsPackId(language) }
}
