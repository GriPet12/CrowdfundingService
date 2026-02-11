package com.gripet12.crowdfundingService.model

import com.gripet12.crowdfundingService.model.enums.FileCategory
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "files")
data class UploadedFile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val originalFileName: String,

    val mimeType: String,

    @Enumerated(EnumType.STRING)
    val category: FileCategory,

    val size: Long,

    @Lob
    val data: ByteArray,

    val uploadedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UploadedFile) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}