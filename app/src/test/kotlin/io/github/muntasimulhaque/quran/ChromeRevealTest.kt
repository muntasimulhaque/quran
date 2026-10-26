package io.github.muntasimulhaque.quran

import io.github.muntasimulhaque.quran.ui.reader.CHROME_REVEAL
import io.github.muntasimulhaque.quran.ui.reader.chromeRevealNeeded
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one chrome reveal: a new install and a release that adds a surface owe
 * the reader one look at the top bar; the same install never repeats it.
 */
class ChromeRevealTest {

    @Test
    fun aNewInstallNeedsTheReveal() {
        assertTrue(chromeRevealNeeded(null))
        assertTrue(chromeRevealNeeded(""))
    }

    @Test
    fun theSameSurfaceIsNeverRevealedTwice() {
        assertFalse(chromeRevealNeeded(CHROME_REVEAL))
    }

    @Test
    fun aSurfaceThatMovedNeedsTheRevealAgain() {
        assertTrue(chromeRevealNeeded("reading-0"))
    }
}
