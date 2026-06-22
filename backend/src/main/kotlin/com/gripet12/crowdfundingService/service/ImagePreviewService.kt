package com.gripet12.crowdfundingService.service

import org.springframework.stereotype.Service
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Service
class ImagePreviewService(
    private val fileStorageService: FileStorageService
) {
    private val cache = mutableMapOf<String, CachedPreview>()
    private val maxCacheEntries = 256

    data class CachedPreview(val bytes: ByteArray, val mimeType: String)

    fun getPreview(fileId: Long, maxWidth: Int): CachedPreview? {
        val safeWidth = maxWidth.coerceIn(64, 1600)
        val cacheKey = "$fileId:$safeWidth"
        synchronized(cache) {
            cache[cacheKey]?.let { return it }
        }

        val file = fileStorageService.getFile(fileId) ?: return null
        if (!file.mimeType.startsWith("image/")) {
            return CachedPreview(file.data, file.mimeType)
        }

        val preview = try {
            createPreview(file.data, file.mimeType, safeWidth)
        } catch (_: Exception) {
            CachedPreview(file.data, file.mimeType)
        }

        synchronized(cache) {
            if (cache.size >= maxCacheEntries) {
                cache.keys.take(cache.size - maxCacheEntries + 1).forEach { cache.remove(it) }
            }
            cache[cacheKey] = preview
        }
        return preview
    }

    private fun createPreview(data: ByteArray, mimeType: String, maxWidth: Int): CachedPreview {
        val original = ImageIO.read(ByteArrayInputStream(data))
            ?: return CachedPreview(data, mimeType)

        val scale = minOf(1.0, maxWidth.toDouble() / original.width)
        if (scale >= 1.0) return CachedPreview(data, mimeType)

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
        return CachedPreview(output.toByteArray(), outputMime)
    }
}
