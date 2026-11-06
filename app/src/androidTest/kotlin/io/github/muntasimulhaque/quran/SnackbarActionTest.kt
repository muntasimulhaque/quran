package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.ui.kit.AppSnackbar
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Snackbar's one action is the app's one pill.
 *
 * The bar used to borrow Material's bare action text, so the only word that
 * acted on the sentence was the one word on the page without a shape. The
 * action is the app's `TextButton` now, with the same 36 dp pill and 48 dp target as
 * every other action (owner decision, the consistency audit).
 */
@RunWith(AndroidJUnit4::class)
class SnackbarActionTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theSnackbarActionWearsTheAppPill() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                val host = remember { SnackbarHostState() }
                LaunchedEffect(Unit) {
                    host.showSnackbar(
                        message = "Saved ayah removed",
                        actionLabel = "Undo",
                    )
                }
                Box(Modifier.fillMaxSize()) {
                    AppSnackbar(hostState = host)
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Undo").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("snackbar-action-pill", useUnmergedTree = true)
            .assertHeightIsEqualTo(36.dp)
    }
}
