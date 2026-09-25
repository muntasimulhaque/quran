package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.ui.reader.ShareCard
import io.github.muntasimulhaque.quran.ui.reader.ShareCardCapture
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The share card is captured whole, however tall the ayah makes it.
 *
 * The card's picture is read off a graphics layer, and on API 28 and up that
 * read-back is a hardware bitmap: a texture, capped by the device's own size
 * limit. A long ayah at a large text scale passes 4,096 px and loses its
 * tail. This test draws the longest ayah in the Book, which is three slices
 * tall, and checks the stitched picture is the card's real height with the
 * last slice's pixels in it.
 */
@RunWith(AndroidJUnit4::class)
class ShareCardCaptureTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun captureTheLongestAyahWhole() {
        var image: ImageBitmap? = null
        var failed = false
        compose.setContent {
            Box(androidx.compose.ui.Modifier.fillMaxSize()) {
                ShareCardCapture(
                    card = LONG_CARD,
                    onImage = { image = it },
                    onFailed = { failed = true },
                )
            }
        }
        compose.waitUntil(timeoutMillis = 60_000) { image != null || failed }
        assertTrue("the capture must not fail", !failed)
        val card = image
        assertNotNull("the card must be captured", card)
        println("SHARE_CARD image=${card!!.width}x${card.height}")
        // The card's own measure at this density is 840 px wide; its height
        // is whatever the text needs, and the point is that nothing capped it.
        assertTrue("the card must be wider than one pixel", card.width > 100)
        assertTrue(
            "a long ayah must keep its tail: ${card.height} px is a slice cap, not a card",
            card.height > 4096,
        )
        // The tail is pixels, not blank: a clipped read-back would leave the
        // bottom of the stitched bitmap transparent, because the card's own
        // paper never is. The last row is read back for that reason.
        val last = Bitmap.createBitmap(
            card.asAndroidBitmap(),
            0,
            card.height - 1,
            card.width,
            1,
        )
        val tail = last.getPixel(card.width / 2, 0)
        println("SHARE_CARD tail=$tail")
        assertTrue(
            "the last slice must be painted, not a transparent gap",
            android.graphics.Color.alpha(tail) == 255,
        )
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
            translation = io.github.muntasimulhaque.quran.core.RichText.footnotes(
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
