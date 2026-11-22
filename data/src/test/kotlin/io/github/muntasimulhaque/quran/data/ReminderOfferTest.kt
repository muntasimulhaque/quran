package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The offer's life, pinned as arithmetic: it stands at a surah's end
 * until the final answer, never at the one boundary Not now moved it
 * off, and the second Not now ends it (owner decision, 21.9).
 */
class ReminderOfferTest {

    @Test
    fun `the offer stands before the reader answers it, while the reminder is off`() {
        assertTrue(AppSettings(dailyAyah = false, reminderAnswered = false).reminderOfferAt(1))
    }

    @Test
    fun `setting the reminder answers the offer`() {
        assertFalse(AppSettings(dailyAyah = true, reminderAnswered = false).reminderOfferAt(1))
    }

    @Test
    fun `an answered offer never returns`() {
        assertFalse(
            AppSettings(
                dailyAyah = false,
                reminderAnswered = true,
                reminderDismissedSurah = 1,
            ).reminderOfferAt(2),
        )
    }

    @Test
    fun `the first Not now moves the ask off the boundary it was tapped on`() {
        val moved = AppSettings(
            dailyAyah = false,
            reminderAnswered = false,
            reminderDismissedSurah = 1,
        )
        assertFalse(moved.reminderOfferAt(1))
    }

    @Test
    fun `the moved ask stands at the end of any other surah the reader finishes`() {
        val moved = AppSettings(
            dailyAyah = false,
            reminderAnswered = false,
            reminderDismissedSurah = 1,
        )
        assertTrue(moved.reminderOfferAt(2))
        assertTrue(moved.reminderOfferAt(114))
    }

    @Test
    fun `the second Not now answers the offer for good`() {
        val answered = AppSettings(
            dailyAyah = false,
            reminderAnswered = true,
            reminderDismissedSurah = 2,
        )
        assertFalse(answered.reminderOfferAt(3))
    }
}
