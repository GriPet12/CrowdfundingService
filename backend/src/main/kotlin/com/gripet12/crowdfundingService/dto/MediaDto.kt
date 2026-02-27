package com.gripet12.crowdfundingService.dto

import java.time.LocalDateTime

data class MediaDto(
    var id: Long? = null,

    var originalFileName: String,

    var mimeType: String,

    var category: String,

    var size: Long,

    var uploadedAt: LocalDateTime
)