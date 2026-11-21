package io.github.muntasimulhaque.quran.ui

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import io.github.muntasimulhaque.quran.data.UiLanguage

/**
 * The platform's half of the language choice: the per-app locale list, which
 * is the door Android 13 puts in the system's own settings for every app that
 * declares one.
 *
 * The app's own Settings row and that system row are two views of one choice.
 * The rule that picks the answer when both have one lives in
 * `:data`'s `chosenLanguageTag`, where a JVM test holds it; this file is only
 * the two calls that read and write the platform's copy
 * (owner decision, 21.3).
 *
 * Below Android 13 there is no per-app list: the app's own mirror is the
 * whole answer and the Settings row recreates the window as it always did.
 */

/**
 * The tags the platform is holding for this app, in its own order, and an
 * empty list on a phone that has no per-app language screen.
 */
fun applicationLocaleTags(context: Context): List<String> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return emptyList()
    val manager = context.getSystemService(LocaleManager::class.java) ?: return emptyList()
    return manager.applicationLocales
        .toLanguageTags()
        .split(',')
        .filter { it.isNotBlank() }
}

/**
 * Hands the reader's choice to the platform, so the system's own row for this
 * app reads the same language as the one the reader just picked.
 *
 * Returns whether the platform now owns the change. From Android 13 the
 * platform recreates the window itself when its locale list changes, so a
 * second `recreate()` would rebuild the page twice for one tap; the caller
 * therefore recreates only when this answers false, which is every release
 * before Android 13 (owner decision, 21.3).
 */
fun handLanguageToSystem(context: Context, language: UiLanguage): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val manager = context.getSystemService(LocaleManager::class.java) ?: return false
    return try {
        manager.applicationLocales = LocaleList.forLanguageTags(language.tag)
        true
    } catch (unsupported: IllegalArgumentException) {
        // A phone whose platform refuses the list keeps the app's own
        // mirror, which is the language the window already speaks, and the
        // caller recreates the page the way it did before this door existed.
        false
    }
}
