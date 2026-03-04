package com.gripet12.crowdfundingService.dto

import java.math.BigDecimal

data class CreateProjectDto(
    val title: String,
    val description: String? = null,
    val goalAmount: BigDecimal,
    val mainImage: Long,

    val mediaIds: List<Long> = emptyList(),
    val categories: List<String> = emptyList()
)
