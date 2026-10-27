package io.github.muntasimulhaque.quran.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Al-Bayan rows carry the sentence and the notes the reader opens. QUL
 * keeps only the first note of most ayahs; the publisher's Word edition keeps
 * all 205, so its notes replace the body's and its count decides the markers.
 * These tests pin that rule and every repair it makes.
 */
class BayanTranslationTest {

    @Test
    fun aNoteBecomesOneMarkerAndOneNote() {
        val parsed = bayanAyah(
            "আলিফ-লাম-মীম।* \n\n*এ অক্ষরগুলো কুরআনের একটি মু‘জিযা।",
        )
        assertEquals("আলিফ-লাম-মীম।[1]", parsed.text)
        assertEquals(listOf("এ অক্ষরগুলো কুরআনের একটি মু‘জিযা।"), parsed.notes)
    }

    @Test
    fun aSecondStarWithNoPublisherNoteIsRemoved() {
        // With no publisher file the old QUL rule stands: the second star had
        // no note, so it leaves nothing behind.
        val parsed = bayanAyah(
            "‘মান্না’* ও ‘সালওয়া’**।\n\n*‘মান্না’ এক ধরনের খাবার।",
        )
        assertEquals("‘মান্না’[1] ও ‘সালওয়া’।", parsed.text)
        assertFalse(parsed.text.contains("*"))
        assertEquals(listOf("‘মান্না’ এক ধরণের খাবার।"), parsed.notes)
    }

    @Test
    fun thePublisherNotesReplaceTheQulBodyAndFillEveryStar() {
        val parsed = bayanAyah(
            "‘মান্না’* ও ‘সালওয়া’**।\n\n*‘মান্না’ এক ধরনের খাবার।",
            sourceNotes = listOf("মান্নার নোট।", "সালওয়ার নোট।"),
        )
        assertEquals("‘মান্না’[1] ও ‘সালওয়া’[2]।", parsed.text)
        assertEquals(listOf("মান্নার নোট।", "সালওয়ার নোট।"), parsed.notes)
    }

    @Test
    fun aBodyWithTwoNotesSplitsAtTheSecondStar() {
        // 57:14 and 72:19 kept both notes in QUL's body, welded by a star.
        val parsed = bayanAyah(
            "তোমরা অপেক্ষা করেছিলে* এবং … আর মহা প্রতারক** তোমাদেরকে …\n\n* আমাদের অমঙ্গলের। **শয়তান।",
        )
        assertEquals("আমাদের অমঙ্গলের।", parsed.notes[0])
        assertEquals("শয়তান।", parsed.notes[1])
        assertTrue(parsed.text.contains("[1]"))
        assertTrue(parsed.text.contains("[2]"))
    }

    @Test
    fun aGeneralNoteGoesAfterItsWord() {
        // 31:12: QUL's revised sentence says জ্ঞান, the word the note
        // explains, so the marker follows that word (owner decision).
        val parsed = bayanAyah(
            "আমি অবশ্যই লুকমানকে জ্ঞান দান করেছিলাম।",
            sourceNotes = listOf("হিকমাত অর্থ: জ্ঞান, প্রজ্ঞা।"),
            anchorAfter = bayanAnchors["31:12"],
        )
        assertEquals("আমি অবশ্যই লুকমানকে জ্ঞান[1] দান করেছিলাম।", parsed.text)
        assertEquals(listOf("হিকমাত অর্থ: জ্ঞান, প্রজ্ঞা।"), parsed.notes)
    }

    @Test
    fun aGeneralNoteWithNoAnchorGoesAtTheEnd() {
        val parsed = bayanAyah(
            "আল্লাহ ও তাঁর রাসূলের পক্ষ থেকে ঘোষণা। \n\n*সূরার প্রথমে বিসমিল্লাহ।",
        )
        assertEquals("আল্লাহ ও তাঁর রাসূলের পক্ষ থেকে ঘোষণা।[1]", parsed.text)
        assertEquals(listOf("সূরার প্রথমে বিসমিল্লাহ।"), parsed.notes)
    }

