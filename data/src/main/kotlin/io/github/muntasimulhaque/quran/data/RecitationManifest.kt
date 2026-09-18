package io.github.muntasimulhaque.quran.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class RecitationPackage(
    val recitation: String,
    val surah: Int,
    val url: String,
    val bytes: Long,
    val sha256: String,
)

/**
 * The published recitation packages, as the content build recorded them: one
 * ZIP per reciter per surah, with its size and SHA-256. The app trusts this
 * manifest and nothing else: a downloaded package is only unpacked after its
 * hash matches, so a corrupted or tampered file cannot reach the player.
 */
class RecitationManifest private constructor(
    private val packages: Map<Pair<String, Int>, RecitationPackage>,
) {

    fun packageFor(recitation: String, surah: Int): RecitationPackage? =
        packages[recitation to surah]

    private fun surahsOf(recitation: String): Set<Int> =
        packages.keys.filter { it.first == recitation }.map { it.second }.toSet()

    fun surahs(recitation: String): Set<Int> = surahsOf(recitation)

    /** The reciters whose packages are actually published. */
    fun publishedRecitations(): Set<String> = packages.keys.map { it.first }.toSet()

    companion object {
        fun load(context: Context): RecitationManifest {
            val json = runCatching {
                context.assets.open("recitations/manifest.json").bufferedReader().use { it.readText() }
            }.getOrNull() ?: return RecitationManifest(emptyMap())
            return parse(json)
        }

        fun parse(json: String): RecitationManifest {
            val out = HashMap<Pair<String, Int>, RecitationPackage>()
            val root = runCatching { JSONObject(json) }.getOrNull() ?: return RecitationManifest(emptyMap())
            val baseUrl = root.optString("baseUrl")
            val recitations = root.optJSONArray("recitations") ?: JSONArray()
            for (index in 0 until recitations.length()) {
                val recitation = recitations.optJSONObject(index) ?: continue
                val id = recitation.optString("id")
                val tag = recitation.optString("tag")
                val surahs = recitation.optJSONArray("surahs") ?: JSONArray()
                for (surahIndex in 0 until surahs.length()) {
                    val surah = surahs.optJSONObject(surahIndex) ?: continue
                    val number = surah.optInt("surah")
                    val asset = surah.optString("asset")
                    val bytes = surah.optLong("bytes")
                    val sha = surah.optString("sha256")
                    if (id.isEmpty() || tag.isEmpty() || asset.isEmpty() || number <= 0 || sha.isEmpty()) continue
                    out[id to number] = RecitationPackage(
                        recitation = id,
                        surah = number,
                        url = "$baseUrl/$tag/$asset",
                        bytes = bytes,
                        sha256 = sha,
                    )
                }
            }
            return RecitationManifest(out)
        }
    }
}
