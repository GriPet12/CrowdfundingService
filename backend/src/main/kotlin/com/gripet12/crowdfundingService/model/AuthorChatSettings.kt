package com.gripet12.crowdfundingService.model

import jakarta.persistence.*

@Entity
@Table(name = "author_chat_settings")
data class AuthorChatSettings(
    @Id
    val authorId: Long,

    val minSubscriptionLevel: Int? = null
)
