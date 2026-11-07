package io.github.muntasimulhaque.quran

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.PlaybackPositionStore
import io.github.muntasimulhaque.quran.data.RecitationStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import io.github.muntasimulhaque.quran.playback.PlaybackService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * The reader's own door to Al-Fatiha: the ayah menu's Play on 1:1 starts the
 * bundled audio with no download offer. The file stays in the app's assets,
 * so there is nothing to fetch and nothing to ask.
 */
@RunWith(AndroidJUnit4::class)
class FatihaPlayTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var original: AppSettings = AppSettings()

    /**
     * A reading on 1:1 in Husary's voice, with neither timing pack on the
     * device and no downloaded audio, which is the first open the app gives a
     * fresh install. Both packs and both Fatiha sets must come back from the
     * build's own assets.
     */
    private val library = object : ExternalResource() {
        override fun before() {
            original = runBlocking { SettingsStore(context).settings.first() }
            LanguagePreference(context).set("en")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                InstrumentationRegistry.getInstrumentation().uiAutomation
                    .grantRuntimePermission(
                        context.packageName,
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
            }
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setTranslationPacks(emptySet())
                    setTafsirPacks(emptySet())
                    setShowTranslation(true)
                    setWordByWord(false)
                    setRecitation("husary")
                }
                PackStore(context).apply {
                    remove("reciter-husary")
                    remove("reciter-minshawi")
                }
                RecitationStore(context).apply {
                    removeAll("husary")
                    removeAll("minshawyMurattal")
                }
                PlaybackPositionStore(context).clear()
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun playOnTheFirstAyahStartsWithoutAnOffer() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("ayah-1:1").performClick()
        val play = compose.activity.getString(R.string.action_play)
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithText(play).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(play).performClick()
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("playback-bar").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "Al-Fatiha is bundled, so Play must not ask for a download",
            compose.onAllNodesWithTag("listen-offer-bar").fetchSemanticsNodes().isEmpty(),
        )
        // Give a delayed offer a chance to appear before the assertion stands.
        Thread.sleep(1_000)
        assertTrue(
            "the offer must stay away after the audio starts",
            compose.onAllNodesWithTag("listen-offer-bar").fetchSemanticsNodes().isEmpty(),
        )
    }

    @After
    fun handTheAppBack() {
        context.stopService(Intent(context, PlaybackService::class.java))
        runBlocking {
            SettingsStore(context).apply {
                setAyah(original.ayah)
                setRecitation(original.recitation)
                setTranslationPacks(original.translationPacks)
                setTafsirPacks(original.tafsirPacks)
                setShowTranslation(original.showTranslation)
                setWordByWord(original.wordByWord)
                original.uiLanguage?.let { setUiLanguage(it) }
            }
            PlaybackPositionStore(context).clear()
        }
    }
}
