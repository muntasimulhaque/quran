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
        "translation-saheeh-en" to listOf("translation-saheeh", "saheeh-qul-crosscheck"),
        "tafsir-ibn-kathir-en" to listOf("tafsir-ibn-kathir-en"),
        "translation-al-bayan-bn" to listOf("translation-al-bayan-bn", "translation-al-bayan-bn-notes"),
        "tafsir-ibn-kathir-bn" to listOf("tafsir-ibn-kathir-bn"),
        "words-en" to listOf("word-by-word-english", "surah-info-en"),
        "words-bn" to listOf("word-by-word-bengali"),
        "reciter-minshawi" to listOf("recitation-minshawi"),
        "reciter-husary" to listOf("recitation-husary"),
    )

    fun datasets(packId: String): List<String> = sources[packId].orEmpty()

    /**
     * The licenses of a pack's datasets, each once, in the order they are
     * listed. With [bangla], each dataset's own Bangla line is read when the
     * manifest carries one, and its English line when it does not, so a pack
     * can never lose a license by not being translated (owner decision, this
     * session).
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
