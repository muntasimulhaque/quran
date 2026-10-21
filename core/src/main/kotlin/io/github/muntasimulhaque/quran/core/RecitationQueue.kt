package io.github.muntasimulhaque.quran.core

/**
 * The surah a recitation playlist is loaded through, and the one surah that
 * may be appended after it.
 *
 * The playlist grows one surah at a time: a start records the surah it is
 * loading, and the player's first report of the new playlist may append the
 * next one. The order of those two acts is the whole rule. A new playlist is
 * reported to its listeners before the call that set it returns, so a start
 * that recorded its surah after handing the playlist over was measured from
 * the old surah: the first report either appended the next surah twice, or,
 * when the start was on a surah's last ayah, appended nothing and offered a
 * package that was already on the device (owner report, this session). The
 * bookkeeping lives here, away from the player, so that order is pinned in
 * the JVM suite.
 */
class RecitationQueue(loadedThrough: Int = 0) {

    /** The surah the playlist was last loaded through. */
    var loadedThrough: Int = loadedThrough
        private set

    /**
     * Records the surah a start is loading. It is called before the playlist
     * is handed to the player, because the player's own report comes first.
     */
    fun load(surah: Int) {
        loadedThrough = surah
    }

    /**
     * The surah to append when the player is at [surah], or null when the
     * playlist is already loaded through it or the Book ends there. The
     * answer moves the queue on, so one transition cannot append the same
     * surah twice.
     */
    fun appendAt(surah: Int): Int? {
        if (surah >= LAST_SURAH || loadedThrough != surah) return null
        val next = surah + 1
        loadedThrough = next
        return next
    }

    private companion object {
        /** The Book's last surah; nothing follows it. */
        const val LAST_SURAH = 114
    }
}
