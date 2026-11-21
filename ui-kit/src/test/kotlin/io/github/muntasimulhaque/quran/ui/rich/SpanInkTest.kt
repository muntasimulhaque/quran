package io.github.muntasimulhaque.quran.ui.rich

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The medallion's arithmetic, pinned without a window.
 *
 * Every node in a popup reports coordinates from that window's own corner, so
 * the rule that places the ayah's menu is pinned here as arithmetic and the
 * anchor is pinned in a compose test (the repository's own trap, 19.3). The
 * cases are the ones the reading meets: a mark drawn from several glyphs, a
 * line whose place on the glass is fractional, and a finger that needs 48 dp
 * around a mark smaller than that.
 */
class SpanInkTest {

    @Test
    fun `the ink of one box is the box`() {
        val box = Rect(4f, 6f, 30f, 40f)
        assertEquals(box, spanInk(listOf(box)))
    }

    @Test
    fun `the ink of a span is the union of its character boxes`() {
        val boxes = listOf(
            Rect(6f, 2f, 9f, 8f),
            Rect(2f, 4f, 5f, 7f),
            Rect(4f, 1f, 12f, 10f),
        )
        assertEquals(Rect(2f, 1f, 12f, 10f), spanInk(boxes))
    }

    @Test
    fun `an empty span has no ink`() {
        assertEquals(Rect.Zero, spanInk(emptyList()))
    }

    @Test
    fun `the place on the glass truncates each edge on its own`() {
        val page = IntRect(left = 10, top = 20, right = 300, bottom = 400)
        val ink = Rect(1.9f, 2.1f, 3.7f, 4.9f)
        assertEquals(
            IntRect(left = 11, top = 22, right = 13, bottom = 24),
            spanPlace(page, ink),
        )
    }

    @Test
    fun `the target grows the shorter side evenly`() {
        val target = spanTarget(Rect(0f, 0f, 20f, 10f), 48f)
        assertEquals(48f, target.width)
        assertEquals(48f, target.height)
        assertEquals(10f, target.center.x)
        assertEquals(5f, target.center.y)
    }

    @Test
    fun `ink wider than the target is not shrunk`() {
        val target = spanTarget(Rect(0f, 0f, 100f, 10f), 48f)
        assertEquals(0f, target.left)
        assertEquals(100f, target.right)
        assertEquals(48f, target.height)
    }
}
