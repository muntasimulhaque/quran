package io.github.muntasimulhaque.quran.data

/**
 * The interface language, when two doors can set it.
 *
 * The app has always had one door: Settings, whose row is the reader's word.
 * Android 13 grew a second one, the per-app language row in the system's own
 * settings, and it exists only for an app that declares `android:localeConfig`
 * (owner decision, 21.3). Two doors are only acceptable while they are two
 * views of one choice, so the precedence has to be a rule and not a habit:
 *
 * 1. **The platform's per-app locales.** This is the door the reader touched
 *    last whenever they used the system's own screen, and Android recreates
 *    the window the moment it changes. An entry the app carries no strings for
 *    is skipped rather than obeyed, so the window can never come up in a
 *    language that does not exist.
 * 2. **The mirror the app's Settings row writes.** On a phone that never used
 *    the system door this is the whole answer, and it is written
 *    synchronously, so a launch can read it before any coroutine runs.
 * 3. **The phone's own language.** A fresh install follows it silently, which
 *    is the app's oldest promise about language.
 * 4. **English**, the fallback the law names.
 *
 * [applicationLocales] arrives as language tags, in the platform's own order.
 * A region tag the app carries the language of (`bn-BD` for `bn`) is that
 * language, because a reader who speaks Bengali from Bangladesh is reading
 * Bangla.
 */
fun chosenLanguageTag(
    applicationLocales: List<String>,
    storedTag: String?,
    phoneTag: String?,
): String {
    applicationLocales.firstNotNullOfOrNull { languageOfTag(it) }?.let { return it.tag }
    languageOfTag(storedTag)?.let { return it.tag }
    return UiLanguage.ofSystem(phoneTag).tag
}

/**
 * Whether the reader's own Settings choice has to be handed to the platform,
 * and with which tag: the per-app locale is the language's own tag, so the
 * system row and the app's row never disagree after this write.
 *
 * It is separate from the write itself because the write needs an Android
 * context and this rule does not: the JVM suite holds the answer, and the
 * window does the calling (owner decision, 21.3).
 */
fun mirroredApplicationLocale(language: UiLanguage): String = language.tag

/**
 * The language a platform tag names, when the tag is one the app carries its
 * strings for. `bn-BD` and `bn` are one language; `fr` is none.
 */
private fun languageOfTag(tag: String?): UiLanguage? =
    UiLanguage.of(tag?.substringBefore('-')?.trim())
