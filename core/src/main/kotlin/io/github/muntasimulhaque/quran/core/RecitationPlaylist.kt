package io.github.muntasimulhaque.quran.core

/**
 * The recitation playlist, described by its own media ids.
 *
 * A media id is "ayah:surah", which is why the player always knows where it
 * is without another query, and why the first ayah of a surah can be found
 * from the ids alone.
 *
 * This is pure arithmetic, kept here because the surah repeat needs it and
 * cannot be left inside the controller where nothing can reach it
 * (owner decision). The playlist is the surah being heard *and*
 * whatever has been appended after it, so the player's own
 * `REPEAT_MODE_ALL` would loop everything that is loaded rather than one
 * surah: the surah repeat is a seek to the index this returns.
 */
object RecitationPlaylist {

    /** The id one ayah of one surah plays under. */
    fun mediaId(ayahNumber: Int, surah: Int): String = "$ayahNumber:$surah"

    /** The surah an id belongs to, or null when the id is not one of ours. */
    fun surahOf(mediaId: String): Int? = mediaId.substringAfter(':').toIntOrNull()

    /** The ayah an id names, or null when the id is not one of ours. */
    fun ayahOf(mediaId: String): Int? = mediaId.substringBefore(':').toIntOrNull()

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
     * is not the end [answer] repeats.
     *
     * The playlist is one media item per ayah, so the player reports an
     * automatic advance at every ayah boundary. The one advance that ends a
     * surah is the one that arrives in another surah: reading the advance
     * alone made every ayah's end look like the surah's end, which restarted
     * the surah at ayah 1 and then looped that ayah forever (owner report,
     * this session). Only the surah repeat asks this question; every other
     * answer is left alone.
     */
    fun endedSurah(
        answer: EndOfAudio,
        playing: Int?,
        arrivingMediaId: String?,
    ): Int? {
        if (answer != EndOfAudio.REPEAT_SURAH) return null
        val arrived = surahOf(arrivingMediaId ?: return null) ?: return null
        return playing?.takeIf { it != arrived }
    }
}
