package com.gripet12.crowdfundingService.dto

import java.time.LocalDateTime

data class ChatMessageDto(
    val messageId: Long,
    val senderId: Long,
    val senderName: String,
    val senderImageId: Long?,
    val text: String,
    val createdAt: LocalDateTime
)

data class SendMessageRequest(
    val text: String
)

data class ChatSettingsDto(
    val authorId: Long,

    val minSubscriptionLevel: Int?
)

data class ChatAccessDto(

    val hasAccess: Boolean,

    val minSubscriptionLevel: Int?,

    val requiredTierName: String?
)
