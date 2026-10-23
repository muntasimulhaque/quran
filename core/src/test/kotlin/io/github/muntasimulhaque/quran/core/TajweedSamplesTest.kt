package io.github.muntasimulhaque.quran.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The legend's words are cut out of a real ayah, never typed: one word for
 * each family, with that word's own text and its own code, so the reading's
 * painter draws the sample exactly the way it draws the page.
 *
 * The fixture is An-Nisa 4:11 as the content database carries it, text and
 * code, because that ayah's words carry all seven families.
 */
class TajweedSamplesTest {

    private val text = "يُوصِيكُمُ ٱللَّهُ فِىٓ أَوۡلَـٰدِكُمۡ‌ۖ لِلذَّكَرِ مِثۡلُ حَظِّ ٱلۡأُنثَيَيۡنِ‌ۚ فَإِن كُنَّ نِسَآءً فَوۡقَ ٱثۡنَتَيۡنِ فَلَهُنَّ ثُلُثَا مَا تَرَكَ‌ۖ وَإِن كَانَتۡ وٰحِدَةً فَلَهَا ٱلنِّصۡفُ‌ۚ وَلِأَبَوَيۡهِ لِكُلِّ وٰحِدٍ مِّنۡهُمَا ٱلسُّدُسُ مِمَّا تَرَكَ إِن كَانَ لَهُۥ وَلَدٌ‌ۚ فَإِن لَّمۡ يَكُن لَّهُۥ وَلَدٌ وَوَرِثَهُۥٓ أَبَوَاهُ فَلِأُمِّهِ ٱلثُّلُثُ‌ۚ فَإِن كَانَ لَهُۥٓ إِخۡوَةٌ فَلِأُمِّهِ ٱلسُّدُسُ‌ۚ مِنۢ بَعۡدِ وَصِيَّةٍ يُوصِى بِهَآ أَوۡ دَيۡنٍ‌ۗ ءَابَآؤُكُمۡ وَأَبۡنَآؤُكُمۡ لَا تَدۡرُونَ أَيُّهُمۡ أَقۡرَبُ لَكُمۡ نَفۡعًا‌ۚ فَرِيضَةً مِّنَ ٱللَّهِ‌ۗ إِنَّ ٱللَّهَ كَانَ عَلِيمًا حَكِيمًا"

    private val code = "00000000000100000000ggg0000000ee00000000000000000000000000000000010000660000000000000060604400000hh066060000001000000000000000004400000000000000000000000000060600000000e000066060000000124400000000000000000000000000000000e009909000000000012000000000044000000000000606000000000e0000066660600aa0a00000000aa0a0000e000009909000000000gg000000000000000004400001200000000000000606000000000gg000000066060000044000012000000000008880800000000000009909000000000ggg0000000000000000000hh000000000000550hh0000000000000055000000000000000000550000000000000000666660600000099090000010000000000044001000000000000000000000000000000"

    @Test
    fun everyFamilyTakesAWordInItsOwnOrder() {
        val samples = TajweedSamples.of(text, code)
        assertEquals(TajweedFamily.entries.toList(), samples.map { it.family })
    }

    @Test
    fun aWordIsTheAyahsOwnCharactersAndCode() {
        for (sample in TajweedSamples.of(text, code)) {
            assertTrue("${sample.family} is not a run of the ayah", indexOf(sample) >= 0)
        }
    }

    @Test
    fun aWordCarriesItsOwnFamily() {
        for (sample in TajweedSamples.of(text, code)) {
            val families = sample.code.mapNotNull { rule ->
                TajweedRule.fromCode(rule)?.let(TajweedFamily::of)
            }
            assertTrue(
                "${sample.family} has no letter of its own family in ${sample.text}",
                sample.family in families,
            )
        }
    }

    @Test
    fun thisAyahGivesTheseWords() {
        val words = TajweedSamples.of(text, code).associate { it.family to it.text }
        assertEquals(
            mapOf(
                TajweedFamily.SILENT to "ٱلسُّدُسُ",
                TajweedFamily.GHUNNAH to "إِنَّ",
                TajweedFamily.QALAQAH to "أَقۡرَبُ",
                TajweedFamily.IKHAFA to "نَفۡعًا",
                TajweedFamily.IQLAB to "مِنۢ",
                TajweedFamily.IDGHAM to "يَكُن",
                TajweedFamily.MADDA to "فِىٓ",
            ),
            words,
        )
    }

    @Test
    fun aLineThatDoesNotCarryTheAyahGivesNothing() {
        assertEquals(emptyList<TajweedSamples.Sample>(), TajweedSamples.of("abc", "00"))
        assertEquals(emptyList<TajweedSamples.Sample>(), TajweedSamples.of("", ""))
    }

    @Test
    fun aFamilyTheAyahDoesNotCarryIsSkipped() {
        val samples = TajweedSamples.of("قَدۡ", "0550")
        assertEquals(listOf(TajweedFamily.QALAQAH), samples.map { it.family })
        assertEquals("قَدۡ", samples.single().text)
    }

    @Test
    fun aWordThatCarriesTwoColorsIsNotASample() {
        // The first word carries two families, the second one. Every family
        // takes the second word, so the legend teaches one color at a time.
        val samples = TajweedSamples.of("أإ ب", "4504")
        assertEquals(listOf(TajweedFamily.GHUNNAH, TajweedFamily.QALAQAH), samples.map { it.family })
        assertEquals(listOf("ب", "أإ"), samples.map { it.text })
    }

    @Test
    fun theShortestWordWinsWhileTheColorStays() {
        // Both words carry one ghunnah letter, so the shorter one is the sample.
        val samples = TajweedSamples.of("أب أبج", "040040")
        assertEquals(listOf(TajweedFamily.GHUNNAH), samples.map { it.family })
        assertEquals("أب", samples.single().text)
    }

    @Test
    fun aPauseSignIsNotPartOfTheWord() {
        // The print glues the waqf sign to the word; the sample is the word.
        val samples = TajweedSamples.of("قَدۡ\u200c\u06da", "0550\u200c0")
        assertEquals("قَدۡ", samples.single().text)
    }

    private fun indexOf(sample: TajweedSamples.Sample): Int {
        var from = 0
        while (true) {
            val at = text.indexOf(sample.text, from)
            if (at < 0) return -1
            if (code.substring(at, at + sample.text.length) == sample.code) return at
            from = at + 1
        }
    }
}
