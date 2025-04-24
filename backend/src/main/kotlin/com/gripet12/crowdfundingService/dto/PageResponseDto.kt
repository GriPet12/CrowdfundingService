package com.gripet12.crowdfundingService.dto

data class PageResponseDto<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val size: Int
)