package io.github.muntasimulhaque.quran.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The QUL Al-Bayan rows carry the translation and at most one note, with the
 * note pasted after a blank line and a star. The reading wants the sentence
 * and a marker it can open, so the star becomes `[1]`, the body becomes the
 * note, and every star QUL did not fill leaves nothing behind.
 */
class BayanTranslationTest {

    @Test
    fun aNoteBecomesOneMarkerAndOneNote() {
        val parsed = bayanAyah(
            "আলিফ-লাম-মীম।* \n\n*এ অক্ষরগুলো কুরআনের একটি মু‘জিযা।",
        )
        assertEquals("আলিফ-লাম-মীম।[1]", parsed.text)
        assertEquals("এ অক্ষরগুলো কুরআনের একটি মু‘জিযা।", parsed.note)
    }

    @Test
    fun aSecondStarWithNoNoteIsRemoved() {
        val parsed = bayanAyah(
            "‘মান্না’* ও ‘সালওয়া’**।\n\n*‘মান্না’ এক ধরণের খাবার।",
        )
        assertEquals("‘মান্না’[1] ও ‘সালওয়া’।", parsed.text)
        assertFalse(parsed.text.contains("*"))
        assertEquals("‘মান্না’ এক ধরণের খাবার।", parsed.note)
    }

    @Test
    fun aStarWithNoNoteLeavesNoMarker() {
        // 34:2 carries a star and no note body in the QUL export.
        val parsed = bayanAyah("আর তিনি পরম দয়ালু, অতিশয় ক্ষমাশীল* ।")
        assertEquals("আর তিনি পরম দয়ালু, অতিশয় ক্ষমাশীল ।", parsed.text)
        assertFalse(parsed.text.contains("[1]"))
        assertNull(parsed.note)
    }

    @Test
    fun aLeadingBulletIsNotPartOfTheNote() {
        val parsed = bayanAyah(
            "আর যখন আমি মূসাকে দিয়েছিলাম কিতাব ও ফুরকান* যাতে...\n\n*- সত্য-মিথ্যার পার্থক্যকারী।",
        )
        assertEquals(
            "আর যখন আমি মূসাকে দিয়েছিলাম কিতাব ও ফুরকান[1] যাতে...",
            parsed.text,
        )
        assertEquals("সত্য-মিথ্যার পার্থক্যকারী।", parsed.note)
    }

    @Test
    fun aVerseWithoutANoteIsUntouched() {
        val parsed = bayanAyah("সমস্ত প্রশংসা আল্লাহর জন্য, যিনি সৃষ্টিকুলের রব।")
        assertEquals("সমস্ত প্রশংসা আল্লাহর জন্য, যিনি সৃষ্টিকুলের রব।", parsed.text)
        assertNull(parsed.note)
    }

    @Test
    fun aNoteWithBracketsKeepsThem() {
        val parsed = bayanAyah(
            "একটি আয়াত*।\n\n*দিনের প্রথম প্রান্তে ফজরের সালাত [তাফসীর ইবন কাসীর, তাইসীরু কারীমির রহমান]।",
        )
        assertEquals("একটি আয়াত[1]।", parsed.text)
        assertTrue(parsed.note.orEmpty().contains("[তাফসীর ইবন কাসীর, তাইসীরু কারীমির রহমান]"))
    }

    @Test
    fun aNoteWithExtraSpaceIsOneLine() {
        val parsed = bayanAyah("শব্দ*\n\n*  নোট   এক  ।")
        assertEquals("শব্দ[1]", parsed.text)
        assertEquals("নোট এক ।", parsed.note)
    }

    @Test
    fun aStarBeforePunctuationKeepsItsPlace() {
        val parsed = bayanAyah("শব্দ* ।\n\n*নোট।")
        assertEquals("শব্দ[1] ।", parsed.text)
        assertEquals("নোট।", parsed.note)
    }

    @Test
    fun aBodyThatStartsWithASecondStarIsStillOneNote() {
        val parsed = bayanAyah("শব্দ*\n\n**নোট।")
        assertEquals("শব্দ[1]", parsed.text)
        assertEquals("নোট।", parsed.note)
    }

    @Test
    fun aNoteWithNoStarIsAttachedAtTheEnd() {
        // 9:1, 21:36, and 34:54 carry a general note and no star.
        val parsed = bayanAyah(
            "আল্লাহ ও তাঁর রাসূলের পক্ষ থেকে ঘোষণা। \n\n*সূরার প্রথমে বিসমিল্লাহ লিপিবদ্ধ করার কথা।",
        )
        assertEquals("আল্লাহ ও তাঁর রাসূলের পক্ষ থেকে ঘোষণা।[1]", parsed.text)
        assertEquals("সূরার প্রথমে বিসমিল্লাহ লিপিবদ্ধ করার কথা।", parsed.note)
    }

    @Test
    fun aStarInsideTheNoteIsRemoved() {
        val parsed = bayanAyah("শব্দ*\n\n*আমাদের অমঙ্গলের। **শয়তান।")
        assertEquals("শব্দ[1]", parsed.text)
        assertEquals("আমাদের অমঙ্গলের। শয়তান।", parsed.note)
    }

    @Test
    fun theExportsLostConjunctSignIsRemoved() {
        // The QUL export has U+00AC where a conjunct glyph was lost. The
        // publisher's text at the same places has no sign there.
        val parsed = bayanAyah("আর তারা উৎফুল\u00AC হয়, পরবর্তীদের থেকে।")
        assertEquals("আর তারা উৎফুল হয়, পরবর্তীদের থেকে।", parsed.text)
    }
}
