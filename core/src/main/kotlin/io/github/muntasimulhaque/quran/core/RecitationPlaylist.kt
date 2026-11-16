package io.github.muntasimulhaque.quran.core

/**
 * The recitation playlist, described by its own media ids.
 *
 * A media id is "runningAyah:surah", which is why the player always knows
 * where it is without another query, and why the first ayah of a surah can be
 * found from the ids alone.
 *
 * The first part is the ayah's running number, 1 to 6236, the content
 * database's key and the reading's place. It is not the ayah's number inside
 * its surah: 16:28 is running 1929, and a pill that printed the running
 * number as the verse's own read 16:1929 (owner report, this session). The
 * reader's number travels beside the id, in the item's own ayah, so nothing
 * has to guess which of the two it was given.
 *
 * This is pure arithmetic, kept here because the surah repeat needs it and
 * cannot be left inside the controller where nothing can reach it
 * (owner decision). The playlist is the surah being heard *and*
 * whatever has been appended after it, so the player's own
 * `REPEAT_MODE_ALL` would loop everything that is loaded rather than one
 * surah: the surah repeat is a seek to the index this returns.
 */
object RecitationPlaylist {

    /** The id one ayah of one surah plays under: its running number and its surah. */
    fun mediaId(runningAyah: Int, surah: Int): String = "$runningAyah:$surah"

    /** The surah an id belongs to, or null when the id is not one of ours. */
    fun surahOf(mediaId: String): Int? = mediaId.substringAfter(':').toIntOrNull()

    /**
     * The running ayah number an id names, or null when the id is not one of
     * ours. This is the content database's key and the reading's place, 1 to
     * 6236, never the ayah's number inside its surah: 2:255 is running 262
     * (owner report, this session).
     */
    fun runningAyahOf(mediaId: String): Int? = mediaId.substringBefore(':').toIntOrNull()

    /**
     * The index the surah starts at, or -1 when the playlist holds none of
     * it. The first index, not the shortest one: a surah's ayahs are in the
     * playlist in the Book's order, and a repeated seek to a later ayah
     * would be a surah that begins in the middle.
     */
    fun startOf(mediaIds: List<String>, surah: Int): Int =
        mediaIds.indexOfFirst { surahOf(it) == surah }

    /**
     * The surah an automatic item advance is ending, or null when the advance
     * stays inside it.
     *
     * The playlist is one media item per ayah, so the player reports an
     * automatic advance at every ayah boundary. The one advance that ends a
     * surah is the one that arrives in another surah: reading the advance
     * alone made every ayah's end look like the surah's end, which restarted
     * the surah at ayah 1 and then looped that ayah forever (owner report, an
     * earlier session).
     *
     * This answers only whether the boundary was crossed. What the boundary
     * then does is the reader's own answer, [EndOfAudio.surahEnding], read at
     * the crossing: the playlist holds the next surah whatever the answer, so
     * a deliberate Next and Previous still carry the reader out of the surah,
     * and the answer decides whether the audio follows it (owner decision, this
     * session).
     */
    fun endedSurah(
        playing: Int?,
        arrivingMediaId: String?,
    ): Int? {
        val arrived = surahOf(arrivingMediaId ?: return null) ?: return null
        return playing?.takeIf { it != arrived }
    }
}
