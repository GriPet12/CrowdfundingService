package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Reward
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RewardRepository : JpaRepository<Reward, Long> {
    fun save(reward: Reward): Reward
}