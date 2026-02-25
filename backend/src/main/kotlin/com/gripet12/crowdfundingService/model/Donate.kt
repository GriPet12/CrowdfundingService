package com.gripet12.crowdfundingService.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.sql.Timestamp

@Entity
@Table(name = "donate")
data class Donate(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val donateId: Long?,

    @ManyToOne
    val donor: User,

    @ManyToOne
    val project: Project,

    val amount: BigDecimal,

    val reward: Int,

    @ManyToOne
    var payment: Payment? = null,

    val isAnonymous: Boolean = false,

    val createAt: Timestamp = Timestamp(System.currentTimeMillis())
)
