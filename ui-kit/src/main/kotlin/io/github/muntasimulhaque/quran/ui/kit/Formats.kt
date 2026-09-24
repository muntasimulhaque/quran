package io.github.muntasimulhaque.quran.ui.kit

import java.util.Locale

/**
 * Human sizes for a download the reader is about to approve. The numbers are
 * printed in US English like every other string in the interface, so a device
 * set to a locale with a decimal comma cannot put one inside a sentence the
 * store or a screenshot shows.
 */
fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes < 1024 * 1024 -> "${(bytes + 512) / 1024} KB"
    bytes < 10 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
    else -> String.format(Locale.US, "%.0f MB", bytes / 1048576.0)
}

/** The reciters, by the names readers use for them. */
fun shortReciterName(id: String, fallback: String): String = when (id) {
    "minshawi" -> "Minshawi"
    "husary" -> "Husary"
    "husary-muallim" -> "Husary Muallim"
    "husary-mujawwad" -> "Husary Mujawwad"
    else -> fallback
}

/**
 * A playback pace as the reader reads it: "1x", "0.75x", never a trailing
 * run of digits. It lives here because the settings page that chooses the
 * pace and the pill that reports it must name it the same way; a reader who
 * chose 0.75x in settings and sees 0.7500001x on the pill has been told two
 * different things about one choice.
 */
fun speedText(speed: Float): String =
    if (speed == speed.toInt().toFloat()) "${speed.toInt()}x" else "${speed}x"
