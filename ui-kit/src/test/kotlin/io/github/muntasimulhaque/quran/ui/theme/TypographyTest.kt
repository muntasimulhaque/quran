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
 * without a red run (owner decision, this session).
 *
 * This is the compact scale: the body is 15, a sheet is 18, an article 18, a
 * section head the body's own 15 at SemiBold, and a sub-head the body's own
 * 15 at Medium. The Quran is 24 on a 2.0 line, the aid's word 22, the inline
 * Arabic 21.6, and the gloss 13.5. The 13 sp "English" source line is the one
 * size that did not move (owner decision, this session).
 */
class TypographyTest {

    @Test
    fun everyJobStandsInItsOwnPlace() {
        assertTrue("a sheet's name outranks a row's", TypeLadder.SHEET_SP > TypeLadder.NAME_SP)
        assertTrue("a name is never smaller than the body", TypeLadder.NAME_SP >= TypeLadder.BODY_SP)
        assertTrue("the body outranks the meta line", TypeLadder.BODY_SP > TypeLadder.META_SP)
        assertTrue("the meta line outranks the caption", TypeLadder.META_SP > TypeLadder.CAPTION_SP)
        assertEquals("the label and the caption share 12", TypeLadder.LABEL_SP, TypeLadder.CAPTION_SP)
        assertTrue("a button's word is a step over a label", TypeLadder.BUTTON_SP > TypeLadder.LABEL_SP)
        assertTrue("a button's word stays under a sheet's name", TypeLadder.BUTTON_SP < TypeLadder.SHEET_SP)
    }

    @Test
    fun theShippedLadderIsTheCompactOne() {
        assertEquals(18f, TypeLadder.SHEET_SP, 0.001f)
        assertEquals(15f, TypeLadder.NAME_SP, 0.001f)
        assertEquals(15f, TypeLadder.BODY_SP, 0.001f)
        assertEquals(13f, TypeLadder.META_SP, 0.001f)
        assertEquals(12f, TypeLadder.CAPTION_SP, 0.001f)
        assertEquals(13f, TypeLadder.BUTTON_SP, 0.001f)
    }

    @Test
    fun aTitleIsNeverSmallerThanTheBodyItTitles() {
        assertTrue(TypeLadder.SHEET_SP >= TypeLadder.BODY_SP)
        assertTrue(TypeLadder.NAME_SP >= TypeLadder.BODY_SP)
    }

    @Test
    fun theDefaultCardIsOneTitleOverBoldBodyHeads() {
        // The frame the owner reported: the card's article title at 18 and
        // both heads at the body's own 15, one SemiBold and one Medium. The
        // hierarchy is the title's three points and the heads' weight, not a
        // ladder of big lines.
        assertEquals(18f, TypeLadder.articleTitleSp(15f), 0.001f)
        assertEquals(15f, TypeLadder.headingSp(15f), 0.001f)
        assertEquals(15f, TypeLadder.subheadingSp(15f), 0.001f)
    }

    @Test
    fun aDocumentKeepsItsOrderAtEveryStepTheReaderCanChoose() {
        // The article title is the one size step, floored at 18 and capped at
        // 22. The heads are the body's size at two weights, so the order holds
        // at 0.8 and 1.2 without any head crossing the title, and the title
        // never falls below the sheet's 18 even at the compact step.
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
        // 15 sp on a 1.5 line: the mobile body's floor, which the owner's
        // long reading asked for after 16.
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
    fun aTitleUsesThePlatformsTitleLeading() {
        assertTrue(TypeLadder.TITLE_LINE >= 1.2f)
        assertTrue(TypeLadder.TITLE_LINE <= 1.35f)
        assertEquals(1.3f, TypeLadder.TITLE_LINE, 0.001f)
    }

    @Test
    fun theQuranVoiceKeepsTheAgreedStaircase() {
        // The owner asked for the Quran one step larger and the prose one
        // smaller. The app sits at 1.6 times the prose, more contrast than the
        // field (Quran for Android 1.4, Quran.com 1.28) and less than the
        // app's own 1.8 before the audit: 24 on 48, with the aid's word two
        // points under, the inline Amiri at nine tenths, and the gloss at
        // nine tenths of the prose.
        assertEquals(24f, TextSize.sp(TypeRole.Arabic, TextSize.DEFAULT), 0.001f)
        assertEquals(48f, TextSize.lineSp(TypeRole.Arabic, TextSize.DEFAULT), 0.001f)
        assertEquals(22f, TextSize.wordSp(TextSize.DEFAULT), 0.001f)
        assertEquals(21.6f, TextSize.inlineArabic(TextSize.DEFAULT), 0.001f)
        assertEquals(13.5f, TextSize.meaningSp(TextSize.DEFAULT), 0.001f)
        assertEquals(
            1.6f,
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
