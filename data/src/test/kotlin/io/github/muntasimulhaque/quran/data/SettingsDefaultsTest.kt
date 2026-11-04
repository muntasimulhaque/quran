package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The page a fresh install opens on is the quiet one.
 *
 * The reminder is a small commitment, so it starts off and is offered after
 * the first surah. The tajweed colors are the print's annotation, not the
 * first page's default. The word aid is the reader's ask. The reminder card
 * is unanswered only after the first-open write, so the stored default for
 * an existing install stays answered and no upgrade is asked again (owner
 * decision, this session).
 */
class SettingsDefaultsTest {

    @Test
    fun aFreshInstallStartsWithTheQuietPage() {
        val settings = AppSettings()
        assertFalse("the daily reminder starts off", settings.dailyAyah)
        assertFalse("the tajweed colors start off", settings.tajweed)
        assertFalse("the word aid starts off", settings.wordByWord)
        assertTrue("an existing install is not asked again", settings.reminderAnswered)
    }
}
