package com.gripet12.crowdfundingService.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

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

    @ManyToOne
    val subscriptionTier: SubscriptionTier?,

    @ManyToOne
    val payment: Payment? = null,

    val tierPrice: BigDecimal,

    var expiresAt: LocalDate? = null,

    @Column(name = "is_active", nullable = false, columnDefinition = "boolean not null default true")
    var active: Boolean = true,

    @Column(nullable = false, length = 10, columnDefinition = "varchar(10) not null default 'MANUAL'")
    val grantType: String = "MANUAL"
)
