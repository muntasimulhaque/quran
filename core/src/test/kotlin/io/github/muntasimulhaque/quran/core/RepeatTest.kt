package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One reader has one answer to "what happens as the reading moves on".
 *
 * The rule is small, and small rules that four separate switches depend on
 * are exactly the ones that rot, so it is pinned here over every stored
 * combination rather than over one example of each. The fifth answer, the
 * stop, is the state where no switch is on at all.
 */
class RepeatTest {

    @Test
    fun oneAnswerIsOnAtATime() {
        for (choice in EndOfAudio.entries) {
            val plan = RepeatPlan(
                continueAyah = false,
                ayah = true,
                surah = true,
                next = true,
            ).with(choice)
            val on = listOf(plan.continueAyah, plan.ayah, plan.surah, plan.next).count { it }
            assertEquals(
                "exactly one answer may be on, and $choice carried $on",
                if (choice == EndOfAudio.STOP_AFTER_AYAH) 0 else 1,
                on,
            )
        }
    }

    @Test
    fun theAnswerChosenIsTheOneKept() {
        assertEquals(EndOfAudio.CONTINUE_AYAH, RepeatPlan().with(EndOfAudio.CONTINUE_AYAH).end)
        assertEquals(EndOfAudio.STOP_AFTER_AYAH, RepeatPlan().with(EndOfAudio.STOP_AFTER_AYAH).end)
        assertEquals(EndOfAudio.REPEAT_AYAH, RepeatPlan().with(EndOfAudio.REPEAT_AYAH).end)
        assertEquals(EndOfAudio.REPEAT_SURAH, RepeatPlan().with(EndOfAudio.REPEAT_SURAH).end)
        assertEquals(EndOfAudio.CONTINUE_SURAH, RepeatPlan().with(EndOfAudio.CONTINUE_SURAH).end)
    }

    @Test
    fun theDefaultIsContinuingToTheNextAyah() {
        // A reader who has never touched the setting, and an install written
        // before the default had its own key, both read the same way.
        assertEquals(EndOfAudio.CONTINUE_AYAH, RepeatPlan().end)
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
        for (continueAyah in listOf(false, true)) {
            for (ayah in listOf(false, true)) {
                for (surah in listOf(false, true)) {
                    for (next in listOf(false, true)) {
                        val plan = RepeatPlan(continueAyah, ayah, surah, next)
                        assertTrue(
                            "$plan is not one of the answers",
                            plan.end in EndOfAudio.entries,
                        )
                        // Round tripping a normalized plan must not move it.
                        assertEquals(plan.end, RepeatPlan().with(plan.end).end)
                    }
                }
            }
        }
    }

    @Test
    fun aSwitchSaysItsAnswerOrTheDefault() {
        assertEquals(EndOfAudio.REPEAT_SURAH, toggled(true, EndOfAudio.REPEAT_SURAH))
        assertEquals(EndOfAudio.CONTINUE_AYAH, toggled(false, EndOfAudio.REPEAT_SURAH))
        assertEquals(EndOfAudio.CONTINUE_SURAH, toggled(true, EndOfAudio.CONTINUE_SURAH))
        assertEquals(EndOfAudio.CONTINUE_AYAH, toggled(false, EndOfAudio.CONTINUE_SURAH))
        assertEquals(EndOfAudio.REPEAT_AYAH, toggled(true, EndOfAudio.REPEAT_AYAH))
        assertEquals(EndOfAudio.CONTINUE_AYAH, toggled(false, EndOfAudio.REPEAT_AYAH))
    }

    @Test
    fun turningOffTheDefaultIsTheStop() {
        // The default switch is the only door to the stop, and it opens both
        // ways: off is the stop, on is the reading carrying on again.
        assertEquals(EndOfAudio.STOP_AFTER_AYAH, toggled(false, EndOfAudio.CONTINUE_AYAH))
        assertEquals(EndOfAudio.CONTINUE_AYAH, toggled(true, EndOfAudio.CONTINUE_AYAH))
    }
}
