package io.github.muntasimulhaque.quran.ui.kit

/** Human sizes for a download the reader is about to approve. */
fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes < 1024 * 1024 -> "${(bytes + 512) / 1024} KB"
    bytes < 10 * 1024 * 1024 -> "%.1f MB".format(bytes / 1048576.0)
    else -> "%.0f MB".format(bytes / 1048576.0)
}

/** The reciters, by the names readers use for them. */
fun shortReciterName(id: String, fallback: String): String = when (id) {
    "minshawi" -> "Minshawi"
    "husary" -> "Husary"
    "husary-muallim" -> "Husary Muallim"
    "husary-mujawwad" -> "Husary Mujawwad"
    else -> fallback
}
