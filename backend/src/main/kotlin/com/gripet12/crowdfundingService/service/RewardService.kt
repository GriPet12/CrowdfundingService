package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.RewardDto
import com.gripet12.crowdfundingService.model.Reward
import com.gripet12.crowdfundingService.repository.ProjectRepository
import com.gripet12.crowdfundingService.repository.RewardRepository
import org.springframework.stereotype.Service

@Service
class RewardService (
    private val rewardRepository: RewardRepository,
    private val projectRepository: ProjectRepository
) {
    fun getAllRewardsFromProject(projectId: Long): List<RewardDto> =
        rewardRepository.findAll()
            .filter { it.project?.projectId == projectId }
            .map { toDto(it) }

    fun addNewReward(reward: RewardDto): Long =
        rewardRepository.save(
            Reward(
                rewardId = 0,
                project = null,
                rewardName = reward.rewardName,
                rewardDescription = reward.rewardDescription,
                minimalAmount = reward.minimalAmount,
                rewardTier = reward.rewardTier,
                isHaveQuantity = reward.isHaveQuantity,
                quantityAvailable = reward.quantityAvailable,
                quantityClaimed = reward.quantityClaimed
            )
        ).rewardId

    fun editReward(rewardId: Long, reward: RewardDto) {
        val rewardToUpdate = rewardRepository.findById(rewardId).orElseThrow { RuntimeException("Reward not found") }
        rewardToUpdate.rewardName = reward.rewardName
        rewardToUpdate.rewardDescription = reward.rewardDescription
        rewardToUpdate.minimalAmount = reward.minimalAmount
        rewardToUpdate.rewardTier = reward.rewardTier
        rewardToUpdate.isHaveQuantity = reward.isHaveQuantity
        rewardToUpdate.quantityAvailable = reward.quantityAvailable
    }

    fun deleteReward(rewardId: Long) {
        rewardRepository.deleteById(rewardId)
    }

    fun addRewardToProject(rewardId: Long, projectId: Long) {
        val project = projectRepository.findById(projectId)
        val reward = rewardRepository.findById(rewardId)

        if (project.isPresent && reward.isPresent) {
            reward.get().project = project.get()
            rewardRepository.save(reward.get())
        } else {
            throw RuntimeException("Project or reward not found")
        }
    }

    fun deleteProjectFromReward(rewardId: Long) {
        val reward = rewardRepository.findById(rewardId).orElseThrow { RuntimeException("Reward not found") }
        reward.project = null
        rewardRepository.save(reward)
    }

    fun toDto(reward: Reward): RewardDto = RewardDto(
        rewardId = reward.rewardId,
        rewardName = reward.rewardName,
        rewardDescription = reward.rewardDescription,
        minimalAmount = reward.minimalAmount,
        rewardTier = reward.rewardTier,
        isHaveQuantity = reward.isHaveQuantity,
        quantityAvailable = reward.quantityAvailable,
        quantityClaimed = reward.quantityClaimed
    )
}