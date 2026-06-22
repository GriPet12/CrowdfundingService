package com.gripet12.crowdfundingService.dto

data class UpdatePostDto(
    val title: String,
    val content: String,
    val visibility: String = "PUBLIC",
    val requiredTierId: Long? = null,
    val mediaIds: List<Long> = emptyList()
)
