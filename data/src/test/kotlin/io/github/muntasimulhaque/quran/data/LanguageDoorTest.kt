package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * One language choice, two doors, and this is the rule that keeps them one
 * answer: the system's per-app row outranks the mirror, the mirror outranks
 * the phone, and a tag the app carries no strings for never wins.
 *
 * Every case is a sentence about a reader: the one who changed the language
 * in the phone's own settings, the one who changed it in the app, the one who
 * never chose at all, and the one whose phone speaks a language the app does
 * not.
 */
class LanguageDoorTest {

    @Test
    fun `a system per-app choice outranks the stored one`() {
        assertEquals(
            UiLanguage.Bangla.tag,
            chosenLanguageTag(
                applicationLocales = listOf("bn"),
                storedTag = "en",
                phoneTag = "en",
            ),
        )
    }

    @Test
    fun `the reader's own Settings choice mirrors back to the system`() {
        // Settings writes the mirror and then the platform's per-app locale;
        // this is the state after that write, and the platform's answer is
        // the one the window must speak.
        assertEquals(
            UiLanguage.Bangla.tag,
            mirroredApplicationLocale(UiLanguage.Bangla),
        )
        assertEquals(
            UiLanguage.Bangla.tag,
            chosenLanguageTag(
                applicationLocales = listOf(mirroredApplicationLocale(UiLanguage.Bangla)),
                storedTag = "bn",
                phoneTag = "en",
            ),
        )
    }

    @Test
    fun `the stored choice is read when the platform holds none`() {
        assertEquals(
            UiLanguage.Bangla.tag,
            chosenLanguageTag(applicationLocales = emptyList(), storedTag = "bn", phoneTag = "en"),
        )
    }

    @Test
    fun `a fresh install follows the phone's language`() {
        assertEquals(
            UiLanguage.Bangla.tag,
            chosenLanguageTag(applicationLocales = emptyList(), storedTag = null, phoneTag = "bn"),
        )
    }

    @Test
    fun `a phone the app has no strings for falls back to English`() {
        assertEquals(
            UiLanguage.English.tag,
            chosenLanguageTag(applicationLocales = emptyList(), storedTag = null, phoneTag = "fr"),
        )
    }

    @Test
    fun `a platform tag the app does not carry never pins the window`() {
        assertEquals(
            UiLanguage.Bangla.tag,
            chosenLanguageTag(
                applicationLocales = listOf("fr"),
                storedTag = "bn",
                phoneTag = "en",
            ),
        )
    }

    @Test
    fun `a region tag reads as its own language`() {
        assertEquals(
            UiLanguage.Bangla.tag,
            chosenLanguageTag(applicationLocales = listOf("bn-BD"), storedTag = null, phoneTag = "en"),
        )
    }

    @Test
    fun `the platform's order is the reader's order`() {
        // The per-app list can carry more than one entry. The first one the
        // app carries is the answer, which is the order the system screen
        // draws them in.
        assertEquals(
            UiLanguage.Bangla.tag,
            chosenLanguageTag(
                applicationLocales = listOf("fr", "bn", "en"),
                storedTag = "en",
                phoneTag = "en",
            ),
        )
    }
}
