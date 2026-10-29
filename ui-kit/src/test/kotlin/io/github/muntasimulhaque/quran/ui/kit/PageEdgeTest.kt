package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Space
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app's one page: a sheet is the page plus its two gutters, and a word
 * stands one gutter inside the page it is drawn on.
 *
 * The widths here are the store's own three profiles, the phone at 411 dp, the
 * seven inch tablet at 600.94 dp, and the ten inch screen at 1280 dp. The rule
 * is arithmetic, so it is pinned on a machine with no screen.
 *
 * The road that was tried and left is pinned here too, from the other side.
 * The ten inch screen used to hand every surface its whole 1280 dp, so the
 * reading's translation ran about 155 characters to the line and a settings
 * row's switch stood most of a screen away from the words that named it. The
 * app then capped every column at 65 characters, which made a line shorten
 * when a reader turned the text down, and that was the worse mistake. A count
 * of characters decides no width here: the page is [Space.Measure], fixed, and
 * whatever the glass has over it belongs to the glass (owner decision, this
 * session).
 */
class PageEdgeTest {

    private val phone = 411.dp
    private val sevenInch = 600.94.dp
    private val tenInch = 1280.dp

    /** The page and the air it keeps on each side. */
    private val sheetOfAPage = Space.Measure + Space.Gutter * 2

    @Test
    fun aSheetIsThePageOnEveryProfile() {
        // Narrower than a page plus its gutters, so the glass is the sheet.
        assertEquals(phone, sheetWidth(phone))
        assertEquals(sevenInch, sheetWidth(sevenInch))
        // Wider than that, so the page holds and the glass keeps the rest.
        assertEquals(sheetOfAPage, sheetWidth(tenInch))
    }

    @Test
    fun noSheetIsEverWiderThanItsGlass() {
        for (width in 320..1400) {
            val sheet = sheetWidth(width.dp)
            assertTrue(
                "a $width dp window was handed a $sheet dp sheet",
                sheet <= width.dp,
            )
        }
    }

    @Test
    fun noLineIsEverWiderThanThePage() {
        for (width in 320..1400) {
            val margin = pageMargin(width.dp)
            val line = width.dp - margin * 2
            assertTrue(
                "a $width dp window ran a $line dp line",
                line <= Space.Measure,
            )
            assertTrue(
                "a $width dp window stood its words $margin dp in, inside a gutter",
                margin >= Space.Gutter,
            )
        }
    }

    @Test
    fun theSevenInchAndTheTenInchReadOnePage() {
        // The seven inch tablet is a page edge to edge, and the ten inch
        // screen is that same width with the rest of the glass as margin.
        assertEquals(Space.Gutter, pageMargin(sevenInch))
        assertEquals(tenInch - Space.Measure, pageMargin(tenInch) * 2)
        assertEquals(Space.Measure, tenInch - pageMargin(tenInch) * 2)
    }

    @Test
    fun theMarginMeetsTheGutterWithNoSeam() {
        // At exactly a page plus two gutters the two halves of the rule meet
        // on the one number, so no window is handed a wider page than its
        // neighbour.
        assertEquals(Space.Gutter, pageMargin(sheetOfAPage))
        assertEquals(Space.Gutter, pageMargin(sheetOfAPage - 1.dp))
        assertEquals(Space.Gutter + 1.dp, pageMargin(sheetOfAPage + 2.dp))
    }

    @Test
    fun aWashedVerseStopsItsWordsAtTheOneEdge() {
        // The reading's list keeps the page's margin less the room a wash
        // reaches past its text, so the wash stands on the page's line and the
        // words one inset inside it.
        val side = pageMargin(phone) - Space.WashInset
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
