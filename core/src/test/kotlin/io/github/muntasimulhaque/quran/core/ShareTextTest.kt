package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Bidi

/**
 * The share text's shape, and the reason for its one invisible character.
 *
 * Plain text has one direction, read off its first strong character by every
 * bidi-aware receiver. Without the mark the Arabic ayah turns the whole
 * message right to left, so the translation is right-aligned and the period
 * at its end is swept to the front of the sentence. These tests pin both
 * halves: the mark is there exactly once and first, and the base direction
 * the receiver will read flips because of it.
 */
class ShareTextTest {

    private val arabic = "ٱللَّهُ لَآ إِلَٰهَ إِلَّا هُوَ"
    private val translation = "Allah - there is no deity except Him."
    private val reference = "Al-Baqarah 2:255"

    @Test
    fun theTextOpensWithOneLeftToRightMark() {
        val text = ShareText.ayah(arabic, translation, reference)
        assertEquals(ShareText.LTR_MARK, text.first())
        assertEquals(1, text.count { it == ShareText.LTR_MARK })
    }

    @Test
    fun theAyatItsTranslationAndItsReferenceKeepTheirOrder() {
        assertEquals(
            "${ShareText.LTR_MARK}$arabic\n\n$translation\n\n$reference",
            ShareText.ayah(arabic, translation, reference),
        )
    }

    @Test
    fun aMissingTranslationLeavesNoEmptyParagraph() {
        assertEquals(
            "${ShareText.LTR_MARK}$arabic\n\n$reference",
            ShareText.ayah(arabic, translation = null, reference = reference),
        )
        assertEquals(
            "${ShareText.LTR_MARK}$arabic\n\n$reference",
            ShareText.ayah(arabic, translation = "   ", reference = reference),
        )
    }

    @Test
    fun theMarkTurnsTheMessageToTheReadingSide() {
        val untouched = Bidi(arabic + "\n\n" + translation, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT)
        val shared = Bidi(
            ShareText.ayah(arabic, translation, reference),
            Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT,
        )
        assertFalse("an Arabic first line turns the message right", untouched.baseIsLeftToRight())
        assertTrue("the mark keeps every line on the reading's left", shared.baseIsLeftToRight())
    }
}
