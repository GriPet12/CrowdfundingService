package com.gripet12.crowdfundingService.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class BalanceSummaryDto(
    val availableBalance: BigDecimal,
    val frozenBalance: BigDecimal,
    val totalEarned: BigDecimal,
    val totalWithdrawn: BigDecimal,
    val platformFeePercent: Int,
    val minWithdrawal: BigDecimal,
    val stripeConnected: Boolean,
    val stripePayoutsEnabled: Boolean,
    val connectAvailable: Boolean
)

data class WithdrawalRequestDto(
    val amount: BigDecimal,
    val payoutMethod: String? = null,
    val payoutDestination: String? = null,
    val recipientName: String? = null
)

data class WithdrawalDto(
    val withdrawalId: Long,
    val amount: BigDecimal,
    val status: String,
    val stripeTransferId: String?,
    val failureReason: String?,
    val payoutMethod: String?,
    val payoutDestination: String?,
    val recipientName: String?,
    val createdAt: LocalDateTime,
    val processedAt: LocalDateTime?
)

data class ConnectOnboardingDto(
    val url: String
)

data class ConnectStatusDto(
    val connected: Boolean,
    val payoutsEnabled: Boolean,
    val accountId: String?,
    val connectAvailable: Boolean
)
