package io.github.muntasimulhaque.quran.ui.theme

import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The type ladder, pinned where a change can break it.
 *
 * The color system has had a measured test since the theme audit, and the type
 * had none: the ayah card shipped twice with its own name and its own heads
 * out of order, and nothing failed. This test is the type's own guard,
 * reading the same [TypeLadder] constants the theme is built from, so a future
 * ladder cannot invert the order, inflate the top, or flatten the steps
 * without a red run.
 *
 * This is the readable scale: a page is 20, a passage's title 16, a subtitle
 * 16, a Book name over a page 16, a Book name in a list 15, a row's name and
 * the app's body 15, a source and a value 13, a label 12, a button 14. The
 * Quran is 26 on a 2.0 line, the aid's word 22, the inline Arabic 23.4, and
 * the gloss 13.5. The Book's prose and the app's prose are both 15; the Book
 * leads by its face, not by a larger body (owner decision, this session).
 */
class TypographyTest {

    @Test
    fun everyJobStandsInItsOwnPlace() {
        assertTrue("a page outranks a passage's title", TypeLadder.SHEET_SP > TypeLadder.ARTICLE_SP)
        // A passage's title and a subtitle share the 16: the title is the
        // Book's Semibold and the subtitle is the app's Medium, so the voice
        // does the separating where a one point gap no longer can.
        assertTrue("a passage's title is at least a subtitle", TypeLadder.ARTICLE_SP >= TypeLadder.SUBTITLE_SP)
        assertTrue("a subtitle is never under a row's name", TypeLadder.SUBTITLE_SP > TypeLadder.NAME_SP)
        assertTrue("a Book name over a page is over the app's row name", TypeLadder.BOOK_NAME_SP > TypeLadder.NAME_SP)
        assertEquals("a Book name in a list is the app's row name", TypeLadder.BOOK_ROW_SP, TypeLadder.NAME_SP)
        assertTrue("a name is never smaller than the body", TypeLadder.NAME_SP >= TypeLadder.BODY_SP)
        assertTrue("the body outranks the meta line", TypeLadder.BODY_SP > TypeLadder.META_SP)
        assertEquals("the meta line and a row's value are one size", TypeLadder.META_SP, TypeLadder.CAPTION_SP)
        assertTrue("the value outranks the label over its group", TypeLadder.CAPTION_SP > TypeLadder.LABEL_SP)
        assertTrue("a button's word is a step over a label", TypeLadder.BUTTON_SP > TypeLadder.LABEL_SP)
        assertTrue("a button's word stays under a page's name", TypeLadder.BUTTON_SP < TypeLadder.SHEET_SP)
        assertEquals("the opening's Latin name is the page's title", TypeLadder.OPENING_NAME_SP, TypeLadder.SHEET_SP)
        assertTrue("the opening's Arabic name outranks its Latin name", TypeLadder.OPENING_ARABIC_SP > TypeLadder.OPENING_NAME_SP)
        assertTrue("a masthead outranks every title", TypeLadder.MASTHEAD_SP > TypeLadder.SHEET_SP)
    }

    @Test
    fun theShippedLadderIsTheReadableOne() {
        assertEquals(20f, TypeLadder.SHEET_SP, 0.001f)
        assertEquals(16f, TypeLadder.ARTICLE_SP, 0.001f)
        assertEquals(16f, TypeLadder.SUBTITLE_SP, 0.001f)
        assertEquals(16f, TypeLadder.BOOK_NAME_SP, 0.001f)
        assertEquals(15f, TypeLadder.BOOK_ROW_SP, 0.001f)
        assertEquals(20f, TypeLadder.OPENING_NAME_SP, 0.001f)
        assertEquals(36f, TypeLadder.OPENING_ARABIC_SP, 0.001f)
        assertEquals(21f, TypeLadder.LIST_ARABIC_SP, 0.001f)
        assertEquals(48f, TypeLadder.MASTHEAD_SP, 0.001f)
        assertEquals(15f, TypeLadder.NAME_SP, 0.001f)
        assertEquals(15f, TypeLadder.BODY_SP, 0.001f)
        assertEquals(13f, TypeLadder.META_SP, 0.001f)
        assertEquals(12f, TypeLadder.LABEL_SP, 0.001f)
        assertEquals(13f, TypeLadder.CAPTION_SP, 0.001f)
        assertEquals(14f, TypeLadder.BUTTON_SP, 0.001f)
        assertEquals(26f, TypeLadder.TAJWEED_SAMPLE_SP, 0.001f)
        assertEquals(18f, TypeLadder.SWATCH_SP, 0.001f)
        assertEquals(24f, TypeLadder.SHARE_ARABIC_SP, 0.001f)
        assertEquals(22f, TypeLadder.SHARE_NAME_SP, 0.001f)
    }

    @Test
    fun aTitleIsNeverSmallerThanTheBodyItTitles() {
        assertTrue(TypeLadder.SHEET_SP >= TypeLadder.BODY_SP)
        assertTrue(TypeLadder.NAME_SP >= TypeLadder.BODY_SP)
        assertTrue(TypeLadder.SUBTITLE_SP >= TypeLadder.BODY_SP)
    }

