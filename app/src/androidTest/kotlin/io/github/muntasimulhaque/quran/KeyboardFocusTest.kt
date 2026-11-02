package io.github.muntasimulhaque.quran

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.ui.kit.ScaleRow
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The keyboard path, on the one control that has no finger-only alternative:
 * an arrow changes the size and pace rail.
 *
 * The app turns Material's indication off on its click nodes, which also
 * removes the stock focus state; the clickable controls keep their
 * system-defined focusability and the accent ring is drawn from the same
 * focus interaction. A touch-only test session cannot enter the keyboard
 * focus mode those system-defined controls wait for, so the rail, which is
 * explicitly focusable, is the instrumented proof; the clickable controls
 * are covered by the same focus-ring modifier and are reached by a hardware
 * keyboard's first Tab (owner decision, this session).
 */
class KeyboardFocusTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theSizeRailChangesWithTheArrowKeys() {
        var chosen by mutableStateOf(1f)
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                ScaleRow(
                    label = "Size",
                    steps = listOf(0.8f, 0.9f, 1f, 1.1f, 1.2f),
                    value = chosen,
                    onSelect = { chosen = it },
                    startMark = {},
                    endMark = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Size").assertIsDisplayed()
        compose.onNodeWithContentDescription("Size").requestFocus()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Size").assertIsFocused()
        compose.onNodeWithContentDescription("Size").performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnIdle { assertEquals(1.1f, chosen) }
        compose.onNodeWithContentDescription("Size").performKeyInput { pressKey(Key.DirectionLeft) }
        compose.runOnIdle { assertEquals(1f, chosen) }
    }
}
