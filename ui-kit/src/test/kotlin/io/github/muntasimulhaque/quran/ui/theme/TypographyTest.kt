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
 * This is the readable scale: a page is 20, a passage's title 17 (capped at
 * 19.5), a subtitle 16, a Book name over a page 16, a Book name in a list 15,
 * a row's name and the app's body 15, a field and the reading body 16, a
 * source and a value 13, a label 12, a button 14, a compact surface 14. The
 * Quran is 26 on a 2.0 line, the aid's word 22, the inline Arabic and the
 * medallion 24, and the gloss 15. The Book's face is the reason the reading
 * takes 16 where the interface takes 15; the sizes are one ladder, and the
 * Bangla ladder reads the same constants (owner decision, this session).
 */
class TypographyTest {

    @Test
    fun everyJobStandsInItsOwnPlace() {
        val article = TypeLadder.articleTitleSp(TextSize.sp(TypeRole.Tafsir, TextSize.DEFAULT))
        assertTrue("a page outranks a passage's title", TypeLadder.SHEET_SP > article)
        // The passage's title is one point over the 16 reading body at the
        // default, and the subtitle is the interface's 16: the voices and the
        // point separate them.
        assertTrue("a passage's title is at least a subtitle", article >= TypeLadder.SUBTITLE_SP)
        assertTrue("a subtitle is never under a row's name", TypeLadder.SUBTITLE_SP > TypeLadder.NAME_SP)
        assertTrue("a Book name over a page is over the app's row name", TypeLadder.BOOK_NAME_SP > TypeLadder.NAME_SP)
        assertEquals("a Book name in a list is the app's row name", TypeLadder.BOOK_ROW_SP, TypeLadder.NAME_SP)
        assertTrue("a name is never smaller than the interface body", TypeLadder.NAME_SP >= TypeLadder.BODY_SP)
        assertEquals(
            "the field and the reading body are one size",
            TypeLadder.FIELD_SP,
            TextSize.sp(TypeRole.Translation, TextSize.DEFAULT),
            0.001f,
        )
        assertTrue("the body outranks the meta line", TypeLadder.BODY_SP > TypeLadder.META_SP)
        assertEquals("the meta line and a row's value are one size", TypeLadder.META_SP, TypeLadder.VALUE_SP)
        assertTrue("the value outranks the label over its group", TypeLadder.VALUE_SP > TypeLadder.LABEL_SP)
        assertTrue("a button's word is a step over a label", TypeLadder.BUTTON_SP > TypeLadder.LABEL_SP)
        assertTrue("a button's word stays under a page's name", TypeLadder.BUTTON_SP < TypeLadder.SHEET_SP)
        assertTrue(
            "a compact surface is over meta and under the body",
            TypeLadder.COMPACT_SP > TypeLadder.META_SP && TypeLadder.COMPACT_SP < TypeLadder.BODY_SP,
        )
        assertEquals("the opening's Latin name is the page's title", TypeLadder.OPENING_NAME_SP, TypeLadder.SHEET_SP)
        assertTrue("the opening's Arabic name outranks its Latin name", TypeLadder.OPENING_ARABIC_SP > TypeLadder.OPENING_NAME_SP)
        assertTrue("a masthead outranks every title", TypeLadder.MASTHEAD_SP > TypeLadder.SHEET_SP)
        assertTrue(
            "the title's cap still leads the largest body",
            TypeLadder.ARTICLE_TITLE_MAX > TextSize.sp(TypeRole.Tafsir, TextSize.STEPS.last()),
        )
        assertTrue("the title's cap stays under the page", TypeLadder.ARTICLE_TITLE_MAX < TypeLadder.SHEET_SP)
    }

