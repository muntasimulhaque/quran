package io.github.muntasimulhaque.quran.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
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
import kotlinx.coroutines.flow.onStart

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "reading",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * The pages the reader can read on. Light and Dark: one warm ground for
 * the day and the dark ground for the night, and System, which follows the
 * phone's own day and night. There is no fourth light page and no pure
 * black: a second light page is a choice a reader cannot articulate against
 * Light, and a page that only differs from Dark by its blackness is one they
 * cannot articulate either. Pure black is the ground the reading science
 * argues against (owner decision: the dark gray of Dark reads for longer
 * without halation, and the OLED power a true black saves over a near-black
 * is a rounding error on a page of text).
 *
 * System is the third value rather than a switch beside the two (owner
 * decision, this session): one choice the whole industry already knows, in
 * place of a swatch and a toggle whose interaction a reader had to work out,
 * and it deletes the state where a Night choice sat under a light system day.
 */
enum class AppTheme { Light, Dark, System }

/**
 * The minute the daily reminder arrives when the reader has never chosen one:
 * eight in the morning, before the day's work starts.
 */
const val DEFAULT_DAILY_MINUTE = 8 * 60

/** The last minute a day has, 23:59. */
const val LAST_MINUTE_OF_DAY = 24 * 60 - 1

/** True for the theme that turns the page over into the dark. */
fun AppTheme.isDark(): Boolean = this == AppTheme.Dark

/**
 * The page the reader actually reads on, once the system has a say.
 *
 * System follows the phone's own dark mode: a dark system is Dark and a
 * light one is Light. Light and Dark are the reader's own and never move,
 * which is the whole of the difference from the switch this replaced (owner
 * decision, this session).
 */
fun AppTheme.resolved(systemDark: Boolean): AppTheme = when (this) {
    AppTheme.System -> if (systemDark) AppTheme.Dark else AppTheme.Light
    else -> this
}

/**
 * Everything the reader has chosen, in one place: where they are, how the
 * page looks, how big each kind of text is, which reciter they hear, and
 * which content packs are on. Page is not stored; the ayah is the place.
 */
