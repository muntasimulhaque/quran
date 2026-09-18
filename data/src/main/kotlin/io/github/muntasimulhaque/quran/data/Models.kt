package io.github.muntasimulhaque.quran.data

data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameSimple: String,
    val nameLatin: String,
    val revelationPlace: String,
    val versesCount: Int,
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

data class WordMeaning(val word: String, val meaning: String?)

data class TafsirPassage(
    val source: String,
    val surah: Int,
    val fromAyah: Int,
    val toAyah: Int,
    val text: String,
)

sealed interface SearchHit {

    data class SurahHit(val surah: Surah) : SearchHit

    data class AyahHit(
        val ayah: Ayah,
        val page: Int,
        val translation: String?,
        val words: List<Word>,
        val matchedPositions: Set<Int>,
    ) : SearchHit
}

data class PagePosition(
    val surah: Int,
    val juz: Int,
    val hizb: Int,
)
