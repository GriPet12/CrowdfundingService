package com.gripet12.crowdfundingService.config

import com.gripet12.crowdfundingService.security.JwtTokenProvider
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val jwtTokenProvider: JwtTokenProvider
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {

        registry.enableSimpleBroker("/topic")

        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS()
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(object : ChannelInterceptor {
            override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
                val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
                if (accessor?.command == StompCommand.CONNECT) {
                    val authHeader = accessor.getFirstNativeHeader("Authorization")
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        val token = authHeader.substring(7)
                        if (jwtTokenProvider.validateToken(token)) {
                            val auth = jwtTokenProvider.getAuthentication(token)
                            accessor.user = auth
                            SecurityContextHolder.getContext().authentication = auth as? UsernamePasswordAuthenticationToken
                        }
                    }
                }
                return message
            }
        })
    }
}
