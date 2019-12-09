package com.zorroa

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Polygon
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO

class StackRender(val title: String, val color: Color, val inputStream: InputStream) {

    fun render(): ReversibleByteArrayOutputStream {

        val image = ImageIO.read(inputStream)
        val canvas = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
        val g2: Graphics2D = canvas.createGraphics()

        g2.color = Color.GRAY
        g2.fillRect(0, 0, image.width, image.height)
        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        )

        g2.stroke = BasicStroke(2f)

        g2.drawImage(image, 0, 0, null)
        g2.color = Color.GRAY
        drawBorder(g2, image, 0)

        g2.color = color

        val poly = Polygon()
        poly.addPoint(canvas.width, 0)
        poly.addPoint(canvas.width, 300)
        poly.addPoint(canvas.width - 300, 0)
        g2.fillPolygon(poly)

        val poly2 = Polygon()
        poly2.addPoint(0, canvas.height)
        poly2.addPoint(200, canvas.height)
        poly2.addPoint(0, canvas.height - 200)
        g2.fillPolygon(poly2)

        g2.color = Color.WHITE

        val font = Font("Arial", Font.PLAIN, 60)
        val affineTransform = AffineTransform()
        affineTransform.setToRotation(Math.toRadians(45.0), 0.0, 0.0)
        val rotatedFont = font.deriveFont(affineTransform)
        g2.setFont(rotatedFont)
        g2.drawString(title, canvas.width - 200, 50)

        g2.dispose()

        val imageOutput = ReversibleByteArrayOutputStream(16384)
        ImageIO.write(canvas, "PNG", imageOutput)

        return imageOutput
    }

    fun drawBorder(g2: Graphics2D, image: BufferedImage, offset: Int) {
        // Left side
        g2.drawLine(offset, offset, offset, image.height + offset)
        // Top
        g2.drawLine(offset, offset, image.width + offset, offset)
        // Bottom
        g2.drawLine(offset, image.height + offset, image.width + offset, image.height + offset)
        // Right Side
        g2.drawLine(image.width + offset, image.height + offset, image.width + offset, offset)
    }
}