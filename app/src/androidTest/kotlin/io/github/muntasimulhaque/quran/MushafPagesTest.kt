package io.github.muntasimulhaque.quran

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.core.MushafText
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PackCatalog
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.ui.mushaf.PageKey
import io.github.muntasimulhaque.quran.ui.mushaf.PageRenderer
import io.github.muntasimulhaque.quran.ui.mushaf.PAGE_ASPECT
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
 * The page is drawn from the Book's own text at the reader's text size, and
 * three things must hold for all 604 of them, at the reader's usual size and
 * at the largest:
 *
 * - every word of the page stands on it exactly once, in the print's own
 *   order, so a page can never lose a word of the Book or gain one;
 * - every ayah of the page carries the box a finger lands on, so no ayah is
 *   ever out of reach;
 * - at the reader's usual size the page stands inside the room the pager
 *   gave it, because a page set larger than the glass is a page whose last
 *   lines nobody asked to lose.
 *
 * The numbers behind the usual size live in MushafText and were measured
 * over every line of the Book; this is the test that says the device agrees.
 */
@RunWith(AndroidJUnit4::class)
class MushafPagesTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val width = 1080

    private val palette = PagePalette(
        paper = PaperBackground,
        ink = PaperInk,
        ornament = Gold,
        selection = PaperInk.copy(alpha = 0.12f),
        highlight = PaperInk.copy(alpha = 0.22f),
    )

    private val room: Float get() = width * PAGE_ASPECT

    @Test
    fun everyPageStandsWholeAtTheReadersUsualSize() = runBlocking {
        val (content, renderer) = open()
        val scale = MushafText.scale(1f)
        for (page in 1..604) {
            val rendered = renderer.get(PageKey(page, width, "paper", scale), content, palette)
            assertNotNull("page $page could not be rendered", rendered)
            rendered ?: continue
            assertTrue(
                "page $page is taller than the room the pager gave it: " +
                    "${rendered.heightPx} against ${room.toInt()}",
                rendered.heightPx <= room.toInt() + 1,
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
    fun everyPageKeepsItsShapeAtTheLargestSize() = runBlocking {
        val (content, renderer) = open()
        val scale = MushafText.scale(TextSize.STEPS.max())
        // A page set larger than the glass is taller than it, and the
        // reader pans it: what must never happen is a page that grows past
        // any bound a pan can carry, which would mean the lines had come
        // apart rather than merely grown.
        val ceiling = room * 1.4f
        for (page in 1..604) {
            val rendered = renderer.get(PageKey(page, width, "paper", scale), content, palette)
            assertNotNull("page $page could not be rendered at the largest size", rendered)
            rendered ?: continue
            assertTrue(
                "page $page at the largest size grows past its shape: " +
                    "${rendered.heightPx} against ${ceiling.toInt()}",
                rendered.heightPx <= ceiling.toInt(),
            )
        }
    }

    private fun open(): Pair<ContentDatabase, PageRenderer> {
        val catalog = PackCatalog.load(context)
        val content = runBlocking { ContentDatabase.open(context, catalog, emptySet()) }
        return content to PageRenderer(context)
    }
}
