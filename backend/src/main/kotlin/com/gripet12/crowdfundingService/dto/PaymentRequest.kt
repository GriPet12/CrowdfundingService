package com.gripet12.crowdfundingService.dto

data class PaymentRequest(
    val orderId: String,

    val amount: Double,

    val donateId: Long,

    val donor: Long,

    val project: Long,

    val creator: Long,

    val reward: Int,

    val paymentStatus: String,

    val isAnonymous: Boolean
)
