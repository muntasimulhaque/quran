package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The reference the card heads a passage with, for the two shapes the packs
 * return: one ayah, and the span a commentary opens before the reader's ayah.
 */
class AyahSpanTest {

    @Test
    fun oneAyahIsOneReference() {
        assertEquals("41:48", ayahSpan(41, 48, 48))
    }

    @Test
    fun aPassageThatOpensEarlierSaysSo() {
        assertEquals("41:46-48", ayahSpan(41, 46, 48))
    }
}
