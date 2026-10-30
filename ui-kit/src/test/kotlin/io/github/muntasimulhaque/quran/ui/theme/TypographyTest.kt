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
 * This is the readable scale: a page is 20, a passage's title 18, a subtitle
 * 16, a Book name 16, a row's name and the app's body 15, a source and a
 * value 13, a label 12, a button 14. The Quran is 24 on a 2.0 line, the aid's
 * word 20, the inline Arabic 21.6, and the gloss 14.4. The Book's prose is 16
 * in its own face and the app's prose is 15 in the interface face, so the two
 * are optically equal and the Book leads by its voice, not by a larger body
 * (owner decision, this session).
 */
class TypographyTest {

    @Test
    fun everyJobStandsInItsOwnPlace() {
        assertTrue("a page outranks a passage's title", TypeLadder.SHEET_SP > TypeLadder.ARTICLE_SP)
        assertTrue("a passage's title outranks a subtitle", TypeLadder.ARTICLE_SP > TypeLadder.SUBTITLE_SP)
        assertTrue("a subtitle is never under a row's name", TypeLadder.SUBTITLE_SP > TypeLadder.NAME_SP)
        assertTrue("a Book name is one point over the app's row name", TypeLadder.BOOK_NAME_SP > TypeLadder.NAME_SP)
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
        assertEquals(18f, TypeLadder.ARTICLE_SP, 0.001f)
        assertEquals(16f, TypeLadder.SUBTITLE_SP, 0.001f)
        assertEquals(16f, TypeLadder.BOOK_NAME_SP, 0.001f)
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
        assertEquals(22f, TypeLadder.TAJWEED_SAMPLE_SP, 0.001f)
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
        // The card's article title at 18 and both heads at the body's own 16,
        // one SemiBold and one Medium. The hierarchy is the title's two
        // points and the heads' weight, not a ladder of big lines.
        assertEquals(18f, TypeLadder.articleTitleSp(16f), 0.001f)
        assertEquals(16f, TypeLadder.headingSp(16f), 0.001f)
        assertEquals(16f, TypeLadder.subheadingSp(16f), 0.001f)
    }

    @Test
    fun aDocumentKeepsItsOrderAtEveryStepTheReaderCanChoose() {
        // The article title is the one size step, floored at 18 and capped at
        // 22. The heads are the body's size at two weights, so the order holds
        // at 0.8 and 1.2 without any head crossing the title; at the largest
        // step the content title (21.6) passes the fixed page title (20),
        // which is allowed: the content may outgrow the chrome.
        assertTrue(TypeLadder.ARTICLE_TITLE > 1f)
        assertTrue(TypeLadder.HEADING == 1f)
        assertTrue(TypeLadder.SUBHEADING == 1f)
        for (step in TextSize.STEPS) {
            val body = TextSize.sp(TypeRole.Tafsir, step)
            val article = TypeLadder.articleTitleSp(body)
            val head = TypeLadder.headingSp(body)
            val sub = TypeLadder.subheadingSp(body)
            assertTrue("the article title must outrank the body at $step", article > body)
            assertEquals("a head is the body's own size at $step", body, head, 0.001f)
            assertEquals("a sub-head is the body's own size at $step", body, sub, 0.001f)
            assertTrue("the article title stays under 22 at $step, was $article", article <= 22f)
            assertTrue("the article title stays over 18 at $step, was $article", article >= 18f)
        }
    }

    @Test
    fun theReadingBodyIsAProseSizeOnAProseLine() {
        // 16 sp on a 1.5 line: Material's body size and the floor the long
        // reading asked for after the 15 sp page read as a subhead.
        assertEquals(16f, TextSize.sp(TypeRole.Translation, TextSize.DEFAULT), 0.001f)
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
    fun theBanglaReadingTakesOnePointOverTheLatin() {
        // 17 against the Latin 16, and the interface 16 against 15: the same
        // relationship between the Book and the chrome in both scripts.
        assertEquals(17f, TextSize.BENGALI_READING_SIZE, 0.001f)
        assertEquals(
            1.0625f,
            TextSize.BENGALI_READING_SIZE / TextSize.sp(TypeRole.Translation, TextSize.DEFAULT),
            0.001f,
        )
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
        // The Quran is the page's anchor at 24; the prose is 16; the aid's
        // word is 20, four points under the verse; the inline Amiri is nine
        // tenths of the verse; and the gloss is nine tenths of the prose.
        assertEquals(24f, TextSize.sp(TypeRole.Arabic, TextSize.DEFAULT), 0.001f)
        assertEquals(48f, TextSize.lineSp(TypeRole.Arabic, TextSize.DEFAULT), 0.001f)
        assertEquals(20f, TextSize.wordSp(TextSize.DEFAULT), 0.001f)
        assertEquals(21.6f, TextSize.inlineArabic(TextSize.DEFAULT), 0.001f)
        assertEquals(14.4f, TextSize.meaningSp(TextSize.DEFAULT), 0.001f)
        assertEquals(
            1.5f,
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
