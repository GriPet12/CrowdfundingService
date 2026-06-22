package com.gripet12.crowdfundingService.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.sql.Timestamp

@Entity
@Table(name = "withdrawals")
class Withdrawal(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val withdrawalId: Long? = null,

    @ManyToOne(optional = false)
    val user: User,

    val amount: BigDecimal,

    @Column(nullable = false, length = 20)
    var status: String = "PENDING",

    @Column(name = "stripe_transfer_id", length = 120)
    var stripeTransferId: String? = null,

    @Column(name = "failure_reason", length = 500)
    var failureReason: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "processed_at")
    var processedAt: Timestamp? = null
)
