package com.gripet12.crowdfundingService.dto

data class CreatePostDto(
    val title: String,
    val content: String,

    val requiredTierId: Long? = null,
    val mediaIds: List<Long> = emptyList()
)
