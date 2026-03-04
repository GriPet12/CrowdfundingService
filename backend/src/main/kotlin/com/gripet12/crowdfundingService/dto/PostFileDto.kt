package com.gripet12.crowdfundingService.dto

data class PostFileDto(
    val id: Long,
    val originalFileName: String,
    val mimeType: String,

    val category: String
)
