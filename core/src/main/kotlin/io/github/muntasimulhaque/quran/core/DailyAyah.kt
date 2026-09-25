package io.github.muntasimulhaque.quran.core

import java.util.TimeZone

/**
 * The ayah of the day: one ayah per local calendar day, the same for every
 * reader on the same day, and the same ayah every time the day is asked
 * about.
 *
 * The reminder is a quiet invitation to read, not a notification stream: one
 * ayah a day, walking the Book in order rather than picking favorites, so a
 * reader who keeps the reminder meets the whole Quran over time instead of
 * the same short list of well known verses. The walk is a straight rotation
 * over the 6,236 ayahs by the local day number, so it is deterministic,
 * offline, and impossible to run out of.
 *
 * The day number is the local epoch day, floored: `floorDiv` rather than `/`
 * so a pre-epoch instant still counts whole days, and the timezone's own
 * offset is added so "today" means the reader's today and not UTC's, which
 * changes at a time they are probably still awake for.
 */
object DailyAyah {

    /** The Quran's ayah count; the numbering is one ascending run. */
    const val TOTAL_AYAHS = 6236

    /** The ayah (1..6236) for the local day containing [nowMillis]. */
    fun numberFor(nowMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Int {
        val localDays = Math.floorDiv(
            nowMillis + timeZone.getOffset(nowMillis),
            DAY_MILLIS,
        )
        // The Book's own first ayah is 1, so the rotation is one past a
        // floorMod of the day count. A pre-epoch day lands inside the range
        // rather than on a negative index.
        return Math.floorMod(localDays, TOTAL_AYAHS.toLong()).toInt() + 1
    }

    private const val DAY_MILLIS = 86_400_000L
}