    @Test
    fun theShippedLadderIsTheReadableOne() {
        assertEquals(20f, TypeLadder.SHEET_SP, 0.001f)
        assertEquals(17f, TypeLadder.articleTitleSp(TextSize.sp(TypeRole.Tafsir, TextSize.DEFAULT)), 0.001f)
        assertEquals(19.5f, TypeLadder.ARTICLE_TITLE_MAX, 0.001f)
        assertEquals(16f, TypeLadder.SUBTITLE_SP, 0.001f)
        assertEquals(16f, TypeLadder.BOOK_NAME_SP, 0.001f)
        assertEquals(15f, TypeLadder.BOOK_ROW_SP, 0.001f)
        assertEquals(20f, TypeLadder.OPENING_NAME_SP, 0.001f)
        assertEquals(36f, TypeLadder.OPENING_ARABIC_SP, 0.001f)
        assertEquals(21f, TypeLadder.LIST_ARABIC_SP, 0.001f)
        assertEquals(48f, TypeLadder.MASTHEAD_SP, 0.001f)
        assertEquals(15f, TypeLadder.NAME_SP, 0.001f)
        assertEquals(15f, TypeLadder.BODY_SP, 0.001f)
        assertEquals(16f, TypeLadder.FIELD_SP, 0.001f)
        assertEquals(13f, TypeLadder.META_SP, 0.001f)
        assertEquals(12f, TypeLadder.LABEL_SP, 0.001f)
        assertEquals(13f, TypeLadder.VALUE_SP, 0.001f)
        assertEquals(14f, TypeLadder.BUTTON_SP, 0.001f)
        assertEquals(14f, TypeLadder.COMPACT_SP, 0.001f)
        assertEquals(26f, TypeLadder.TAJWEED_SAMPLE_SP, 0.001f)
        assertEquals(18f, TypeLadder.SWATCH_SP, 0.001f)
        // The card is the reading at its default step: 26 Arabic to 16 prose,
        // the same 1.625 the page keeps, and 24 inline under it.
        assertEquals(26f, TypeLadder.SHARE_ARABIC_SP, 0.001f)
        assertEquals(22f, TypeLadder.SHARE_NAME_SP, 0.001f)
        assertEquals(24f, TypeLadder.SHARE_INLINE_SP, 0.001f)
    }

    @Test
    fun aTitleIsNeverSmallerThanTheBodyItTitles() {
        assertTrue(TypeLadder.SHEET_SP >= TypeLadder.BODY_SP)
        assertTrue(TypeLadder.NAME_SP >= TypeLadder.BODY_SP)
        assertTrue(TypeLadder.SUBTITLE_SP >= TypeLadder.BODY_SP)
        assertTrue(TypeLadder.FIELD_SP >= TypeLadder.BODY_SP)
    }

    @Test
    fun theDefaultCardIsOneTitleOverBoldBodyHeads() {
        // The card's article title at 17, one point over the body's 16, and
        // both heads at the body's own size, one Semibold and one Medium. The
        // hierarchy is the title's point and the heads' weight, not a ladder
        // of big lines (owner decision, this session).
        assertEquals(17f, TypeLadder.articleTitleSp(16f), 0.001f)
        assertEquals(16f, TypeLadder.headingSp(16f), 0.001f)
        assertEquals(16f, TypeLadder.subheadingSp(16f), 0.001f)
    }

    @Test
    fun aDocumentKeepsItsOrderAtEveryStepTheReaderCanChoose() {
        // The article title is one point over the body until the cap, and the
        // heads are the body's size at two weights. The cap is 19.5, half a
        // point under the page's own 20, so the title still leads the 19.2
        // body at the largest step and the content cannot outgrow the chrome.
        assertEquals(1f, TypeLadder.ARTICLE_TITLE_LEAD, 0.001f)
        assertEquals(19.5f, TypeLadder.ARTICLE_TITLE_MAX, 0.001f)
        assertTrue(TypeLadder.HEADING == 1f)
        assertTrue(TypeLadder.SUBHEADING == 1f)
        for (step in TextSize.STEPS) {
            val body = TextSize.sp(TypeRole.Tafsir, step)
            val article = TypeLadder.articleTitleSp(body)
            val head = TypeLadder.headingSp(body)
            val sub = TypeLadder.subheadingSp(body)
            assertTrue("the article title must outrank the body at $step", article > body)
            assertEquals(
                "the article title is one point over the body until the cap at $step",
                minOf(body + 1f, TypeLadder.ARTICLE_TITLE_MAX),
                article,
                0.001f,
            )
            assertEquals("a head is the body's own size at $step", body, head, 0.001f)
            assertEquals("a sub-head is the body's own size at $step", body, sub, 0.001f)
            assertTrue("the article title stays under the page's 20 at $step, was $article", article < TypeLadder.SHEET_SP)
        }
    }

