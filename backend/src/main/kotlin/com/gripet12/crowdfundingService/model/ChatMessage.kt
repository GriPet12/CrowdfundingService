package com.gripet12.crowdfundingService.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "chat_messages")
data class ChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val messageId: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_owner_id", nullable = false)
    val chatOwner: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    val sender: User,

    @Column(nullable = false, length = 1000)
    val text: String,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
