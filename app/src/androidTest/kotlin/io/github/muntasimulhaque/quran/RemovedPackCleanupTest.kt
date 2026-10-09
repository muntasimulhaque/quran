package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.study.R as StudyR
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * A pack the catalog no longer names can never be read again, so the app
 * drops it at launch and takes the dead id out of the reader's settings with
 * it. As-Sa'di's Arabic tafsir is the pack this exists for: a reader who had
 * installed it kept bytes the app could no longer show, and a settings list
 * that still named it. The test stands a stray pack in the reader's own
 * storage and reads the proof back after the library opens.
 */
@RunWith(AndroidJUnit4::class)
class RemovedPackCleanupTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            val stray = File(context.filesDir, "packs/$STRAY")
            stray.mkdirs()
            File(stray, "$STRAY.db").writeText("a pack the catalog no longer names")
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setTafsirPacks(setOf(STRAY))
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun aPackTheCatalogNoLongerNamesIsRemoved() {
        val studyPage = compose.activity.getString(StudyR.string.study_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(studyPage).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "the stray pack must be gone from the device",
            STRAY !in PackStore(context).installed(),
        )
        val settings = runBlocking { SettingsStore(context).settings.first() }
        assertTrue(
            "the dead id must be gone from the reader's settings",
            STRAY !in settings.tafsirPacks,
        )
    }

    private companion object {
        /** The pack the catalog stopped naming when the Arabic tafsir left. */
        const val STRAY = "tafsir-as-sadi-ar"
    }
}
