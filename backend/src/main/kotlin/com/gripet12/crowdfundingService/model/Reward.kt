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
    var project: Project?,

    var rewardName: String,

    var rewardDescription: String,

    var minimalAmount: BigDecimal,

    var rewardTier: String,

    var isHaveQuantity: Boolean,

    var quantityAvailable: Int,

    val quantityClaimed: Int

)
