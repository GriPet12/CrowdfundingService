package com.gripet12.crowdfundingService.dto

import java.math.BigDecimal
import java.sql.Timestamp

data class DonationDto(
    val donationId: Long?,
    val projectTitle: String?,
    val creatorName: String?,
    val rewardName: String?,
    val amount: BigDecimal,
    val paymentStatus: String?,
    val createdAt: Timestamp?
)
