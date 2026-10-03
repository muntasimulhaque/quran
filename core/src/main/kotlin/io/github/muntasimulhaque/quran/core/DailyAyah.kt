package io.github.muntasimulhaque.quran.core

import java.util.Random
import java.util.TimeZone

/**
 * The ayah of the day: one ayah per local calendar day, the same for every
 * reader on the same day, and the same ayah every time the day is asked
 * about.
 *
 * The reminder is a quiet invitation to read, not a notification stream, and a
 * walk through the Book in its own order is not that: a reader who keeps the
 * reminder can tell what tomorrow brings without reading it, and one verse a
 * day in the order it is printed is a verse the app is counting through. What
 * the walk gave was coverage, and coverage is what the day brings now. The
 * day deals a different ayah every morning, and every ayah in the Book still
 * comes exactly once before the deck is filled again (owner decision).
 *
 * The deck is the Book's 6,236 ayahs shuffled, one card a day, and a fresh
 * shuffle for every pass through it: `day mod 6,236` is the slot dealt and
 * `day div 6,236` is the pass, so the first seventeen years are one order and
 * the seventeen after that another, about seventeen years apart again. Nothing
 * about it is stored. The day number alone draws the shuffle, with the JDK's
 * own generator seeded by the pass, which is why the whole thing survives a
 * reinstall, an update, and a cleared preference untouched, and why two
 * readers on the same morning are reading the same verse.
 *
 * The cost is a deck in memory, 6,236 cards shuffled on the ask, which is a
 * fraction of a millisecond and happens once a morning in the receiver
 * rather than on the main thread. The two passes are drawn independently, so
 * the last card of one pass and the first of the next can be the same verse,
 * once in some thousands of boundaries (owner decision).
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
        return card(
            slot = Math.floorMod(localDays, TOTAL_AYAHS.toLong()).toInt(),
            pass = Math.floorDiv(localDays, TOTAL_AYAHS.toLong()),
        )
    }

    /**
     * One card of one pass of the deck.
     *
     * The shuffle is the JDK's own generator on a seeded stream, which is
     * specified rather than accidental, so the same pass deals the same deck
     * on every phone, on every release of Android, and in every test: that is
     * what lets the day's ayah be a pure function of the clock. The seed
     * scramble inside [Random] spreads a small seed, so a pass number that
     * counts from one does not deal a recognisable opening.
     */
    private fun card(slot: Int, pass: Long): Int {
        val deck = IntArray(TOTAL_AYAHS) { it + 1 }
        val random = Random(pass * GOLDEN_GAMMA)
        for (from in deck.lastIndex downTo 1) {
            val to = random.nextInt(from + 1)
            val held = deck[from]
            deck[from] = deck[to]
            deck[to] = held
        }
        return deck[slot]
    }

    /**
     * The multiplier that spreads the pass number across the generator's
     * seed: the golden ratio in 64 bits, written in the two's complement form
     * a signed 64-bit literal can hold.
     */
    private const val GOLDEN_GAMMA = -0x61c8864680b583ebL

    private const val DAY_MILLIS = 86_400_000L
}