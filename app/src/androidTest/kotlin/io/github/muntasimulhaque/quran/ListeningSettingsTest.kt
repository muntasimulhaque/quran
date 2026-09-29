package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.ui.settings.ListeningPage
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Listening page carries the pace, the repeat, and the continuation.
 *
 * The continuation switch is the reader's word for the packages that follow,
 * so the row must be there and must report the choice it was given: turning
 * it on from this page and from the pill are the same value, the same as the
 * pace and the repeat before it (owner decision, D-105).
 *
 * The switch is looked for in the unmerged tree, and it has to be: the row
 * itself is the door and the switch is its own control inside it, so the
 * merged tree reads one control for the pair and the switch's own tag is not
 * in it. Every test that names a switch in a row that opens a page asks for
 * the unmerged tree, and this one did not, so it was reading a node that is
 * not there (thirty-seventh session).
 */
@RunWith(AndroidJUnit4::class)
class ListeningSettingsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theContinuationSwitchReportsItsOwnChoice() {
        var continuation = false
        compose.setContent {
            QuranTheme(theme = AppTheme.Paper) {
                ListeningPage(
                    settings = AppSettings(continueSurah = continuation),
                    onSpeed = {},
                    onRepeat = {},
                    onContinue = { continuation = it },
                )
            }
        }
        compose.onNodeWithTag("switch-continue", useUnmergedTree = true).performClick()
        assertTrue("the continue switch must report the choice", continuation)
    }
}
