package io.github.muntasimulhaque.quran.core

/**
 * The reading list shows one surah as an opening item, then one item per ayah,
 * then a closing line. The reader's place is an ayah; the list speaks in
 * indices. This is the only place the two meet.
 *
 * It is a small class with a large consequence: the place is written from the
 * list's own scroll, so an off-by-one here records the wrong ayah and the
 * reader loses where they were. That is exactly what happened when the
 * opening item was counted as the surah's first ayah.
 */
object AyahList {

    /** The list index that shows [ayah], or the opening item when it is not here. */
    fun indexOf(ayahs: List<Int>, ayah: Int): Int {
        val found = ayahs.indexOf(ayah)
        return if (found < 0) 0 else found + 1
    }

    /**
     * The ayah a list index shows: the opening item is the surah's first ayah,
     * and the closing line belongs to no ayah, so it keeps the place as it is.
     */
    fun ayahAt(ayahs: List<Int>, index: Int): Int? =
        if (index <= 0) ayahs.firstOrNull() else ayahs.getOrNull(index - 1)

    /**
     * The index the list has to move to show [place], or null when it is
     * already showing it.
     *
     * The place is an ayah and nothing else, so a settle writes the ayah the
     * list is already at, and the list stays exactly where the reader's
     * finger left it. It moves only when the place came from somewhere else:
     * a jump, a link, a reminder, the app opening.
     *
     * The surah's opening is the one case outside that rule, and it is kept
     * here rather than in the caller because it is arithmetic about this list
     * and not a property of the reading. The opening is index 0 and its ayah
     * is the surah's first, index 1, so a settle with the opening on the glass
     * writes a place the list is one item above. That place is truthful, so
     * the list must not answer it by scrolling the opening away under the
     * reader's own gesture. It answers null and the opening stays.
     */
    fun moveTarget(ayahs: List<Int>, place: Int, firstVisible: Int): Int? {
        val target = indexOf(ayahs, place)
        if (target == firstVisible) return null
        if (target == 1 && firstVisible == 0) return null
        return target
    }
}
