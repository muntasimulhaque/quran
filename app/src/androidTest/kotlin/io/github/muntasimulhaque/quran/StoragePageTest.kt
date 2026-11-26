package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.StorageSummary
import io.github.muntasimulhaque.quran.ui.settings.StoragePage
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The storage page's two large acts are asked about before they run.
 *
 * The page is the only place that adds the downloads up, and the only place
 * that can clear a reciter's audio or every pronunciation in one tap. The
 * question is the same shape as every other removal in the app, and the
 * confirm answer is the one that performs it (owner decision, 23.1).
 */
@RunWith(AndroidJUnit4::class)
class StoragePageTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun thePageNamesTheWeightsAndTheRecitationDoorAsks() {
        var removed = false
        compose.setContent {
            QuranTheme {
                StoragePage(
                    summary = {
                        StorageSummary(
                            contentBytes = 1_000,
                            recitationBytes = 2_000,
                            pronunciationBytes = 3_000,
                            partialBytes = 4_000,
                            freeBytes = 5_000,
                            totalBytes = 15_000,
                        )
                    },
                    onRemoveAllRecitations = { removed = true },
                    onRemoveAllWordAudio = {},
                )
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("storage-free").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("storage-recitations-action").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("remove-all-recitations-title")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("remove-all-recitations-confirm").performClick()
        compose.waitUntil(10_000) { removed }
        assertTrue("the confirm must perform the removal", removed)
    }
}
