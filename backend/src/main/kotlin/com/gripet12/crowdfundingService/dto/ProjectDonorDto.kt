package com.gripet12.crowdfundingService.dto

import java.math.BigDecimal
import java.sql.Timestamp

data class ProjectDonorDto(
    val donorId: Long?,
    val username: String,
    val imageId: Long?,
    val totalAmount: BigDecimal,
    val donationsCount: Int,
    val lastDonatedAt: Timestamp?,
    val anonymous: Boolean = false
)
