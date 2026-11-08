package io.github.muntasimulhaque.quran

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.UiLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The ayah number's press shape, measured on the glass.
 *
 * The flash used to be the row's own bounds: no air on the leading end, so the
 * digits touched it, 4 dp above and below, and the reader's marks stood inside
 * it. It is the app's compact pill now, 8 dp of air at each end and above and
 * below, inside the 48 dp target, and the reference stays on the rail the
 * translation under it keeps (owner decision, this session). A press cannot be
 * photographed without a finger on it, so this reads the shape's bounds.
 */
@RunWith(AndroidJUnit4::class)
class AyahDoorShapeTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun prepare() {
        LanguagePreference(context).set("en")
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage(UiLanguage.English.tag)
                setAyah(1)
                setShowTranslation(true)
                setWordByWord(false)
            }
        }
        Thread.sleep(300)
    }

    @Test
    fun theDoorKeepsItsAirOnEverySide() {
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java))
        rule.waitUntil(timeoutMillis = 60_000) {
            rule.onAllNodesWithTag("door-1:1", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        val door = rule.onNodeWithTag("door-1:1", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val flash = rule.onNode(
            hasTestTag("door-flash") and hasAnyAncestor(hasTestTag("door-1:1")),
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        val reference = rule.onNode(
            hasTestTag("door-reference") and hasAnyAncestor(hasTestTag("door-1:1")),
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        val block = rule.onNodeWithTag("ayah-1:1", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        assertEquals(
            "the door owes the finger its 48 dp",
            48f,
            (door.bottom - door.top).value,
            1f,
        )
        assertEquals(
            "the flash is the line plus its 8 dp above and below",
            (reference.bottom - reference.top).value + 16f,
            (flash.bottom - flash.top).value,
            1.5f,
        )
        assertEquals(
            "the flash stands on the door's leading edge",
            0f,
            (flash.left - door.left).value,
            1f,
        )
        assertEquals(
            "the flash is centred in the target",
            (door.bottom - flash.bottom).value,
            (flash.top - door.top).value,
            1f,
        )
        assertEquals(
            "the digits keep 8 dp of air from the flash",
            8f,
            (reference.left - flash.left).value,
            1f,
        )
        // The number still stands on the rail the translation under it keeps:
        // the wash's own inset, not the flash's.
        assertEquals(
            "the reference keeps the page's rail",
            8f,
            (reference.left - block.left).value,
            1f,
        )
    }
}
