package com.gripet12.crowdfundingService.dto

data class ReceivedProjectFollowDto(
    val followerId: Long,
    val followerName: String,
    val followerImageId: Long?,
    val projectId: Long,
    val projectTitle: String
)
