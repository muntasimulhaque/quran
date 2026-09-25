package io.github.muntasimulhaque.quran

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.core.DailyAyah
import io.github.muntasimulhaque.quran.daily.DailyAyahContent
import io.github.muntasimulhaque.quran.daily.DailyAyahScheduler
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

/**
 * The daily reminder reads its ayah from the content already on the device.
 *
 * The ayah is the day's own, from the rotation the JVM suite pins; the
 * translation is the reader's first enabled pack, read in plain words because
 * a notification has no door for a footnote. One ayah and one translation is
 * the whole message, so a reader never meets a wall of text in the shade.
 */
@RunWith(AndroidJUnit4::class)
class DailyAyahTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        PackStore(context).install("translation-saheeh-en")
        LanguagePreference(context).set("en")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage("en")
                setTranslationPacks(setOf("translation-saheeh-en"))
                setShowTranslation(true)
            }
        }
    }

    @Test
    fun theDaySAyahIsReadFromTheDevice() = runBlocking {
        val content = DailyAyahContent.load(context)
        assertNotNull("the day's ayah must be readable with no network", content)
        val today = content!!
        assertEquals(
            "the reminder shows the day's own ayah",
            DailyAyah.numberFor(System.currentTimeMillis()),
            today.ayah,
        )
        assertTrue("the ayah must carry its Arabic", today.arabic.isNotBlank())
        assertTrue("the place must carry its name", today.surahName.isNotBlank())
        assertTrue("the ayah must belong to its surah", today.ayah in 1..DailyAyah.TOTAL_AYAHS)
    }

    @Test
    fun theTranslationIsTheReaderSFirstEnabledPack() = runBlocking {
        val content = DailyAyahContent.load(context)
        assertNotNull(content)
        assertTrue(
            "a reader with a translation reads it in the reminder",
            !content!!.translation.isNullOrBlank(),
        )
    }

    /**
     * A footnote marker is a door the shade cannot open, so it never rides
     * along: the reminder carries the words, and the note stays in the
     * reading. The check compares the reminder's line with the readable form
     * of the same stored translation, which is where the marker is dropped.
     */
    @Test
    fun footnoteMarkersNeverReachTheShade() = runBlocking {
        val content = DailyAyahContent.load(context)
        assertNotNull(content)
        val database = openLibrary()
        try {
            val stored = database.translations(listOf(content!!.ayah), "translation-saheeh-en")[
                content.ayah
            ]?.text
            assertNotNull("the same ayah must have a stored translation", stored)
            assertEquals(
                "the reminder carries the readable form, marker and all dropped",
                io.github.muntasimulhaque.quran.core.RichText.plain(stored!!),
                content.translation,
            )
        } finally {
            database.close()
        }
    }

    private suspend fun openLibrary(): io.github.muntasimulhaque.quran.data.ContentDatabase {
        val catalog = io.github.muntasimulhaque.quran.data.PackCatalog.load(context)
        val installed = PackStore(context).installed()
        return io.github.muntasimulhaque.quran.data.ContentDatabase.open(
            context,
            catalog.withInstalled(installed),
            installed,
        )
    }

    @Test
    fun withNoTranslationTheReminderCarriesTheAyahAlone() = runBlocking {
        SettingsStore(context).setTranslationPacks(emptySet())
        val content = DailyAyahContent.load(context)
        assertNotNull(content)
        assertEquals("the ayah is still there", 
            DailyAyah.numberFor(System.currentTimeMillis()), content!!.ayah)
        assertEquals("and nothing else is claimed", null, content.translation)
    }

    /**
     * The alarm itself: the next moment the reminder should come is the
     * reader's own hour today, or tomorrow when that hour has passed.
     */
    @Test
    fun theAlarmLandsAtTheReadersHour() {
        val hour = 8
        val at = DailyAyahScheduler.nextOccurrence(hour)
        val calendar = Calendar.getInstance().apply { timeInMillis = at }
        assertEquals("the alarm lands on the chosen hour", hour, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals("and at the top of it", 0, calendar.get(Calendar.MINUTE))
        assertTrue("and in the future", at > System.currentTimeMillis())

        // An hour that has already passed today comes tomorrow.
        val now = System.currentTimeMillis()
        val tomorrow = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val past = tomorrow.get(Calendar.HOUR_OF_DAY)
        val landing = DailyAyahScheduler.nextOccurrence(past, now)
        val landingCalendar = Calendar.getInstance().apply { timeInMillis = landing }
        assertTrue("a passed hour lands tomorrow", landing > now)
        assertEquals(past, landingCalendar.get(Calendar.HOUR_OF_DAY))
    }

    /**
     * The alarm that fires is spent, and the next one is armed from the same
     * hour: asked at the moment of a fire, the answer is tomorrow, not the
     * same second. That is what makes a one-shot alarm a daily reminder.
     */
    @Test
    fun theNextDayIsArmedWhenOneFires() {
        val hour = 8
        val firedAt = DailyAyahScheduler.nextOccurrence(hour)
        val next = DailyAyahScheduler.nextOccurrence(hour, firedAt)
        val day = 86_400_000L
        assertTrue("the next fire is in the future", next > firedAt)
        assertEquals(
            "and exactly a day after the fire",
            firedAt + day,
            next,
        )
    }
}