data class AppSettings(
    /**
     * Where the reader is: the ayah, and nothing else.
     *
     * The place used to carry how many pixels into the ayah the reader had
     * scrolled, tagged with the width and font scale it was measured in. A
     * pixel is a fact about one layout, and every change the app invites (a
     * rotation, one of the five text sizes, the translation, a new phone)
     * made it meaningless. The app answered a meaningless depth by throwing
     * it away and putting the ayah at the top, which landed on the reader as
     * a jump in the middle of their reading (owner report: the landscape
     * reading stepped ayah by ayah). The place is an ayah now, so there is
     * nothing to expire and nothing to correct. A reader who stops inside a
     * long ayah comes back to the top of that ayah, and everything else the
     * app keeps already speaks in ayahs: the Juz label, the history, the
     * arrivals, the reminders, the links (owner decision, this session).
     */
    val ayah: Int = 1,
    val theme: AppTheme = AppTheme.Light,
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
     * what they have; this says what the page shows. On by default: a
     * reader who added a translation means to see it until they say
     * otherwise.
     *
     * The tafsir has no such switch. It is drawn only where the reader asks
     * for it, from the ayah pill's own Tafsir mark, so the packs are its one
     * setting, the way the reciter's voice is the recitation's (owner
     * decision, this session).
     */
    val showTranslation: Boolean = true,
    val wordByWord: Boolean = false,
    /**
     * Whether the reading draws the QPC tajweed colors. On from the first
     * launch: the rules are the print's own annotation, and the reader can
     * turn them off in one switch (owner decision, this session).
     */
    val tajweed: Boolean = true,
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
    /**
     * The chrome surface the reader has already been shown. Empty on a new
     * install; the value is the surface set that added the reveal. A release
     * that adds a chrome surface changes the reveal key, so the next launch
     * shows the top bar once more without adding a permanent bar.
     */
    val chromeReveal: String = "",
) {
    val arabicSp: Float get() = TextSize.sp(TypeRole.Arabic, arabicSize)
    val arabicLineSp: Float get() = TextSize.lineSp(TypeRole.Arabic, arabicSize)
    /**
     * The Arabic beside prose, in the translation and in the tafsir alike:
     * the one rule [TextSize.inlineArabic] keeps.
     */
    val arabicInlineSp: Float get() = TextSize.inlineArabic(arabicSize)

    /**
     * The prose's step over the Latin base in the reader's script: 17 over
     * 16 for Bangla, 1 for English. Every prose surface reads through the
     * getters below, so the note sheet, the ayah card, the settings sample,
     * and the reading all take the script's size from one rule (owner
     * decision, this session).
     */
    private val prose: Float
        get() = if (UiLanguage.of(uiLanguage) == UiLanguage.Bangla) {
            TextSize.BENGALI_READING_SIZE / TextSize.sp(TypeRole.Translation, TextSize.DEFAULT)
        } else {
            1f
        }

    /** The line the prose takes per em: the script's own, never a borrowed one. */
    private val proseLine: Float
        get() = if (UiLanguage.of(uiLanguage) == UiLanguage.Bangla) {
            TextSize.BENGALI_PROSE_RATIO
        } else {
            TextSize.LATIN_PROSE_RATIO
        }

    val translationSp: Float get() = TextSize.sp(TypeRole.Translation, translationSize) * prose
    val translationLineSp: Float get() = translationSp * proseLine
    val tafsirSp: Float get() = TextSize.sp(TypeRole.Tafsir, tafsirSize) * prose
    val tafsirLineSp: Float get() = tafsirSp * proseLine
    /**
     * The aid's Arabic word stays where the verse is, four points under it: it
     * is the Quran's own word, not a line of prose, so the script's prose step
     * does not reach it (owner decision, this session).
     */
    val wordsSp: Float get() = TextSize.wordSp(wordsSize)
    val wordsLineSp: Float get() = TextSize.lineSp(TypeRole.Words, wordsSize)
    val wordsMeaningSp: Float get() = TextSize.meaningSp(wordsSize) * prose
    val wordsMeaningLineSp: Float get() = wordsMeaningSp * proseLine

    fun sizeOf(role: TypeRole): Float = when (role) {
        TypeRole.Arabic -> arabicSize
        TypeRole.Translation -> translationSize
        TypeRole.Tafsir -> tafsirSize
        TypeRole.Words -> wordsSize
    }
}

/**
 * The sizes and switches the reading draws at, in the reader's own script,
 * taken out of [AppSettings] as one value of their own.
 *
 * The reading hands this to every ayah it draws instead of the whole settings
 * object. A settled scroll writes a new [AppSettings] with the reader's place
 * in it, so the object is unequal to itself on every turn of the wheel, and a
 * parameter that changed invalidates the composable that holds it: handing
 * the whole object to every ayah on the page recomposed every one of them to
 * redraw nothing. A size or a switch the reader actually changed still moves
 * every ayah, because that is what it is for.
 */
data class ReadingMetrics(
    val showTranslation: Boolean,
    val wordByWord: Boolean,
    val tajweed: Boolean,
    /** Whether the reading follows the reciter as it moves from ayah to ayah. */
    val followReciter: Boolean,
    /** The packs the reading draws, which shape every row it loads. */
    val translationPacks: Set<String>,
    val arabicSp: Float,
    val arabicLineSp: Float,
    val arabicInlineSp: Float,
    val translationSp: Float,
    val translationLineSp: Float,
    val wordsSp: Float,
    val wordsLineSp: Float,
    val wordsMeaningSp: Float,
    val wordsMeaningLineSp: Float,
)

/**
 * The reading's own view of the reader's choices, in the script they are
 * drawn in.
 *
 * The sizes are the getters above, which carry the reader's script: a Bangla
 * reading takes the translation, the tafsir, and the meaning at the script's
 * own size and its 1.722 line, while the Arabic does not move, because the
 * Quran's text is data and not a locale. The aid's Arabic word does not move
 * either: it is a copy of the verse's word, four points under it, so a larger
 * gloss can never push it past the verse (owner decision, this session: the
 * metrics were language-blind, and the Bangla reading was drawn at the Latin
 * sizes).
 */
