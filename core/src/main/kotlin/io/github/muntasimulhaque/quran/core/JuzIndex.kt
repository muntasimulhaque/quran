package io.github.muntasimulhaque.quran.core

/**
 * The juz of every ayah, from the first ayah of each juz.
 *
 * The reading moves its place on every scroll settle, and the top bar's juz
 * label follows that place. Asking the library for the juz each time would
 * be a disk read on the frame the place moves, so the content's own thirty
 * boundaries are read once and this turns them into one array read.
 */
object JuzIndex {

    /**
     * One juz per ayah number, indexed by ayah, from the first ayah numbers
     * of juz 1 through 30 in order. An ayah before the first boundary stays
     * in juz 1, and the last boundary's juz carries to [totalAyahs], so a
     * lookup never falls off the Book.
     */
    fun of(firstAyahs: List<Int>, totalAyahs: Int): IntArray {
        val at = IntArray(totalAyahs + 1) { 1 }
        var juz = 1
        var next = 0
        for (ayah in 1..totalAyahs) {
            // Every boundary at or before this ayah moves the juz forward.
            while (next < firstAyahs.size && ayah >= firstAyahs[next]) {
                juz = next + 1
                next++
            }
            at[ayah] = juz
        }
        return at
    }
}