    @Test
    fun theDefaultCardIsOneTitleOverBoldBodyHeads() {
        // The card's article title at 16, one point over the body's 15, and
        // both heads at the body's own size, one Semibold and one Medium. The
        // hierarchy is the title's point and the heads' weight, not a ladder
        // of big lines (owner decision, this session).
        assertEquals(16f, TypeLadder.articleTitleSp(15f), 0.001f)
        assertEquals(15f, TypeLadder.headingSp(15f), 0.001f)
        assertEquals(15f, TypeLadder.subheadingSp(15f), 0.001f)
    }

    @Test
    fun aDocumentKeepsItsOrderAtEveryStepTheReaderCanChoose() {
        // The article title is one point over the body at every step, and the
        // heads are the body's size at two weights. The title never reaches
        // the page's own 20, even at the largest step, so the content cannot
        // outgrow the chrome.
        assertEquals(1f, TypeLadder.ARTICLE_TITLE_LEAD, 0.001f)
        assertTrue(TypeLadder.HEADING == 1f)
        assertTrue(TypeLadder.SUBHEADING == 1f)
        for (step in TextSize.STEPS) {
            val body = TextSize.sp(TypeRole.Tafsir, step)
            val article = TypeLadder.articleTitleSp(body)
            val head = TypeLadder.headingSp(body)
            val sub = TypeLadder.subheadingSp(body)
            assertTrue("the article title must outrank the body at $step", article > body)
            assertEquals("the article title is one point over the body at $step", body + 1f, article, 0.001f)
            assertEquals("a head is the body's own size at $step", body, head, 0.001f)
            assertEquals("a sub-head is the body's own size at $step", body, sub, 0.001f)
            assertTrue("the article title stays under the page's 20 at $step, was $article", article < TypeLadder.SHEET_SP)
        }
    }

    @Test
    fun theReadingBodyIsAProseSizeOnAProseLine() {
        // 15 sp on a 1.5 line: the app's own body, in the Book's face. The
        // line stays the book's 1.5, so the smaller letters keep the page's
        // air (owner decision, this session).
        assertEquals(15f, TextSize.sp(TypeRole.Translation, TextSize.DEFAULT), 0.001f)
        assertEquals(1.5f, TextSize.LATIN_PROSE_RATIO, 0.001f)
        for (step in TextSize.STEPS) {
            val body = TextSize.sp(TypeRole.Translation, step)
            val line = TextSize.lineSp(TypeRole.Translation, step)
            val ratio = line / body
            assertTrue(
                "the Latin prose line must stay between 1.4 and 1.7 at $step, was $ratio",
                ratio >= 1.4f && ratio <= 1.7f,
            )
        }
    }

    @Test
    fun theBanglaReadingTakesTheLatinSizeAndItsOwnLine() {
        // The sizes are the same in both scripts; the line is Bengali's own,
        // because its matras and conjuncts need the taller box.
        assertEquals(15f, BengaliReading.fontSize.value, 0.001f)
        assertEquals(15f * 31f / 18f, BengaliReading.lineHeight.value, 0.001f)
        assertEquals(31f / 18f, TextSize.BENGALI_PROSE_RATIO, 0.001f)
    }

    @Test
    fun aTitleUsesThePlatformsTitleLeading() {
        assertTrue(TypeLadder.TITLE_LINE >= 1.2f)
        assertTrue(TypeLadder.TITLE_LINE <= 1.35f)
        assertEquals(1.3f, TypeLadder.TITLE_LINE, 0.001f)
    }

    @Test
    fun theQuranVoiceKeepsTheAgreedStaircase() {
        // The Quran is the page's anchor at 26; the prose is 15; the aid's
        // word is 22, four points under the verse; the inline Amiri is nine
        // tenths of the verse; and the gloss is nine tenths of the prose. The
        // verse to prose ratio is 1.733 because the prose came down and the
        // verse stayed (owner decision, this session).
        assertEquals(26f, TextSize.sp(TypeRole.Arabic, TextSize.DEFAULT), 0.001f)
        assertEquals(52f, TextSize.lineSp(TypeRole.Arabic, TextSize.DEFAULT), 0.001f)
        assertEquals(22f, TextSize.wordSp(TextSize.DEFAULT), 0.001f)
        assertEquals(23.4f, TextSize.inlineArabic(TextSize.DEFAULT), 0.001f)
        assertEquals(13.5f, TextSize.meaningSp(TextSize.DEFAULT), 0.001f)
        assertEquals(
            26f / 15f,
            TextSize.sp(TypeRole.Arabic, TextSize.DEFAULT) /
                TextSize.sp(TypeRole.Translation, TextSize.DEFAULT),
            0.001f,
        )
        for (step in TextSize.STEPS) {
            assertTrue(
                "the aid's word must stay under the verse at $step",
                TextSize.wordSp(step) < TextSize.sp(TypeRole.Arabic, step),
            )
            assertTrue(
                "the inline Arabic must stay under the verse at $step",
                TextSize.inlineArabic(step) < TextSize.sp(TypeRole.Arabic, step),
            )
            assertTrue(
                "the gloss must stay under the translation at $step",
                TextSize.meaningSp(step) < TextSize.sp(TypeRole.Translation, step),
            )
        }
    }
}
