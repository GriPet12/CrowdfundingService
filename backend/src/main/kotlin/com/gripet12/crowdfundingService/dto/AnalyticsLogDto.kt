package com.gripet12.crowdfundingService.dto

data class AnalyticsLogDto(

    var logId: Long,

    var user: Long?,

    var action: String,

    var activityModelType: String,

    var activityModelId: Long,

    var categories: Set<Long?> = HashSet(),
)
