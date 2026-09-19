package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.uikit.R

/** Every glyph the interface needs, drawn by hand so nothing is borrowed. */
enum class Icon {
    Browse,
    Search,
    Settings,
    Bookmark,
    BookmarkFilled,
    Play,
    Pause,
    Next,
    Previous,
    Close,
    MushafPage,
    StudyPage,
    Share,
    More,
    Chevron,
}

@Composable
fun IconGlyph(
    icon: Icon,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (icon) {
            Icon.Browse -> {
                val rows = listOf(0.30f, 0.5f, 0.70f)
                for (y in rows) {
                    drawCircle(tint, radius = w * 0.045f, center = Offset(w * 0.2f, h * y))
                    drawLine(
                        tint,
                        Offset(w * 0.36f, h * y),
                        Offset(w * 0.82f, h * y),
                        strokeWidth = w * 0.075f,
                    )
                }
            }
            Icon.Search -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.26f,
                    center = Offset(w * 0.44f, h * 0.44f),
                    style = Stroke(width = w * 0.085f),
                )
                drawLine(
                    tint,
                    Offset(w * 0.63f, h * 0.63f),
                    Offset(w * 0.84f, h * 0.84f),
                    strokeWidth = w * 0.09f,
                )
            }
            Icon.Settings -> {
                for (y in listOf(0.32f, 0.68f)) {
                    drawLine(tint, Offset(w * 0.18f, h * y), Offset(w * 0.82f, h * y), w * 0.075f)
                }
                drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.36f, h * 0.32f))
                drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.66f, h * 0.68f))
            }
            Icon.Bookmark, Icon.BookmarkFilled -> {
                val path = Path().apply {
                    moveTo(w * 0.28f, h * 0.18f)
                    lineTo(w * 0.72f, h * 0.18f)
                    lineTo(w * 0.72f, h * 0.84f)
                    lineTo(w * 0.5f, h * 0.68f)
                    lineTo(w * 0.28f, h * 0.84f)
                    close()
                }
                if (icon == Icon.BookmarkFilled) {
                    drawPath(path, tint)
                } else {
                    drawPath(path, tint, style = Stroke(width = w * 0.08f))
                }
            }
            Icon.Play -> drawPath(playPath(w, h), tint)
            Icon.Pause -> {
                drawRoundRect(tint, Offset(w * 0.28f, h * 0.2f), Size(w * 0.16f, h * 0.6f), CornerRadius(w * 0.05f))
                drawRoundRect(tint, Offset(w * 0.56f, h * 0.2f), Size(w * 0.16f, h * 0.6f), CornerRadius(w * 0.05f))
            }
            Icon.Next -> {
                drawPath(playPath(w * 0.62f, h * 0.72f, offset = Offset(w * 0.1f, h * 0.14f)), tint)
                drawRoundRect(tint, Offset(w * 0.74f, h * 0.16f), Size(w * 0.11f, h * 0.68f), CornerRadius(w * 0.04f))
            }
            Icon.Previous -> {
                drawPath(playPath(w * 0.62f, h * 0.72f, offset = Offset(w * 0.28f, h * 0.14f), flip = true), tint)
                drawRoundRect(tint, Offset(w * 0.15f, h * 0.16f), Size(w * 0.11f, h * 0.68f), CornerRadius(w * 0.04f))
            }
            Icon.Close -> {
                drawLine(tint, Offset(w * 0.26f, h * 0.26f), Offset(w * 0.74f, h * 0.74f), w * 0.09f)
                drawLine(tint, Offset(w * 0.74f, h * 0.26f), Offset(w * 0.26f, h * 0.74f), w * 0.09f)
            }
            // The printed page: a leaf of paper with the Mushaf's own ruled
            // lines on it, drawn a little wider than tall, as a page is.
            Icon.MushafPage -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.2f, h * 0.13f),
                    size = Size(w * 0.6f, h * 0.74f),
                    cornerRadius = CornerRadius(w * 0.07f),
                    style = Stroke(w * 0.075f),
                )
                for (index in 0 until 5) {
                    val y = h * (0.29f + index * 0.13f)
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.31f, y),
                        end = Offset(w * (if (index == 4) 0.51f else 0.69f), y),
                        strokeWidth = w * 0.055f,
                    )
                }
            }
            // The study page: every ayah with its reading set underneath it,
            // which is exactly what the ayah view shows.
            Icon.StudyPage -> {
                for (index in 0 until 3) {
                    val top = h * (0.14f + index * 0.29f)
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(w * 0.16f, top),
                        size = Size(w * 0.68f, h * 0.2f),
                        cornerRadius = CornerRadius(w * 0.05f),
                        style = Stroke(w * 0.065f),
                    )
                    drawLine(
                        color = tint.copy(alpha = 0.55f),
                        start = Offset(w * 0.28f, top + h * 0.11f),
                        end = Offset(w * 0.72f, top + h * 0.11f),
                        strokeWidth = w * 0.05f,
                    )
                }
            }
            Icon.Share -> {
                val top = Offset(w * 0.62f, h * 0.22f)
                val left = Offset(w * 0.28f, h * 0.5f)
                val bottom = Offset(w * 0.62f, h * 0.78f)
                drawLine(tint, left, top, w * 0.06f)
                drawLine(tint, left, bottom, w * 0.06f)
                drawCircle(tint, radius = w * 0.11f, center = top, style = Stroke(w * 0.07f))
                drawCircle(tint, radius = w * 0.11f, center = bottom, style = Stroke(w * 0.07f))
                drawCircle(tint, radius = w * 0.09f, center = left)
            }
            Icon.More -> {
                for (x in listOf(0.28f, 0.5f, 0.72f)) {
                    drawCircle(tint, radius = w * 0.07f, center = Offset(w * x, h * 0.5f))
                }
            }
            Icon.Chevron -> {
                drawLine(tint, Offset(w * 0.28f, h * 0.4f), Offset(w * 0.5f, h * 0.62f), w * 0.085f)
                drawLine(tint, Offset(w * 0.5f, h * 0.62f), Offset(w * 0.72f, h * 0.4f), w * 0.085f)
            }
        }
    }
}

private fun playPath(w: Float, h: Float, offset: Offset = Offset.Zero, flip: Boolean = false): Path =
    Path().apply {
        val x0 = if (flip) 0.78f else 0.22f
        val x1 = if (flip) 0.22f else 0.78f
        moveTo(offset.x + w * x0, offset.y + h * 0.1f)
        lineTo(offset.x + w * x1, offset.y + h * 0.5f)
        lineTo(offset.x + w * x0, offset.y + h * 0.9f)
        close()
    }

/**
 * An icon button: 48 dp of touch, 22 dp of ink, one spoken label. The touch
 * target is the smallest the guidelines allow, never the size of the icon.
 */
@Composable
fun IconButton(
    icon: Icon,
    description: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onBackground,
    active: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        IconGlyph(
            icon = icon,
            tint = if (active) MaterialTheme.colorScheme.primary else tint.copy(alpha = 0.82f),
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * One quiet text action with an icon above it, for the ayah bar.
 */
@Composable
fun TextAction(label: String, icon: Icon, onClick: () -> Unit, active: Boolean = false) {
    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconGlyph(
            icon = icon,
            tint = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f)
            },
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** The surah and its part, as a quiet line of type. */
@Composable
fun ReadingTitle(surah: String, detail: String?, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = surah,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (!detail.isNullOrBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 1.dp),
            )
        }
    }
}
