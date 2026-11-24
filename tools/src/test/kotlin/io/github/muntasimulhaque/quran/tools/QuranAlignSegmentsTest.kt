package io.github.muntasimulhaque.quran.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranAlignSegmentsTest {

    @Test
    fun `the drawn word is the export index plus one`() {
        assertEquals(1, QuranAlignSegments.position("husary", 2, 255, 0))
        assertEquals(50, QuranAlignSegments.position("husary", 2, 255, 49))
        assertEquals(1, QuranAlignSegments.position("minshawi", 79, 10, 0))
    }

    @Test
    fun `a muqata'ah gap moves every word after the head`() {
        // husary 12:1 is `الٓر` and four words; the aligner counted the head's
        // letters as words, so its index 0 is the head and the rest are holes.
        val regions = regions(
            "husary", 12, 1,
            segment(0, 1, 30, 5900),
            segment(2, 3, 6850, 7470),
            segment(3, 4, 7480, 8870),
            segment(4, 5, 8880, 10450),
            segment(5, 6, 10460, 12520),
        )
        assertEquals(listOf(1, 2, 3, 4, 5), regions.map { it.word })
        assertEquals(listOf(30L, 6850L, 7480L, 8880L, 10460L), regions.map { it.startMs })
    }

    @Test
    fun `a muqata'ah two letters long moves by two`() {
        val regions = regions(
            "minshawi", 10, 1,
            segment(0, 1, 550, 5850),
            segment(3, 4, 5860, 6480),
            segment(4, 5, 6490, 7500),
            segment(5, 6, 7510, 8710),
            segment(6, 7, 8720, 9770),
        )
        assertEquals(listOf(1, 2, 3, 4, 5), regions.map { it.word })
    }

    @Test
    fun `a gap that is not pinned is not smoothed`() {
        // The same shape, one ayah over: nothing moves, so the word map has
        // exactly the three rules and no quiet fourth one.
        val regions = regions(
            "minshawi", 10, 2,
            segment(0, 1, 550, 900),
            segment(2, 3, 1000, 1400),
        )
        assertEquals(listOf(1, 3), regions.map { it.word })
    }

    @Test
    fun `the joined name at the end of surah 37 is one word through both its regions`() {
        val regions = regions(
            "husary", 37, 130,
            segment(0, 1, 650, 2350),
            segment(1, 2, 2360, 4680),
            segment(2, 3, 4690, 5410),
            segment(3, 4, 5420, 8520),
        )
        assertEquals(listOf(1, 2, 3, 3), regions.map { it.word })
    }

    @Test
    fun `a span names the word it starts with and is held`() {
        val regions = regions("husary", 4, 12, segment(17, 19, 24680, 26000))
        assertEquals(listOf(18), regions.map { it.word })
        assertEquals(emptyList<Int>(), QuranAlignSegments.uncovered(regions, 18).drop(17))
        assertEquals((1..17).toList(), QuranAlignSegments.uncovered(regions, 18))
    }

    @Test
    fun `a region that ends before it starts is dropped`() {
        val regions = regions(
            "husary", 9, 66,
            segment(5, 6, 8410, 11450),
            segment(6, 7, 13750, 11450),
            segment(7, 8, 13690, 15700),
        )
        assertEquals(listOf(6, 8), regions.map { it.word })
        assertEquals(listOf(1, 2, 3, 4, 5, 7, 9).sorted(), QuranAlignSegments.uncovered(regions, 9).sorted())
    }

    @Test
    fun `regions come back in time order whatever the file said`() {
        val regions = regions(
            "husary", 2, 1,
            segment(2, 3, 3000, 3500),
            segment(0, 1, 1000, 1500),
            segment(1, 2, 2000, 2500),
        )
        assertEquals(listOf(1, 2, 3), regions.map { it.word })
    }

    @Test
    fun `the stored json is the four number shape the database reads`() {
        val regions = regions("husary", 1, 1, segment(0, 1, 50, 510))
        assertEquals("[[0,1,50,510]]", QuranAlignSegments.json(regions))
    }

    @Test
    fun `the timing file is read as the aligner writes it`() {
        val ayahs = requireNotNull(
            QuranAlignSegments.parse(
                "[{\"ayah\":1,\"segments\":[[0,1,60,610]],\"surah\":1,\"stats\":{}}]",
            ),
        )
        assertEquals(1, ayahs.size)
        assertEquals("1:1", ayahs.single().key)
        assertEquals(60L, ayahs.single().segments.single().startMs)
    }

    @Test
    fun `a file that is not the aligner's shape is refused`() {
        assertNull(QuranAlignSegments.parse("not json"))
        assertNull(QuranAlignSegments.parse("[{\"surah\":1}]"))
    }

    @Test
    fun `every recitation the app ships has a timing file and a pinned list`() {
        for (reciter in listOf("husary", "minshawi")) {
            assertTrue("$reciter has no timing dataset", reciter in QuranAlignSegments.DATASETS)
            assertTrue("$reciter has no pinned words", reciter in QuranAlignSegments.UNCOVERED)
        }
    }

    private fun regions(
        reciter: String,
        surah: Int,
        ayah: Int,
        vararg segments: QuranAlignSegments.ExportSegment,
    ): List<QuranAlignSegments.Region> =
        QuranAlignSegments.regions(reciter, surah, ayah, segments.toList())

    private fun segment(from: Int, to: Int, startMs: Long, endMs: Long) =
        QuranAlignSegments.ExportSegment(from, to, startMs, endMs)
}
