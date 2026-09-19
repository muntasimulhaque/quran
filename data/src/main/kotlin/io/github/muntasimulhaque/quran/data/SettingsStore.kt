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

/**
 * Everything the reader has chosen, in one place: where they are, how the
 * page looks, how big each kind of text is, which reciter they hear, and
 * which content packs are on. Page is not stored; the ayah is, so both
 * reading modes share one position.
 */
data class AppSettings(
    val ayah: Int = 1,
    val mode: ReadingMode = ReadingMode.Mushaf,
    val theme: AppTheme = AppTheme.Paper,
    val arabicSize: Int = TextSize.DEFAULT,
    val translationSize: Int = TextSize.DEFAULT,
    val tafsirSize: Int = TextSize.DEFAULT,
    val wordsSize: Int = TextSize.DEFAULT,
    val recitation: String = "minshawi",
    val keepAwake: Boolean = true,
    val followReciter: Boolean = true,
    val showFootnotes: Boolean = false,
    val translationPack: String = "",
    val tafsirPacks: Set<String> = emptySet(),
    val wordByWord: Boolean = false,
    val longPressHintShown: Boolean = false,
) {
    val arabicSp: Float get() = TextSize.sp(TypeRole.Arabic, arabicSize)
    val arabicLineSp: Float get() = TextSize.lineSp(TypeRole.Arabic, arabicSize)
    val translationSp: Float get() = TextSize.sp(TypeRole.Translation, translationSize)
    val translationLineSp: Float get() = TextSize.lineSp(TypeRole.Translation, translationSize)
    val tafsirSp: Float get() = TextSize.sp(TypeRole.Tafsir, tafsirSize)
    val tafsirLineSp: Float get() = TextSize.lineSp(TypeRole.Tafsir, tafsirSize)
    val wordsSp: Float get() = TextSize.sp(TypeRole.Words, wordsSize)

    fun sizeOf(role: TypeRole): Int = when (role) {
        TypeRole.Arabic -> arabicSize
        TypeRole.Translation -> translationSize
        TypeRole.Tafsir -> tafsirSize
        TypeRole.Words -> wordsSize
    }
}

/**
 * Reads and writes the reader's choices. It stores an ayah, not a page, so
 * switching between the Mushaf and the study view never loses the place; the
 * page is derived from the ayah when it is needed.
 */
class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsStore.data.map { preferences ->
        val ayah = preferences[AYAH] ?: 1
        // The four sizes arrived after the one size did, so a reader who had
        // chosen one keeps that choice for every kind of text.
        val legacy = preferences[TEXT_SIZE]
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
            arabicSize = TextSize.step(preferences[ARABIC_SIZE] ?: legacy ?: TextSize.DEFAULT),
            translationSize = TextSize.step(
                preferences[TRANSLATION_SIZE] ?: legacy ?: TextSize.DEFAULT,
            ),
            tafsirSize = TextSize.step(preferences[TAFSIR_SIZE] ?: legacy ?: TextSize.DEFAULT),
            wordsSize = TextSize.step(preferences[WORDS_SIZE] ?: legacy ?: TextSize.DEFAULT),
            recitation = preferences[RECITATION] ?: "minshawi",
            keepAwake = preferences[KEEP_AWAKE] ?: true,
            followReciter = preferences[FOLLOW_RECITER] ?: true,
            showFootnotes = preferences[SHOW_FOOTNOTES] ?: false,
            translationPack = preferences[TRANSLATION_PACK] ?: "",
            tafsirPacks = preferences[TAFSIR_PACKS] ?: emptySet(),
            wordByWord = preferences[WORD_BY_WORD] ?: false,
            longPressHintShown = preferences[HINT_SHOWN] ?: false,
        )
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

    suspend fun setTypeSize(role: TypeRole, step: Int) {
        val value = TextSize.step(step)
        context.settingsStore.edit {
            it[keyOf(role)] = value
            it.remove(TEXT_SIZE)
        }
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

    suspend fun setWordByWord(show: Boolean) {
        context.settingsStore.edit { it[WORD_BY_WORD] = show }
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

    private fun keyOf(role: TypeRole): Preferences.Key<Int> = when (role) {
        TypeRole.Arabic -> ARABIC_SIZE
        TypeRole.Translation -> TRANSLATION_SIZE
        TypeRole.Tafsir -> TAFSIR_SIZE
        TypeRole.Words -> WORDS_SIZE
    }

    private companion object {
        val AYAH = intPreferencesKey("ayah")
        val MODE = stringPreferencesKey("mode")
        val THEME = stringPreferencesKey("theme")
        val TEXT_SIZE = intPreferencesKey("text_size")
        val ARABIC_SIZE = intPreferencesKey("arabic_size")
        val TRANSLATION_SIZE = intPreferencesKey("translation_size")
        val TAFSIR_SIZE = intPreferencesKey("tafsir_size")
        val WORDS_SIZE = intPreferencesKey("words_size")
        val RECITATION = stringPreferencesKey("recitation")
        val KEEP_AWAKE = booleanPreferencesKey("keep_awake")
        val FOLLOW_RECITER = booleanPreferencesKey("follow_reciter")
        val SHOW_FOOTNOTES = booleanPreferencesKey("show_footnotes")
        val TRANSLATION_PACK = stringPreferencesKey("translation_pack")
        val TAFSIR_PACKS = stringSetPreferencesKey("tafsir_packs")
        val WORD_BY_WORD = booleanPreferencesKey("word_by_word")
        val HINT_SHOWN = booleanPreferencesKey("hint_shown")
    }
}
