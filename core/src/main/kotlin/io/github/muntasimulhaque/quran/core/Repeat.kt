package io.github.muntasimulhaque.quran.core

/**
 * What happens when the recitation being heard ends: the ayah again, the
 * surah again, or the surah after this one.
 *
 * These are three answers to one question, so they are one value and not
 * three switches (owner decision). Two of them on at once is a
 * promise the player cannot keep: a surah that repeats never ends, so
 * "continue to the next surah" would never come, and a reader who had turned
 * both on would sit waiting for a surah that never arrives. One reader has
 * one answer, and the switch that carries it is the one they last turned on.
 */
enum class EndOfAudio { OFF, REPEAT_AYAH, REPEAT_SURAH, CONTINUE }

/**
 * The three stored switches read as the one answer they stand for.
 *
 * The switches are still stored as three keys, because that is the shape
 * every install already has and a key nobody has written yet costs a reader
 * nothing. This is the read and the write of them: [with] is what a tap
 * does, [end] is what a stored set means, and the data layer is the only
 * place that knows there are three keys at all.
 *
 * A stored set can hold more than one true, because the build before the
 * exclusivity let a reader turn the ayah repeat and the continuation on
 * together. [end] settles that by keeping the narrower promise, because the
 * narrower one is the one the reader can still hear working: the ayah that
 * repeats, else the surah, else the next surah.
 */
data class RepeatPlan(
    val ayah: Boolean = false,
    val surah: Boolean = false,
    val next: Boolean = false,
) {
    /** The plan that carries [chosen] and nothing else. */
    fun with(chosen: EndOfAudio): RepeatPlan = when (chosen) {
        EndOfAudio.OFF -> OFF
        EndOfAudio.REPEAT_AYAH -> RepeatPlan(ayah = true)
        EndOfAudio.REPEAT_SURAH -> RepeatPlan(surah = true)
        EndOfAudio.CONTINUE -> RepeatPlan(next = true)
    }

    /** What this plan means, as one answer. */
    val end: EndOfAudio
        get() = when {
            ayah -> EndOfAudio.REPEAT_AYAH
            surah -> EndOfAudio.REPEAT_SURAH
            next -> EndOfAudio.CONTINUE
            else -> EndOfAudio.OFF
        }

    companion object {
        /** Nothing repeats and nothing continues: the reading plays on once. */
        val OFF = RepeatPlan()
    }
}

/**
 * What a switch says: the answer it carries when it is on, and none when it
 * is off.
 *
 * Every switch that chooses one of the three answers is written this way, so
 * a row cannot express "on, and something else as well": the switch reports
 * what it means, and the plan in [RepeatPlan.with] is what the app keeps.
 */
fun chosenBy(turnedOn: Boolean, choice: EndOfAudio): EndOfAudio =
    if (turnedOn) choice else EndOfAudio.OFF
