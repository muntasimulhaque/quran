package io.github.muntasimulhaque.quran.tools

import io.github.muntasimulhaque.quran.tools.StoreCardSheet.StoreCardLayout
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.FontFormatException
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.LineBreakMeasurer
import java.awt.font.TextAttribute
import java.awt.font.TextLayout
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.text.AttributedString
import kotlin.math.roundToInt

/** The app's own two faces, loaded once and handed to every card. */
class CardTypefaces(val display: Font, val label: Font)

/**
 * Draws one card.
 *
 * The type is wrapped once and every line is drawn as a [TextLayout] made in
 * the same [context] the test measures it in, so what a test proves is what
 * the card shows and no second line-breaking rule can exist beside the first.
 * Everything is drawn in the app's own colors, on an opaque ground, with
 * antialiasing, fractional metrics, and bicubic interpolation: the capture is
 * the only photograph on the card, and it is resampled once.
 */
object StoreCardPaint {

    /**
     * The one measuring context. Antialiasing and fractional metrics are on,
     * which is what the output turns on, so a line's width is the width it is
     * drawn at (owner decision, 22.4).
     */
    val context = FontRenderContext(null, true, true)

    /** The app's display and label faces, at the paths the app ships them. */
    fun typefaces(root: File): CardTypefaces = CardTypefaces(
        display = load(root, "content-assets/src/main/res/font/literata_variable.ttf"),
        label = load(root, "content-assets/src/main/res/font/inter_variable.ttf"),
    )

    private fun load(root: File, relative: String): Font {
        val file = File(root, relative)
        if (!file.isFile) throw IllegalStateException("the typeface is missing: ${file.path}")
        return try {
            Font.createFont(Font.TRUETYPE_FONT, file)
        } catch (error: FontFormatException) {
            throw IllegalStateException("the typeface is not readable: ${file.path}", error)
        } catch (error: IOException) {
            throw IllegalStateException("the typeface could not be read: ${file.path}", error)
        }
    }

