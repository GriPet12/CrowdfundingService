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

        val user = userRepository.findByUsername(username).orElse(null)
            ?: return ResponseEntity.status(401).build()

        val donations = donateRepository.findAllByDonorUserId(user.userId!!)

        val dtos = donations.map { donate ->
            val rewardName: String? = if (donate.reward > 0) {
                rewardRepository.findById(donate.reward.toLong())
                    .map { it.rewardName }
                    .orElse(null)
            } else null

            val creatorName: String? = if (donate.project == null) {

                donate.creator?.username
            } else null

            DonationDto(
                donationId = donate.donateId,
                projectTitle = donate.project?.title,
                creatorName = donate.project?.creator?.username ?: creatorName,
                rewardName = rewardName,
                amount = donate.amount,
                paymentStatus = donate.payment?.status,
                createdAt = donate.createAt
            )
        }

        return ResponseEntity.ok(dtos)
    }
}
