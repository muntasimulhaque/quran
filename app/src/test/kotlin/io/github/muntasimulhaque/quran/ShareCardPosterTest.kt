package io.github.muntasimulhaque.quran

import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.reader.ShareCardMeasure
import io.github.muntasimulhaque.quran.ui.reader.ShareCardPosterRatio
import io.github.muntasimulhaque.quran.ui.reader.ShareCardWidths
import io.github.muntasimulhaque.quran.ui.reader.shareCardWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The card's shape, chosen by arithmetic on a machine with no screen.
 *
 * The card is one column of reading with a height the ayah decides, so a long
 * ayah used to leave the app as a 1:3 or a 1:9 strip. The card now widens
 * before it grows into one, and this is the rule that decides by how much:
 * the ladder's width whose measured card stands closest to
 * [ShareCardPosterRatio], stopping at the first card at or under the ratio
 * because every wider one is shorter still.
 *
 * The numbers below are the shapes the owner's two cards had when the rule
 * was written: Al-Baqarah 2:255, which the card the owner shared carried at
 * 1:3.1, and 2:282, the longest ayah, at about 1:7.3. The preview reports
 * the chosen width through `onWidth` and the capture draws that same width,
 * so this is the one answer the two surfaces share.
 */
class ShareCardPosterTest {

    @Test
    fun theLongAyahWidensToItsPoster() {
        // 2:255 measured at the ladder; the preview stops at 480, the first
        // card at or under 2:1, and 400 is the closer of the three.
        assertEquals(
            400.dp,
            shareCardWidth(
                listOf(
                    ShareCardMeasure(320.dp, 960.dp),
                    ShareCardMeasure(400.dp, 860.dp),
                    ShareCardMeasure(480.dp, 692.dp),
                ),
            ),
        )
    }

    @Test
    fun theLongestAyahTakesTheWidestCard() {
        // 2:282 never reaches 2:1, so the widest card is the closest it gets.
        assertEquals(
            560.dp,
            shareCardWidth(
                listOf(
                    ShareCardMeasure(320.dp, 2348.dp),
                    ShareCardMeasure(400.dp, 1820.dp),
                    ShareCardMeasure(480.dp, 1508.dp),
                    ShareCardMeasure(560.dp, 1268.dp),
                ),
            ),
        )
    }

    @Test
    fun anOrdinaryAyahKeepsOneColumn() {
        // A card already squarer than the poster is not widened: every wider
        // card is shorter still, and farther from the shape the card wants.
        assertEquals(320.dp, shareCardWidth(listOf(ShareCardMeasure(320.dp, 380.dp))))
    }

    @Test
    fun theWidthWhoseCardIsClosestWins() {
        // At 320 the card is 2.5 times tall and at 400 it is 1.9: the 400 card
        // is the nearer one even though the 320 card already reads as a card.
        assertEquals(
            400.dp,
            shareCardWidth(
                listOf(
                    ShareCardMeasure(320.dp, 800.dp),
                    ShareCardMeasure(400.dp, 760.dp),
                ),
            ),
        )
    }

    @Test
    fun aTieGoesToTheNarrowerCard() {
        // 2.5 and 1.5 are equally far from the 2:1 the card wants; the narrow
        // card keeps the type large, so the tie goes to it.
        assertEquals(
            320.dp,
            shareCardWidth(
                listOf(
                    ShareCardMeasure(320.dp, 800.dp),
                    ShareCardMeasure(400.dp, 600.dp),
                ),
            ),
        )
    }

    @Test
    fun theLadderIsTheCardsOwn() {
        assertEquals("the card's one column", 320.dp, ShareCardWidths.first())
        assertEquals("the card's widest page", 560.dp, ShareCardWidths.last())
        assertTrue(
            "the ladder rises",
            ShareCardWidths.zipWithNext().all { (narrow, wide) -> wide > narrow },
        )
        assertEquals("the poster ratio", 2f, ShareCardPosterRatio, 0.001f)
        assertEquals(
            "an unmeasured card keeps the one column",
            320.dp,
            shareCardWidth(emptyList()),
        )
    }
}
