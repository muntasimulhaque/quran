package io.github.muntasimulhaque.quran.data

/**
 * One place a root or a dictionary form stands: the ayah, the word's own
 * position in it, and the word as the page writes it.
 *
 * The count the word sheet names and this list are one number. The root's
 * count is the pack's own rows, not the source's field, so a reader who opens
 * the places sees exactly the words the line promised (owner decision, this
 * session). [ayahNumber] is the Book's running number, which the reading
 * jumps by; [surah] and [ayah] are the reference the list labels.
 *
 * Only the drawn word travels: the gloss belongs to the word sheet the reader
 * left, and a list of thousands of meanings is a word-by-word reading of the
 * Book rather than a place list (owner decision, this session).
 */
data class WordPlace(
    val ayahNumber: Int,
    val surah: Int,
    val ayah: Int,
    val position: Int,
    val word: String,
) {
    /** The reference the app's labels use, for example 2:255. */
    val reference: String get() = "$surah:$ayah"
}

/**
 * The places of one surah, in the Book's order, under the name of the surah
 * they stand in.
 *
 * A root's places run through the whole Book, and a flat list of two thousand
 * rows says nothing about where the reader is. The page groups them by surah,
 * the map the Book already has, and every group opens with the surah's name
 * (owner decision, this session).
 */
data class WordPlaceGroup(
    val surah: Int,
    val places: List<WordPlace>,
)

/**
 * The grouped places, in the order the query gave them: the Book's order.
 *
 * The grouping is pure so the page's scroll arithmetic can be pinned without
 * a database: the list is ordered by the running ayah number, and a surah's
 * ayahs are contiguous there, so each surah is one run (owner decision, this
 * session).
 */
fun wordPlaceGroups(places: List<WordPlace>): List<WordPlaceGroup> =
    places.groupBy { it.surah }.map { (surah, group) -> WordPlaceGroup(surah, group) }

/**
 * The item index of the reader's own place in the grouped list, or zero when
 * the place is not in it.
 *
 * Each group is one head item and its place items, so the index of a place is
 * the room every earlier group takes plus its own head. This is the row the
 * reader's place wears the selected wash on; the list opens at
 * [wordPlaceOpenIndex], its surah's head, so the row is named by the group it
 * stands in (owner decision, this session).
 */
fun wordPlaceItemIndex(
    groups: List<WordPlaceGroup>,
    ayahNumber: Int,
    position: Int,
): Int {
    var index = 0
    for (group in groups) {
        val within = group.places.indexOfFirst {
            it.ayahNumber == ayahNumber && it.position == position
        }
        if (within >= 0) return index + 1 + within
        index += 1 + group.places.size
    }
    return 0
}

/**
 * The item index the list opens at: the head of the surah the reader's place
 * stands in, or zero when the place is not in the list.
 *
 * The current row alone at the top would leave its surah's name scrolled off,
 * a row with no group above it. The head names the group the reader is in,
 * and the current row stands one item under it; the reader's place is still
 * the list's first row after its own surah's head (owner decision, this
 * session).
 */
fun wordPlaceOpenIndex(
    groups: List<WordPlaceGroup>,
    ayahNumber: Int,
    position: Int,
): Int {
    var index = 0
    for (group in groups) {
        val within = group.places.indexOfFirst {
            it.ayahNumber == ayahNumber && it.position == position
        }
        if (within >= 0) return index
        index += 1 + group.places.size
    }
    return 0
}
