package com.gripet12.crowdfundingService.dto

import java.math.BigDecimal
import java.time.LocalDate

data class SubscriptionDto(
    val subscriptionId: Long?,
    val creatorId: Long,
    val creatorName: String,
    val creatorImageId: Long?,
    val tierName: String,
    val tierLevel: Int,
    val tierPrice: BigDecimal,
    val paymentStatus: String,
    val expiresAt: LocalDate?,
    val isActive: Boolean,
    val grantType: String
)
