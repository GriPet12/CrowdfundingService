package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.model.UploadedFile
import com.gripet12.crowdfundingService.model.enums.FileCategory
import com.gripet12.crowdfundingService.repository.FileRepository
import com.gripet12.crowdfundingService.util.ImagePreviewGenerator
import org.apache.tika.Tika
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class FileStorageService(
    private val fileRepository: FileRepository
) {
    private val tika = Tika()

    @Transactional
    fun uploadFile(file: MultipartFile): UploadedFile {
        val bytes = file.bytes
        val originalName = file.originalFilename ?: "unknown"
        val mimeType = detectMimeType(bytes, originalName)
        val category = determineCategory(mimeType, originalName)
        val preview = ImagePreviewGenerator.create(bytes, mimeType)

        val entity = UploadedFile(
            originalFileName = originalName,
            mimeType = mimeType,
            category = category,
            size = file.size,
            data = bytes,
            previewData = preview?.bytes
        )

        return fileRepository.save(entity)
    }

    private fun detectMimeType(bytes: ByteArray, originalFilename: String): String {
        val detected = tika.detect(bytes, originalFilename)
        if (detected != "application/octet-stream") return detected
        return mimeFromExtension(originalFilename) ?: detected
    }

    private fun mimeFromExtension(filename: String): String? {
        return when (filename.substringAfterLast('.', "").lowercase()) {
            "mp4", "m4v" -> "video/mp4"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> null
        }
    }

    private fun determineCategory(mimeType: String, originalFilename: String): FileCategory {
        return when {
            mimeType.startsWith("image/") -> FileCategory.PHOTO
            mimeType.startsWith("video/") -> FileCategory.VIDEO
            mimeType.startsWith("audio/") -> FileCategory.AUDIO
            isVideoExtension(originalFilename) -> FileCategory.VIDEO
            isAudioExtension(originalFilename) -> FileCategory.AUDIO
            isImageExtension(originalFilename) -> FileCategory.PHOTO
            else -> FileCategory.OTHER
        }
    }

    private fun isVideoExtension(filename: String) =
        filename.substringAfterLast('.', "").lowercase() in setOf("mp4", "m4v", "mov", "webm", "avi", "mkv", "mpeg", "mpg")

    private fun isAudioExtension(filename: String) =
        filename.substringAfterLast('.', "").lowercase() in setOf("mp3", "wav", "ogg", "flac", "aac", "m4a")

    private fun isImageExtension(filename: String) =
        filename.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "gif", "webp", "svg")

    @Transactional(readOnly = true)
    fun getFile(id: Long): UploadedFile? = fileRepository.findById(id).orElse(null)
}
