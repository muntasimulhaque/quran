package io.github.muntasimulhaque.quran

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * The word aid, end to end: the reader's language is set, its word list is
 * installed, the switch is on, and a tap on the first Arabic word opens its
 * meaning under the verse.
 *
 * The aid is no longer a field of pairs under every ayah. The page stays the
 * page, and the reader asks for one word at a time (owner decision, this
 * session). The library is assembled before the activity starts, so no
 * network is involved; development builds carry every pack.
 */
@RunWith(AndroidJUnit4::class)
class WordByWordTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    private val library = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val store = PackStore(context)
            store.install("translation-al-bayan-bn")
            store.install("words-bn")
            val settings = SettingsStore(context)
            runBlocking {
                settings.setUiLanguage("bn")
                settings.setTranslationPacks(setOf("translation-al-bayan-bn"))
                settings.setWordByWord(true)
                settings.setAyah(1)
                for (role in TypeRole.entries) settings.setTypeSize(role, TextSize.DEFAULT)
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun tappingAWordOpensItsMeaningInTheReadersLanguage() {
        val page = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 180_000) {
            compose.onAllNodesWithContentDescription(page)
                .fetchSemanticsNodes().isNotEmpty()
        }
        // The first word of Al-Fatihah 1:1, in the print's own spelling. The
        // line is right-aligned, so the first word stands at its right edge.
        val line = compose.onAllNodesWithText(
            "بِسۡمِ",
            substring = true,
            useUnmergedTree = true,
        ).onFirst()
        line.performTouchInput {
            click(Offset(width - 32f, height / 2f))
        }
        // The first Bangla meaning of the word stands under the verse.
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithText("নামে", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
