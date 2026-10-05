package io.github.muntasimulhaque.quran

import org.junit.Assert.assertEquals
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
}
