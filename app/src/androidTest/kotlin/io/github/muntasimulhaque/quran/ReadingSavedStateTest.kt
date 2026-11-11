package io.github.muntasimulhaque.quran

import android.content.Context
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.SavedStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.UiLanguage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A saved ayah says so where the tap that made it lives.
 *
 * The reading used to draw the app's filled bookmark beside a saved ayah's
 * number. The mark answered no touch, and it was the third place for one
 * fact: Browse's Saved sheet lists the ayah, and this ayah's own menu says
 * the keep on the row that made it. So the page draws nothing for a save and
 * the menu's row wears the state in its own word, while the verse goes on
 * saying the keep to a screen reader, because the reader who cannot see a
 * lit row is the one who needed the mark most (owner decision, this session).
 *
 * This test saves 2:255 before the reading starts, reads the page back, and
 * then takes the keep away from the row that says it, so the word, the state
 * and the tap are all measured on the glass.
 */
@RunWith(AndroidJUnit4::class)
class ReadingSavedStateTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun prepare() {
        LanguagePreference(context).set("en")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage(UiLanguage.English.tag)
                setAyah(AYAT_AL_KURSI)
                setShowTranslation(true)
                setWordByWord(false)
            }
            SavedStore(context).apply {
                toggle(AYAT_AL_KURSI)
                close()
            }
        }
        Thread.sleep(300)
    }

    @After
    fun cleanUp() {
        runBlocking {
            SavedStore(context).apply {
                unsave(AYAT_AL_KURSI)
                close()
            }
        }
    }

    @Test
    fun theSaveIsDrawnNowhereAndSaidAtTheRowThatMadeIt() {
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java))
        rule.waitUntil(timeoutMillis = 60_000) {
            rule.onAllNodesWithTag("door-2:255", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        // The page draws nothing of the reader's Save.
        rule.onAllNodesWithTag("door-saved", useUnmergedTree = true).assertCountEquals(0)
        // The verse still speaks it, so a reader who cannot see the menu's
        // lit row learns it while reading.
        rule.onNode(
            hasTestTag("ayah-2:255") and
                SemanticsMatcher("saved in the verse's state") { node ->
                    node.config.getOrNull(SemanticsProperties.StateDescription)
                        ?.contains("Saved") == true
                },
        ).assertExists()
        // The row that made the keep is where the state is read, in a word.
        rule.onNodeWithTag("door-2:255").performClick()
        rule.onNodeWithText("Saved").assertIsDisplayed()
        // And it is the same row that takes the keep away.
        rule.onNodeWithText("Saved").performClick()
        rule.waitUntil(timeoutMillis = 30_000) {
            rule.onAllNodesWithText("Removed").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("door-2:255").performClick()
        rule.waitUntil(timeoutMillis = 30_000) {
            rule.onAllNodesWithText("Save").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Save").assertIsDisplayed()
    }

    private companion object {
        /** 2:255 in the Quran's sequential numbering. */
        const val AYAT_AL_KURSI = 7 + 255
    }
}
