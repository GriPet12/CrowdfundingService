package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.ChatMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m JOIN FETCH m.sender s LEFT JOIN FETCH s.image WHERE m.chatOwner.userId = :ownerId ORDER BY m.createdAt ASC")
    fun findByChatOwnerUserId(ownerId: Long): List<ChatMessage>

    @Query("SELECT m FROM ChatMessage m JOIN FETCH m.sender s LEFT JOIN FETCH s.image WHERE m.chatOwner.userId = :ownerId AND m.messageId > :afterId ORDER BY m.createdAt ASC")
    fun findByChatOwnerUserIdAndMessageIdGreaterThan(ownerId: Long, afterId: Long): List<ChatMessage>

    fun findByMessageIdAndChatOwnerUserId(messageId: Long, ownerId: Long): ChatMessage?
}