    @Test
    fun theReadingBodyIsAProseSizeOnAProseLine() {
        // 16 sp on a 1.5 line: the Book's body, in the Book's face. The line
        // stays the book's 1.5, and 16 is the point at which Literata's 0.507
        // x-height matches Inter's 15 (owner decision, this session).
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
    fun theBanglaReadingTakesTheLatinSizeAndItsOwnLine() {
        // The sizes are the same in both scripts; the line is Bengali's own,
        // because its matras and conjuncts need the taller box.
        assertEquals(16f, BengaliReading.fontSize.value, 0.001f)
        assertEquals(16f * 31f / 18f, BengaliReading.lineHeight.value, 0.001f)
        assertEquals(31f / 18f, TextSize.BENGALI_PROSE_RATIO, 0.001f)
    }

    @Test
    fun theBanglaLadderReadsTheSameSizesAsTheEnglishOne() {
        // The two ladders are one ladder: every used slot is the same
        // TypeLadder constant, and only the face and the line differ. A
        // future size that is edited in one place cannot leave the other
        // script behind.
        assertEquals(TypeLadder.SHEET_SP, BanglaTypography.titleLarge.fontSize.value, 0.001f)
        assertEquals(TypeLadder.BOOK_NAME_SP, BanglaTypography.titleMedium.fontSize.value, 0.001f)
        assertEquals(TypeLadder.NAME_SP, BanglaTypography.titleSmall.fontSize.value, 0.001f)
        assertEquals(TypeLadder.FIELD_SP, BanglaTypography.bodyLarge.fontSize.value, 0.001f)
        assertEquals(TypeLadder.BODY_SP, BanglaTypography.bodyMedium.fontSize.value, 0.001f)
        assertEquals(TypeLadder.META_SP, BanglaTypography.bodySmall.fontSize.value, 0.001f)
        assertEquals(TypeLadder.BUTTON_SP, BanglaTypography.labelLarge.fontSize.value, 0.001f)
        assertEquals(TypeLadder.LABEL_SP, BanglaTypography.labelMedium.fontSize.value, 0.001f)
        assertEquals(TypeLadder.VALUE_SP, BanglaTypography.labelSmall.fontSize.value, 0.001f)
    }

    @Test
    fun theBanglaDisplaySlotsAreTheInterfaceFace() {
        // A theme display slot is chrome; the Book's display lines are asked
        // for by name, in Amiri Quran or the reading voice.
        assertEquals(NotoSansBengali, BanglaTypography.displayLarge.fontFamily)
        assertEquals(NotoSansBengali, BanglaTypography.headlineSmall.fontFamily)
    }

    @Test
    fun aTitleUsesThePlatformsTitleLeading() {
        assertTrue(TypeLadder.TITLE_LINE >= 1.2f)
        assertTrue(TypeLadder.TITLE_LINE <= 1.35f)
        assertEquals(1.3f, TypeLadder.TITLE_LINE, 0.001f)
    }

    @Test
    fun theQuranVoiceKeepsTheAgreedStaircase() {
        // The Quran is the page's anchor at 26; the prose is 16; the inline
        // Arabic and the medallion are 24, a step under the verse. The verse
        // to prose ratio is 1.625 (owner decision, this session). The word
        // meaning sheet draws the Quran's word at 26 and its gloss at 16, so
        // there is no separate aid size to pin.
        assertEquals(26f, TextSize.sp(TypeRole.Arabic, TextSize.DEFAULT), 0.001f)
        assertEquals(52f, TextSize.lineSp(TypeRole.Arabic, TextSize.DEFAULT), 0.001f)
        assertEquals(24f, TextSize.inlineArabic(TextSize.DEFAULT), 0.001f)
        assertEquals(24f, TextSize.medallionSp(TextSize.DEFAULT), 0.001f)
        assertEquals(
            26f / 16f,
            TextSize.sp(TypeRole.Arabic, TextSize.DEFAULT) /
                TextSize.sp(TypeRole.Translation, TextSize.DEFAULT),
            0.001f,
        )
        for (step in TextSize.STEPS) {
            assertTrue(
                "the inline Arabic must stay under the verse at $step",
                TextSize.inlineArabic(step) < TextSize.sp(TypeRole.Arabic, step),
            )
            assertTrue(
                "the medallion must stay under the verse at $step",
                TextSize.medallionSp(step) < TextSize.sp(TypeRole.Arabic, step),
            )
        }
    }
}
