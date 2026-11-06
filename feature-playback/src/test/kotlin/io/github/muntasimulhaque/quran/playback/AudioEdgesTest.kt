package io.github.muntasimulhaque.quran.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioEdgesTest {

    @Test
    fun `an edge is read back as the two numbers the build wrote`() {
        val edges = AudioEdges.parse(
            sequenceOf(
                "# reciter ayah first last duration",
                "minshawi 1 500 4290 4960",
                "husary 1 100 4520 5120",
            ),
        )
        assertEquals(500, edges.firstMs("minshawi", 1))
        assertEquals(4290, edges.lastMs("minshawi", 1))
        assertEquals(100, edges.firstMs("husary", 1))
        assertEquals(4520, edges.lastMs("husary", 1))
    }

    @Test
    fun `an ayah with no line has no edge`() {
        val edges = AudioEdges.parse(sequenceOf("minshawi 1 500 4290 4960"))
        assertEquals(NO_EDGE_MS, edges.firstMs("minshawi", 2))
        assertEquals(NO_EDGE_MS, edges.lastMs("husary", 1))
    }

    @Test
    fun `a line the audio cannot explain is ignored`() {
        val edges = AudioEdges.parse(
            sequenceOf(
                "minshawi 1 4290 500 4960",
                "minshawi 2 -1 3000 4000",
                "minshawi 3 1000 1000 2000",
                "minshawi 0 0 100 200",
                "minshawi 4 100 200",
            ),
        )
        assertEquals(NO_EDGE_MS, edges.firstMs("minshawi", 1))
        assertEquals(NO_EDGE_MS, edges.firstMs("minshawi", 2))
        assertEquals(NO_EDGE_MS, edges.firstMs("minshawi", 3))
        assertEquals(NO_EDGE_MS, edges.firstMs("minshawi", 0))
        assertEquals(100, edges.firstMs("minshawi", 4))
    }

    @Test
    fun `a first sound at zero is a real edge`() {
        val edges = AudioEdges.parse(sequenceOf("minshawi 1 0 900 1000"))
        assertEquals(0, edges.firstMs("minshawi", 1))
        assertEquals(900, edges.lastMs("minshawi", 1))
    }
}
