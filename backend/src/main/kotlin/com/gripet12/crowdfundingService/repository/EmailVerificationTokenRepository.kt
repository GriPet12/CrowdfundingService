package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.EmailVerificationToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface EmailVerificationTokenRepository : JpaRepository<EmailVerificationToken, Long> {
    fun findByToken(token: String): Optional<EmailVerificationToken>
    fun findByUserUserId(userId: Long): Optional<EmailVerificationToken>
    fun deleteByUserUserId(userId: Long)
}
