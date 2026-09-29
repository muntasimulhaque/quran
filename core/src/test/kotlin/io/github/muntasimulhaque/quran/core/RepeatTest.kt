package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One reader has one answer to "what happens when the audio ends".
 *
 * The rule is small, and small rules that three separate switches depend on
 * are exactly the ones that rot, so it is pinned here over every stored
 * combination rather than over one example of each.
 */
class RepeatTest {

    @Test
    fun turningOneOnLeavesTheOtherTwoOff() {
        for (choice in EndOfAudio.entries) {
            val plan = RepeatPlan(ayah = true, surah = true, next = true).with(choice)
            val on = listOf(plan.ayah, plan.surah, plan.next).count { it }
            assertEquals(
                "exactly one answer may be on, and $choice carried $on",
                if (choice == EndOfAudio.OFF) 0 else 1,
                on,
            )
        }
    }

    @Test
    fun theAnswerChosenIsTheOneKept() {
        assertEquals(EndOfAudio.REPEAT_AYAH, RepeatPlan().with(EndOfAudio.REPEAT_AYAH).end)
        assertEquals(EndOfAudio.REPEAT_SURAH, RepeatPlan().with(EndOfAudio.REPEAT_SURAH).end)
        assertEquals(EndOfAudio.CONTINUE, RepeatPlan().with(EndOfAudio.CONTINUE).end)
        assertEquals(EndOfAudio.OFF, RepeatPlan(ayah = true).with(EndOfAudio.OFF).end)
    }

    @Test
    fun aStoredSetWithTwoOnKeepsTheNarrowerPromise() {
        // A build before the exclusivity let a reader turn the ayah repeat
        // and the continuation on together. The narrower one is the one they
        // can still hear working, so that is what the read settles on.
        assertEquals(EndOfAudio.REPEAT_AYAH, RepeatPlan(ayah = true, next = true).end)
        assertEquals(
            EndOfAudio.REPEAT_AYAH,
            RepeatPlan(ayah = true, surah = true, next = true).end,
        )
        assertEquals(EndOfAudio.REPEAT_SURAH, RepeatPlan(surah = true, next = true).end)
    }

    @Test
    fun everyStoredSetIsReadableAsOneAnswer() {
        for (ayah in listOf(false, true)) {
            for (surah in listOf(false, true)) {
                for (next in listOf(false, true)) {
                    val plan = RepeatPlan(ayah = ayah, surah = surah, next = next)
                    assertTrue("$plan is not one of the four answers", plan.end in EndOfAudio.entries)
                    // Round tripping a normalized plan must not move it.
                    assertEquals(plan.end, RepeatPlan().with(plan.end).end)
                }
            }
        }
    }

    @Test
    fun aSwitchSaysItsAnswerOrNone() {
        assertEquals(EndOfAudio.REPEAT_SURAH, chosenBy(true, EndOfAudio.REPEAT_SURAH))
        assertEquals(EndOfAudio.OFF, chosenBy(false, EndOfAudio.REPEAT_SURAH))
        assertEquals(EndOfAudio.OFF, chosenBy(false, EndOfAudio.OFF))
    }
}