fun AppSettings.readingMetrics(): ReadingMetrics = ReadingMetrics(
    showTranslation = showTranslation,
    wordByWord = wordByWord,
    tajweed = tajweed,
    followReciter = followReciter,
    translationPacks = translationPacks,
    arabicSp = arabicSp,
    arabicLineSp = arabicLineSp,
    arabicInlineSp = arabicInlineSp,
    translationSp = translationSp,
    translationLineSp = translationLineSp,
    wordsSp = wordsSp,
    wordsLineSp = wordsLineSp,
    wordsMeaningSp = wordsMeaningSp,
    wordsMeaningLineSp = wordsMeaningLineSp,
)

/**
 * Reads and writes the reader's choices. It stores an ayah, not a page.
 */
class SettingsStore(private val context: Context) {

    /**
     * The synchronous mirror of the page and its switch, read by a launch
     * before the DataStore has answered. It is written beside every write
     * above, so the two can never drift.
     */
    private val themePreference = ThemePreference(context)

    /**
     * The settings as they stood at launch: the page and its automatic
     * switch, read before any coroutine runs, so the first frame is
     * composed on the page the reader last chose. Everything else stays at
     * its default until the store's own read lands below.
     */
    fun snapshot(): AppSettings = themePreference.snapshot()

    /**
     * The reader's choices, read once the retired tafsir switch has been
     * carried. The migration runs before the first emission, not in a launch
     * of its own, so no reader of this flow can meet the state the switch
     * left behind (owner decision, this session).
     */
    val settings: Flow<AppSettings> = context.settingsStore.data
        .onStart { carryRetiredTafsirSwitch() }
        .map { preferences ->
        val choices = preferences.asMap()
        val ayah = preferences[AYAH] ?: 1
        // The four sizes arrived after the one size did, so a reader who had
        // chosen one keeps that choice for every kind of text, and a step
        // stored as an index by the build that shipped it keeps its scale.
        val legacy = sizeOf(choices, TEXT_SIZE)
        AppSettings(
            ayah = ayah.coerceIn(1, 6236),
            theme = storedTheme(preferences),
            uiLanguage = preferences[UI_LANGUAGE],
            arabicSize = sizeOf(choices, ARABIC_SIZE, legacy),
            translationSize = sizeOf(choices, TRANSLATION_SIZE, legacy),
            tafsirSize = sizeOf(choices, TAFSIR_SIZE, legacy),
            wordsSize = sizeOf(choices, WORDS_SIZE, legacy),
            recitation = preferences[RECITATION] ?: "husary",
            followReciter = preferences[FOLLOW_RECITER] ?: true,
            playbackSpeed = (preferences[PLAYBACK_SPEED] ?: 1f).coerceIn(MIN_SPEED, MAX_SPEED),
            endOfAudio = storedEnd(preferences),
            translationPacks = translationPacks(preferences),
            tafsirPacks = tafsirPacks(preferences),
            showTranslation = preferences[SHOW_TRANSLATION] ?: true,
            wordByWord = preferences[WORD_BY_WORD] ?: false,
            tajweed = preferences[TAJWEED] ?: true,
            dailyAyah = preferences[DAILY_AYAH] ?: true,
            dailyAyahMinute = storedDailyMinute(preferences),
            chromeReveal = preferences[CHROME_REVEAL] ?: "",
        )
    }

    /**
     * Writes the reader's place. It is one number, the ayah, because a place
     * that carried a pixel depth had to be corrected whenever the page changed
     * shape, and the correction landed on the reader as a jump
     * (owner decision, this session).
     */
    suspend fun setAyah(ayah: Int) {
        context.settingsStore.edit { it[AYAH] = ayah.coerceIn(1, 6236) }
    }

