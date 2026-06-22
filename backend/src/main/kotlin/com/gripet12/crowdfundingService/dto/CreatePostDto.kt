package com.gripet12.crowdfundingService.dto

import java.math.BigDecimal

data class CreatePostDto(
    val title: String,
    val content: String,
    val visibility: String = "PUBLIC",
    val requiredTierId: Long? = null,
    val minDonationAmount: BigDecimal? = null,
    val mediaIds: List<Long> = emptyList()
)
