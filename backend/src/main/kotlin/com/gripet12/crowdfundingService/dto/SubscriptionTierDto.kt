package com.gripet12.crowdfundingService.dto

data class SubscriptionTierDto(
    val tierId: Long? = null,
    val creatorId: Long,
    val name: String,
    val description: String,
    val amount: Long,
    val level: Int
)
