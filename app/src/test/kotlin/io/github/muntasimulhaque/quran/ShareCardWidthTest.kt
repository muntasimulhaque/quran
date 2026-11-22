package io.github.muntasimulhaque.quran

import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.reader.ShareCardFeedLimit
import io.github.muntasimulhaque.quran.ui.reader.ShareCardMeasure
import io.github.muntasimulhaque.quran.ui.reader.ShareCardWidths
import io.github.muntasimulhaque.quran.ui.reader.shareCardWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The card's shape, chosen by arithmetic on a machine with no screen.
 *
 * A feed gives a picture the feed's own width only while the picture stands
 * within [ShareCardFeedLimit], and a taller one is contained into a shorter
 * box. The card widens until it stands inside that limit, and this is the
 * rule that decides by how much: the narrowest rung whose measured card
 * fits, or the widest rung when no rung does.
 *
 * The numbers below are the shapes the two cards measured when the rule was
 * re-aimed at the feed: Al-Baqarah 2:255, the card the owner shared when
 * Facebook stood it narrow, and 2:282, the longest ayah. The three narrower
 * 2:255 measures were recorded when the ladder was written, and the 560 one
 * was taken from the capture's own 3x print on quran_phone, because a
 * card's height is a drawn thing and not arithmetic. The preview reports
 * the chosen width through `onWidth` and the capture draws that same width,
 * so this is the one answer the two surfaces share.
 */
class ShareCardWidthTest {

    @Test
    fun theLongAyahWidensIntoTheFeed() {
        // 2:255 measured at the ladder: the first three rungs stand outside
        // the limit and the 560 card is the first inside it.
        assertEquals(
            560.dp,
            shareCardWidth(
                listOf(
                    ShareCardMeasure(320.dp, 960.dp),
                    ShareCardMeasure(400.dp, 860.dp),
                    ShareCardMeasure(480.dp, 692.dp),
                    // The capture printed 2,027 px at its 3x, a 1.207 shape.
                    ShareCardMeasure(560.dp, 676.dp),
                ),
            ),
        )
    }

    @Test
    fun theLongestAyahTakesTheWidestCard() {
        // 2:282 never fits the limit, so the widest card is the closest it
        // gets.
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
        // A card already inside the limit is not widened: every wider rung
        // only spreads the same type over more dp.
        assertEquals(320.dp, shareCardWidth(listOf(ShareCardMeasure(320.dp, 380.dp))))
    }

    @Test
    fun theCardStaysAtTheFirstRungInsideTheLimit() {
        // At 320 the card stands outside the limit and at 400 it is inside:
        // the 400 card is the narrowest that fits, so the type stays as
        // large as the limit allows.
        assertEquals(
            400.dp,
            shareCardWidth(
                listOf(
                    ShareCardMeasure(320.dp, 560.dp),
                    ShareCardMeasure(400.dp, 470.dp),
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
        assertEquals("the feed's own portrait, five quarters tall", 1.25f, ShareCardFeedLimit, 0.001f)
        assertEquals(
            "an unmeasured card keeps the one column",
            320.dp,
            shareCardWidth(emptyList()),
        )
    }
}
