package io.github.muntasimulhaque.quran

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput

/**
 * A tap on the study page's own paper, above the first ayah.
 *
 * The page's exact center is not a safe tap. A translation's footnote marker
 * is a door, and it sits in the flow of the text: at the center of Al-Fatihah
 * 1:1 there is a marker, so a test that taps `center` opens the footnote
 * sheet instead of the chrome and every wait after it fails. That is what
 * made the suite look broken when it was the tap that was unlucky.
 *
 * The paper's own edge is the reading's quiet margin: the study list keeps
 * 20 dp of side padding on the narrowest phone, and no ayah, marker, or
 * control reaches into it. Tapping the page's exact center is not safe: a
 * translation's footnote marker sits in the flow of the text, and at the
 * center of Al-Fatihah 1:1 one of them is under the finger, so the tap opens
 * the footnote sheet instead of the chrome (found by this suite, D-097).
 */
fun SemanticsNodeInteraction.tapThePaper() = performTouchInput {
    click(Offset(4f, centerY))
}
