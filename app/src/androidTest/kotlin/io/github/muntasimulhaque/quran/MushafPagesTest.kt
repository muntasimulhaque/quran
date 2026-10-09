package io.github.muntasimulhaque.quran

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.core.PageFrame
import io.github.muntasimulhaque.quran.core.PageTextLayout
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PackCatalog
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.ui.mushaf.PageKey
import io.github.muntasimulhaque.quran.ui.mushaf.PageRenderer
import io.github.muntasimulhaque.quran.ui.theme.Gold
import io.github.muntasimulhaque.quran.ui.theme.PagePalette
import io.github.muntasimulhaque.quran.ui.theme.PaperBackground
import io.github.muntasimulhaque.quran.ui.theme.PaperInk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every page of the Book, laid out and drawn on the device it will be read
 * on, with the face's real metrics and nothing approximated.
 *
 * The page is drawn from the Book's own text at the page's own type, which the
 * engine measures from the Book's own words, and four things must hold for all
 * 604 of them, at the reader's usual size and at the largest:
 *
 * - every word of the page stands on it exactly once, in the print's own
 *   order, so a page can never lose a word of the Book or gain one;
 * - every ayah of the page carries the box a finger lands on, so no ayah is
 *   ever out of reach;
 * - **the page carries the print's own lines.** The layout table says which
 *   words stand on which of the page's lines, and that is what is drawn: the
 *   page is as many slots as the print has lines, not a line fewer. This is
 *   the gate the page went two releases without, and it is the one that
 *   caught the twelve-line page;
 * - at the reader's usual size no line of any page runs past its measure,
 *   because a line past the measure is a line whose words have been re-set.
 *
 * The page's own type is a property of the shipped face and the Book's own
 * words, and the face is hash-pinned in the manifest, so a face whose metrics
 * move fails here rather than quietly re-setting the Book.
 */
@RunWith(AndroidJUnit4::class)
class MushafPagesTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val glassWidth = 1080
    private val glassHeight = 1920

    private val palette = PagePalette(
        paper = PaperBackground,
        ink = PaperInk,
        ornament = Gold,
        selection = PaperInk.copy(alpha = 0.12f),
        highlight = PaperInk.copy(alpha = 0.22f),
    )

    @Test
    fun everyPageCarriesThePrintsOwnLinesAtTheReadersUsualSize() = runBlocking {
        val (content, renderer) = open()
        for (page in 1..604) {
            val key = PageKey(page, glassWidth, glassHeight, "paper", step = 1f)
            val rendered = renderer.get(key, content, palette)
            assertNotNull("page $page could not be rendered", rendered)
            rendered ?: continue
            val printLines = content.pageLines(page)
            assertEquals(
                "page $page must carry the print's own lines, one slot each: " +
                    "${rendered.ayahOrder.size} ayahs on ${printLines.size} lines",
                printLines.size,
                rendered.lines,
            )
            val pageWords = content.pageWords(page)
            assertEquals(
                "page $page places every word exactly once",
                pageWords.size,
                rendered.words.size,
            )
            val ayahs = content.ayahsForPage(page)
            assertEquals(
                "page $page reads the ayahs it carries",
                ayahs.map { it.number }.sorted(),
                rendered.ayahNumbers.sorted(),
            )
            for (word in rendered.words) {
                assertTrue(
                    "a word of page $page stands off the page: ${word.left},${word.top}",
                    word.left >= -1f && word.top >= -1f &&
                        word.right <= rendered.widthPx + 1f && word.bottom <= rendered.heightPx + 1f,
                )
            }
        }
    }

    @Test
    fun everyPageStandsWholeInsideItsGlass() = runBlocking {
        val (content, renderer) = open()
        for (page in 1..604) {
            val rendered = renderer.get(PageKey(page, glassWidth, glassHeight, "paper", 1f), content, palette)
            assertNotNull("page $page could not be rendered", rendered)
            rendered ?: continue
            // The page fills the glass it was drawn for and is never taller
            // than it, so no line of the Book is ever off the bottom of the
            // screen and no ayah is ever out of a finger's reach.
            assertTrue(
                "page $page is taller than its glass: ${rendered.heightPx} against $glassHeight",
                rendered.heightPx <= glassHeight + 1,
            )
            assertTrue(
                "page $page does not fill the glass it was drawn for: " +
                    "${rendered.heightPx} of $glassHeight",
                rendered.heightPx >= glassHeight - glassHeight / 10,
            )
        }
    }

    @Test
    fun everyLineOfEveryPageStandsInsideItsMeasure() = runBlocking {
        val (content, renderer) = open()
        var linesChecked = 0
        for (page in 1..604) {
            val key = PageKey(page, glassWidth, glassHeight, "paper", 1f)
            val layout = PageTextLayout.layout(
                lines = content.pageLines(page).map { line ->
                    io.github.muntasimulhaque.quran.core.LayoutLine(
                        line = line.line,
                        type = line.type,
                        centered = line.centered,
                        firstWordId = line.firstWordId,
                        lastWordId = line.lastWordId,
                        surah = line.surah,
                    )
                },
                words = content.pageWords(page).associate { word ->
                    word.id to io.github.muntasimulhaque.quran.core.LayoutWord(
                        id = word.id,
                        ayah = word.ayah,
                        position = word.position,
                        marker = word.marker,
                        text = word.text,
                    )
                },
                measure = { text ->
                    android.graphics.Paint().apply {
                        typeface = android.graphics.Typeface.createFromAsset(
                            context.assets,
                            "fonts/UthmanicHafs_V22.ttf",
                        )
                        textSize = 1000f
                    }.measureText(text) / 1000f
                },
                spaceEm = SPACE_EM,
                roundelEm = PageFrame::roundelWidthEm,
                step = 1f,
            )
            for (slot in layout.slots) {
                if (slot.kind != io.github.muntasimulhaque.quran.core.SlotKind.Text) continue
                val last = slot.words.lastOrNull() ?: continue
                if (slot.words.size < 2) continue
                val end = last.start + last.width
                assertTrue(
                    "a line of page $page runs past the measure: $end against " +
                        "${PageFrame.EM_PER_LINE / layout.scale}",
                    end <= PageFrame.EM_PER_LINE / layout.scale + 0.01f,
                )
                linesChecked++
            }
            assertNotNull(renderer.get(key, content, palette))
        }
        assertTrue("the Book's lines were measured", linesChecked > 8000)
    }

    @Test
    fun aLargerStepIsStillThePrintsOwnLinesWhereTheyFit() = runBlocking {
        val (content, renderer) = open()
        // A reader who asks for larger text asks the page to grow, and the
        // lines that no longer hold their words flow. What must never happen
        // is a page that loses a word or a line that passes its measure.
        for (step in listOf(0.65f, 0.75f, 0.85f, TextSize.STEPS.max())) {
            for (page in 1..604 step 7) {
                val rendered = renderer.get(
                    PageKey(page, glassWidth, glassHeight, "paper", step),
                    content,
                    palette,
                )
                assertNotNull("page $page could not be rendered at step $step", rendered)
                rendered ?: continue
                assertEquals(
                    "page $page at step $step places every word exactly once",
                    content.pageWords(page).size,
                    rendered.words.size,
                )
            }
        }
    }

    private fun open(): Pair<ContentDatabase, PageRenderer> {
        val catalog = PackCatalog.load(context)
        val content = runBlocking { ContentDatabase.open(context, catalog, emptySet()) }
        return content to PageRenderer(context)
    }

    private companion object {
        /** The word gap the shipped face carries naturally, in its ems. */
        const val SPACE_EM = 0.22f
    }
}
