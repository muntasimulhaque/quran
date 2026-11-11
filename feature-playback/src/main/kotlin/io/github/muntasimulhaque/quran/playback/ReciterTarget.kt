package io.github.muntasimulhaque.quran.playback

/**
 * The ayah a voice chosen on the pill is heard from.
 *
 * The reader's request is the fallback, never the answer, because the request
 * is a snapshot: [requested] is the ayah Play was pressed on and it never
 * moves again, while the audio does. A reader who starts on 2:1, listens on
 * to 2:5, and then picks another voice was dropped back at 2:1, which reads as
 * "the reciter starts from the first ayah of the surah"; and after the audio
 * had carried on into the next surah, the same choice took them out of that
 * surah entirely (owner report, this session).
 *
 * [playing] is the ayah the player is on now, published on every item
 * transition, so it is the one control the pill has. The request is still
 * needed for the window where nothing is playing yet: a package on its way
 * belongs to an ayah the player has never been handed, and that is the ayah a
 * voice chosen now has to be asked about.
 */
fun reciterTarget(playing: Int?, requested: Int): Int = playing ?: requested
