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

data class PagePosition(
    val surah: Int,
    val juz: Int,
    val hizb: Int,
)
