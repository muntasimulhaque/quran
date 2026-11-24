package io.github.muntasimulhaque.quran.tools

/**
 * The one registry of the packs this project ships.
 *
 * A pack is what the reader installs; a dataset is what the project audits,
 * hashes, and credits. Each pack's row carries both halves of that: what it
 * is called in English and Bangla, who made it, its version, whether the app
 * ships it or the reader downloads it, and which manifest datasets it is
 * built from. The catalog, the About page's credits, and the released files
 * all read this table, so adding, replacing, or retiring a translation, a
 * tafsir, a reciter, or a word list is one row here and one build step, never
 * a file in the app (owner decision, 20.2).
 *
 * The Saheeh cross-check is not listed: it is a build-time audit copy of the
 * same translation, never a byte of the pack. Listing it put its QUL line on
 * the pack's About page, which claimed a distribution route the pack does
 * not use (owner decision, 14.5).
 */
object PackSources {

    /**
     * One pack: the unit the reader installs, with the datasets it is built
     * from and every word the catalog needs to describe it.
     */
    data class Pack(
        val id: String,
        /** The pack's type, as the catalog's own wire name. */
        val type: String,
        val name: String,
        /**
         * The name in Bangla, the way a Bangla interface draws it. It rides in
         * the catalog beside the English name, so a pack is named by its own
         * entry and never by a table in the app (owner decision, 20.2).
         */
        val nameBn: String = "",
        val language: String,
        val credit: String,
        /** The pack's credit in Bangla, read beside the English one. */
        val creditBn: String = "",
        val version: String,
        val shipped: Boolean,
        val ayahs: Int,
        /** The manifest datasets this pack is built from. */
        val datasets: List<String>,
    )

    /**
     * One interface language's own reading: the translation, tafsir, and
     * word list the catalog's `defaults` block names for it.
     */
    data class LanguageDefault(
        val tag: String,
        val translation: String,
        val tafsir: String,
        val words: String,
    )

