package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.model.UploadedFile
import com.gripet12.crowdfundingService.model.enums.FileCategory
import com.gripet12.crowdfundingService.repository.FileRepository
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
        val mimeType = tika.detect(bytes)
        val category = determineCategory(mimeType)

        val entity = UploadedFile(
            originalFileName = file.originalFilename ?: "unknown",
            mimeType = mimeType,
            category = category,
            size = file.size,
            data = bytes
        )

        return fileRepository.save(entity)
    }

    private fun determineCategory(mimeType: String): FileCategory {
        return when {
            mimeType.startsWith("image/") -> FileCategory.PHOTO
            mimeType.startsWith("video/") -> FileCategory.VIDEO
            mimeType.startsWith("audio/") -> FileCategory.AUDIO
            else -> FileCategory.OTHER
        }
    }

    @Transactional(readOnly = true)
    fun getImage(id: Long): UploadedFile? = fileRepository.findById(id)
        .filter { it.category == FileCategory.PHOTO }
        .orElse(null)

    @Transactional(readOnly = true)
    fun getFile(id: Long): UploadedFile? = fileRepository.findById(id).orElse(null)

    @Transactional
    fun deleteFile(id: Long) {
        fileRepository.deleteById(id)
    }
}