package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.PreviewProjectDto
import com.gripet12.crowdfundingService.model.Project
import com.gripet12.crowdfundingService.repository.ProjectRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecommendationService(
    private val projectRepository: ProjectRepository
) {
    @Transactional(readOnly = true)
    fun getRecommendationsForCurrentUser(pageable: Pageable): Page<PreviewProjectDto> =
        loadPopularPage(pageable)

    @Transactional(readOnly = true)
    fun getProjectRecommendations(userId: Long?, pageable: Pageable): Page<PreviewProjectDto> =
        loadPopularPage(pageable)

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
