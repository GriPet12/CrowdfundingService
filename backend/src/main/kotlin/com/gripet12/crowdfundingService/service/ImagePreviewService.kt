package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.repository.FileRepository
import com.gripet12.crowdfundingService.util.ImagePreviewGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ImagePreviewService(
    private val fileRepository: FileRepository,
    private val fileStorageService: FileStorageService
) {
    data class CachedPreview(val bytes: ByteArray, val mimeType: String)

    @Transactional
    fun getPreview(fileId: Long, maxWidth: Int): CachedPreview? {
        val safeWidth = maxWidth.coerceIn(64, 1600)
        val storedPreview = fileRepository.findPreviewDataById(fileId)
        if (storedPreview != null && storedPreview.isNotEmpty()) {
            val mimeType = fileRepository.findMimeTypeById(fileId) ?: "image/jpeg"
            val outputMime = if (mimeType.contains("png", ignoreCase = true)) "image/png" else "image/jpeg"
            return CachedPreview(storedPreview, outputMime)
        }

        val file = fileStorageService.getFile(fileId) ?: return null
        if (!file.mimeType.startsWith("image/")) {
            return CachedPreview(file.data, file.mimeType)
        }

        val preview = ImagePreviewGenerator.create(file.data, file.mimeType, safeWidth)
            ?: return CachedPreview(file.data, file.mimeType)

        fileRepository.updatePreviewData(fileId, preview.bytes)
        return CachedPreview(preview.bytes, preview.mimeType)
    }
}
