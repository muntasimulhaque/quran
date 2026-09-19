package io.github.muntasimulhaque.quran.core

/**
 * The study list shows one surah as an opening item, then one item per ayah,
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
}
