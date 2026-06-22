package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.CreateProjectDto
import com.gripet12.crowdfundingService.dto.MediaDto
import com.gripet12.crowdfundingService.dto.PageResponseDto
import com.gripet12.crowdfundingService.dto.PreviewProjectDto
import com.gripet12.crowdfundingService.dto.ProjectDto
import com.gripet12.crowdfundingService.model.Project
import com.gripet12.crowdfundingService.repository.AnalyticsLogRepository
import com.gripet12.crowdfundingService.repository.CategoryRepository
import com.gripet12.crowdfundingService.repository.DonateRepository
import com.gripet12.crowdfundingService.repository.FileRepository
import com.gripet12.crowdfundingService.repository.ProjectFollowRepository
import com.gripet12.crowdfundingService.repository.ProjectRepository
import com.gripet12.crowdfundingService.repository.RewardRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import com.gripet12.crowdfundingService.util.searchPattern
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository,
    private val categoryRepository: CategoryRepository,
    private val analyticsLogRepository: AnalyticsLogRepository,
    private val projectFollowRepository: ProjectFollowRepository,
    private val rewardRepository: RewardRepository,
    private val donateRepository: DonateRepository
) {

    private fun currentUserId(): Long {
        val username = SecurityContextHolder.getContext().authentication.name
        return userRepository.findByUsername(username)
            .orElseThrow { IllegalStateException("User not found") }
            .userId!!
    }

    @Transactional(readOnly = true)
    fun getProjectsPage(
        page: Int,
        size: Int,
        creatorId: Long?,
        search: String? = null,
        categoryId: Long? = null,
        sortBy: String = "hotnessScore",
        sortDir: String = "desc"
    ): PageResponseDto<PreviewProjectDto> {
        val allowedSorts = setOf("hotnessScore", "collectedAmount", "title", "createdAt")
        val safeSort = if (sortBy in allowedSorts) sortBy else "hotnessScore"
        val direction = if (sortDir.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(direction, safeSort))

        // Both owner and visitors see only ACTIVE projects (non-PENDING, non-REJECTED).
        // PENDING/REJECTED are accessible only via the dedicated moderation tab.
        val projectsPage = when {
            creatorId != null ->
                projectRepository.findByCreatorUserIdAndStatusNotInPageable(
                    creatorId, listOf("PENDING", "REJECTED"), pageable
                )
            search != null || categoryId != null ->
                projectRepository.findByFilters(searchPattern(search), categoryId, 0, pageable)
            else ->
                projectRepository.findByStatusNotIn(listOf("PENDING", "REJECTED"), pageable)
        }

        return toPageResponse(projectsPage)
    }

    @Transactional(readOnly = true)
    fun getMyPendingProjects(page: Int, size: Int): PageResponseDto<PreviewProjectDto> {
        val userId = currentUserId()
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return toPageResponse(projectRepository.findByCreatorUserIdAndStatus(userId, "PENDING", pageable))
    }

    @Transactional(readOnly = true)
    fun getMyRejectedProjects(page: Int, size: Int): PageResponseDto<PreviewProjectDto> {
        val userId = currentUserId()
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return toPageResponse(projectRepository.findByCreatorUserIdAndStatus(userId, "REJECTED", pageable))
    }

    private fun toPageResponse(projectsPage: Page<Project>): PageResponseDto<PreviewProjectDto> {
        val ids = projectsPage.content.mapNotNull { it.projectId }
        val withCategories = if (ids.isNotEmpty())
            projectRepository.findAllWithCategoriesByIds(ids).associateBy { it.projectId }
        else emptyMap()
        val content = projectsPage.content.map { p ->
            (withCategories[p.projectId] ?: p).toPreviewProjectDto()
        }
        return PageResponseDto(
            content = content,
            totalElements = projectsPage.totalElements,
            totalPages = projectsPage.totalPages,
            currentPage = projectsPage.number,
            size = projectsPage.size
        )
    }

    @Transactional(readOnly = true)
    fun getProject(id: Long): ProjectDto? =
        projectRepository.findById(id).orElse(null)?.toProjectDto()

    @Transactional
    fun createProject(dto: CreateProjectDto): ProjectDto {
        val creatorId = currentUserId()
        val creator = userRepository.findById(creatorId).orElseThrow()
        val mainImage = fileRepository.findById(dto.mainImage)
            .orElseThrow { IllegalArgumentException("Main image not found") }
        val media = dto.mediaIds.mapNotNull { fileRepository.findById(it).orElse(null) }.toMutableSet()
        val cats = dto.categories.mapNotNull { categoryRepository.findByCategoryName(it).orElse(null) }.toMutableSet()

        val project = Project(
            creator = creator,
            title = dto.title,
            description = dto.description,
            goalAmount = dto.goalAmount,
            collectedAmount = java.math.BigDecimal.ZERO,
            status = "PENDING",
            mainImage = mainImage,
            media = media,
            categories = cats
        )
        return projectRepository.save(project).toProjectDto()
    }

    @Transactional
    fun updateProject(id: Long, dto: CreateProjectDto): ProjectDto {
        val project = projectRepository.findById(id)
            .orElseThrow { RuntimeException("Project not found") }

        val requesterId = currentUserId()
        if (project.creator.userId != requesterId) throw IllegalAccessException("Not allowed")

        val newMainImage = fileRepository.findById(dto.mainImage)
            .orElseThrow { IllegalArgumentException("Main image not found") }
        val newMedia = dto.mediaIds.mapNotNull { fileRepository.findById(it).orElse(null) }.toMutableSet()
        val newCats = dto.categories.mapNotNull { categoryRepository.findByCategoryName(it).orElse(null) }.toMutableSet()

        // If the project was REJECTED, reset to PENDING so it goes back for admin re-review
        val newStatus = if (project.status == "REJECTED") "PENDING" else project.status

        val updated = project.copy(
            title = dto.title,
            description = dto.description,
            goalAmount = dto.goalAmount,
            mainImage = newMainImage,
            media = newMedia,
            categories = newCats,
            status = newStatus
        )
        return projectRepository.save(updated).toProjectDto()
    }

    fun canDelete(id: Long): Map<String, Any> {
        val project = projectRepository.findById(id)
            .orElseThrow { RuntimeException("Project not found") }
        val hasDonations = project.collectedAmount > java.math.BigDecimal.ZERO
        return mapOf("canDelete" to !hasDonations, "hasDonations" to hasDonations)
    }

    @Transactional
    fun deleteProject(id: Long) {
        val project = projectRepository.findById(id)
            .orElseThrow { NoSuchElementException("Project not found") }
        if (project.collectedAmount > java.math.BigDecimal.ZERO) {
            project.status = "CANCELLED"
            projectRepository.save(project)
            return
        }
        purgeProjectDependencies(id)
        projectRepository.deleteById(id)
    }

    private fun purgeProjectDependencies(projectId: Long) {
        analyticsLogRepository.deleteByProjectProjectId(projectId)
        projectFollowRepository.deleteByProjectProjectId(projectId)
        rewardRepository.deleteByProjectProjectId(projectId)
        donateRepository.deleteByProjectProjectId(projectId)
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
            categories = categories.map { it.categoryName }.toSet()
        )

    private fun Project.toProjectDto(): ProjectDto =
        ProjectDto(
            projectId = projectId,
            creator = creator.userId,
            title = title,
            goalAmount = goalAmount,
            collectedAmount = collectedAmount,
            status = status,
            description = description,
            hotnessScore = hotnessScore,
            mainImage = mainImage?.id,
            media = media.map {
                MediaDto(
                    id = it.id,
                    originalFileName = it.originalFileName,
                    mimeType = it.mimeType,
                    category = it.category.name,
                    size = it.size,
                    uploadedAt = it.uploadedAt
                )
            }.toSet(),
            categories = categories.map { it.categoryName }.toSet()
        )

}