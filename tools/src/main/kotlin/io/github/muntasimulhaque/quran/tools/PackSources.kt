package io.github.muntasimulhaque.quran.tools

/**
 * Which manifest datasets each pack is built from.
 *
 * A pack is what the reader installs; a dataset is what the project audits,
 * hashes, and credits. The two do not share names (the Saheeh International
 * pack is built from the translation dataset and the cross-check dataset, and
 * the English word list carries the surah introductions), so the mapping is
 * written down once, here, and both the database and the catalog read it. That
 * is what makes the credits screen in the app show the real license of every
 * dataset a pack contains instead of a note to go and read a file.
 */
object PackSources {

    private val sources: Map<String, List<String>> = mapOf(
        "core" to listOf(
            "quran-script-kfgqpc",
            "mushaf-glyph-v2",
            "mushaf-layout-v2",
            "quran-font-hafs",
            "mushaf-fonts-v2",
            "metadata-surah-names",
            "metadata-ayah",
            "metadata-juz",
            "metadata-sajda",
            "tanzil-uthmani",
        ),
        "translation-saheeh-en" to listOf("translation-saheeh", "saheeh-qul-crosscheck"),
        "tafsir-ibn-kathir-en" to listOf("tafsir-ibn-kathir-en"),
        "tafsir-as-sadi-ar" to listOf("tafsir-as-sadi-ar"),
        "translation-taisirul-quran-bn" to listOf("translation-taisirul-quran-bn"),
        "tafsir-ibn-kathir-bn" to listOf("tafsir-ibn-kathir-bn"),
        "words-en" to listOf("word-by-word-english", "surah-info-en"),
        "words-bn" to listOf("word-by-word-bengali"),
        "reciter-minshawi" to listOf("recitation-minshawi"),
        "reciter-husary" to listOf("recitation-husary"),
    )

    fun datasets(packId: String): List<String> = sources[packId].orEmpty()

    /** The licenses of a pack's datasets, each once, in the order they are listed. */
    fun licenses(manifest: Manifest, packId: String): List<String> =
        datasets(packId).mapNotNull { id -> manifest.datasets.firstOrNull { it.id == id }?.license }
            .filter { it.isNotBlank() }
            .distinct()
}