    /** Writes the chrome surface the reader has now been shown. */
    suspend fun setChromeReveal(surface: String) {
        context.settingsStore.edit { it[CHROME_REVEAL] = surface }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.settingsStore.edit {
            it[THEME] = when (theme) {
                AppTheme.Light -> "light"
                AppTheme.Dark -> "dark"
                AppTheme.System -> "system"
            }
        }
        themePreference.setTheme(theme)
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

    suspend fun setTajweed(tajweed: Boolean) {
        context.settingsStore.edit { it[TAJWEED] = tajweed }
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

    suspend fun setTranslationPacks(packs: Set<String>) {
        context.settingsStore.edit { it[TRANSLATION_PACKS] = packs }
    }

    suspend fun setTafsirPacks(packs: Set<String>) {
        context.settingsStore.edit { it[TAFSIR_PACKS] = packs }
    }

    /**
     * Carries the retired tafsir switch into the pack choice, once.
     *
     * A reader who turned the switch off said they want no tafsir on the
     * reading; with the switch gone, that answer is the empty pack set, and
     * the pack they chose (and may still read in search) is left installed
     * and simply unchosen. The key is written true in the same edit, so no
     * later read meets the old answer again: this build cannot write it
     * false, so a false value only ever means an install from before (owner
     * decision, this session).
     */
    private suspend fun carryRetiredTafsirSwitch() {
        context.settingsStore.edit { preferences ->
            if (preferences[LEGACY_SHOW_TAFSIR] == false) {
                preferences[TAFSIR_PACKS] = emptySet()
                preferences[LEGACY_SHOW_TAFSIR] = true
            }
        }
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
     * The page the reader reads on, read from whichever shape this install
     * stored. The theme was two grounds and a separate automatic switch; it
     * is now one of three values, System among them. An install that turned
     * the switch on lands on System, the value that does what the switch did;
     * one that left it off keeps the ground it chose, the old Paper as Light
     * and Night as Dark. The removed "sepia" and "black" pages land on the
     * ground that replaced them, as they always have.
     */
    private fun storedTheme(preferences: Preferences): AppTheme {
        val auto = preferences[AUTO_NIGHT] == true
        return when (preferences[THEME]) {
            "light" -> AppTheme.Light
            "dark" -> AppTheme.Dark
            "system" -> AppTheme.System
            "night", "black" -> if (auto) AppTheme.System else AppTheme.Dark
            else -> if (auto) AppTheme.System else AppTheme.Light
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
     * The tafsirs the reader has on. It reads the retired master switch
     * first: a reader who had turned tafsir off lands on no tafsir chosen,
     * and the switch itself is written away in the same breath (see
     * [carryRetiredTafsirSwitch]).
     */
    private fun tafsirPacks(preferences: Preferences): Set<String> =
        if (preferences[LEGACY_SHOW_TAFSIR] == false) {
            emptySet()
        } else {
            preferences[TAFSIR_PACKS] ?: emptySet()
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
        /**
         * The automatic switch the third theme value replaced. Written by no
         * build since System became a value; read only to carry an install
         * that had it on across to System (owner decision, this session).
         */
        val AUTO_NIGHT = booleanPreferencesKey("auto_night")
        val UI_LANGUAGE = stringPreferencesKey("ui_language")
        val TEXT_SIZE = floatPreferencesKey("text_size")
        val ARABIC_SIZE = floatPreferencesKey("arabic_size")
        val TRANSLATION_SIZE = floatPreferencesKey("translation_size")
        val TAFSIR_SIZE = floatPreferencesKey("tafsir_size")
        val WORDS_SIZE = floatPreferencesKey("words_size")
        val RECITATION = stringPreferencesKey("recitation")
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
        /**
         * The tafsir's master switch, which the packs replaced. Written by
         * no build since the switch was removed; read only to carry an
         * install that had it off across to no tafsir chosen, and written
         * true at that one migration (owner decision, this session).
         */
        val LEGACY_SHOW_TAFSIR = booleanPreferencesKey("show_tafsir")
        val WORD_BY_WORD = booleanPreferencesKey("word_by_word")
        val TAJWEED = booleanPreferencesKey("tajweed")
        val DAILY_AYAH = booleanPreferencesKey("daily_ayah")

        /** Written by builds before the picker took minutes; read, never written. */
        val DAILY_AYAH_HOUR = intPreferencesKey("daily_ayah_hour")
        val DAILY_AYAH_MINUTE = intPreferencesKey("daily_ayah_minute")
        val CHROME_REVEAL = stringPreferencesKey("chrome_reveal")
    }
}

