package com.gripet12.crowdfundingService.dto

data class PostResponseDto(
    val postId: Long,
    val masterId: Long,
    val title: String,
    val description: String,

    val requiredTierLevel: Int?,
    val requiredTierName: String?,

    val hasAccess: Boolean,

    val banned: Boolean,

    val files: List<PostFileDto>,
    val visibility: String,
    val likeCount: Long,
    val likedByMe: Boolean,
    val commentCount: Long
)
