package io.github.muntasimulhaque.quran.data

import io.github.muntasimulhaque.quran.core.SearchQuery

data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameSimple: String,
    val nameLatin: String,
    val revelationPlace: String,
    val versesCount: Int,
    val bismillahPre: Boolean = false,
)

data class PageLine(
    val line: Int,
    val type: String,
    val centered: Boolean,
    val firstWordId: Int,
    val lastWordId: Int,
    val surah: Int,
)

data class Word(
    val id: Int,
    val position: Int,
    val text: String,
    val glyph: String,
    val translation: String?,
)

/** One glyph on a Mushaf page, with the ayah and word it belongs to. */
data class PageWord(
    val id: Int,
    val ayah: Int,
    val position: Int,
    val marker: Boolean,
    val glyph: String,
)

data class Ayah(
    val number: Int,
    val surah: Int,
    val ayah: Int,
    val verseKey: String,
    val text: String,
)

data class AyahLocation(val ayah: Ayah, val page: Int)

data class Footnote(val number: Int, val text: String)

data class TranslationText(val text: String, val footnotes: List<Footnote>)

/**
 * One translation of one ayah, with the pack it came from. More than one may
 * be turned on, and each is drawn in its own column under the ayah, so the
 * line carries the name it belongs to rather than staying anonymous.
 */
data class TranslationLine(
    val pack: String,
    val packName: String,
    val language: String,
    val text: TranslationText,
)

data class WordMeaning(val word: String, val meaning: String?)

data class TafsirPassage(
    val source: String,
    val surah: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val text: String,
)

data class Recitation(
    val id: String,
    val name: String,
    val credit: String,
)

data class WordSegment(
    val wordFrom: Int,
    val wordTo: Int,
    val startMs: Long,
    val endMs: Long,
)

data class RecitationAyah(
    val audioPath: String,
    val segments: List<WordSegment>,
)

sealed interface SearchHit {

    data class SurahHit(val surah: Surah) : SearchHit

    /** A direct hit on a reference the reader typed; always first. */
    data class ReferenceHit(val ayah: Ayah, val page: Int) : SearchHit

    /**
     * One ayah, with every source that matched it. A row can carry the Arabic
     * match, the selected translation, a word meaning, and one tafsir at once.
     */
    data class AyahHit(
        val ayah: Ayah,
        val page: Int,
        val arabicMatchedWords: Set<Int>,
        val translation: TranslationHit?,
        val wordMeaning: String?,
    ) : SearchHit

    /** A tafsir passage, which usually spans several ayahs. */
    data class TafsirHitResult(
        val pack: String,
        val packName: String,
        val surah: Int,
        val surahName: String,
        val fromAyah: Int,
        val toAyah: Int,
        val ayah: Ayah,
        val page: Int,
        val text: String,
        val ranges: List<IntRange>,
    ) : SearchHit
}

/** A matched translation, with the ranges to mark inside it. */
data class TranslationHit(
    val pack: String,
    val packName: String,
    val text: String,
    val ranges: List<IntRange>,
    val footnotes: List<Footnote>,
)

/** How many matches each source produced, for the one line summary. */
data class SearchCounts(
    val arabic: Int = 0,
    val translation: Int = 0,
    val tafsir: Int = 0,
    val words: Int = 0,
    val surahs: Int = 0,
) {
    val total: Int get() = arabic + translation + tafsir + words + surahs
}

data class SearchResults(
    val hits: List<SearchHit> = emptyList(),
    val counts: SearchCounts = SearchCounts(),
    val capped: Boolean = false,
)

/**
 * Which sources one search reads. Everything is on until the reader turns it
 * off: the promise of the search sheet is one field over everything the
 * reader has, and the filters are there for the reader who is drowning in
 * results, not for one who is looking.
 */
data class SearchSources(
    val text: Boolean = true,
    val surahs: Boolean = true,
    val references: Boolean = true,
    val words: Boolean = true,
    val translations: Boolean = true,
    val tafsirs: Boolean = true,
) {
    /** The whole library, which is where every search starts. */
    companion object {
        val ALL = SearchSources()
    }
}

/** Everything one search reads, already resolved against the reader's choices. */
data class SearchRequest(
    val query: SearchQuery,
    val translationPacks: List<String> = emptyList(),
    val tafsirPacks: List<String> = emptyList(),
    val packNames: Map<String, String> = emptyMap(),
    val packLanguages: Map<String, String> = emptyMap(),
    /** The word list to read meanings from; English when the reader has none of their language. */
    val wordsPack: String = "words-en",
    /** The sources the reader has not filtered out. */
    val sources: SearchSources = SearchSources.ALL,
    val limit: Int = 200,
)

data class PagePosition(
    val surah: Int,
    val juz: Int,
    val hizb: Int,
)

enum class PackType {
    Translation,
    Tafsir,
    Script,
    Recitation,
    Words;

    companion object {
        fun of(raw: String): PackType = when (raw) {
            "translation" -> Translation
            "tafsir" -> Tafsir
            "script" -> Script
            "recitation" -> Recitation
            "words" -> Words
            else -> Translation
        }
    }
}

/**
 * One unit of content the reader can have, or not have: the core text and
 * page layout, a translation, a tafsir, a word list, a script. The catalog
 * in the app says what exists; the store says what is on this device.
 */
data class ContentPack(
    val id: String,
    val type: PackType,
    val name: String,
    val language: String,
    val credit: String,
    val license: String,
    val version: String,
    val shipped: Boolean,
    val ayahs: Int,
    val bytes: Long,
    val sha256: String = "",
    val url: String? = null,
    val installed: Boolean = false,
)

/** The first ayah of one juz, with the reference it lands on. */
data class JuzStart(
    val juz: Int,
    val ayah: Int,
    val surah: Int,
    val verseKey: String,
)

/** One downloaded surah of one reciter, offered for removal. */
data class DownloadedSurah(val surah: Int, val name: String, val bytes: Long)

/** Everything one study row needs: the ayah, its words, and its translations. */
data class StudyRow(
    val ayah: Ayah,
    val words: List<Word>,
    val translations: List<TranslationLine> = emptyList(),
    val meanings: List<WordMeaning> = emptyList(),
)
