package io.github.muntasimulhaque.quran

import android.content.Context
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
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
 * A saved ayah's own door says so.
 *
 * Save closes the menu and the page looked exactly as it did before, so the
 * only way to learn that the tap worked was to open the menu again. The door
 * now draws the app's filled bookmark beside its number, and says the keep to
 * a screen reader with the menu's own state. This test saves 2:255 before the
 * reading starts and reads the mark back on the page (owner decision, this
 * session).
 */
@RunWith(AndroidJUnit4::class)
class ReadingSavedMarkTest {

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
    fun aSavedAyahWearsTheBookmarkAndSaysIt() {
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java))
        rule.waitUntil(timeoutMillis = 60_000) {
            rule.onAllNodesWithTag("door-saved", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("door-saved", useUnmergedTree = true).assertIsDisplayed()
        // The mark is a drawing, so the state is the door's own words as well.
        rule.onNode(
            hasContentDescription("Ayah 2:255 actions", substring = true) and
                SemanticsMatcher("saved in the door's state") { node ->
                    node.config.getOrNull(SemanticsProperties.StateDescription)
                        ?.contains("Saved") == true
                },
        ).assertExists()
    }

    private companion object {
        /** 2:255 in the Quran's sequential numbering. */
        const val AYAT_AL_KURSI = 7 + 255
    }
}
