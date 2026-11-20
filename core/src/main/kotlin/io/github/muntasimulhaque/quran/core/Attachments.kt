package io.github.muntasimulhaque.quran.core

/**
 * The working set one screen may hold on its database connection, and the
 * budget it must stay inside.
 *
 * The library on the device and the library on the glass are two different
 * things. A reader may keep fifty translations and hear one voice; the page
 * draws two columns, the card shows one tafsir, the word sheet reads one
 * list and its root. So the set handed to SQLite is what the screen is
 * reading, never what the phone is holding: the files stay files until a
 * surface asks for one.
 *
 * The set exists because SQLite attaches at most ten databases to a
 * connection (owner report, 20.5: the reminder attached every installed
 * voice, the last pack fell past the tenth, and a downloaded word list read
 * as missing). [BUDGET] keeps one of the ten spare, and the gate in
 * `:tools` fails the build when the catalog could push any surface past it,
 * so a set that does not fit is caught on the bench and never in a
 * reader's hands.
 *
 * The order is priority, and the first entries are what a reader would miss
 * least to lose: the translations the page draws, the word list and its
 * root, the voice being heard, and the tafsirs the card may open. A
 * truncation would keep the reading whole before it kept an unopened card,
 * but the gate is what makes a truncation impossible.
 */
object Attachments {

    /** Attached databases one connection may hold, one under SQLite's ten. */
    const val BUDGET = 9

    /**
     * The most translation columns one page may hold.
     *
     * A page of the Book with four translations under every ayah is already
     * a comparison; a fifth adds height and not reading. Four is also the
     * number that leaves the budget its other packs: four columns, the word
     * list, its root, one voice, and the two tafsir cards a reading language
     * offers make nine, exactly [BUDGET] (owner decision, 20.5). The reader
     * may install any number; the page shows four, and one turned off makes
     * room for another.
     */
    const val COLUMNS = 4

    /**
     * Everything the reading's screen can read: its translation columns,
     * the word list and its root, the voice being heard, and the tafsirs the
     * ayah card offers. All of them are the reader's enabled set, never the
     * shelf.
     */
    fun screen(
        translations: List<String>,
        tafsirs: List<String>,
        words: String? = null,
        root: String? = null,
        voice: String? = null,
    ): List<String> = within(
        buildList {
            addAll(translations.distinct().take(COLUMNS))
            words?.let(::add)
            root?.let(::add)
            voice?.let(::add)
            addAll(tafsirs)
        },
    )

    /**
     * The daily reminder: one ayah, and the translation it shows. Nothing
     * else is read, so nothing else is attached.
     */
    fun reminder(translations: List<String>): List<String> = within(translations)

    /**
     * One search: the enabled packs of the language being searched and its
     * word list. A reader with nine installed translations and one enabled
     * searches one; the other eight keep their files and their rows.
     */
    fun search(
        translations: List<String>,
        tafsirs: List<String>,
        words: String? = null,
    ): List<String> = within(
        buildList {
            addAll(translations)
            words?.let(::add)
            addAll(tafsirs)
        },
    )

    /** The set as a connection takes it: distinct, in priority order, in budget. */
    private fun within(ids: List<String>): List<String> = ids.distinct().take(BUDGET)
}
