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
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).build()

        val userId = userRepository.findUserIdByUsername(username)
            ?: return ResponseEntity.status(401).build()

        val rows = donateRepository.findAllByDonorUserId(userId)

        val dtos = rows.map { row ->
            val donationId  = (row[0] as? Long)
            val projectTitle = row[1] as? String
            val creatorName  = row[2] as? String
            val rewardId     = (row[3] as? Int) ?: 0
            val amount       = row[4] as BigDecimal
            val paymentStatus = row[5] as? String
            val createdAt    = row[6] as? Timestamp

            val rewardName: String? = if (rewardId > 0) {
                rewardRepository.findById(rewardId.toLong()).map { it.rewardName }.orElse(null)
            } else null

            DonationDto(
                donationId    = donationId,
                projectTitle  = projectTitle,
                creatorName   = creatorName,
                rewardName    = rewardName,
                amount        = amount,
                paymentStatus = paymentStatus,
                createdAt     = createdAt
            )
        }

        return ResponseEntity.ok(dtos)
    }
}
