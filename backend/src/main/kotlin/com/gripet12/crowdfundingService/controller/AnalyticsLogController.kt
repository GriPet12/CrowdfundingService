package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.CreatorStatsDto
import com.gripet12.crowdfundingService.service.AnalyticsLogService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/analytics")
class AnalyticsLogController(
    private val analyticsLogService: AnalyticsLogService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun safe(block: () -> Unit) {
        try { block() } catch (e: Exception) { log.debug("Analytics silenced: ${e.message}") }
    }

    @PostMapping("/project/{projectId}/click")
    fun logProjectClick(@PathVariable projectId: Long) = safe { analyticsLogService.clickOnProject(projectId) }

    @PostMapping("/project/{projectId}/view")
    fun logProjectView(@PathVariable projectId: Long) = safe { analyticsLogService.viewOnProject(projectId) }

    @PostMapping("/project/{projectId}/donate")
    fun logProjectDonate(@PathVariable projectId: Long) = safe { analyticsLogService.donateOnProject(projectId) }

    @PostMapping("/project/{projectId}/follow")
    fun logProjectFollow(@PathVariable projectId: Long) = safe { analyticsLogService.followOnProject(projectId) }

    @PostMapping("/creator/{creatorId}/click")
    fun logCreatorClick(@PathVariable creatorId: Long) = safe { analyticsLogService.clickOnCreator(creatorId) }

    @PostMapping("/creator/{creatorId}/view")
    fun logCreatorView(@PathVariable creatorId: Long) = safe { analyticsLogService.viewOnCreator(creatorId) }

    @PostMapping("/creator/{creatorId}/subscribe")
    fun logCreatorSubscribe(@PathVariable creatorId: Long) = safe { analyticsLogService.subscribeOnUser(creatorId) }

    @PostMapping("/creator/{creatorId}/donate")
    fun logCreatorDonate(@PathVariable creatorId: Long) = safe { analyticsLogService.donateOnCreator(creatorId) }

    @PostMapping("/creator/{creatorId}/follow")
    fun logCreatorFollow(@PathVariable creatorId: Long) = safe { analyticsLogService.followOnCreator(creatorId) }

    @GetMapping("/creator/{creatorId}/dashboard")
    fun getCreatorDashboard(@PathVariable creatorId: Long): ResponseEntity<CreatorStatsDto> =
        ResponseEntity.ok(analyticsLogService.getCreatorDashboard(creatorId))
}