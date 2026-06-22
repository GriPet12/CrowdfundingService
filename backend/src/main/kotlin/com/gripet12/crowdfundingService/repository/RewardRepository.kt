package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Reward
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RewardRepository : JpaRepository<Reward, Long> {
    fun save(reward: Reward): Reward

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Reward r WHERE r.project.projectId = :projectId")
    fun deleteByProjectProjectId(@Param("projectId") projectId: Long)
}