package com.gripet12.crowdfundingService.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class AdminUserDto(
    val id: Long?,
    val username: String,
    val email: String,
    val role: String,
    val banned: Boolean,
    val imageId: Long?,
    val createdAt: LocalDateTime?
)

data class AdminProjectDto(
    val projectId: Long?,
    val title: String,
    val creatorName: String,
    val category: String?,
    val goalAmount: BigDecimal,
    val raisedAmount: BigDecimal,
    val status: String?,
    val banned: Boolean = false,
    val createdAt: LocalDateTime? = null
)

data class AdminPostDto(
    val postId: Long,
    val title: String,
    val authorId: Long,
    val authorName: String,
    val requiredTierId: Long?,
    val banned: Boolean = false,
    val createdAt: LocalDateTime? = null
)

data class CategoryRequestDto(
    val name: String,
    val description: String? = null
)

data class CategoryResponseDto(
    val id: Long,
    val name: String,
    val description: String?
)

data class TransactionDto(
    val id: Long?,
    val type: String,
    val fromUser: String?,
    val toUser: String?,
    val amount: BigDecimal,
    val status: String,
    val createdAt: LocalDateTime?
)

data class TransactionSummaryDto(
    val totalDonations: BigDecimal,
    val donationsCount: Long,
    val totalWithdrawals: BigDecimal,
    val withdrawalsCount: Long,
    val totalSubscriptions: BigDecimal,
    val subscriptionsCount: Long,
    val totalVolume: BigDecimal,
    val totalCount: Long
)
