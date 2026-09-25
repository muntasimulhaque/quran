package io.github.muntasimulhaque.quran.data

/**
 * Whether a search row draws its word meaning.
 *
 * The meaning was drawn as its own labelled block under every row that had
 * one, and a reader who searched a word that also appears in the translation
 * saw the same fact twice: the translation already carried the highlight, and
 * the block repeated it (owner report, D-097). The block is now drawn only
 * when it is the row's **only** evidence of the match, so a row is never a
 * list of the same match said twice.
 *
 * The rule is pure and lives here, beside [orderSearchHits], so it is pinned
 * by a JVM test rather than by a device glance.
 */
fun shouldShowWordMeaning(hit: SearchHit.AyahHit): Boolean =
    hit.wordMeaning != null &&
        hit.arabicMatchedWords.isEmpty() &&
        hit.translation?.ranges.isNullOrEmpty()
