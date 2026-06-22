package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.UploadedFile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface FileRepository : JpaRepository<UploadedFile, Long> {
    @Query("SELECT f.previewData FROM UploadedFile f WHERE f.id = :id")
    fun findPreviewDataById(id: Long): ByteArray?

    @Query("SELECT f.mimeType FROM UploadedFile f WHERE f.id = :id")
    fun findMimeTypeById(id: Long): String?

    @Modifying
    @Transactional
    @Query("UPDATE UploadedFile f SET f.previewData = :preview WHERE f.id = :id")
    fun updatePreviewData(id: Long, preview: ByteArray): Int
}