    /**
     * [text] broken into the lines it makes in [width], in [font]. The
     * measure is the drawn one: a test that counts lines counts the card.
     */
    fun wrap(text: String, font: Font, width: Double): List<String> {
        if (text.isBlank()) return emptyList()
        val attributed = AttributedString(text)
        attributed.addAttribute(TextAttribute.FONT, font)
        val measurer = LineBreakMeasurer(attributed.iterator, context)
        val lines = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val before = measurer.position
            measurer.nextLayout(width.toFloat()) ?: break
            val end = measurer.position
            if (end <= before) break
            lines += text.substring(start, end).trimEnd()
            start = end
        }
        return lines
    }

    /** The capture fitted into its slot, its aspect kept, centered. */
    fun fitted(slot: Rectangle, captureWidth: Int, captureHeight: Int): Rectangle {
        require(captureWidth > 0 && captureHeight > 0) {
            "a capture has a size: ${captureWidth}x$captureHeight"
        }
        val scale = minOf(
            slot.width.toDouble() / captureWidth,
            slot.height.toDouble() / captureHeight,
        )
        val width = (captureWidth * scale).roundToInt().coerceAtMost(slot.width)
        val height = (captureHeight * scale).roundToInt().coerceAtMost(slot.height)
        return Rectangle(
            slot.x + (slot.width - width) / 2,
            slot.y + (slot.height - height) / 2,
            width,
            height,
        )
    }

    /**
     * The outer edge of the drawn outline: the fitted capture grown by the
     * pad and the stroke. A test keeps this inside the canvas, because an
     * outline that leaves the glass is a card Play crops.
     */
    fun outlineBounds(box: Rectangle, layout: StoreCardLayout): Rectangle2D.Double {
        val grow = layout.pad.toDouble() + layout.stroke
        return Rectangle2D.Double(
            box.x - grow,
            box.y - grow,
            box.width + 2 * grow,
            box.height + 2 * grow,
        )
    }

    /** The line pitch the sub-line is set on, one step off its size. */
    fun labelLeading(size: Int): Int = (size * 1.3).roundToInt()

    /** One card, at its form's own canvas, with no alpha channel. */
    fun paint(
        capture: BufferedImage,
        card: StoreCardSheet.StoreCard,
        face: StoreCardSheet.CardFace,
        layout: StoreCardLayout,
        type: CardTypefaces,
    ): BufferedImage {
        val image = BufferedImage(
            layout.form.width,
            layout.form.height,
            BufferedImage.TYPE_INT_RGB,
        )
        val graphics = image.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
            )
            graphics.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON,
            )
            graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC,
            )
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.color = rgb(face.ground)
            graphics.fillRect(0, 0, image.width, image.height)
            paintType(graphics, card, face, layout, type)
            paintFrame(graphics, capture, face, layout)
        } finally {
            graphics.dispose()
        }
        return image
    }

    /**
     * The type band: the headline in its fixed band, the sub-line in its own,
     * and the gold rule under both. A band is centered, never top-aligned, so
     * a one-line caption and a two-line caption both sit right under the
     * paper's edge rather than one of them hanging from the top.
     */
    private fun paintType(
        graphics: Graphics2D,
        card: StoreCardSheet.StoreCard,
        face: StoreCardSheet.CardFace,
        layout: StoreCardLayout,
        type: CardTypefaces,
    ) {
        val display = type.display.deriveFont(Font.PLAIN, layout.displaySize.toFloat())
        val label = type.label.deriveFont(Font.PLAIN, layout.labelSize.toFloat())
        val lines = wrap(card.headline, display, layout.textWidth.toDouble())
        val block = layout.displayLeading * lines.size
        val top = layout.displayTop + (layout.displayHeight - block) / 2
        lines.forEachIndexed { index, line ->
            drawRun(
                graphics = graphics,
                text = line,
                font = display,
                leading = layout.displayLeading,
                left = layout.textX,
                top = top + layout.displayLeading * index,
                width = layout.textWidth,
                color = rgb(face.word),
            )
        }
        card.sub?.let { sub ->
            val leading = labelLeading(layout.labelSize)
            drawRun(
                graphics = graphics,
                text = sub,
                font = label,
                leading = leading,
                left = layout.textX,
                top = layout.labelTop + (layout.labelHeight - leading) / 2,
                width = layout.textWidth,
                color = rgb(face.muted),
            )
        }
        graphics.color = rgb(face.rule)
        graphics.fillRect(
            (layout.form.width - layout.ruleWidth) / 2,
            layout.ruleY,
            layout.ruleWidth,
            layout.ruleHeight,
        )
    }

    /**
     * One line, on its own pitch, centered in the card's column. The baseline
     * seats the line's own box in the pitch, so a card with one headline line
     * and a card with two put neither of them at a different weight of air.
     */
    private fun drawRun(
        graphics: Graphics2D,
        text: String,
        font: Font,
        leading: Int,
        left: Int,
        top: Int,
        width: Int,
        color: Color,
    ) {
        val line = TextLayout(text, font, context)
        val x = left + (width - line.advance) / 2f
        val metrics = font.getLineMetrics(text, context)
        val baseline = top + (leading - (metrics.ascent + metrics.descent)) / 2f + metrics.ascent
        graphics.color = color
        line.draw(graphics, x, baseline)
    }

    /**
     * The capture and its outline. The clip rounds the capture's own corners
     * by [StoreCardLayout.radius], and the outline sits one pad outside it, so
     * the drawing reads as the page held over the card rather than as an image
     * pasted on it.
     */
    private fun paintFrame(
        graphics: Graphics2D,
        capture: BufferedImage,
        face: StoreCardSheet.CardFace,
        layout: StoreCardLayout,
    ) {
        val box = fitted(layout.frame, capture.width, capture.height)
        val clip = RoundRectangle2D.Double(
            box.x.toDouble(),
            box.y.toDouble(),
            box.width.toDouble(),
            box.height.toDouble(),
            layout.radius * 2.0,
            layout.radius * 2.0,
        )
        val previous = graphics.clip
        graphics.clip = clip
        graphics.drawImage(capture, box.x, box.y, box.width, box.height, null)
        graphics.clip = previous
        val grow = layout.pad + layout.stroke / 2.0
        val outline = RoundRectangle2D.Double(
            box.x - grow,
            box.y - grow,
            box.width + 2 * grow,
            box.height + 2 * grow,
            (layout.radius + layout.pad) * 2.0,
            (layout.radius + layout.pad) * 2.0,
        )
        graphics.color = rgb(face.outline)
        graphics.stroke = BasicStroke(layout.stroke.toFloat())
        graphics.draw(outline)
    }

    /** The card's own color: the theme's RGB, never its alpha. */
    private fun rgb(argb: Int): Color = Color(argb and 0xFFFFFF)
}
