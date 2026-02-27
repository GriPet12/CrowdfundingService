package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.SubscriptionTierDto
import com.gripet12.crowdfundingService.model.SubscriptionTier
import com.gripet12.crowdfundingService.repository.SubscriptionTierRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SubscriptionTierService(
    private val tierRepository: SubscriptionTierRepository,
    private val userRepository: UserRepository
) {

    private fun currentUserId(): Long {
        val username = SecurityContextHolder.getContext().authentication.name
        return userRepository.findByUsername(username)
            .orElseThrow { IllegalStateException("User not found") }
            .userId!!
    }

    @Transactional(readOnly = true)
    fun getTiersByCreator(creatorId: Long): List<SubscriptionTierDto> =
        tierRepository.findByCreatorId(creatorId).map { it.toDto() }

    @Transactional
    fun createTier(dto: SubscriptionTierDto): SubscriptionTierDto {
        val creatorId = currentUserId()
        val tier = SubscriptionTier(
            tierId = null,
            creatorId = creatorId,
            name = dto.name,
            description = dto.description,
            amount = dto.amount,
            level = dto.level
        )
        return tierRepository.save(tier).toDto()
    }

    @Transactional
    fun deleteTier(tierId: Long) {
        val creatorId = currentUserId()
        val tier = tierRepository.findByTierIdAndCreatorId(tierId, creatorId)
            ?: throw IllegalStateException("Tier not found or access denied")
        tierRepository.delete(tier)
    }

    private fun SubscriptionTier.toDto() = SubscriptionTierDto(
        tierId = tierId,
        creatorId = creatorId,
        name = name,
        description = description,
        amount = amount,
        level = level
    )
}
