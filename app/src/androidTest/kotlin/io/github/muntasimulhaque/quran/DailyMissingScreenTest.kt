package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.ui.settings.DailyPage
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The phone with no settings screen is told so, and is offered no door.
 *
 * The Daily page already carries the one line that says where the phone
 * stands, and the one door the phone offers. A phone whose settings app
 * answers nothing is the same shape of fact, except that there is no door to
 * draw, so the line stays and the word goes (owner decision, 19.3).
 * The sentence used to be a Toast, which no tap can put away and which no
 * screen reader can act on.
 */
@RunWith(AndroidJUnit4::class)
class DailyMissingScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun theDailyPage(noNotificationScreen: Boolean) {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                Box(Modifier.fillMaxSize()) {
                    DailyPage(
                        settings = AppSettings(dailyAyah = true),
                        notificationsBlocked = true,
                        notificationScreenMissing = noNotificationScreen,
                        onDailyAyah = {},
                        onDailyAyahTime = {},
                        onOpenNotificationSettings = {},
                    )
                }
            }
        }
    }

    @Test
    fun thePhoneWithoutTheScreenSaysSoAndOffersNoDoor() {
        theDailyPage(noNotificationScreen = true)
        compose.onNodeWithText(
            "Your phone is not showing notifications for this app, and this phone has no screen " +
                "to change that, so the daily ayah cannot arrive.",
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Notification settings").assertDoesNotExist()
    }

    @Test
    fun thePhoneWithTheScreenKeepsItsDoor() {
        theDailyPage(noNotificationScreen = false)
        compose.onNodeWithText("Notification settings").performScrollTo().assertIsDisplayed()
    }
}
