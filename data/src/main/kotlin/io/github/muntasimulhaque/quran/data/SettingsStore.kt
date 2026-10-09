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
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.core.RepeatPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("reading")

enum class AppTheme { Paper, Sepia, Night, Black }

/**
 * The minute the daily reminder arrives when the reader has never chosen one:
 * eight in the morning, before the day's work starts.
 */
const val DEFAULT_DAILY_MINUTE = 8 * 60

/** The last minute a day has, 23:59. */
const val LAST_MINUTE_OF_DAY = 24 * 60 - 1

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
 * which content packs are on. Page is not stored; the ayah is the place.
 */
data class AppSettings(
    val ayah: Int = 1,
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
    /**
     * The reader's reciter. Husary is the default for a new install (owner
     * decision, 4.5): the murattal voice most readers know, with the quiet
     * second voice kept as the fallback when a reciter is removed.
     */
    val recitation: String = "husary",
    val keepAwake: Boolean = true,
    val followReciter: Boolean = true,
    /** How fast the recitation plays: the reader's own pace, remembered. */
    val playbackSpeed: Float = 1f,
    /**
     * What happens as the recitation being heard moves on: the next ayah,
     * nothing further after this one, the ayah again, the surah again, or the
     * surah after this one.
     *
     * One value and not four switches, because two of them on at once is a
     * promise the player cannot keep: a surah that repeats never ends, so
     * the continuation would never come (owner decision). Continuing to the
     * next ayah is the default; turning it off is the stop, and a download
     * still waits for the reader's word: the continuation is that word,
     * given once for every surah that follows (owner decision).
     */
    val endOfAudio: EndOfAudio = EndOfAudio.CONTINUE_AYAH,
    val translationPacks: Set<String> = emptySet(),
    val tafsirPacks: Set<String> = emptySet(),
    /**
     * Whether the reading draws what the reader has chosen. The packs say
     * what they have; these say what the page shows. Both are on by default:
     * a reader who added a translation or a tafsir means to see it until
     * they say otherwise.
     */
    val showTranslation: Boolean = true,
    val showTafsir: Boolean = true,
    val wordByWord: Boolean = false,
    /**
     * The daily reminder: one ayah, once a day, at the reader's own time.
     * On from the first launch (owner decision, 2.3): the reminder is the
     * quietest thing the app can do, a silent line in the shade with no sound
     * and no counting, and rule twelve names it as one of the three things
     * this app is for. A reader who does not want it turns it off in one tap
     * on the Daily ayah page, and the alarm is cleared in the same tap.
     */
    val dailyAyah: Boolean = true,
    /**
     * The minute of the day the reminder arrives, in the reader's own local
     * time: minutes from midnight, so one whole number carries the hour and
     * the minute and the picker has nothing to reconcile.
     */
    val dailyAyahMinute: Int = DEFAULT_DAILY_MINUTE,
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
 * Reads and writes the reader's choices. It stores an ayah, not a page.
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
            recitation = preferences[RECITATION] ?: "husary",
            keepAwake = preferences[KEEP_AWAKE] ?: true,
            followReciter = preferences[FOLLOW_RECITER] ?: true,
            playbackSpeed = (preferences[PLAYBACK_SPEED] ?: 1f).coerceIn(MIN_SPEED, MAX_SPEED),
            endOfAudio = storedEnd(preferences),
            translationPacks = translationPacks(preferences),
            tafsirPacks = preferences[TAFSIR_PACKS] ?: emptySet(),
            showTranslation = preferences[SHOW_TRANSLATION] ?: true,
            showTafsir = preferences[SHOW_TAFSIR] ?: true,
            wordByWord = preferences[WORD_BY_WORD] ?: false,
            dailyAyah = preferences[DAILY_AYAH] ?: true,
            dailyAyahMinute = storedDailyMinute(preferences),
            longPressHintShown = preferences[HINT_SHOWN] ?: false,
        )
    }

    suspend fun setAyah(ayah: Int) {
        context.settingsStore.edit { it[AYAH] = ayah.coerceIn(1, 6236) }
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

    /**
     * What happens as the reading moves on, written as the keys it has always
     * been written as, in one edit.
     *
     * The keys move together or not at all: a process that died between two
     * writes would leave a reader with the ayah repeating and the next surah
     * being fetched behind it, which is the impossible pair the value above
     * exists to prevent (owner decision).
     */
    suspend fun setEndOfAudio(end: EndOfAudio) {
        val plan = RepeatPlan().with(end)
        context.settingsStore.edit {
            it[CONTINUE_AYAH] = plan.continueAyah
            it[REPEAT_AYAH] = plan.ayah
            it[REPEAT_SURAH] = plan.surah
            it[CONTINUE_SURAH] = plan.next
        }
    }

    suspend fun setShowTranslation(show: Boolean) {
        context.settingsStore.edit { it[SHOW_TRANSLATION] = show }
    }

    suspend fun setShowTafsir(show: Boolean) {
        context.settingsStore.edit { it[SHOW_TAFSIR] = show }
    }

    suspend fun setWordByWord(show: Boolean) {
        context.settingsStore.edit { it[WORD_BY_WORD] = show }
    }

    suspend fun setDailyAyah(enabled: Boolean) {
        context.settingsStore.edit { it[DAILY_AYAH] = enabled }
    }

    /** The moment the daily reminder arrives: minutes from midnight. */
    suspend fun setDailyAyahTime(minuteOfDay: Int) {
        context.settingsStore.edit { it[DAILY_AYAH_MINUTE] = minuteOfDay.coerceIn(0, LAST_MINUTE_OF_DAY) }
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
     * The packs a language choice asked for, kept across a launch: the reader
     * chose a language, so its reading arrives without another question, and a
     * download interrupted by the process or the connection continues on the
     * next attempt instead of being forgotten.
     */
    suspend fun pendingPacks(): Set<String> =
        context.settingsStore.data.first()[PENDING_PACKS] ?: emptySet()

    suspend fun addPendingPacks(packs: Set<String>) {
        if (packs.isEmpty()) return
        context.settingsStore.edit {
            it[PENDING_PACKS] = (it[PENDING_PACKS] ?: emptySet()) + packs
        }
    }

    suspend fun removePendingPacks(packs: Set<String>) {
        if (packs.isEmpty()) return
        context.settingsStore.edit {
            it[PENDING_PACKS] = (it[PENDING_PACKS] ?: emptySet()) - packs
        }
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
     * The reading's chosen answer, as the stored keys stand.
     *
     * A build before the exclusivity let a reader turn two of them
     * on, so the read settles that case the way [RepeatPlan.end] does: the
     * narrower promise is the one kept, because it is the one the reader can
     * still hear working. `continue_ayah` is absent from every install
     * before this one, and an unwritten key reads as on, which is the
     * behavior those installs already had.
     */
    private fun storedEnd(preferences: Preferences): EndOfAudio = RepeatPlan(
        continueAyah = preferences[CONTINUE_AYAH] ?: true,
        ayah = preferences[REPEAT_AYAH] ?: false,
        surah = preferences[REPEAT_SURAH] ?: false,
        next = preferences[CONTINUE_SURAH] ?: false,
    ).end

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

    /**
     * The moment the reminder arrives, read from whichever key this install
     * has: the minute of the day the picker writes, or the hour a build
     * before 2.3 wrote (a reader who chose 5 in that build keeps 5:00).
     */
    private fun storedDailyMinute(preferences: Preferences): Int {
        preferences[DAILY_AYAH_MINUTE]?.let { return it.coerceIn(0, LAST_MINUTE_OF_DAY) }
        preferences[DAILY_AYAH_HOUR]?.let { return (it.coerceIn(0, 23)) * 60 }
        return DEFAULT_DAILY_MINUTE
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
        /**
         * The keys one answer is stored as. `repeat_ayah` and
         * `continue_surah` were written before the answer was one value;
         * `repeat_surah` is the third key that made, added later; and
         * `continue_ayah` is the default's own key, added last. An install
         * that predates any of them reads the missing key as its own
         * default, which costs a reader nothing.
         */
        val CONTINUE_AYAH = booleanPreferencesKey("continue_ayah")
        val REPEAT_AYAH = booleanPreferencesKey("repeat_ayah")
        val REPEAT_SURAH = booleanPreferencesKey("repeat_surah")
        val CONTINUE_SURAH = booleanPreferencesKey("continue_surah")
        val TRANSLATION_PACK = stringPreferencesKey("translation_pack")
        val TRANSLATION_PACKS = stringSetPreferencesKey("translation_packs")
        val TAFSIR_PACKS = stringSetPreferencesKey("tafsir_packs")
        val PENDING_PACKS = stringSetPreferencesKey("pending_packs")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val SHOW_TAFSIR = booleanPreferencesKey("show_tafsir")
        val WORD_BY_WORD = booleanPreferencesKey("word_by_word")
        val DAILY_AYAH = booleanPreferencesKey("daily_ayah")

        /** Written by builds before the picker took minutes; read, never written. */
        val DAILY_AYAH_HOUR = intPreferencesKey("daily_ayah_hour")
        val DAILY_AYAH_MINUTE = intPreferencesKey("daily_ayah_minute")
        val HINT_SHOWN = booleanPreferencesKey("hint_shown")
    }
}

