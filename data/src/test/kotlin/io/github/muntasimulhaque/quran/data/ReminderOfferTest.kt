package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderOfferTest {

    @Test
    fun `the offer stands before the reader answers it, while the reminder is off`() {
        assertTrue(AppSettings(dailyAyah = false, reminderAnswered = false).reminderOffer)
    }

    @Test
    fun `setting the reminder answers the offer`() {
        assertFalse(AppSettings(dailyAyah = true, reminderAnswered = false).reminderOffer)
    }

    @Test
    fun `an answered offer never returns`() {
        assertFalse(AppSettings(dailyAyah = false, reminderAnswered = true).reminderOffer)
    }
}
