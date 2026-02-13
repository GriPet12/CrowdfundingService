package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.AnalyticsLog
import com.gripet12.crowdfundingService.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AnalyticsLogRepository : JpaRepository<AnalyticsLog, Long> {
    fun getAnalyticsLogsByUser(user: User): MutableList<AnalyticsLog>

    @Query("SELECT a FROM AnalyticsLog a WHERE a.activityModelType = 'PROJECT' AND a.activityModelId = :projectId")
    fun getAnalyticsLogsByProject(projectId: Long): List<AnalyticsLog>

    @Query("SELECT a FROM AnalyticsLog a WHERE a.activityModelType = 'CREATOR' AND a.activityModelId = :creatorId")
    fun getAnalyticsLogsByCreator(creatorId: Long): List<AnalyticsLog>
}