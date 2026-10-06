package io.github.muntasimulhaque.quran.core

/**
 * What happens as the recitation being heard moves on: the next ayah, nothing
 * further, the ayah again, the surah again, or the next surah.
 *
 * These are five answers to one question, so they are one value and not five
 * switches (owner decision). Two of them on at once is a promise the player
 * cannot keep: a surah that repeats never ends, so "continue to the next
 * surah" would never come, and a reader who had turned both on would sit
 * waiting for a surah that never arrives. One reader has one answer, and the
 * switch that carries it is the one they last turned on.
 *
 * The state with no answer at all is [STOP_AFTER_AYAH]: the reading stops when
 * the current ayah ends. The reader reaches it by turning off [CONTINUE_AYAH],
 * the default, which is why the stop has no switch of its own (owner
 * decision).
 */
enum class EndOfAudio {
    /** Move to the next ayah. The default, and the way the app has always played. */
    CONTINUE_AYAH,

    /** Stop when the current ayah ends. Reached by turning [CONTINUE_AYAH] off. */
    STOP_AFTER_AYAH,

    REPEAT_AYAH,
    REPEAT_SURAH,
    CONTINUE_SURAH,
}

/**
 * The four switches read as the one answer they stand for.
 *
 * The switches are still stored as their own keys, because that is the shape
 * every install already has and a key nobody has written yet costs a reader
 * nothing. This is the read and the write of them: [with] is what a tap does,
 * [end] is what a stored set means, and the data layer is the only place that
 * knows there are separate keys at all.
 *
 * [continueAyah] is the default and its own key, added after the other three:
 * an install written before it has no key, reads as on, and behaves exactly as
 * it did before, which is the point of choosing it this way. A stored set can
 * hold more than one true, because the build before the exclusivity let a
 * reader turn two on together. [end] settles that by keeping the narrower
 * promise, because the narrower one is the one the reader can still hear
 * working: the ayah, else the surah, else the next surah. Under those three
 * sits the base, which is the reader's own choice between continuing to the
 * next ayah and the stop.
 */
data class RepeatPlan(
    /** Move to the next ayah. Off means stop after the ayah. */
    val continueAyah: Boolean = true,
    val ayah: Boolean = false,
    val surah: Boolean = false,
    val next: Boolean = false,
) {
    /** The plan that carries [chosen] and nothing else. */
    fun with(chosen: EndOfAudio): RepeatPlan = when (chosen) {
        EndOfAudio.CONTINUE_AYAH -> RepeatPlan()
        EndOfAudio.STOP_AFTER_AYAH -> RepeatPlan(continueAyah = false)
        EndOfAudio.REPEAT_AYAH -> RepeatPlan(continueAyah = false, ayah = true)
        EndOfAudio.REPEAT_SURAH -> RepeatPlan(continueAyah = false, surah = true)
        EndOfAudio.CONTINUE_SURAH -> RepeatPlan(continueAyah = false, next = true)
    }

    /** What this plan means, as one answer. */
    val end: EndOfAudio
        get() = when {
            ayah -> EndOfAudio.REPEAT_AYAH
            surah -> EndOfAudio.REPEAT_SURAH
            next -> EndOfAudio.CONTINUE_SURAH
            continueAyah -> EndOfAudio.CONTINUE_AYAH
            else -> EndOfAudio.STOP_AFTER_AYAH
        }
}

/**
 * What a switch says when it is tapped.
 *
 * Tapping an off switch chooses the answer it names. Tapping it on again
 * leaves that answer, and the reading returns to the default, continuing to
 * the next ayah, which is what the reader was doing before they asked for
 * anything special. The default switch is the one exception: turning
 * [CONTINUE_AYAH] off is the reader's stop, and it is the only door to
 * [STOP_AFTER_AYAH], so the stop needs no switch of its own.
 */
fun toggled(turnedOn: Boolean, choice: EndOfAudio): EndOfAudio = when {
    turnedOn -> choice
    choice == EndOfAudio.CONTINUE_AYAH -> EndOfAudio.STOP_AFTER_AYAH
    else -> EndOfAudio.CONTINUE_AYAH
}
