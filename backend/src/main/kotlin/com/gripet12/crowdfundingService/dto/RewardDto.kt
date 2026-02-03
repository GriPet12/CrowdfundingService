package com.gripet12.crowdfundingService.dto

import java.math.BigDecimal

data class RewardDto(

    val rewardId: Long,

    val rewardName: String,

    val rewardDescription: String,

    val minimalAmount: BigDecimal,

    val rewardTier: String,

    val isHaveQuantity: Boolean,

    val quantityAvailable: Int,

    val quantityClaimed: Int

)
