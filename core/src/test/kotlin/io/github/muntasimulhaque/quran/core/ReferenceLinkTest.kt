package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReferenceLinkTest {

    @Test
    fun `a relative range names its surah and its first ayah`() {
        assertEquals(Search.Reference(2, 67), ReferenceLink.parse("/2/67-73"))
        assertEquals(Search.Reference(58, 1), ReferenceLink.parse("58/1-4"))
    }

    @Test
    fun `a bare surah names the surah alone`() {
        assertEquals(Search.Reference(2, null), ReferenceLink.parse("/2"))
    }

    @Test
    fun `a link outside the Book names nothing`() {
        assertNull(ReferenceLink.parse("/115/1"))
        assertNull(ReferenceLink.parse("/2/0"))
        assertNull(ReferenceLink.parse("/2/1/3"))
        assertNull(ReferenceLink.parse("https://quran.com/2"))
        assertNull(ReferenceLink.parse(""))
    }
}