    @Test
    fun aQulNoteThePublisherPlacesElsewhereIsDropped() {
        // 34:54 carries 34:2's note. The publisher's placement wins.
        val note = "যমীনে যা প্রবেশ করে তন্মধ্যে রয়েছে বৃষ্টির পানি।"
        val parsed = bayanAyah(
            "আর তাদের ও তারা যা কামনা করত তার মধ্যে অন্তরাল করা হবে।\n\n*$note",
            allSourceNotes = setOf(normalizeNote(note)),
        )
        assertTrue(parsed.notes.isEmpty())
        assertFalse(parsed.text.contains("[1]"))
    }

    @Test
    fun aStarWithNoNoteAndNoPublisherNoteLeavesNoMarker() {
        val parsed = bayanAyah("আর তিনি পরম দয়ালু, অতিশয় ক্ষমাশীল* ।")
        assertFalse(parsed.text.contains("[1]"))
        assertFalse(parsed.text.contains("*"))
        assertTrue(parsed.notes.isEmpty())
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
        assertEquals(listOf("সত্য-মিথ্যার পার্থক্যকারী।"), parsed.notes)
    }

    @Test
    fun aVerseWithoutANoteIsUntouched() {
        val parsed = bayanAyah("সমস্ত প্রশংসা আল্লাহর জন্য, যিনি সৃষ্টিকুলের রব।")
        assertEquals("সমস্ত প্রশংসা আল্লাহর জন্য, যিনি সৃষ্টিকুলের রব।", parsed.text)
        assertTrue(parsed.notes.isEmpty())
    }

    @Test
    fun aNoteWithBracketsKeepsThem() {
        val parsed = bayanAyah(
            "একটি আয়াত*।\n\n*দিনের প্রথম প্রান্তে ফজরের সালাত [তাফসীর ইবন কাসীর]।",
        )
        assertEquals("একটি আয়াত[1]।", parsed.text)
        assertTrue(parsed.notes.first().contains("[তাফসীর ইবন কাসীর]"))
    }

    @Test
    fun aBidiMarkIsNotPartOfTheNote() {
        // 5:97's third note carries U+200E between two letters.
        val parsed = bayanAyah(
            "শব্দ*।",
            sourceNotes = listOf("হজযাত্রীগণ পশু গলায় মালা পরিয়ে চি\u200Eহ্নত করে নিয়ে যায়।"),
        )
        assertEquals("হজযাত্রীগণ পশু গলায় মালা পরিয়ে চিহ্নত করে নিয়ে যায়।", parsed.notes.first())
    }

    @Test
    fun aStrayLeadingPeriodIsNotPartOfTheNote() {
        // 18:80's publisher note begins with a stray period.
        val parsed = bayanAyah(
            "শব্দ*।",
            sourceNotes = listOf(". তাঁর আশংকা নিছক ধারণা ছিল না।"),
        )
        assertEquals("শব্দ[1]।", parsed.text)
        assertEquals(listOf("তাঁর আশংকা নিছক ধারণা ছিল না।"), parsed.notes)
    }

    @Test
    fun theMisspelledTypeIsTheCorrectOne() {
        // ধরন is the misspelling; ধরণ is the owner's spelling, in the
        // sentence and in the note alike.
        val parsed = bayanAyah(
            "এর মাঝামাঝি ধরনের।* \n\n*এক ধরনের খাবার।",
        )
        assertEquals("এর মাঝামাঝি ধরণের।[1]", parsed.text)
        assertEquals(listOf("এক ধরণের খাবার।"), parsed.notes)
    }

    @Test
    fun aLegacyArabicNoteFallsBackToTheQulBody() {
        // The Word file stores 33:37's Arabic quotation in a legacy font; the
        // QUL body has the same note in Unicode.
        val parsed = bayanAyah(
            "একটি আয়াত*।\n\n*فَلَمَّا قَضَىٰ زَيْدٌ مِّنْهَا وَطَرًا এর আভিধানিক অর্থ হল।",
            sourceNotes = listOf("\uFB85 \uFB86 \uFB87 \uFB88 \uFB89 এর আভিধানিক অর্থ হল।"),
        )
        assertEquals("একটি আয়াত[1]।", parsed.text)
        assertTrue(parsed.notes.first().startsWith("فَلَمَّا"))
    }

    @Test
    fun theExportsLostConjunctSignIsRemoved() {
        val parsed = bayanAyah("আর তারা উৎফুল\u00AC হয়, পরবর্তীদের থেকে।")
        assertEquals("আর তারা উৎফুল হয়, পরবর্তীদের থেকে।", parsed.text)
    }
}
