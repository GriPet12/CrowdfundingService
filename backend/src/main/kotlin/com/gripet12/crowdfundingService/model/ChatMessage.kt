package com.gripet12.crowdfundingService.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import lombok.AllArgsConstructor
import lombok.NoArgsConstructor

@Entity
@Table(name = "chat_messages")
@AllArgsConstructor
@NoArgsConstructor
data class ChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val messageId: Long,

    @ManyToOne
    val sender: User,

    @ManyToOne
    val subject: Subscription,

    @ManyToOne
    val project: Project,

    val messageContent: String
)
