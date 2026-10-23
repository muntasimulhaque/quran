package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.platform.testTag
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.ui.settings.TajweedPage
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The legend shows a word for every family, in the family's own color, and it
 * names every rule under it. The words are the ayah's own, cut by
 * [TajweedSamples]; this test keeps the page honest about both halves: the
 * seven families and the eighteen rules are on the page, and the page it
 * draws is kept as a frame beside the test's own result.
 *
 * The frame is not part of the store set. It is written beside the test so a
 * change to the legend can be looked at the way the tour's frames are looked
 * at, without a ninth frame in the listing.
 */
class TajweedLegendTest {

    @get:Rule
    val compose = createComposeRule()

    /** An-Nisa 4:11, the ayah whose words carry all seven families. */
    private val ayah = Ayah(
        number = 504,
        surah = 3,
        ayah = 49,
        verseKey = "4:11",
        text = "يُوصِيكُمُ ٱللَّهُ فِىٓ أَوۡلَـٰدِكُمۡ‌ۖ لِلذَّكَرِ مِثۡلُ حَظِّ ٱلۡأُنثَيَيۡنِ‌ۚ فَإِن كُنَّ نِسَآءً فَوۡقَ ٱثۡنَتَيۡنِ فَلَهُنَّ ثُلُثَا مَا تَرَكَ‌ۖ وَإِن كَانَتۡ وٰحِدَةً فَلَهَا ٱلنِّصۡفُ‌ۚ وَلِأَبَوَيۡهِ لِكُلِّ وٰحِدٍ مِّنۡهُمَا ٱلسُّدُسُ مِمَّا تَرَكَ إِن كَانَ لَهُۥ وَلَدٌ‌ۚ فَإِن لَّمۡ يَكُن لَّهُۥ وَلَدٌ وَوَرِثَهُۥٓ أَبَوَاهُ فَلِأُمِّهِ ٱلثُّلُثُ‌ۚ فَإِن كَانَ لَهُۥٓ إِخۡوَةٌ فَلِأُمِّهِ ٱلسُّدُسُ‌ۚ مِنۢ بَعۡدِ وَصِيَّةٍ يُوصِى بِهَآ أَوۡ دَيۡنٍ‌ۗ ءَابَآؤُكُمۡ وَأَبۡنَآؤُكُمۡ لَا تَدۡرُونَ أَيُّهُمۡ أَقۡرَبُ لَكُمۡ نَفۡعًا‌ۚ فَرِيضَةً مِّنَ ٱللَّهِ‌ۗ إِنَّ ٱللَّهَ كَانَ عَلِيمًا حَكِيمًا",
        tajweed = "00000000000100000000ggg0000000ee00000000000000000000000000000000010000660000000000000060604400000hh066060000001000000000000000004400000000000000000000000000060600000000e000066060000000124400000000000000000000000000000000e009909000000000012000000000044000000000000606000000000e0000066660600aa0a00000000aa0a0000e000009909000000000gg000000000000000004400001200000000000000606000000000gg000000066060000044000012000000000008880800000000000009909000000000ggg0000000000000000000hh000000000000550hh0000000000000055000000000000000000550000000000000000666660600000099090000010000000000044001000000000000000000000000000000",
    )

    private val families = listOf(
        "Silent letters", "Ghunnah", "Qalqalah", "Ikhfa", "Iqlab", "Idgham", "Madda",
    )

    private val rules = listOf(
        "Hamzat ul Wasl", "Lam Shamsiyyah", "Silent", "Ghunnah", "Qalqalah", "Ikhfa",
        "Ikhfa Shafawi", "Iqlab", "Idgham with Ghunnah", "Idgham without Ghunnah",
        "Idgham Shafawi", "Idgham Mutajanisayn", "Idgham Mutaqaribayn",
        "Madd Normal", "Madd Permissible", "Madd Munfasil",
        "Madd Muttasil", "Madd Necessary",
    )

    @Test
    fun everyFamilyAndEveryRuleIsNamed() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light) {
                TajweedPage(
                    settings = AppSettings(),
                    sample = { ayah },
                )
            }
        }
        compose.waitForIdle()
        for (name in families + rules) {
            assertTrue(
                "$name is not on the legend",
                compose.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty(),
            )
        }
    }

    @Test
    fun theLegendKeepsItsOwnFrames() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light) {
                TajweedPage(
                    settings = AppSettings(),
                    sample = { ayah },
                )
            }
        }
        compose.waitForIdle()
        keep("10-tajweed-legend-top", compose.onRoot().captureToImage().asAndroidBitmap())

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Madd Necessary"))
        compose.waitForIdle()
        keep("11-tajweed-legend-foot", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    private fun keep(name: String, bitmap: Bitmap) {
        val directory = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "shots",
        ).apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
