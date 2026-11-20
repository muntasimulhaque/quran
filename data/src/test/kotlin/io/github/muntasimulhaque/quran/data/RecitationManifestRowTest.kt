package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The row rule a recitation package must pass before the app will fetch it.
 *
 * The size a package promises is the transfer's own ceiling and the progress
 * line's denominator, so a row with no size is refused rather than read as
 * "size unknown". The manifest's parser is Android's JSON, which a machine
 * without a screen cannot load, so the rule lives in one pure function here
 * and the parser only asks it; this file is the half of the contract the JVM
 * gate can hold, and the device suite keeps the URL and the store's own side.
 */
class RecitationManifestRowTest {

    private val hash = "a".repeat(64)

    @Test
    fun `a row with a real package is accepted`() {
        assertTrue(recitationRowIsValid("surah-002.zip", 2, 123_456L, hash))
    }

    @Test
    fun `a row with no size is refused`() {
        assertFalse(recitationRowIsValid("surah-002.zip", 2, 0, hash))
    }

    @Test
    fun `a row with a negative size is refused`() {
        assertFalse(recitationRowIsValid("surah-002.zip", 2, -1, hash))
    }

    @Test
    fun `a row outside the Book's surahs is refused`() {
        assertFalse(recitationRowIsValid("surah-002.zip", 115, 123_456L, hash))
    }

    @Test
    fun `a row whose asset is not a zip is refused`() {
        assertFalse(recitationRowIsValid("surah-002.db", 2, 123_456L, hash))
    }

    @Test
    fun `a row with a malformed checksum is refused`() {
        assertFalse(recitationRowIsValid("surah-002.zip", 2, 123_456L, "abc"))
    }
}
