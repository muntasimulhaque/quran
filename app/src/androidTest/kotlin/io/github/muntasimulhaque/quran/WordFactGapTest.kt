package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpRect
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The word sheet's facts, measured: the room between two of them, and the line
 * the Root fact's reading rides.
 *
 * A fact is a block: its own head, its own value, its own count. So it opens
 * a [Space.Section] under whatever stands above it, never a [Space.Block],
 * and the owner's frame is why. With a block between two facts and a section
 * only after the entry, one fact's count and the next fact's label stood
 * apart by almost the same air as a label and its own value, and the three
 * facts read as one column of type rather than three entries (owner decision,
 * this session).
 *
 * The measurement is the fact's label against the fact's own top, and against
 * the fact before it. The label is the fact's first child, so the first number
 * is the room the fact opens with and the second is the room the reader sees
 * between one fact and the next. Both legs live here: the default step and the
 * largest, because the room is a dp and the type is not.
 *
 * The Root fact's reading is measured the same way and for the same reason:
 * the transliteration shares the letters' baseline, so its box starts a few dp
 * into theirs, where a Latin pinned to the line's top starts at zero. That was
 * the sheet's one line that its KDoc promised and the code did not draw (owner
 * report, this session).
 *
 * The sheet is its own window, so every node below is measured inside that
 * window: a difference between two of its bounds is the room on the glass,
 * while a popup's absolute place would be read from the window's own corner.
 *
 * The test hands the app back as it found it, because every instrumented class
 * in a run shares one process and one set of settings.
 */
@RunWith(AndroidJUnit4::class)
class WordFactGapTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** The shipped step: the room every reader sees first. */
    @Test
    fun everyFactOpensASectionUnderTheOneBeforeIt() = atTheStep(TextSize.DEFAULT)

    /** The largest step: the room is a dp, so the type moving must not move it. */
    @Test
    fun theSectionsHoldAtTheLargestTypeStep() = atTheStep(TextSize.STEPS.last())

    /**
     * The transliteration rides the letters' line. Its box starts 9 dp into
     * the Arabic's, where its own shallow baseline meets the letters' deep one,
     * and never at the line's top, which is where it stood while only the
     * Latin declared the alignment and the Root fact read as four lines.
     */
    @Test
    fun theRootsReadingRidesTheLettersLine() = atTheStep(TextSize.DEFAULT) {
        val arabic = compose.onNodeWithTag("word-sheet-root-arabic", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val latin = compose.onNodeWithTag("word-sheet-root-latin", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val drop = latin.top.value - arabic.top.value
        assertTrue(
            "the transliteration must share the letters' line: its box starts $drop dp into " +
                "the letters' box, where a Latin pinned to the line's top starts at 0",
            drop > 4f,
        )
    }

    /**
     * Seeds [step], opens the first word of Al-Fatiha's own sheet, and holds
     * the facts it drew to the rule, then whatever [check] adds.
     */
    private fun atTheStep(step: Float, check: () -> Unit = {}) {
        val original = runBlocking { SettingsStore(context).settings.first() }
        try {
            onTheSheet(step) { facts ->
                assertTheRhythm(facts)
                check()
            }
        } finally {
            restore(original)
        }
    }

    /** Every fact opens a section, and the ones after the first stand one apart. */
    private fun assertTheRhythm(facts: List<String>) {
        assertTrue(
            "the sheet must draw two facts at least, found ${facts.size}",
            facts.size >= 2,
        )
        val boxes = facts.map { fact(it) }
        val labels = facts.map { label(it) }
        assertEquals(
            "the first fact must open a section under the entry, and " +
                "${facts.first()} opens ${labels.first().top.value - boxes.first().top.value} dp in",
            Space.Section.value,
            (labels.first().top - boxes.first().top).value,
            TOLERANCE,
        )
        for (index in 1 until facts.size) {
            val gap = labels[index].top.value - boxes[index - 1].bottom.value
            assertEquals(
                "two facts must stand a section apart, never a block's 16, and " +
                    "${facts[index - 1]} stands $gap dp above ${facts[index]}",
                Space.Section.value,
                gap,
                TOLERANCE,
            )
        }
    }

    /** Seeds the reader, opens the reading, and hands the drawn facts to [check]. */
    private fun onTheSheet(step: Float, check: (List<String>) -> Unit) {
        seed(step)
        val reading = ActivityScenario.launch(MainActivity::class.java)
        try {
            val page = context.getString(ReadingR.string.reading_page_description)
            compose.waitUntil(timeoutMillis = 180_000) {
                compose.onAllNodesWithContentDescription(page)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            // Al-Fatiha's first word, held where it is drawn: the line runs
            // right to left, so the word begins at the line's right edge.
            compose.onAllNodesWithText(FIRST_WORD, substring = true, useUnmergedTree = true)
                .onFirst()
                .holdTheFirstWord()
            compose.waitUntil(timeoutMillis = 60_000) {
                compose.onAllNodesWithTag(SHEET, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            // The lemma and the grammar are the two study lines every word
            // with an entry carries, so the sheet is whole when both stand.
            for (tag in REQUIRED) {
                compose.waitUntil(timeoutMillis = 60_000) {
                    compose.onAllNodesWithTag(tag, useUnmergedTree = true)
                        .fetchSemanticsNodes().isNotEmpty()
                }
            }
            check(FACTS.filter { drawn(it) })
        } finally {
            reading.close()
        }
    }

    /** A reader on Al-Fatiha's first verse, with the word list on, at [step]. */
    private fun seed(step: Float) {
        val packs = PackStore(context)
        packs.install(WORDS)
        packs.install(ROOTS)
        LanguagePreference(context).set("en")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage("en")
                setAyah(1)
                setTheme(AppTheme.Light)
                setWordByWord(true)
                for (role in TypeRole.entries) setTypeSize(role, step)
            }
        }
    }

    /** The sheet's facts, in the order the sheet draws them. */
    private fun drawn(tag: String): Boolean =
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    /** One fact's own box, which starts at the room it opens with. */
    private fun fact(tag: String): DpRect =
        compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    /** One fact's label, the fact's first child. */
    private fun label(tag: String): DpRect =
        compose.onNodeWithTag("$tag-label", useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun restore(original: AppSettings) {
        runBlocking {
            SettingsStore(context).apply {
                setAyah(original.ayah)
                setTranslationPacks(original.translationPacks)
                setTafsirPacks(original.tafsirPacks)
                setShowTranslation(original.showTranslation)
                setWordByWord(original.wordByWord)
                original.uiLanguage?.let { setUiLanguage(it) }
                setTheme(original.theme)
                for (role in TypeRole.entries) setTypeSize(role, original.sizeOf(role))
            }
        }
    }

    private companion object {
        /** 1:1's first word, which opens a sheet with a study entry. */
        const val FIRST_WORD = "بِسۡمِ"

        /** The sheet's own tag, drawn by WordSheet. */
        const val SHEET = "word-sheet"

        /** The word list and the roots a study entry needs. */
        const val WORDS = "words-en"
        const val ROOTS = "word-root"

        /** Every fact the sheet can draw, in the order it draws them. */
        val FACTS = listOf("word-sheet-root", "word-sheet-lemma", "word-sheet-grammar")

        /** The two facts every word with a study entry carries. */
        val REQUIRED = listOf("word-sheet-lemma", "word-sheet-grammar")

        /** The sub-pixel slack a measured dp is allowed: nothing. */
        const val TOLERANCE = 0.5f
    }
}
