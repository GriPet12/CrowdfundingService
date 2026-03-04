package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.model.EmailVerificationToken
import com.gripet12.crowdfundingService.model.User
import com.gripet12.crowdfundingService.repository.EmailVerificationTokenRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val tokenRepository: EmailVerificationTokenRepository,
    private val userRepository: UserRepository,
    @Value("\${frontend.url:http://localhost:5173}") private val frontendUrl: String
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun sendVerificationEmail(user: User) {

        tokenRepository.deleteByUserUserId(user.userId!!)

        val token = UUID.randomUUID().toString()
        tokenRepository.save(
            EmailVerificationToken(token = token, user = user)
        )

        try {
            val message = SimpleMailMessage()
            message.setTo(user.email)
            message.subject = "Підтвердіть вашу електронну пошту — Crowdfunding"
            message.text = """
                Вітаємо, ${user.username}!

                Для підтвердження вашої електронної адреси перейдіть за посиланням:
                $frontendUrl/verify-email?token=$token&uid=${user.userId}

                Посилання дійсне протягом 24 годин.

                Якщо ви не реєструвалися на нашому сайті — проігноруйте цей лист.
            """.trimIndent()

            mailSender.send(message)
        } catch (e: Exception) {
            log.error("Failed to send verification email to ${user.email}: ${e.message}", e)
        }
    }

    @Transactional
    fun verifyEmail(token: String, userId: Long?): Boolean {
        val verificationToken = tokenRepository.findByToken(token).orElse(null)

        if (verificationToken == null) {
            if (userId != null) {
                val user = userRepository.findById(userId).orElse(null)
                if (user != null && user.isVerified) return true
            }
            throw IllegalArgumentException("INVALID_TOKEN")
        }

        if (verificationToken.expiresAt.isBefore(LocalDateTime.now())) {
            tokenRepository.delete(verificationToken)
            throw IllegalArgumentException("TOKEN_EXPIRED")
        }

        val user = verificationToken.user

        if (user.isVerified) {
            tokenRepository.delete(verificationToken)
            return true
        }

        userRepository.save(user.copy(isVerified = true))
        tokenRepository.delete(verificationToken)
        return true
    }

    @Transactional
    fun resendVerificationEmail(email: String) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("USER_NOT_FOUND") }

        if (user.isVerified) throw IllegalArgumentException("ALREADY_VERIFIED")

        tokenRepository.findByUserUserId(user.userId!!)
            .ifPresent { existing ->
                val secondsSinceLastSend = java.time.Duration.between(existing.createdAt, LocalDateTime.now()).seconds
                if (secondsSinceLastSend < 60) {
                    val waitSeconds = 60 - secondsSinceLastSend
                    throw IllegalArgumentException("RESEND_TOO_SOON:$waitSeconds")
                }
            }

        sendVerificationEmail(user)
    }
}
