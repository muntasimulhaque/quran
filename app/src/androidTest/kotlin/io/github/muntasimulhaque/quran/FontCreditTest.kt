package io.github.muntasimulhaque.quran

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.feature.settings.R as SettingsR
import io.github.muntasimulhaque.quran.ui.settings.AboutPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The fonts are credited where a reader already is, and their terms travel
 * with them.
 *
 * The page names every face and the hand that made it, and it holds no door:
 * the license screen was a second copy of the same six credits followed by a
 * text no reader asked for, and its opening sentence called the KFGQPC Hafs
 * face OFL when the file carries KFGQPC's own terms (owner report; owner
 * decision, this session). What the five open faces need is a copy of their
 * license in the package, and it ships there beside them, the way every
 * library's LICENSE travels in META-INF.
 */
@RunWith(AndroidJUnit4::class)
class FontCreditTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theAboutPageCreditsEveryFaceAndHoldsNoDoor() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            MaterialTheme {
                AboutPage(packs = emptyList(), version = "12.5 (127)")
            }
        }

        // The faces are credited by name and by hand: the holder of the
        // Quran's own script, and the designer of the reading voice.
        compose.onNodeWithText(context.getString(SettingsR.string.credit_page_fonts))
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText(context.getString(SettingsR.string.credit_literata))
            .performScrollTo()
            .assertIsDisplayed()
        // The hand that made a face and the use it is put to stand on one
        // line, so a credit is a name and one line about it (owner decision,
        // this session).
        val literata = context.getString(SettingsR.string.credit_literata_by) +
            " · " + context.getString(SettingsR.string.credit_literata_use)
        compose.onNodeWithText(literata)
            .performScrollTo()
            .assertIsDisplayed()

        assertEquals(
            "About holds no door: the page is facts and credits",
            0,
            compose.onAllNodes(hasClickAction(), useUnmergedTree = true).fetchSemanticsNodes().size,
        )
    }

    /**
     * The copy of the license the five open faces ship under is still in the
     * package. No screen opens it, and this is the test that keeps it from
     * being deleted as an unused asset: it is the license accompanying the
     * fonts, not a page of the app.
     */
    @Test
    fun theOpenFontLicenseStillTravelsWithTheFonts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val text = context.assets.open("OFL-1.1.txt").bufferedReader().use { it.readText() }
        assertTrue(
            "the package must carry the license the five open faces are under",
            text.contains("SIL OPEN FONT LICENSE"),
        )
        assertTrue(text.contains("PERMISSION & CONDITIONS"))
    }
}
