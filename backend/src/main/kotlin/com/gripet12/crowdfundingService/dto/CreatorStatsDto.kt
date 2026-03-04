package com.gripet12.crowdfundingService.dto

data class CreatorStatsDto(

    val totalViews: Long,
    val totalFollowers: Long,
    val totalSubscribers: Long,
    val totalPosts: Long,
    val totalLikes: Long,
    val totalComments: Long,
    val totalDonationsAmount: Double,
    val totalDonationsCount: Long,

    val activityByDay: List<DayActivityDto>,

    val donationsByType: List<PieSliceDto>,

    val topPosts: List<TopPostDto>
)

data class DayActivityDto(
    val date: String,      
    val views: Long,
    val follows: Long,
    val donates: Long,
    val subscriptions: Long
)

data class PieSliceDto(
    val label: String,
    val value: Double
)

data class TopPostDto(
    val postId: Long,
    val title: String,
    val likes: Long,
    val comments: Long
)
