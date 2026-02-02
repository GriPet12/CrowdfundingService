package com.gripet12.crowdfundingService.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import lombok.AllArgsConstructor
import lombok.NoArgsConstructor
import java.math.BigDecimal

@Entity
@Table(name = "donate")
@AllArgsConstructor
@NoArgsConstructor
data class Donate(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val donateId: Long,

    @ManyToOne
    val donor: User,

    @ManyToOne
    val project: Project,

    val amount: BigDecimal,

    val reward: Int,

    val paymentStatus: String,

    val isAnonymous: Boolean = false
)
