package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The edge a control wears when its outline has to be seen.
 *
 * A hairline is a divider and may sit a shade off its ground. A control's
 * boundary is not a divider: it is the thing that says a swatch is a swatch,
 * and it owes the eye the same 3:1 non-text contrast WCAG asks of every
 * control boundary (owner decision, the consistency audit).
 *
 * The tone is the theme's own ink at [Control]. It is not another grey: the
 * ink already turns over with the ground, and half of it measures 3.03:1 on
 * the paper page, 3.06:1 on a sheet, 4.30:1 on the night page, and 4.15:1 on
 * a night sheet. A palette edit that sinks any of those pairs fails
 * `ContrastTest`.
 */
object Edge {
    /** The share of the theme's ink a control boundary keeps. */
    const val Control = 0.5f
}

/** The visible boundary of a control on the current ground. */
@Composable
fun controlEdge(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = Edge.Control)
