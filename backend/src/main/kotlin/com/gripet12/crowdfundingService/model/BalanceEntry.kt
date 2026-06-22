package com.gripet12.crowdfundingService.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.sql.Timestamp

@Entity
@Table(
    name = "balance_entries",
    uniqueConstraints = [UniqueConstraint(name = "uk_balance_idempotency", columnNames = ["idempotency_key"])]
)
class BalanceEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val entryId: Long? = null,

    @ManyToOne(optional = false)
    val user: User,

    val amount: BigDecimal,

    @Column(name = "entry_type", nullable = false, length = 20)
    val entryType: String,

    @Column(name = "idempotency_key", nullable = false, length = 120)
    val idempotencyKey: String,

    val description: String? = null,

    @Column(name = "project_id")
    var projectId: Long? = null,

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    var frozen: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Timestamp = Timestamp(System.currentTimeMillis())
)
