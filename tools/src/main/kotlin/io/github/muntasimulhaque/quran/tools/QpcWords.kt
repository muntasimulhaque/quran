package io.github.muntasimulhaque.quran.tools

/**
 * The four places where QUL's word-by-word export splits a pair that the
 * QUL word list and the recitation keep as one word.
 *
 * The list lives here rather than inside the database build because two
 * sources now travel through it: the recitation timing packs and the word
 * pronunciation pack. A word number that one of them moves has to move the
 * same way in the other, or the sheet's speaker plays the word beside the
 * one the reader asked for.
 */
object QpcWords {

    data class Merge(val surah: Int, val ayah: Int, val position: Int)

    val merges = listOf(
        Merge(2, 181, 3),
        Merge(8, 6, 4),
        Merge(13, 37, 8),
        Merge(37, 130, 3),
    )

    /**
     * The source word number a recitation segment names, moved through the
     * same merge the reading word went through. The second half of a joined
     * pair names the joined word; every word after it moves down by one.
     */
    fun position(surah: Int, ayah: Int, source: Int): Int {
        var position = source
        for (merge in merges) {
            if (merge.surah != surah || merge.ayah != ayah) continue
            if (source == merge.position + 1) return merge.position
            if (source > merge.position + 1) position -= 1
        }
        return position
    }
}
