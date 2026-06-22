package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.HomePageDto
import com.gripet12.crowdfundingService.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HomeService(
    private val projectService: ProjectService,
    private val categoryRepository: CategoryRepository,
    private val followService: FollowService
) {
    @Transactional(readOnly = true)
    fun getHomePage(
        page: Int,
        size: Int,
        search: String?,
        categoryId: Long?,
        sortBy: String,
        sortDir: String
    ): HomePageDto {
        val projects = projectService.getProjectsPage(page, size, null, search, categoryId, sortBy, sortDir)
        val categories = categoryRepository.findAll()
        val projectIds = projects.content.mapNotNull { it.projectId }
        val followedProjectIds = if (projectIds.isEmpty()) {
            emptyList()
        } else {
            try {
                followService.getFollowedProjectIds(projectIds).toList()
            } catch (_: Exception) {
                emptyList()
            }
        }

        return HomePageDto(
            projects = projects,
            categories = categories,
            followedProjectIds = followedProjectIds
        )
    }
}
