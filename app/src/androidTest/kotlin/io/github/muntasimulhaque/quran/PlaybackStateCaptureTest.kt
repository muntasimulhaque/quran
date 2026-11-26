package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.playback.ListenOffer
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.playback.PlaybackBar
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The pill's four states and the reader's largest text, photographed from
 * fixtures rather than from a network transfer.
 *
 * The offer, the transfer, the failure, and the 200 percent reading are real
 * compositions of the shipped bar. A device test that waited for a real
 * download would either need the network or would photograph whichever
 * percent the emulator reached. These frames are review evidence, not store
 * frames, and they write only through the runner's own output directory
 * (owner decision, 23.0).
 */
@RunWith(AndroidJUnit4::class)
class PlaybackStateCaptureTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun captureTheOffer() {
        show {
            Pill(
                offer = ListenOffer(
                    reciter = "husary",
                    reciterName = "Husary",
                    surah = 60,
                    surahName = "Al-Mumtahanah",
                    ayah = 1,
                    bytes = 9_395_684L,
                    options = options(),
                ),
                offerPrice = "9.0 MB",
            )
        }
        capture("review-listen-offer")
    }

    @Test
    fun captureTheTransfer() {
        show {
            Pill(
                state = PlaybackUiState(
                    connected = true,
                    pendingDownloadSurah = 2,
                    pendingDownloadBytes = 186L * 1024 * 1024,
                    downloadProgress = 0.42f,
                ),
                pendingLabel = "Continue to Al-Baqarah · 186 MB",
                pendingAudio = "Al-Baqarah · 186 MB",
            )
        }
        capture("review-listen-progress")
    }

    @Test
    fun captureTheFailure() {
        show {
            Pill(
                offer = ListenOffer(
                    reciter = "husary",
                    reciterName = "Husary",
                    surah = 2,
                    surahName = "Al-Baqarah",
                    ayah = 1,
                    bytes = 186L * 1024 * 1024,
                    options = options(),
                    failed = true,
                ),
                offerPrice = "186 MB",
            )
        }
        capture("review-listen-failure")
    }

    @Test
    fun captureTheLargeEnglishReading() {
        show(language = UiLanguage.English, largeText = true) {
            Pill(
                state = playing(
                    reference = "60:13",
                    placeName = "Al-Mumtahanah",
                    ayahInSurah = 13,
                ),
                reference = "60:13",
                placeName = "Al-Mumtahanah",
            )
        }
        capture("review-playback-large-en")
    }

    @Test
    fun captureTheLargeBanglaReading() {
        show(language = UiLanguage.Bangla, largeText = true) {
            Pill(
                state = playing(
                    reference = "83:36",
                    placeName = "আল-মুতাফফিফীন",
                    ayahInSurah = 36,
                ),
                reference = "83:36",
                placeName = "আল-মুতাফফিফীন",
            )
        }
        capture("review-playback-large-bn")
    }

    private fun playing(reference: String, placeName: String, ayahInSurah: Int) =
        PlaybackUiState(
            connected = true,
            isPlaying = true,
            recitation = "husary",
            ayahNumber = 5_000,
            surah = reference.substringBefore(':').toInt(),
            ayahInSurah = ayahInSurah,
            reference = reference,
            placeName = placeName,
        )

    private fun options(): List<ListenOption> = listOf(
        ListenOption("husary", "Husary", 9_395_684L),
        ListenOption("minshawi", "Minshawi", 177_000_000L),
    )

    private fun show(
        language: UiLanguage = UiLanguage.English,
        largeText: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = language) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, if (largeText) 2f else 1f),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(Modifier.width(360.dp)) { content() }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Composable
    private fun Pill(
        state: PlaybackUiState = PlaybackUiState(),
        offer: ListenOffer? = null,
        offerPrice: String = "",
        reference: String? = null,
        placeName: String? = null,
        pendingLabel: String? = null,
        pendingAudio: String? = null,
    ) {
        PlaybackBar(
            state = state,
            offer = offer,
            offerPrice = offerPrice,
            reciterName = "Husary",
            reciterId = "husary",
            reciterOptions = options(),
            reference = reference,
            placeName = placeName,
            ayahInSurah = state.ayahInSurah,
            pendingLabel = pendingLabel,
            pendingAudio = pendingAudio,
            onToggle = {},
            onNext = {},
            onPrevious = {},
            onReciter = {},
            onDownload = {},
            onStop = {},
            onCancelDownload = {},
        )
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = screenshotOutputDirectory()
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
