package io.github.muntasimulhaque.quran

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.feature.settings.R as SettingsR
import io.github.muntasimulhaque.quran.ui.settings.AboutPage
import io.github.muntasimulhaque.quran.ui.settings.LicensesPage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The font license is readable inside the app.
 *
 * The About page names each face and its copyright holder, and the Fonts
 * group ends in a row that opens the full SIL Open Font License 1.1. This
 * reaches the page the way a reader does, through that row, and reads the
 * license's own words back from it (owner decision, 11.1).
 */
@RunWith(AndroidJUnit4::class)
class FontLicenseTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theLicenseRowOpensTheFullOflText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            var open by remember { mutableStateOf(false) }
            MaterialTheme {
                if (open) {
                    LicensesPage()
                } else {
                    AboutPage(
                        packs = emptyList(),
                        version = "11.1 (113)",
                        onOpenLicenses = { open = true },
                    )
                }
            }
        }

        compose.onNodeWithText(context.getString(SettingsR.string.about_open_licenses))
            .performScrollTo()
            .performClick()

        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("SIL OPEN FONT LICENSE", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("PERMISSION & CONDITIONS", substring = true).assertExists()
        compose.onNodeWithText("THE FONT SOFTWARE IS PROVIDED", substring = true).assertExists()
    }
}
