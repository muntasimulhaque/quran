package io.github.muntasimulhaque.quran.tools

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioEdgesTest {

    private val edges = AudioEdges(File("."))

    @Test
    fun `the input duration is read from ffmpeg's own header`() {
        assertEquals(
            4970L,
            edges.parseDurationMs("  Duration: 00:00:04.97, start: 0.000000, bitrate: 128 kb/s"),
        )
        assertEquals(
            61_470L,
            edges.parseDurationMs("  Duration: 00:01:01.47, start: 0.000000, bitrate: 128 kb/s"),
        )
        assertNull(edges.parseDurationMs("no duration here"))
    }

    @Test
    fun `the line is ten percent of the window's loud end`() {
        val threshold = edges.threshold(doubleArrayOf(0.0, 0.0, 0.0, 0.1, 0.2))
        assertEquals(0.01, threshold, 0.0001)
    }

    @Test
    fun `a silent window has a floor the room cannot cross`() {
        assertEquals(0.004, edges.threshold(doubleArrayOf(0.0, 0.0, 0.0, 0.0)), 0.0001)
    }

    @Test
    fun `the first sound is the first run of three frames`() {
        assertEquals(
            10L,
            edges.firstSpeech(0.05, doubleArrayOf(0.0, 0.1, 0.1, 0.1, 0.0, 0.1)),
        )
    }

    @Test
    fun `a window that never reaches the line has no first sound`() {
        assertEquals(-1L, edges.firstSpeech(0.05, doubleArrayOf(0.0, 0.0, 0.0, 0.01, 0.0)))
        assertEquals(-1L, edges.firstSpeech(0.05, DoubleArray(0)))
    }

    @Test
    fun `the last sound ends the last run of three frames`() {
        assertEquals(
            40L,
            edges.lastSpeech(0.05, doubleArrayOf(0.0, 0.1, 0.1, 0.1, 0.0, 0.0)),
        )
    }

    @Test
    fun `a file that ends with speech ends at the window's end`() {
        assertEquals(
            60L,
            edges.lastSpeech(0.05, doubleArrayOf(0.0, 0.0, 0.0, 0.1, 0.1, 0.1)),
        )
    }

    @Test
    fun `the envelope reads one frame per ten milliseconds`() {
        val samples = 320
        val bytes = ByteArray(samples * 2)
        for (index in 0 until samples) {
            val value = 16384
            bytes[index * 2] = (value and 0xff).toByte()
            bytes[index * 2 + 1] = ((value shr 8) and 0xff).toByte()
        }
        val frames = edges.envelope(bytes)
        assertEquals(2, frames.size)
        assertEquals(0.5, frames[0], 0.01)
        assertEquals(0.5, frames[1], 0.01)
    }
}
