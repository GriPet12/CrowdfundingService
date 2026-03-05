package com.gripet12.crowdfundingService.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "payments")
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var paymentId: Long? = null,

    @Column(unique = true, nullable = true)
    val orderReference: String = UUID.randomUUID().toString(),

    val amount: BigDecimal,

    var status: String
)
