package com.gripet12.crowdfundingService.dto

import java.time.LocalDateTime

data class AnalyticsLogDto(
    var logId: Long,
    var userId: Long?,
    var projectId: Long?,
    var targetUserId: Long?,
    var actionType: String,
    var actionTime: LocalDateTime,
    var ipAddress: String?
)
