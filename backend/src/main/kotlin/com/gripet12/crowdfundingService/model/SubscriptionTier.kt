package com.gripet12.crowdfundingService.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "subscription_tiers")
data class SubscriptionTier(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val tierId: Long?,

    val amount: Long,

    val name: String,

    val description: String,

    val level: Int
)
