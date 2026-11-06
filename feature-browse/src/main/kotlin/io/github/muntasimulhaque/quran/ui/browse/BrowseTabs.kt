package io.github.muntasimulhaque.quran.ui.browse

import io.github.muntasimulhaque.quran.feature.browse.R

/**
 * The four places Browse can show. A tab is a place, not a toggle: its chosen
 * chip wears the solid accent, while a search source's chosen chip wears the
 * accent's tint and a check. Both are the app's one [AppChip] and share its
 * measure, press, and target; only the meaning of "chosen" differs.
 */
internal enum class BrowseTab(val labelRes: Int) {
    Surahs(R.string.browse_tab_surahs),
    Juz(R.string.browse_tab_juz),
    LastRead(R.string.browse_tab_last_read),
    Saved(R.string.browse_tab_saved),
}
