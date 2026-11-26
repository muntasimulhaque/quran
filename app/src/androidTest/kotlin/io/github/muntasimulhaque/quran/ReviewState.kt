package io.github.muntasimulhaque.quran

import android.content.Context
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TypeRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * The phone a capture class found, kept so the class can hand it back whole.
 *
 * A capture is evidence, and evidence that depends on what the class before it
 * left behind is not evidence. This helper records the reader's settings and
 * the packs on the device, then restores both after the class has finished.
 * The restore is deliberately additive for packs: a pack the class added is
 * removed, and a pack the phone had before is put back from the build's own
 * assets. The reader's saved ayahs, notes, and reading place are untouched.
 */
internal class ReviewState(context: Context) {

    private val appContext = context.applicationContext
    private val settings = SettingsStore(appContext)
    private val packs = PackStore(appContext)
    private var original: AppSettings? = null
    private var originalPacks: Set<String> = emptySet()

    /** Records the phone as it stands. Call once, before the class writes. */
    fun save() {
        original = runBlocking { settings.settings.first() }
        originalPacks = packs.installed()
    }

    /** Installs every pack a capture needs, from the build's own assets. */
    fun install(vararg ids: String) {
        for (id in ids) {
            check(packs.install(id)) { "the build does not carry the pack $id" }
        }
    }

    /** Puts the settings and the pack set back, whatever the class did. */
    fun restore() {
        val before = original ?: return
        runBlocking {
            settings.apply {
                setUiLanguage(before.uiLanguage ?: "en")
                setAyah(before.ayah)
                setTheme(before.theme)
                setTranslationPacks(before.translationPacks)
                setTafsirPacks(before.tafsirPacks)
                setShowTranslation(before.showTranslation)
                setWordByWord(before.wordByWord)
                setTajweed(before.tajweed)
                setRecitation(before.recitation)
                setFollowReciter(before.followReciter)
                setPlaybackSpeed(before.playbackSpeed)
                setEndOfAudio(before.endOfAudio)
                setDailyAyah(before.dailyAyah)
                setDailyAyahTime(before.dailyAyahMinute)
                setReminderAnswered(before.reminderAnswered)
                setReminderDismissedSurah(before.reminderDismissedSurah)
                for (role in TypeRole.entries) {
                    setTypeSize(role, before.sizeOf(role))
                }
            }
        }
        val current = packs.installed()
        current.filterNot { it in originalPacks }.forEach { packs.remove(it) }
        originalPacks.filterNot { it in current }.forEach { id ->
            check(packs.install(id)) { "the phone's pack $id could not be put back" }
        }
        original = null
    }
}
