package io.github.muntasimulhaque.quran.playback

import io.github.muntasimulhaque.quran.core.RecitationPlaylist

/**
 * The place one playing item names, in the reader's own numbers.
 *
 * The media id carries the ayah's running number, which is the content
 * database's key and the reading's place, and the pill draws the verse's own
 * "16:28" instead. Those are two different fields: 16:28 is running 1929, and
 * a pill that printed the running number read 16:1929 (owner report, this
 * session). The ayah's own number is passed in from the item's ayah and the
 * key is built here, where one JVM test holds it.
 */
internal data class PlayingPlace(
    val surah: Int,
    val ayahInSurah: Int,
    /** The pill's key, the same string the database stores as `verse_key`. */
    val reference: String,
)

/**
 * The surah and the ayah's own number the id names, or null when the id is
 * not one of ours or the ayah's own number was never built beside it.
 */
internal fun playingPlace(mediaId: String, ayahInSurah: Int?): PlayingPlace? {
    val surah = RecitationPlaylist.surahOf(mediaId) ?: return null
    if (surah < 1) return null
    if (ayahInSurah == null || ayahInSurah < 1) return null
    return PlayingPlace(
        surah = surah,
        ayahInSurah = ayahInSurah,
        reference = "$surah:$ayahInSurah",
    )
}
