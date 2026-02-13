package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.AnalyticsLogDto
import com.gripet12.crowdfundingService.model.AnalyticsLog
import com.gripet12.crowdfundingService.repository.AnalyticsLogRepository
import com.gripet12.crowdfundingService.repository.CategoryRepository
import com.gripet12.crowdfundingService.repository.ProjectRepository
import com.gripet12.crowdfundingService.repository.UserRepository

class AnalyticsLogService (
    private val analyticsLogRepository: AnalyticsLogRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val categoryRepository: CategoryRepository
) {
    fun clickOnProject(projectId: Long, userId: Long) {
        val log = AnalyticsLog(
            logId = 0,
            user = userRepository.findById(userId).orElseThrow(),
            action = "CLICK",
            activityModelType = "PROJECT",
            activityModelId = projectId,
            categories = projectRepository.findByProjectId(projectId).categories
        )
        analyticsLogRepository.save(log)
    }

    fun clickOnCreator(creatorId: Long, userId: Long) {
        val log = AnalyticsLog(
            logId = 0,
            user = userRepository.findById(userId).orElseThrow(),
            action = "CLICK",
            activityModelType = "CREATOR",
            activityModelId = creatorId,
            categories = emptySet()
        )
        analyticsLogRepository.save(log)
    }

    fun viewOnProject(projectId: Long, userId: Long) {
        val log = AnalyticsLog(
            logId = 0,
            user = userRepository.findById(userId).orElseThrow(),
            action = "VIEW",
            activityModelType = "PROJECT",
            activityModelId = projectId,
            categories = projectRepository.findByProjectId(projectId).categories
        )
        analyticsLogRepository.save(log)
    }

    fun donateOnProject(projectId: Long, userId: Long) {
        val log = AnalyticsLog(
            logId = 0,
            user = userRepository.findById(userId).orElseThrow(),
            action = "DONATE",
            activityModelType = "PROJECT",
            activityModelId = projectId,
            categories = projectRepository.findByProjectId(projectId).categories
        )
        analyticsLogRepository.save(log)
    }

    fun subscribeOnUser(creatorId: Long, userId: Long) {
        val log = AnalyticsLog(
            logId = 0,
            user = userRepository.findById(userId).orElseThrow(),
            action = "SUBSCRIBE",
            activityModelType = "CREATOR",
            activityModelId = creatorId,
            categories = emptySet()
        )
        analyticsLogRepository.save(log)
    }

    fun getUserStats(userId: Long) : List<AnalyticsLogDto> {
        val user = userRepository.findById(userId).orElseThrow()
        return analyticsLogRepository.getAnalyticsLogsByUser(user).map {modelToDto(it)}.toList()
    }

    fun getStatsOnProject(projectId: Long) : List<AnalyticsLogDto> =
        analyticsLogRepository.getAnalyticsLogsByProject(projectId).map {modelToDto(it)}.toList()

    fun getStatsOnCreator(creatorId: Long) : List<AnalyticsLogDto> =
        analyticsLogRepository.getAnalyticsLogsByCreator(creatorId).map {modelToDto(it)}.toList()


    fun modelToDto(analyticsLog: AnalyticsLog) =
        AnalyticsLogDto(
            logId = analyticsLog.logId,
            user = analyticsLog.user.userId,
            action = analyticsLog.action,
            activityModelType = analyticsLog.activityModelType,
            activityModelId = analyticsLog.activityModelId,
            categories = analyticsLog.categories.mapNotNull { it?.categoryId }.toSet()
        )


}