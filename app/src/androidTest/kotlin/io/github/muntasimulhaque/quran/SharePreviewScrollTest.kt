package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.ui.reader.ShareCard
import io.github.muntasimulhaque.quran.ui.reader.ShareCardPreviewBox
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The share sheet's preview scrolls a long card instead of clipping it.
 *
 * The height cap has to sit on the viewport, outside the scroll. With the
 * two swapped the cap lands on the card itself, the card is held at 340 dp,
 * the scroll has nothing to scroll, and the tail is clipped off the bottom:
 * a reader with a long ayah sees the first lines and a cut (owner report,
 * D-097). The test draws the same box the sheet draws, with the longest ayah
 * in the Book, and scrolls the translation's last words into view. A clipped
 * card cannot answer that scroll, so this fails on the old order and passes
 * on the new one.
 */
@RunWith(AndroidJUnit4::class)
class SharePreviewScrollTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theFootOfALongCardCanBeScrolledTo() {
        compose.setContent { ShareCardPreviewBox(LONG_CARD) }
        compose.waitForIdle()
        // The tail exists even when it is clipped; that is the card being
        // measured. Bringing it into the viewport is what the reader does,
        // and what a clamped card cannot answer.
        val tail = compose.onAllNodesWithText(
            "knowing of all things.",
            substring = true,
            useUnmergedTree = true,
        ).onFirst()
        tail.performScrollTo()
        tail.assertIsDisplayed()
    }

    private companion object {
        /**
         * Al-Baqarah 2:282, the longest ayah in the Quran, with its Saheeh
         * International translation: the card the owner shared when they saw
         * a long ayah cut.
         */
        val LONG_CARD = ShareCard(
            arabic = "يَـٰٓأَيُّهَا ٱلَّذِينَ آمَنُوا إِذَا تَدَايَنتُم بِدَيْنٍ إِلَىٰ أَجَلٍ مُّسَمًّى " +
                "فَٱكْتُبُوهُ ۚ وَلْيَكْتُب بَّيْنَكُمْ كَاتِبٌ بِٱلْعَدْلِ ۚ وَلَا يَأْبَ كَاتِبٌ أَن " +
                "يَكْتُبَ كَمَا عَلَّمَهُ ٱللَّهُ ۖ فَلْيَكْتُبْ وَلْيُمْلِلِ ٱلَّذِى عَلَيْهِ ٱلْحَقُّ " +
                "وَلْيَتَّقِ ٱللَّهَ رَبَّهُ ۚ وَلَا يَبْخَسْ مِنْهُ شَيْئًا ۚ فَإِن كَانَ ٱلَّذِى " +
                "عَلَيْهِ ٱلْحَقُّ سَفِيهًا أَوْ ضَعِيفًا أَوْ لَا يَسْتَطِيعُ أَن يُمِلَّ هُوَ " +
                "فَلْيُمْلِلْ وَلِيُّهُۥ بِٱلْعَدْلِ ۚ وَٱسْتَشْهِدُوا شَهِيدَيْنِ مِن رِّجَالِكُمْ ۖ " +
                "فَإِن لَّمْ يَكُونَا رَجُلَيْنِ فَرَجُلٌ وَٱمْرَأَتَانِ مِمَّن تَرْضَوْنَ مِنَ " +
                "ٱلشُّهَدَآءِ أَن تَضِلَّ إِحْدَىٰهُمَا فَتُذَكِّرَ إِحْدَىٰهُمَا ٱلْأُخْرَىٰ ۚ وَلَا " +
                "يَأْبَ ٱلشُّهَدَآءُ إِذَا مَا دُعُوا ۚ وَلَا تَسْأَمُوٓا أَن تَكْتُبُوهُ صَغِيرًا " +
                "أَوْ كَبِيرًا إِلَىٰ أَجَلِهِۦ ۚ ذَٰلِكُمْ أَقْسَطُ عِندَ ٱللَّهِ وَأَقْوَمُ " +
                "لِلشَّهَٰدَةِ وَأَدْنَىٰٓ أَلَّا تَرْتَابُوٓا ۖ إِلَّآ أَن تَكُونَ تِجَٰرَةً " +
                "حَاضِرَةً تُدِيرُونَهَا بَيْنَكُمْ فَلَيْسَ عَلَيْكُمْ جُنَاحٌ أَلَّا " +
                "تَكْتُبُوهَا ۗ وَأَشْهِدُوٓا إِذَا تَبَايَعْتُمْ ۚ وَلَا يُضَآرَّ كَاتِبٌ وَلَا " +
                "شَهِيدٌ ۚ وَإِن تَفْعَلُوا فَإِنَّهُۥ فُسُوقٌ بِكُمْ ۗ وَٱتَّقُوا ٱللَّهَ ۖ " +
                "وَيُعَلِّمُكُمُ ٱللَّهُ ۗ وَٱللَّهُ بِكُلِّ شَىْءٍ عَلِيمٌ",
            translation = RichText.footnotes(
                "O you who have believed, when you contract a debt for a specified term, " +
                    "write it down. And let a scribe write [it] between you in justice. Let no " +
                    "scribe refuse to write as Allah has taught him. So let him write and let " +
                    "the one who has the obligation dictate. And let him fear Allah, his Lord, " +
                    "and not leave anything out of it. But if the one who has the obligation " +
                    "is of limited understanding or weak or unable to dictate himself, then " +
                    "let his guardian dictate in justice. And bring to witness two witnesses " +
                    "from among your men. And if there are not two men [available], then a man " +
                    "and two women from those whom you accept as witnesses - so that if one of " +
                    "the women errs, then the other can remind her. And let not the witnesses " +
                    "refuse when they are called upon. And do not be [too] weary to write it, " +
                    "whether it is small or large, for its [specified] term. That is more just " +
                    "in the sight of Allah and stronger as evidence and more likely to prevent " +
                    "doubt between you, except when it is an immediate transaction which you " +
                    "conduct among yourselves. For [then] there is no blame upon you if you do " +
                    "not write it. And take witnesses when you conclude a contract. Let no " +
                    "scribe or witness be caused to harm. And if you do that, indeed, it is " +
                    "sin in you. And fear Allah. And Allah is teaching you. And Allah is " +
                    "knowing of all things.",
            ),
            reference = "Al-Baqarah 2:282",
            text = "plain text",
        )
    }
}
