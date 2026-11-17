package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeaningfulGlossTest {

    @Test
    fun `a gloss with no letter is not a meaning`() {
        assertNull(meaningfulGloss("\""))
        assertNull(meaningfulGloss("("))
        assertNull(meaningfulGloss("  "))
        assertNull(meaningfulGloss(null))
    }

    @Test
    fun `a real gloss keeps its words and its punctuation`() {
        assertEquals("নামে", meaningfulGloss(" নামে "))
        assertEquals("(of) Allah", meaningfulGloss("(of) Allah"))
    }
}
