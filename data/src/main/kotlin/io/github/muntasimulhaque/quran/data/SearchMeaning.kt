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

/**
 * Which Arabic a search row draws, if any.
 *
 * Every ayah row used to open with the Arabic, up to four lines of it, whether
 * or not the Arabic had anything to do with the search. Measured on the
 * shipped content database: a Latin query never matches the Arabic at all, so
 * for the English word "mercy" the 144 ayahs the translation matched each
 * carried an Arabic block that matched nothing, and the 9 ayahs only a word
 * meaning reached carried the same block beside a gloss that named one word
 * (owner report, 2.3). So a row draws its Arabic in one of three ways:
 *
 * - the Arabic **is** the match, and the ayah is drawn whole with the matched
 *   words washed;
 * - a word meaning is the **only** evidence, and the matched **words** are
 *   drawn in the ayah's own order, washed, so the row answers "why is this
 *   ayah here" with the word that earned it rather than with a page of text;
 * - the row has no other evidence, which is a reference the reader typed, and
 *   the ayah is drawn plain: then it is all the row has to say.
 *
 * Anything else draws no Arabic at all, and a row the translation already
 * washed is in that group on purpose: the word line would be the same match
 * said twice, which is the very thing this rule's neighbour exists to stop
 * (D-097). Dropping the rest is a legibility decision, not a simplification:
 * four lines of Arabic that never matched is the tallest thing on the row and
 * the least informative.
 */
fun arabicLineFor(hit: SearchHit.AyahHit): SearchArabicLine =
    when {
        hit.arabicMatchedWords.isNotEmpty() -> SearchArabicLine.Ayah
        !hit.translation?.ranges.isNullOrEmpty() -> SearchArabicLine.None
        shouldShowWordMeaning(hit) && hit.matchedWordText.isNotEmpty() ->
            SearchArabicLine.Words(hit.matchedWordText)
        hit.wordMeaning == null -> SearchArabicLine.Ayah
        else -> SearchArabicLine.None
    }

/** What a row draws of the Arabic, and how much of it. */
sealed interface SearchArabicLine {
    /** The whole ayah, with [SearchHit.AyahHit.arabicMatchedWords] washed. */
    data object Ayah : SearchArabicLine

    /** Only the words that matched, in the ayah's own order. */
    data class Words(val words: List<String>) : SearchArabicLine

    /** Nothing: the row's evidence is elsewhere. */
    data object None : SearchArabicLine
}
