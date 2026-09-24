package io.github.muntasimulhaque.quran.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("reading")

enum class ReadingMode { Mushaf, Study }

enum class AppTheme { Paper, Sepia, Night, Black }

/** True for the two themes that turn the page over into the dark. */
fun AppTheme.isDark(): Boolean = this == AppTheme.Night || this == AppTheme.Black

/**
 * The page the reader actually reads on, once the system has a say.
 *
 * With automatic night mode off, the choice is the choice. With it on, the
 * system's dark mode selects Night, and a light system day shows the page the
 * reader chose; a reader whose own choice is a night page is shown Paper in
 * the light, because a switch that changed nothing by day would not be a
 * switch at all.
 */
fun AppTheme.resolved(autoNight: Boolean, systemDark: Boolean): AppTheme = when {
    !autoNight -> this
    systemDark -> AppTheme.Night
    isDark() -> AppTheme.Paper
    else -> this
}

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
    /** When on, the page turns over with the system's own day and night. */
    val autoNight: Boolean = false,
    /**
     * The language the interface is written in, and the content it reads.
     * Null until the reader has chosen, which is what shows the welcome
     * screen once, on the first launch and on the update that added it.
     */
    val uiLanguage: String? = null,
    val arabicSize: Float = TextSize.DEFAULT,
    val translationSize: Float = TextSize.DEFAULT,
    val tafsirSize: Float = TextSize.DEFAULT,
    val wordsSize: Float = TextSize.DEFAULT,
    val recitation: String = "minshawi",
    val keepAwake: Boolean = true,
    val followReciter: Boolean = true,
    /** How fast the recitation plays: the reader's own pace, remembered. */
    val playbackSpeed: Float = 1f,
    /** True when one ayah repeats until the reader stops it. */
    val repeatAyah: Boolean = false,
    val translationPacks: Set<String> = emptySet(),
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
    val tafsirArabicSp: Float get() = TextSize.tafsirArabic(tafsirSize)
    val wordsSp: Float get() = TextSize.sp(TypeRole.Words, wordsSize)
    val wordsMeaningSp: Float get() = TextSize.meaningSp(wordsSize)

    fun sizeOf(role: TypeRole): Float = when (role) {
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
        val choices = preferences.asMap()
        val ayah = preferences[AYAH] ?: 1
        // The four sizes arrived after the one size did, so a reader who had
        // chosen one keeps that choice for every kind of text, and a step
        // stored as an index by the build that shipped it keeps its scale.
        val legacy = sizeOf(choices, TEXT_SIZE)
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
            autoNight = preferences[AUTO_NIGHT] ?: false,
            uiLanguage = preferences[UI_LANGUAGE],
            arabicSize = sizeOf(choices, ARABIC_SIZE, legacy),
            translationSize = sizeOf(choices, TRANSLATION_SIZE, legacy),
            tafsirSize = sizeOf(choices, TAFSIR_SIZE, legacy),
            wordsSize = sizeOf(choices, WORDS_SIZE, legacy),
            recitation = preferences[RECITATION] ?: "minshawi",
            keepAwake = preferences[KEEP_AWAKE] ?: true,
            followReciter = preferences[FOLLOW_RECITER] ?: true,
            playbackSpeed = (preferences[PLAYBACK_SPEED] ?: 1f).coerceIn(MIN_SPEED, MAX_SPEED),
            repeatAyah = preferences[REPEAT_AYAH] ?: false,
            translationPacks = translationPacks(preferences),
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

    suspend fun setAutoNight(follow: Boolean) {
        context.settingsStore.edit { it[AUTO_NIGHT] = follow }
    }

    suspend fun setUiLanguage(tag: String) {
        LanguagePreference(context).set(tag)
        context.settingsStore.edit { it[UI_LANGUAGE] = tag }
    }

    /**
     * The language and the content it reads, in one write, so a process that
     * dies mid-choice can never keep a Bangla interface over English
     * defaults: the language and the packs it brings land together or not at
     * all. The boot-time mirror is written first, because the Activity that
     * recreates for the new locale reads it synchronously.
     */
    suspend fun setLanguage(language: UiLanguage, translationPacks: Set<String>, tafsirPacks: Set<String>) {
        LanguagePreference(context).set(language.tag)
        context.settingsStore.edit {
            it[UI_LANGUAGE] = language.tag
            it[TRANSLATION_PACKS] = translationPacks
            it[TAFSIR_PACKS] = tafsirPacks
        }
    }

    suspend fun setTypeSize(role: TypeRole, value: Float) {
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

    suspend fun setPlaybackSpeed(speed: Float) {
        context.settingsStore.edit { it[PLAYBACK_SPEED] = speed.coerceIn(MIN_SPEED, MAX_SPEED) }
    }

    suspend fun setRepeatAyah(repeat: Boolean) {
        context.settingsStore.edit { it[REPEAT_AYAH] = repeat }
    }

    suspend fun setWordByWord(show: Boolean) {
        context.settingsStore.edit { it[WORD_BY_WORD] = show }
    }

    suspend fun setLongPressHintShown() {
        context.settingsStore.edit { it[HINT_SHOWN] = true }
    }

    suspend fun setTranslationPacks(packs: Set<String>) {
        context.settingsStore.edit { it[TRANSLATION_PACKS] = packs }
    }

    suspend fun setTafsirPacks(packs: Set<String>) {
        context.settingsStore.edit { it[TAFSIR_PACKS] = packs }
    }

    /**
     * One stored size as a scale. The preference is read by name off the raw
     * map, not through a typed key, because the build that shipped 0.2 wrote
     * these as step indices: reading an Int through a Float key throws, and a
     * reader who updated the app must never meet a crash for a text size.
     *
     * A stored scale keeps its own meaning (1.6 becomes the largest step of
     * today's list, not the third one); an index is looked up in the list the
     * index was written against.
     */
    private fun sizeOf(
        choices: Map<Preferences.Key<*>, Any?>,
        key: Preferences.Key<Float>,
        legacy: Float? = null,
        fallback: Float = TextSize.DEFAULT,
    ): Float {
        val stored = choices.entries.firstOrNull { it.key.name == key.name }?.value ?: legacy
        return when (stored) {
            is Float -> TextSize.step(stored)
            is Int -> TextSize.step(TextSize.LEGACY_STEPS[stored.coerceIn(0, 4)])
            else -> fallback
        }
    }

    /**
     * The translations the reader has on. It was one pack before a reader
     * could read two at once, so the old single value is read as a set of one
     * and cleared once it has been carried over.
     */
    private fun translationPacks(preferences: Preferences): Set<String> {
        preferences[TRANSLATION_PACKS]?.let { return it }
        val single = preferences[TRANSLATION_PACK]
        return if (single.isNullOrBlank()) emptySet() else setOf(single)
    }

    private fun keyOf(role: TypeRole): Preferences.Key<Float> = when (role) {
        TypeRole.Arabic -> ARABIC_SIZE
        TypeRole.Translation -> TRANSLATION_SIZE
        TypeRole.Tafsir -> TAFSIR_SIZE
        TypeRole.Words -> WORDS_SIZE
    }

    private companion object {
        /** The recitation pace the reader may choose, and its bounds. */
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 1.5f

        val AYAH = intPreferencesKey("ayah")
        val MODE = stringPreferencesKey("mode")
        val THEME = stringPreferencesKey("theme")
        val AUTO_NIGHT = booleanPreferencesKey("auto_night")
        val UI_LANGUAGE = stringPreferencesKey("ui_language")
        val TEXT_SIZE = floatPreferencesKey("text_size")
        val ARABIC_SIZE = floatPreferencesKey("arabic_size")
        val TRANSLATION_SIZE = floatPreferencesKey("translation_size")
        val TAFSIR_SIZE = floatPreferencesKey("tafsir_size")
        val WORDS_SIZE = floatPreferencesKey("words_size")
        val RECITATION = stringPreferencesKey("recitation")
        val KEEP_AWAKE = booleanPreferencesKey("keep_awake")
        val FOLLOW_RECITER = booleanPreferencesKey("follow_reciter")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val REPEAT_AYAH = booleanPreferencesKey("repeat_ayah")
        val TRANSLATION_PACK = stringPreferencesKey("translation_pack")
        val TRANSLATION_PACKS = stringSetPreferencesKey("translation_packs")
        val TAFSIR_PACKS = stringSetPreferencesKey("tafsir_packs")
        val WORD_BY_WORD = booleanPreferencesKey("word_by_word")
        val HINT_SHOWN = booleanPreferencesKey("hint_shown")
    }
}