    /**
     * Every pack, in the order the catalog lists them. The order is the
     * catalog's and the download page's, so a pack keeps its place when
     * another joins it.
     */
    val packs: List<Pack> = listOf(
        Pack(
            id = "core",
            type = "script",
            name = "Quran text",
            nameBn = "কুরআনের লেখা",
            language = "ar",
            credit = "QPC Hafs, word by word, with tajweed; audited against the QUL QPC text",
            creditBn = "QPC \u09b9\u09be\u09ab\u09b8, \u09b6\u09ac\u09cd\u09a6\u09c7 \u09b6\u09ac\u09cd\u09a6\u09c7, \u09a4\u09be\u099c\u09ac\u09c0\u09a6\u09b8\u09b9; QUL-\u098f\u09b0 QPC \u09b2\u09c7\u0996\u09be\u09b0 \u09b8\u09be\u09a5\u09c7 \u09af\u09be\u099a\u09be\u0987\u0995\u09c3\u09a4",
            // The QPC switch replaced the core's content; the version moves
            // so a held core is replaced rather than kept (owner decision,
            // 10.4). Version 3 carries the four word merges and the works-
            // with-the-word-list positions. Version 4 drops the print's page
            // signs from the search index, so the first word under the rub el
            // hizb is found by a reader typing it (owner report, 10.7).
            // Version 5 carries the surah introductions, so every reader has
            // them without a word list.
            // Version 6 carries the recall column and its index, so a
            // reader's modern spelling finds a verse the exact index cannot
            // name (owner decision, 19.7).
            // Version 7 carries the recitation map, so any voice's audio
            // folder is readable without attaching that voice's timing pack
            // (owner decision, 20.0).
            // Version 8 narrows the map to the two voices the catalog
            // offers, Husary and Minshawi, so the map names no voice the
            // reader cannot choose (owner decision, 22.6).
            version = "8",
            shipped = true,
            ayahs = 6236,
            datasets = listOf(
                "quran-script-qpc-hafs",
                "quran-script-qpc-hafs-ayah",
                "quran-font-hafs",
                "metadata-surah-names",
                "surah-names-bn",
                "metadata-juz",
                "metadata-hizb",
                "metadata-rub",
                "metadata-manzil",
                "metadata-ruku",
                "metadata-sajda",
            ),
        ),
        Pack(
            id = "translation-saheeh-en",
            type = "translation",
            name = "Saheeh International",
            nameBn = "সহীহ ইন্টারন্যাশনাল",
            language = "en",
            credit = "Noor International Center, via QuranEnc; 3:32 corrected to the published reading",
            creditBn = "Noor International Center, QuranEnc-এর মাধ্যমে; ৩:৩২ প্রকাশিত পাঠ অনুযায়ী সংশোধিত",
            version = "1.1.2+2",
            shipped = true,
            ayahs = 6236,
            datasets = listOf("translation-saheeh"),
        ),
        Pack(
            id = "tafsir-ibn-kathir-en",
            type = "tafsir",
            name = "Ibn Kathir",
            nameBn = "ইবনে কাসীর",
            language = "en",
            credit = "Tafsir Ibn Kathir, via the Quranic Universal Library",
            creditBn = "\u09a4\u09be\u09ab\u09b8\u09c0\u09b0 \u0987\u09ac\u09a8\u09c7 \u0995\u09be\u09b8\u09c0\u09b0, \u0995\u09c1\u09b0\u0986\u09a8\u09bf\u0995 \u0987\u0989\u09a8\u09bf\u09ad\u09be\u09b0\u09cd\u09b8\u09be\u09b2 \u09b2\u09be\u0987\u09ac\u09cd\u09b0\u09c7\u09b0\u09bf \u09a5\u09c7\u0995\u09c7",
            version = "QUL",
            shipped = false,
            ayahs = 6236,
            datasets = listOf("tafsir-ibn-kathir-en"),
        ),
        Pack(
            id = "translation-al-bayan-bn",
            type = "translation",
            name = "Al-Bayan Simple Translation",
            nameBn = "আল-বায়ান সরল অর্থানুবাদ",
            language = "bn",
            credit = "Al-Bayan Foundation Bangladesh, via the Quranic Universal Library and IslamHouse.com",
            creditBn = "আল-বায়ান ফাউন্ডেশন বাংলাদেশ, কুরআনিক ইউনিভার্সাল লাইব্রেরি ও ইসলামহাউস থেকে",
            version = "1.0.1",
            shipped = true,
            ayahs = 6236,
            datasets = listOf("translation-al-bayan-bn", "translation-al-bayan-bn-notes"),
        ),
        Pack(
            id = "tafsir-ibn-kathir-bn",
            type = "tafsir",
            name = "Ibn Kathir",
            nameBn = "ইবনে কাসীর",
            language = "bn",
            credit = "Tafsir Ibn Kathir (Bangla), via the Quranic Universal Library",
            creditBn = "\u09a4\u09be\u09ab\u09b8\u09c0\u09b0 \u0987\u09ac\u09a8\u09c7 \u0995\u09be\u09b8\u09c0\u09b0 (\u09ac\u09be\u0982\u09b2\u09be), \u0995\u09c1\u09b0\u0986\u09a8\u09bf\u0995 \u0987\u0989\u09a8\u09bf\u09ad\u09be\u09b0\u09cd\u09b8\u09be\u09b2 \u09b2\u09be\u0987\u09ac\u09cd\u09b0\u09c7\u09b0\u09bf \u09a5\u09c7\u0995\u09c7",
            version = "QUL",
            shipped = false,
            ayahs = 6236,
            datasets = listOf("tafsir-ibn-kathir-bn"),
        ),
        Pack(
            id = "reciter-minshawi",
            type = "recitation",
            name = "Minshawi",
            nameBn = "মিনশাবি",
            language = "ar",
            credit = "Muhammad Siddiq Al-Minshawi, via the Quranic Universal Library",
            creditBn = "\u09ae\u09c1\u09b9\u09be\u09ae\u09cd\u09ae\u09a6 \u09b8\u09bf\u09a6\u09cd\u09a6\u09bf\u0995 \u0986\u09b2-\u09ae\u09bf\u09a8\u09b6\u09be\u09ac\u09bf, \u0995\u09c1\u09b0\u0986\u09a8\u09bf\u0995 \u0987\u0989\u09a8\u09bf\u09ad\u09be\u09b0\u09cd\u09b8\u09be\u09b2 \u09b2\u09be\u0987\u09ac\u09cd\u09b0\u09c7\u09b0\u09bf \u09a5\u09c7\u0995\u09c7",
            version = "quran-align-2016-11-24",
            shipped = true,
            ayahs = 6236,
            datasets = listOf("recitation-minshawi", "quran-align-minshawi"),
        ),
        Pack(
            id = "reciter-husary",
            type = "recitation",
            name = "Husary",
            nameBn = "হুসারি",
            language = "ar",
            credit = "Mahmoud Khalil Al-Husary, via the Quranic Universal Library",
            creditBn = "\u09ae\u09be\u09b9\u09ae\u09c1\u09a6 \u0996\u09b2\u09bf\u09b2 \u0986\u09b2-\u09b9\u09c1\u09b8\u09be\u09b0\u09bf, \u0995\u09c1\u09b0\u0986\u09a8\u09bf\u0995 \u0987\u0989\u09a8\u09bf\u09ad\u09be\u09b0\u09cd\u09b8\u09be\u09b2 \u09b2\u09be\u0987\u09ac\u09cd\u09b0\u09c7\u09b0\u09bf \u09a5\u09c7\u0995\u09c7",
            version = "quran-align-2016-11-24",
            shipped = true,
            ayahs = 6236,
            datasets = listOf("recitation-husary", "quran-align-husary"),
        ),
        Pack(
            id = "words-en",
            type = "words",
            name = "Word meanings",
            nameBn = "শব্দের অর্থ",
            language = "en",
            credit = "Quran.com word by word, via the Quranic Universal Library; grammar from the Quranic Arabic Corpus (Dr. Kais Dukes)",
            creditBn = "Quran.com শব্দে শব্দে, কুরআনিক ইউনিভার্সাল লাইব্রেরি থেকে; ব্যাকরণ কুরআনিক আরবিক কর্পাস (ড. কায়েস ডিউকস) থেকে",
            // Version 2 carries every word's grammar and lemma, beside its
            // meaning: the sheet's study lines arrive with the word list the
            // reader already turned on. Version 3 carries the lemma index the
            // places page reads, so a dictionary form's places are one lookup
            // rather than a pass over the whole list
            // (owner decision, 18.9).
            version = "QUL 3",
            shipped = false,
            ayahs = 6236,
            datasets = listOf("word-by-word-english", "surah-info-en", "word-morphology"),
        ),
        Pack(
            id = "words-bn",
            type = "words",
            name = "Word meanings",
            nameBn = "শব্দের অর্থ",
            language = "bn",
            credit = "Bangla word by word, via the Quranic Universal Library; grammar from the Quranic Arabic Corpus (Dr. Kais Dukes)",
            creditBn = "বাংলা শব্দে শব্দে, কুরআনিক ইউনিভার্সাল লাইব্রেরি থেকে; ব্যাকরণ কুরআনিক আরবিক কর্পাস (ড. কায়েস ডিউকস) থেকে",
            // Version 2 carries every word's grammar and lemma, beside its
            // meaning: the sheet's study lines arrive with the word list the
            // reader already turned on. Version 3 carries the lemma index the
            // places page reads (owner decision, 18.9).
            version = "QUL 3",
            shipped = false,
            ayahs = 6236,
            datasets = listOf("word-by-word-bengali", "word-morphology"),
        ),
        // The root is one small table for the whole Book and no language of
        // its own: the letters are Arabic and the Latin beside them is the
        // letters' names (owner decision, 17.7).
        Pack(
            id = "word-root",
            type = "word-root",
            name = "Word roots",
            nameBn = "শব্দের মূল",
            language = "ar",
            credit = "Quranic morphology, via the Quranic Universal Library",
            creditBn = "\u0995\u09c1\u09b0\u0986\u09a8\u09bf\u0995 \u09ae\u09b0\u09ab\u09cb\u09b2\u099c\u09bf, \u0995\u09c1\u09b0\u0986\u09a8\u09bf\u0995 \u0987\u0989\u09a8\u09bf\u09ad\u09be\u09b0\u09cd\u09b8\u09be\u09b2 \u09b2\u09be\u0987\u09ac\u09cd\u09b0\u09c7\u09b0\u09bf \u09a5\u09c7\u0995\u09c7",
            // Version 2 writes every root's count from its own rows, so the
            // count the sheet names is the list the door opens, and adds the
            // root index the places page reads. The source's own count
            // disagreed for fourteen roots; the pack's rows are the app's
            // truth (owner decision, 18.9).
            version = "QUL 2",
            shipped = false,
            ayahs = 6236,
            datasets = listOf("word-root"),
        ),
    )

