package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.PreviewProjectDto
import com.gripet12.crowdfundingService.model.Project
import com.gripet12.crowdfundingService.repository.AnalyticsLogRepository
import com.gripet12.crowdfundingService.repository.ProjectRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class RecommendationService(
    private val analyticsLogRepository: AnalyticsLogRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository
) {
    fun getProjectRecommendations(userId: Long, pageable: Pageable): Page<PreviewProjectDto> {
        val user = userRepository.findById(userId).orElse(null)

        val allProjects = projectRepository.findAll()

        if (user == null) {
            return paginateProjects(allProjects, pageable)
        }

        val userLogs = analyticsLogRepository.getAnalyticsLogsByUser(user)

        if (userLogs.isEmpty()) {
            return paginateProjects(allProjects, pageable)
        }

        val categoryWeights = mutableMapOf<Long, Double>()

        for (log in userLogs) {
            val weight = when (log.action) {
                "DONATE" -> 5.0
                "SUBSCRIBE" -> 4.0
                "VIEW" -> 2.0
                "CLICK" -> 1.0
                else -> 0.5
            }

            for (category in log.categories) {
                category?.categoryId?.let { catId ->
                    categoryWeights[catId] = categoryWeights.getOrDefault(catId, 0.0) + weight
                }
            }
        }

        val sortedProjects = allProjects.sortedByDescending { project ->
            calculateProjectScore(project, categoryWeights)
        }

        return paginateProjects(sortedProjects, pageable)
    }

    private fun calculateProjectScore(project: Project, categoryWeights: Map<Long, Double>): Double {
        if (categoryWeights.isEmpty()) return 0.0

        var score = 0.0
        for (category in project.categories) {
            category?.categoryId?.let { catId ->
                score += categoryWeights.getOrDefault(catId, 0.0)
            }
        }
        return score
    }


    private fun paginateProjects(projects: List<Project>, pageable: Pageable): Page<PreviewProjectDto> {
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, projects.size)

        val pageContent = if (start < projects.size) {
            projects.subList(start, end).map { it.toPreviewProjectDto() }
        } else {
            emptyList()
        }

        return PageImpl(pageContent, pageable, projects.size.toLong())
    }

    private fun Project.toPreviewProjectDto(): PreviewProjectDto =
        PreviewProjectDto(
            projectId = projectId,
            creatorId = creator.userId,
            title = title,
            goalAmount = goalAmount,
            collectedAmount = collectedAmount,
            status = status,
            hotnessScore = hotnessScore,
            mainImage = mainImage?.id
        )
}