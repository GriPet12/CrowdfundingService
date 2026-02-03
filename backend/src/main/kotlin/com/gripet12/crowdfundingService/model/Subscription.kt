package com.gripet12.crowdfundingService.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "subscriptions")
data class Subscription(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val subscriptionId: Long? = null,

    @ManyToOne
    val subscrber: User,

    @ManyToOne
    val creator: User,

    //val subscriptionTier: SubscriptionTier,

    val status: String,

    val tierPrice: BigDecimal
)