    /**
     * The word pronunciation pack. It has no database of its own: its
     * manifest names one ZIP per surah, and the Al-Fatiha files ride in the
     * app's assets. The catalog entry is what the About page reads for the
     * voice, the source, and the CC BY 4.0 timing credit.
     *
     * The pronunciation is cut from QUL Alafasy ayah audio at quran-align
     * boundaries. Both are credited; the app ships only the word clips. A
     * second voice's word audio is a pack of this type with its own row, not
     * a change in the app (owner decision, 20.2).
     */
    val wordAudio: Pack = Pack(
        id = "word-pronunciation-alafasy",
        type = "word-audio",
        name = "Word pronunciation",
        language = "ar",
        credit = "Mishari Rashid al-Afasy, via the Quranic Universal Library; word timings by quran-align",
        creditBn = "মিশারি রশিদ আল-আফাসি, কুরআনিক ইউনিভার্সাল লাইব্রেরি থেকে; শব্দের সময় quran-align",
        version = "1.0",
        shipped = false,
        ayahs = 6236,
        datasets = listOf("quran-recitation-alafasy", "quran-align-alafasy"),
    )

    /**
     * What each interface language reads, written into the catalog's
     * `defaults` block.
     *
     * A language's own translation, tafsir, and word list are content: a
     * second English, or a translation in a language the app carries no
     * interface for yet, is a pack and a row here, never a constant in the
     * app. The app's interface languages are its `values-xx` folders, so a
     * new *interface* language is still a translation pass and a folder; a
     * new *content* language is a row in this table
     * (owner decision, 20.2).
     */
    val languageDefaults: List<LanguageDefault> = listOf(
        LanguageDefault(
            tag = "en",
            translation = "translation-saheeh-en",
            tafsir = "tafsir-ibn-kathir-en",
            words = "words-en",
        ),
        LanguageDefault(
            tag = "bn",
            translation = "translation-al-bayan-bn",
            tafsir = "tafsir-ibn-kathir-bn",
            words = "words-bn",
        ),
    )

    /** Every pack this project ships, the pronunciation set included. */
    val all: List<Pack> get() = packs + wordAudio

    /** One pack by id, or null when no row names it. */
    fun byId(id: String): Pack? = all.firstOrNull { it.id == id }

    fun datasets(packId: String): List<String> = byId(packId)?.datasets.orEmpty()

    /**
     * The licenses of a pack's datasets, each once, in the order they are
     * listed. With [bangla], each dataset's own Bangla line is read when the
     * manifest carries one, and its English line when it does not, so a pack
     * can never lose a license by not being translated
     * (owner decision, 8.8).
     */
    fun licenses(manifest: Manifest, packId: String, bangla: Boolean = false): List<String> =
        datasets(packId).mapNotNull { id ->
            val dataset = manifest.datasets.firstOrNull { it.id == id } ?: return@mapNotNull null
            if (bangla) {
                dataset.licenseBn?.takeIf { it.isNotBlank() } ?: dataset.license
            } else {
                dataset.license
            }
        }
            .filter { it.isNotBlank() }
            .distinct()
}
