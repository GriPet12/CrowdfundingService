package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.ChatAccessDto
import com.gripet12.crowdfundingService.dto.ChatMessageDto
import com.gripet12.crowdfundingService.dto.ChatSettingsDto
import com.gripet12.crowdfundingService.model.AuthorChatSettings
import com.gripet12.crowdfundingService.model.ChatMessage
import com.gripet12.crowdfundingService.repository.AuthorChatSettingsRepository
import com.gripet12.crowdfundingService.repository.ChatMessageRepository
import com.gripet12.crowdfundingService.repository.SubscriptionRepository
import com.gripet12.crowdfundingService.repository.SubscriptionTierRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatService(
    private val chatMessageRepository: ChatMessageRepository,
    private val chatSettingsRepository: AuthorChatSettingsRepository,
    private val userRepository: UserRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val tierRepository: SubscriptionTierRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private fun currentUserIdOrNull(): Long? {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated || auth.name == "anonymousUser") return null
        return userRepository.findByUsername(auth.name).orElse(null)?.userId
    }

    private fun currentUserId(): Long =
        currentUserIdOrNull() ?: throw IllegalStateException("Not authenticated")

    @Transactional(readOnly = true)
    fun getSettings(authorId: Long): ChatSettingsDto {
        val settings = chatSettingsRepository.findById(authorId).orElse(null)
        return ChatSettingsDto(authorId = authorId, minSubscriptionLevel = settings?.minSubscriptionLevel)
    }

    @Transactional
    fun updateSettings(authorId: Long, minSubscriptionLevel: Int?) {
        val callerId = currentUserId()
        if (callerId != authorId) throw IllegalAccessException("Access denied")
        chatSettingsRepository.save(AuthorChatSettings(authorId = authorId, minSubscriptionLevel = minSubscriptionLevel))
    }

    @Transactional(readOnly = true)
    fun checkAccess(authorId: Long): ChatAccessDto {
        val settings = chatSettingsRepository.findById(authorId).orElse(null)
        val minLevel = settings?.minSubscriptionLevel

        if (minLevel == null) {
            return ChatAccessDto(hasAccess = true, minSubscriptionLevel = null, requiredTierName = null)
        }

        val userId = currentUserIdOrNull()

        if (userId != null && userId == authorId) {
            return ChatAccessDto(hasAccess = true, minSubscriptionLevel = minLevel, requiredTierName = null)
        }

        if (userId == null) {
            val tierName = tierRepository.findByCreatorId(authorId)
                .filter { it.level >= minLevel }
                .minByOrNull { it.level }?.name
            return ChatAccessDto(hasAccess = false, minSubscriptionLevel = minLevel, requiredTierName = tierName)
        }

        val hasSub = subscriptionRepository.findActiveSubscriptionsBySubscriberAndCreator(userId, authorId)
            .any { sub -> (sub.subscriptionTier?.level ?: 0) >= minLevel }

        val tierName = if (!hasSub) {
            tierRepository.findByCreatorId(authorId)
                .filter { it.level >= minLevel }
                .minByOrNull { it.level }?.name
        } else null

        return ChatAccessDto(hasAccess = hasSub, minSubscriptionLevel = minLevel, requiredTierName = tierName)
    }

    @Transactional(readOnly = true)
    fun getMessages(authorId: Long): List<ChatMessageDto> {
        val access = checkAccess(authorId)
        if (!access.hasAccess) throw IllegalAccessException("Access denied")
        return chatMessageRepository.findByChatOwnerUserId(authorId).map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getNewMessages(authorId: Long, afterId: Long): List<ChatMessageDto> {
        val access = checkAccess(authorId)
        if (!access.hasAccess) throw IllegalAccessException("Access denied")
        return chatMessageRepository.findByChatOwnerUserIdAndMessageIdGreaterThan(authorId, afterId).map { it.toDto() }
    }


    @Transactional
    fun sendMessageWs(authorId: Long, text: String, senderUsername: String): ChatMessageDto {
        if (text.isBlank() || text.length > 1000) throw IllegalArgumentException("Invalid message text")

        val sender = userRepository.findByUsername(senderUsername)
            .orElseThrow { IllegalStateException("User not found") }
        val senderId = sender.userId!!

        val settings = chatSettingsRepository.findById(authorId).orElse(null)
        val minLevel = settings?.minSubscriptionLevel
        if (minLevel != null && senderId != authorId) {
            val hasSub = subscriptionRepository.findApprovedBySubscrberUserId(senderId)
                .any { sub -> sub.creator.userId == authorId && (sub.subscriptionTier?.level ?: 0) >= minLevel }
            if (!hasSub) throw IllegalAccessException("Access denied")
        }

        val chatOwner = userRepository.getReferenceById(authorId)
        val message = chatMessageRepository.save(
            ChatMessage(chatOwner = chatOwner, sender = sender, text = text.trim())
        )
        val saved = chatMessageRepository.findById(message.messageId!!).orElseThrow()
        val dto = saved.toDto()

        messagingTemplate.convertAndSend("/topic/chat/$authorId", dto)

        return dto
    }

    @Transactional
    fun deleteMessage(authorId: Long, messageId: Long) {
        val callerId = currentUserId()
        if (callerId != authorId) throw IllegalAccessException("Access denied")
        val message = chatMessageRepository.findByMessageIdAndChatOwnerUserId(messageId, authorId)
            ?: throw IllegalArgumentException("Message not found")
        chatMessageRepository.delete(message)
    }

    private fun ChatMessage.toDto() = ChatMessageDto(
        messageId = messageId!!,
        senderId = sender.userId!!,
        senderName = sender.username,
        senderImageId = sender.image?.id,
        text = text,
        createdAt = createdAt
    )
}
