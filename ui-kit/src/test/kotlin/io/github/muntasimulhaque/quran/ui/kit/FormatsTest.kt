package io.github.muntasimulhaque.quran.ui.kit

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * The size rule every download prints: the unit is a word of the interface's
 * own language and the digits are that language's own too, so a Bangla reader
 * never meets an English unit beside a Bengali number (owner decision, this
 * session). The rule is pure, so it is pinned here without a window.
 */
class FormatsTest {

    private val bangla = Locale.forLanguageTag("bn")

    @Test
    fun sizesReadInTheInterfacesOwnDigitsAndWords() {
        assertEquals("৩০০ কেবি", formatBytes(300L * 1024, bangla, "কেবি", "এমবি"))
        assertEquals("২.৮ এমবি", formatBytes(2_936_012L, bangla, "কেবি", "এমবি"))
        assertEquals("২২ এমবি", formatBytes(22L * 1024 * 1024, bangla, "কেবি", "এমবি"))
    }

    @Test
    fun sizesReadInEnglishForAnEnglishInterface() {
        assertEquals("300 KB", formatBytes(300L * 1024, Locale.US, "KB", "MB"))
        assertEquals("2.8 MB", formatBytes(2_936_012L, Locale.US, "KB", "MB"))
        assertEquals("22 MB", formatBytes(22L * 1024 * 1024, Locale.US, "KB", "MB"))
    }

    @Test
    fun aSizeThatIsNotThereSaysNothing() {
        assertEquals("", formatBytes(0L, bangla, "কেবি", "এমবি"))
    }
}
