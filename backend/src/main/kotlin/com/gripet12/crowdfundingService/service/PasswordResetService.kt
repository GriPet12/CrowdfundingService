package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.model.PasswordResetToken
import com.gripet12.crowdfundingService.repository.PasswordResetTokenRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Service
class PasswordResetService(
    private val mailSender: JavaMailSender,
    private val tokenRepository: PasswordResetTokenRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${frontend.url:http://localhost:5173}") private val frontendUrl: String
) {
    private val log = LoggerFactory.getLogger(PasswordResetService::class.java)

    @Transactional
    fun requestPasswordReset(email: String) {
        val user = userRepository.findByEmail(email.trim()).orElse(null) ?: return

        tokenRepository.findByUserUserId(user.userId!!).ifPresent { existing ->
            val secondsSinceLastSend = Duration.between(existing.createdAt, LocalDateTime.now()).seconds
            if (secondsSinceLastSend < 60) return
        }

        sendResetEmail(user)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun sendResetEmail(user: com.gripet12.crowdfundingService.model.User) {
        tokenRepository.deleteByUserUserId(user.userId!!)

        val token = UUID.randomUUID().toString()
        tokenRepository.save(PasswordResetToken(token = token, user = user))

        try {
            val message = SimpleMailMessage()
            message.setTo(user.email)
            message.subject = "Відновлення пароля — Crowdfunding"
            message.text = """
                Вітаємо, ${user.username}!

                Щоб встановити новий пароль, перейдіть за посиланням:
                $frontendUrl/reset-password?token=$token

                Посилання дійсне протягом 1 години.

                Якщо ви не запитували відновлення пароля — проігноруйте цей лист.
            """.trimIndent()
            mailSender.send(message)
        } catch (e: Exception) {
            log.error("Failed to send password reset email to ${user.email}: ${e.message}", e)
        }
    }

    @Transactional
    fun resetPassword(token: String, newPassword: String) {
        val resetToken = tokenRepository.findByToken(token).orElseThrow {
            IllegalArgumentException("INVALID_TOKEN")
        }

        if (resetToken.expiresAt.isBefore(LocalDateTime.now())) {
            tokenRepository.delete(resetToken)
            throw IllegalArgumentException("TOKEN_EXPIRED")
        }

        val user = resetToken.user
        userRepository.save(user.copy(password = passwordEncoder.encode(newPassword)))
        tokenRepository.delete(resetToken)
        tokenRepository.deleteByUserUserId(user.userId!!)
    }
}
