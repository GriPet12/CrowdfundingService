package com.gripet12.crowdfundingService.util

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object ImagePreviewGenerator {
    data class Preview(val bytes: ByteArray, val mimeType: String)

    fun create(data: ByteArray, mimeType: String, maxWidth: Int = 640): Preview? {
        if (!mimeType.startsWith("image/")) return null

        val original = ImageIO.read(ByteArrayInputStream(data)) ?: return null
        val scale = minOf(1.0, maxWidth.toDouble() / original.width)
        if (scale >= 1.0) {
            return Preview(data, mimeType)
        }

        val targetWidth = (original.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (original.height * scale).toInt().coerceAtLeast(1)
        val resized = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val graphics = resized.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.drawImage(original, 0, 0, targetWidth, targetHeight, null)
        graphics.dispose()

        val format = if (mimeType.contains("png", ignoreCase = true)) "png" else "jpeg"
        val outputMime = if (format == "png") "image/png" else "image/jpeg"
        val output = ByteArrayOutputStream()
        ImageIO.write(resized, format, output)
        return Preview(output.toByteArray(), outputMime)
    }
}
