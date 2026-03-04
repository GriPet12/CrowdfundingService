package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.model.EmailVerificationToken
import com.gripet12.crowdfundingService.model.User
import com.gripet12.crowdfundingService.repository.EmailVerificationTokenRepository
import com.gripet12.crowdfundingService.repository.UserRepository
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
                $frontendUrl/verify-email?token=$token

                Посилання дійсне протягом 24 годин.

                Якщо ви не реєструвалися на нашому сайті — проігноруйте цей лист.
            """.trimIndent()

            mailSender.send(message)
        } catch (e: Exception) {

        }
    }

    @Transactional
    fun verifyEmail(token: String): Boolean {
        val verificationToken = tokenRepository.findByToken(token)
            .orElseThrow { IllegalArgumentException("INVALID_TOKEN") }

        if (verificationToken.expiresAt.isBefore(LocalDateTime.now())) {
            tokenRepository.delete(verificationToken)
            throw IllegalArgumentException("TOKEN_EXPIRED")
        }

        val user = verificationToken.user

        val verifiedUser = User(
            userId = user.userId,
            username = user.username,
            password = user.password,
            email = user.email,
            isVerified = true,
            image = user.image,
            roles = user.roles,
            banned = user.banned,
            createdAt = user.createdAt
        )
        userRepository.save(verifiedUser)
        tokenRepository.delete(verificationToken)
        return true
    }

    @Transactional
    fun resendVerificationEmail(email: String) {
        val user = userRepository.findAll()
            .find { it.email == email }
            ?: throw IllegalArgumentException("USER_NOT_FOUND")

        if (user.isVerified) throw IllegalArgumentException("ALREADY_VERIFIED")

        sendVerificationEmail(user)
    }
}
