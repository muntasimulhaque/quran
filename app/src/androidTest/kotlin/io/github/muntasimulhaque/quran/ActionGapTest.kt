package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.core.TextRun
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.ui.reader.AyahShareSheet
import io.github.muntasimulhaque.quran.ui.reader.ShareCard
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import io.github.muntasimulhaque.quran.ui.theme.Space
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The share sheet's two doors stand on the app's one action gap.
 *
 * They stood 12 dp apart while the reminder dialog's Set and Cancel stood 20,
 * because the platform lays its own dialog actions 8 dp apart and the app added
 * its whole gap on top. The owner's eye called the dialog's pair the good one
 * and the sheet's pair too tight, so the gap moved to the number the dialog
 * already drew and every pair in the app stands on it now (owner decision, this
 * session).
 *
 * Both doors are wider than the 48 dp target each one keeps, so the node a
 * reader taps is the pill a reader sees and the two bounds below are the
 * shapes, not the invisible targets. Every node here is in one window, which
 * is what makes the measurement readable: the sheet is the window, and the
 * two doors share its origin.
 */
@RunWith(AndroidJUnit4::class)
class ActionGapTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theShareSheetsDoorsStandTheActionGapApart() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light) {
                AyahShareSheet(
                    card = CARD,
                    onShareImage = {},
                    onShareText = {},
                    onDismiss = {},
                )
            }
        }
        val image = context.getString(R.string.share_image)
        val text = context.getString(R.string.share_text)
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText(image).fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        val first = compose.onNodeWithText(image).getUnclippedBoundsInRoot()
        val second = compose.onNodeWithText(text).getUnclippedBoundsInRoot()

        assertEquals(
            "the two doors stand the app's action gap apart",
            Space.Action.value,
            (second.left - first.right).value,
            0.5f,
        )
    }

    private companion object {
        /** Al-Ikhlas 112:1, short enough to leave the doors on one screen. */
        val CARD = ShareCard(
            arabic = "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
            translation = listOf(TextRun("Say, \"He is Allah, the One.")),
            reference = "Al-Ikhlas 112:1",
            text = "Say, \"He is Allah, the One. Al-Ikhlas 112:1",
            surahArabic = "الإخلاص",
        )
    }
}
