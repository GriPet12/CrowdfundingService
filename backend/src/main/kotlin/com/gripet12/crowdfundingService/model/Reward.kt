package com.gripet12.crowdfundingService.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "rewards")
data class Reward(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val rewardId: Long,

    @ManyToOne
    val project: Project?,

    val rewardName: String,

    val rewardDescription: String,

    val minimalAmount: BigDecimal,

    val rewardTier: String,

    val isHaveQuantity: Boolean,

    val quantityAvailable: Int,

    val quantityClaimed: Int

)
