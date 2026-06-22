package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.DonationDto
import com.gripet12.crowdfundingService.repository.DonateRepository
import com.gripet12.crowdfundingService.repository.RewardRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.sql.Timestamp

@RestController
@RequestMapping("/donations")
class DonationController(
    private val donateRepository: DonateRepository,
    private val userRepository: UserRepository,
    private val rewardRepository: RewardRepository
) {

    @GetMapping("/my")
    fun getMyDonations(): ResponseEntity<List<DonationDto>> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        val rows = donateRepository.findAllByDonorUserId(userId)
        return ResponseEntity.ok(rows.map { mapSentDonation(it) })
    }

    @GetMapping("/received")
    fun getReceivedDonations(): ResponseEntity<List<DonationDto>> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        val rows = donateRepository.findAllByCreatorUserId(userId)
        return ResponseEntity.ok(rows.map { mapReceivedDonation(it) })
    }

    private fun currentUserId(): Long? {
        val username = SecurityContextHolder.getContext().authentication?.name ?: return null
        return userRepository.findUserIdByUsername(username)
    }

    private fun mapSentDonation(row: Array<Any?>): DonationDto {
        val donationId = row[0] as? Long
        val projectTitle = (row[1] as? String)?.takeIf { it.isNotBlank() }
        val creatorName = (row[2] as? String)?.takeIf { it.isNotBlank() }
        val rewardId = (row[3] as? Number)?.toInt() ?: 0
        val amount = row[4] as BigDecimal
        val paymentStatus = row[5] as? String
        val createdAt = row[6] as? Timestamp

        return DonationDto(
            donationId = donationId,
            projectTitle = projectTitle,
            creatorName = creatorName,
            rewardName = rewardName(rewardId),
            amount = amount,
            paymentStatus = paymentStatus,
            createdAt = createdAt
        )
    }

    private fun mapReceivedDonation(row: Array<Any?>): DonationDto {
        val donationId = row[0] as? Long
        val projectTitle = (row[1] as? String)?.takeIf { it.isNotBlank() }
        val donorName = (row[2] as? String)?.takeIf { it.isNotBlank() }
        val rewardId = (row[3] as? Number)?.toInt() ?: 0
        val amount = row[4] as BigDecimal
        val paymentStatus = row[5] as? String
        val createdAt = row[6] as? Timestamp
        val anonymous = row[7] as? Boolean ?: false

        return DonationDto(
            donationId = donationId,
            projectTitle = projectTitle,
            creatorName = null,
            donorName = if (anonymous) "Анонім" else donorName,
            rewardName = rewardName(rewardId),
            amount = amount,
            paymentStatus = paymentStatus,
            createdAt = createdAt,
            anonymous = anonymous
        )
    }

    private fun rewardName(rewardId: Int): String? {
        if (rewardId <= 0) return null
        return rewardRepository.findById(rewardId.toLong()).map { it.rewardName }.orElse(null)
    }
}
