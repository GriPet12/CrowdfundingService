package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.PasswordResetToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
    fun findByToken(token: String): Optional<PasswordResetToken>
    fun findByUserUserId(userId: Long): Optional<PasswordResetToken>
    fun deleteByUserUserId(userId: Long)
}
