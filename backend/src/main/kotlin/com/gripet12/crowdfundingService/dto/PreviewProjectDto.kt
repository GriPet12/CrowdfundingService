package com.gripet12.crowdfundingService.dto

import java.math.BigDecimal

data class PreviewProjectDto(
    val projectId: Long?,

    val creatorId: Long?,

    val title: String,

    val goalAmount: BigDecimal,

    val collectedAmount: BigDecimal,

    val status: String?,

    val mainImage: Long?
)
