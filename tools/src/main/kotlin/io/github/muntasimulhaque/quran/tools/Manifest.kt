package io.github.muntasimulhaque.quran.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Manifest(
    val contentVersion: String,
    val datasets: List<Dataset>,
)

@Serializable
data class Dataset(
    val id: String,
    val name: String,
    val source: String,
    val url: String,
    val resourceId: String,
    val format: String,
    val license: String,
    val credit: String,
    @SerialName("usedFor") val usedFor: String,
    val path: String,
    val bytes: Long,
    val sha256: String,
    val assetUrl: String? = null,
    /** Listed so the reader knows what to download; not required by the build yet. */
    val pending: Boolean = false,
)

private val json = Json { ignoreUnknownKeys = true }

fun loadManifest(root: File): Manifest =
    json.decodeFromString(File(root, "content/manifest.json").readText())
