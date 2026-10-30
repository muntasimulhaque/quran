package io.github.muntasimulhaque.quran.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A variable family must set the weight it declares.
 *
 * Compose uses a [androidx.compose.ui.text.font.FontWeight] to pick a member
 * of a family, but a variable file still draws its default instance (400)
 * unless that member's `wght` axis is set. The Bangla app shipped with a
 * family that declared Medium and SemiBold on one file and set neither, and
 * because every style also turns font synthesis off, every Bangla title, row
 * name, and bold run was drawn at Regular. Nothing failed: the family looked
 * correct in the source and the screen was quietly flat (owner report, this
 * session).
 *
 * The families are read from the theme's own source because the drawn weight
 * is a platform property, not a value a JVM test can measure without a
 * device. The guard is narrow on purpose: it fails the day a variable family
 * goes back to a raw Font member, which is the day the axis goes back to its
 * default.
 */
class VariableWeightTest {

    @Test
    fun everyVariableFamilyBuildsItsMembersWithTheWeightAxis() {
        val type = themeSource().readText()
        for (family in FAMILIES) {
            val start = type.indexOf("val $family = FontFamily(")
            assertTrue("the $family family was not found in Type.kt", start >= 0)
            val end = type.indexOf("\n)\n", start)
            assertTrue("the $family family has no closing line", end > start)
            val block = type.substring(start, end)
            assertTrue(
                "$family must build its members with variableCut, so the declared weight reaches the axis",
                block.contains("variableCut("),
            )
            assertFalse(
                "$family must not declare a raw Font member, which draws the file's default instance",
                block.contains("Font(R.font."),
            )
        }
    }

    private fun themeSource(): File {
        val working = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(working, SOURCE),
            File(working.parentFile ?: working, "ui-kit/$SOURCE"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Type.kt was not found from $working")
    }

    private companion object {
        const val SOURCE =
            "src/main/kotlin/io/github/muntasimulhaque/quran/ui/theme/Type.kt"

        /** The four families drawn from variable files. */
        val FAMILIES = listOf("Inter", "NotoSansBengali", "NotoSerifBengali", "Literata")
    }
}
