package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.PreviewProjectDto
import com.gripet12.crowdfundingService.model.Project
import com.gripet12.crowdfundingService.repository.AnalyticsLogRepository
import com.gripet12.crowdfundingService.repository.ProjectRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecommendationService(
    private val analyticsLogRepository: AnalyticsLogRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository
) {
    private fun currentUserIdOrNull(): Long? {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated || auth.name == "anonymousUser") return null
        return userRepository.findByUsername(auth.name).orElse(null)?.userId
    }

    @Transactional(readOnly = true)
    fun getRecommendationsForCurrentUser(pageable: Pageable): Page<PreviewProjectDto> {
        val userId = currentUserIdOrNull()
        return getProjectRecommendations(userId, pageable)
    }

    @Transactional(readOnly = true)
    fun getProjectRecommendations(userId: Long?, pageable: Pageable): Page<PreviewProjectDto> {
        if (userId == null) {
            return loadPopularPage(pageable)
        }

        val user = userRepository.findById(userId).orElse(null)
            ?: return loadPopularPage(pageable)

        val userLogs = analyticsLogRepository.getAnalyticsLogsByUser(user)
        if (userLogs.isEmpty()) {
            return loadPopularPage(pageable)
        }

        val activeProjects = projectRepository
            .findActivePublic(PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "hotnessScore")))
            .content

        val categoryWeights = mutableMapOf<Long, Double>()
        for (log in userLogs) {
            val weight = when (log.actionType) {
                "DONATE"    -> 5.0
                "SUBSCRIBE" -> 4.0
                "FOLLOW"    -> 3.0
                "VIEW"      -> 2.0
                "CLICK"     -> 1.0
                else        -> 0.5
            }

            val categories: Set<com.gripet12.crowdfundingService.model.Category?> = when {
                log.project != null -> log.project.categories
                log.targetUser != null -> activeProjects
                    .filter { it.creator.userId == log.targetUser.userId }
                    .flatMap { it.categories }
                    .toSet()
                else -> emptySet()
            }
            for (category in categories) {
                category?.categoryId?.let { catId ->
                    categoryWeights[catId] = categoryWeights.getOrDefault(catId, 0.0) + weight
                }
            }
        }

        val maxHotness = activeProjects.maxOfOrNull { it.hotnessScore }?.takeIf { it > 0 } ?: 1.0
        val sorted = activeProjects.sortedByDescending { project ->
            val catScore = project.categories.sumOf { cat ->
                cat?.categoryId?.let { categoryWeights.getOrDefault(it, 0.0) } ?: 0.0
            }
            catScore * 0.7 + (project.hotnessScore / maxHotness) * 0.3
        }

        return paginateProjects(sorted, pageable)
    }

    private fun loadPopularPage(pageable: Pageable): Page<PreviewProjectDto> {
        val sortedPageable = PageRequest.of(
            pageable.pageNumber,
            pageable.pageSize,
            Sort.by(Sort.Direction.DESC, "hotnessScore")
        )
        val projectsPage = projectRepository.findActivePublic(sortedPageable)
        return PageImpl(
            projectsPage.content.map { it.toPreviewProjectDto() },
            projectsPage.pageable,
            projectsPage.totalElements
        )
    }

    private fun paginateProjects(projects: List<Project>, pageable: Pageable): Page<PreviewProjectDto> {
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, projects.size)
        val pageContent = if (start < projects.size)
            projects.subList(start, end).map { it.toPreviewProjectDto() }
        else emptyList()
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
            mainImage = mainImage?.id,
            categories = categories.map { it?.categoryName }.toSet()
        )
}
