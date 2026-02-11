package com.gripet12.crowdfundingService.dto

import java.math.BigDecimal

data class ProjectDto(
    val projectId: Long?,

    val creator: Long?,

    val title: String,

    val goalAmount: BigDecimal,

    val collectedAmount: BigDecimal,

    val status: String? = null,

    val mainImage: Long?,

    val media: Set<Long?> = HashSet(),

    val categories: Set<Long?> = HashSet()
)