package io.github.muntasimulhaque.quran.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("reading")

enum class ReadingMode { Mushaf, Study }

enum class AppTheme { Paper, Sepia, Night, Black }

/** Text size steps, from the smallest comfortable to the largest readable. */
enum class TextSize(val arabicSp: Int, val latinSp: Int, val arabicLineSp: Int, val latinLineSp: Int) {
    Small(22, 15, 44, 24),
    Normal(26, 17, 52, 27),
    Large(30, 19, 60, 30),
    Largest(34, 22, 68, 35),
    ExtraLarge(40, 25, 80, 40);

    companion object {
        fun of(index: Int): TextSize = entries.getOrElse(index) { Normal }
    }
}

/**
 * Everything the reader has chosen, in one place: where they are, how the
 * page looks, which reciter they hear, and which content packs are on. Page
 * is not stored; the ayah is, so both reading modes share one position.
 */
data class AppSettings(
    val ayah: Int = 1,
    val mode: ReadingMode = ReadingMode.Mushaf,
    val theme: AppTheme = AppTheme.Paper,
    val textSize: TextSize = TextSize.Normal,
    val recitation: String = "minshawi",
    val keepAwake: Boolean = true,
    val followReciter: Boolean = true,
    val showFootnotes: Boolean = false,
    val translationPack: String = "",
    val tafsirPacks: Set<String> = emptySet(),
    /** 0 is off, 1 is dim, 2 is darker. */
    val dimLevel: Int = 0,
    val longPressHintShown: Boolean = false,
)

/**
 * Reads and writes the reader's choices. It stores an ayah, not a page, so
 * switching between the Mushaf and the study view never loses the place; the
 * page is derived from the ayah when it is needed.
 */
class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsStore.data.map { preferences ->
        val ayah = preferences[AYAH] ?: 1
        AppSettings(
            ayah = ayah.coerceIn(1, 6236),
            mode = when (preferences[MODE]) {
                "study" -> ReadingMode.Study
                else -> ReadingMode.Mushaf
            },
            theme = when (preferences[THEME]) {
                "sepia" -> AppTheme.Sepia
                "night" -> AppTheme.Night
                "black" -> AppTheme.Black
                else -> AppTheme.Paper
            },
            textSize = TextSize.of(preferences[TEXT_SIZE] ?: 1),
            recitation = preferences[RECITATION] ?: "minshawi",
            keepAwake = preferences[KEEP_AWAKE] ?: true,
            followReciter = preferences[FOLLOW_RECITER] ?: true,
            showFootnotes = preferences[SHOW_FOOTNOTES] ?: false,
            translationPack = preferences[TRANSLATION_PACK] ?: "",
            tafsirPacks = preferences[TAFSIR_PACKS] ?: emptySet(),
            dimLevel = preferences[DIM_LEVEL] ?: 0,
            longPressHintShown = preferences[HINT_SHOWN] ?: false,
        )
    }

    /** The page to open on when no ayah has ever been stored (an upgrade). */
    val legacyPage: Flow<Int?> = context.settingsStore.data.map { preferences ->
        preferences[LEGACY_PAGE]
    }

    suspend fun setAyah(ayah: Int) {
        context.settingsStore.edit { it[AYAH] = ayah.coerceIn(1, 6236) }
    }

    suspend fun setMode(mode: ReadingMode) {
        context.settingsStore.edit { it[MODE] = if (mode == ReadingMode.Study) "study" else "mushaf" }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.settingsStore.edit {
            it[THEME] = when (theme) {
                AppTheme.Paper -> "paper"
                AppTheme.Sepia -> "sepia"
                AppTheme.Night -> "night"
                AppTheme.Black -> "black"
            }
        }
    }

    suspend fun setTextSize(size: TextSize) {
        context.settingsStore.edit { it[TEXT_SIZE] = size.ordinal }
    }

    suspend fun setRecitation(recitation: String) {
        context.settingsStore.edit { it[RECITATION] = recitation }
    }

    suspend fun setKeepAwake(keep: Boolean) {
        context.settingsStore.edit { it[KEEP_AWAKE] = keep }
    }

    suspend fun setFollowReciter(follow: Boolean) {
        context.settingsStore.edit { it[FOLLOW_RECITER] = follow }
    }

    suspend fun setShowFootnotes(show: Boolean) {
        context.settingsStore.edit { it[SHOW_FOOTNOTES] = show }
    }

    suspend fun setDimLevel(level: Int) {
        context.settingsStore.edit { it[DIM_LEVEL] = level.coerceIn(0, 2) }
    }

    suspend fun setLongPressHintShown() {
        context.settingsStore.edit { it[HINT_SHOWN] = true }
    }

    suspend fun setTranslationPack(pack: String) {
        context.settingsStore.edit { it[TRANSLATION_PACK] = pack }
    }

    suspend fun setTafsirPacks(packs: Set<String>) {
        context.settingsStore.edit { it[TAFSIR_PACKS] = packs }
    }

    private companion object {
        val AYAH = intPreferencesKey("ayah")
        val LEGACY_PAGE = intPreferencesKey("page")
        val MODE = stringPreferencesKey("mode")
        val THEME = stringPreferencesKey("theme")
        val TEXT_SIZE = intPreferencesKey("text_size")
        val RECITATION = stringPreferencesKey("recitation")
        val KEEP_AWAKE = booleanPreferencesKey("keep_awake")
        val FOLLOW_RECITER = booleanPreferencesKey("follow_reciter")
        val SHOW_FOOTNOTES = booleanPreferencesKey("show_footnotes")
        val TRANSLATION_PACK = stringPreferencesKey("translation_pack")
        val TAFSIR_PACKS = stringSetPreferencesKey("tafsir_packs")
        val DIM_LEVEL = intPreferencesKey("dim_level")
        val HINT_SHOWN = booleanPreferencesKey("hint_shown")
    }
}
