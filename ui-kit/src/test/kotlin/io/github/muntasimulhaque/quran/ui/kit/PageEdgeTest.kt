package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Space
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app's one page: a sheet is the glass, and every word stands one gutter
 * inside it.
 *
 * The widths here are the store's own three profiles, the phone at 411 dp, the
 * seven inch tablet at 601 dp, and the ten inch screen at 1280 dp. The rule is
 * arithmetic, so it is pinned on a machine with no screen.
 *
 * The road not taken is pinned with it. Until this session the ten inch screen
 * kept a fixed 512 dp page centered on a glass nearly three times its width,
 * the seven inch tablet's search results stood 64 dp inside a sheet that was
 * its own window, and the reading's words stopped at a 65 character measure
 * whatever the glass was: a count of characters had become the app's margin.
 * These legs fail if a page is ever narrower than the window it is drawn in,
 * which is the shape that mistake takes when it comes back.
 */
class PageEdgeTest {

    private val phone = 411.dp
    private val sevenInch = 600.94.dp
    private val tenInch = 1280.dp

    @Test
    fun aSheetIsTheGlassOnEveryProfile() {
        assertEquals(phone, sheetWidth(phone))
        assertEquals(sevenInch, sheetWidth(sevenInch))
        assertEquals(tenInch, sheetWidth(tenInch))
    }

    @Test
    fun noWindowIsEverHandedANarrowerPage() {
        for (width in 320..1280) {
            val sheet = sheetWidth(width.dp)
            assertTrue(
                "a $width dp window was handed a $sheet dp page",
                sheet == width.dp,
            )
        }
    }

    @Test
    fun aWashedVerseStopsItsWordsAtTheOneEdge() {
        // The reading's list keeps the one gutter less the room a wash reaches
        // past its text, so the wash stands on the outer line and the words on
        // the app's own edge.
        val side = Space.Gutter - Space.WashInset
        assertEquals("the reading's words", Space.Gutter, side + Space.WashInset)
        assertEquals("the wash's line", 20f, side.value, 0.001f)
        assertEquals("the app's edge", 28f, Space.Gutter.value, 0.001f)
    }

    @Test
    fun aSheetRowStopsItsWordsAtTheSameEdge() {
        // A row's press card is inset by SheetRowGutter and the caller pads
        // the rest of the gutter, so a settings row's words land on the line
        // the reading's words land on.
        val words = SheetRowGutter + (Space.Gutter - SheetRowGutter)
        assertEquals(Space.Gutter, words)
    }
}
