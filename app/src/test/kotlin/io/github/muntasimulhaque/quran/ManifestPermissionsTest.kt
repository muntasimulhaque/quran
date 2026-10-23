package io.github.muntasimulhaque.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The law names the exact permissions the app may declare: INTERNET
 * for content from the project's own Releases, POST_NOTIFICATIONS,
 * the two foreground service flags for the recitation, the exact
 * alarm for the daily reminder's time, and RECEIVE_BOOT_COMPLETED to
 * re-arm that reminder after a reboot. The source manifest is what
 * this app asks for on its own; a library may merge more in later,
 * and those are documented rather than fought. A dependency that
 * merges a permission changes the set silently, so the set is pinned
 * here, where a change fails the build, and not only in a paragraph.
 */
class ManifestPermissionsTest {

    @Test
    fun theAppDeclaresExactlyTheLawfulPermissions() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val declared = Regex("""<uses-permission\s+android:name="([^"]+)"""")
            .findAll(manifest)
            .map { it.groupValues[1] }
            .toSet()
        assertEquals(
            setOf(
                "android.permission.INTERNET",
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
                "android.permission.SCHEDULE_EXACT_ALARM",
                "android.permission.RECEIVE_BOOT_COMPLETED",
            ),
            declared,
        )
    }

    @Test
    fun theMergedManifestAddsOnlyTheDocumentedLibraryPermissions() {
        val merged = File(
            "build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml",
        )
        assertTrue("the merged manifest was not built before this test", merged.isFile)
        val declared = Regex("""<uses-permission\s+android:name="([^"]+)""")
            .findAll(merged.readText())
            .map { it.groupValues[1] }
            .toSet()
        assertEquals(
            setOf(
                "android.permission.INTERNET",
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
                "android.permission.SCHEDULE_EXACT_ALARM",
                "android.permission.RECEIVE_BOOT_COMPLETED",
                // Media3 and AndroidX merge these in; the privacy policy
                // names them. A new one is a change the law requires the
                // owner to sign off, so it must fail here first.
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.WAKE_LOCK",
                "io.github.muntasimulhaque.quran.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
            ),
            declared,
        )
    }
}
