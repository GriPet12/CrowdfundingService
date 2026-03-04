package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.ChatAccessDto
import com.gripet12.crowdfundingService.dto.ChatMessageDto
import com.gripet12.crowdfundingService.dto.ChatSettingsDto
import com.gripet12.crowdfundingService.dto.SendMessageRequest
import com.gripet12.crowdfundingService.service.ChatService
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/chat")
class ChatController(private val chatService: ChatService) {

    @MessageMapping("/chat/{authorId}/send")
    fun wsSendMessage(
        @DestinationVariable authorId: Long,
        request: SendMessageRequest,
        headerAccessor: SimpMessageHeaderAccessor
    ) {
        val username = headerAccessor.user?.name ?: return
        try {
            chatService.sendMessageWs(authorId, request.text, username)
        } catch (_: Exception) {}
    }

    @GetMapping("/{authorId}/access")
    fun checkAccess(@PathVariable authorId: Long): ResponseEntity<ChatAccessDto> =
        ResponseEntity.ok(chatService.checkAccess(authorId))

    @GetMapping("/{authorId}/messages")
    fun getMessages(
        @PathVariable authorId: Long,
        @RequestParam(required = false) after: Long?
    ): ResponseEntity<List<ChatMessageDto>> =
        try {
            val messages = if (after != null)
                chatService.getNewMessages(authorId, after)
            else
                chatService.getMessages(authorId)
            ResponseEntity.ok(messages)
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(403).build()
        }

    @DeleteMapping("/{authorId}/messages/{messageId}")
    fun deleteMessage(
        @PathVariable authorId: Long,
        @PathVariable messageId: Long
    ): ResponseEntity<Void> =
        try {
            chatService.deleteMessage(authorId, messageId)
            ResponseEntity.noContent().build()
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(403).build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }

    @GetMapping("/{authorId}/settings")
    fun getSettings(@PathVariable authorId: Long): ResponseEntity<ChatSettingsDto> =
        ResponseEntity.ok(chatService.getSettings(authorId))

    @PutMapping("/{authorId}/settings")
    fun updateSettings(
        @PathVariable authorId: Long,
        @RequestBody dto: ChatSettingsDto
    ): ResponseEntity<Void> =
        try {
            chatService.updateSettings(authorId, dto.minSubscriptionLevel)
            ResponseEntity.noContent().build()
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(403).build()
        }
}
