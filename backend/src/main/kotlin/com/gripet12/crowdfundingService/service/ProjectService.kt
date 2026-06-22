package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.CreateProjectDto
import com.gripet12.crowdfundingService.dto.MediaDto
import com.gripet12.crowdfundingService.dto.PageResponseDto
import com.gripet12.crowdfundingService.dto.PreviewProjectDto
import com.gripet12.crowdfundingService.dto.ProjectDonorDto
import com.gripet12.crowdfundingService.dto.ProjectDto
import com.gripet12.crowdfundingService.model.Project
import com.gripet12.crowdfundingService.repository.AnalyticsLogRepository
import org.hibernate.Hibernate
import com.gripet12.crowdfundingService.repository.CategoryRepository
import com.gripet12.crowdfundingService.repository.DonateRepository
import com.gripet12.crowdfundingService.repository.FileRepository
import com.gripet12.crowdfundingService.repository.ProjectFollowRepository
import com.gripet12.crowdfundingService.repository.ProjectRepository
import com.gripet12.crowdfundingService.repository.RewardRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import com.gripet12.crowdfundingService.util.searchPattern
import org.springframework.context.annotation.Lazy
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
    private val donateRepository: DonateRepository,
    @Lazy private val balanceService: BalanceService
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

        // Public listings show only approved (ACTIVE) projects.
        val projectsPage = when {
            creatorId != null ->
                projectRepository.findByCreatorUserIdAndStatus(creatorId, "ACTIVE", pageable)
            search != null || categoryId != null ->
                projectRepository.findByFilters(searchPattern(search), categoryId, 0, pageable)
            else ->
                projectRepository.findActivePublic(pageable)
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
        val projects = projectsPage.content
        val needsCategoryFetch = projects.any { !Hibernate.isInitialized(it.categories) }
        val byId = if (needsCategoryFetch) {
            val ids = projects.mapNotNull { it.projectId }
            if (ids.isEmpty()) emptyMap()
            else projectRepository.findAllWithCategoriesByIds(ids).associateBy { it.projectId }
        } else {
            emptyMap()
        }
        val content = projects.map { project ->
            (byId[project.projectId] ?: project).toPreviewProjectDto()
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
    fun getProject(id: Long): ProjectDto? {
        val project = projectRepository.findById(id).orElse(null) ?: return null
        if (project.status == "ACTIVE" && !project.banned) return project.toProjectDto()

        val auth = SecurityContextHolder.getContext().authentication
        if (auth != null && auth.isAuthenticated && auth.name != "anonymousUser") {
            val user = userRepository.findByUsername(auth.name).orElse(null)
            if (user?.userId == project.creator.userId) return project.toProjectDto()
            if (auth.authorities.any { it.authority == "ROLE_ADMIN" }) return project.toProjectDto()
        }
        return null
    }

    @Transactional(readOnly = true)
    fun getProjectDonors(projectId: Long): List<ProjectDonorDto> {
        val project = projectRepository.findById(projectId)
            .orElseThrow { NoSuchElementException("Project not found") }
        if (!project.fundraisingClosed) {
            throw IllegalStateException("Список меценатів доступний лише після закриття збору")
        }

        val donations = donateRepository.findApprovedByProjectId(projectId)
        if (donations.isEmpty()) return emptyList()

        val identified = donations
            .filter { !it.isAnonymous && it.donor?.userId != null }
            .groupBy { it.donor!!.userId!! }
            .map { (_, list) ->
                val donor = list.first().donor!!
                ProjectDonorDto(
                    donorId = donor.userId,
                    username = donor.username,
                    imageId = donor.image?.id,
                    totalAmount = list.fold(java.math.BigDecimal.ZERO) { acc, d -> acc.add(d.amount) },
                    donationsCount = list.size,
                    lastDonatedAt = list.maxOf { it.createAt },
                    anonymous = false
                )
            }
            .sortedByDescending { it.totalAmount }

        val anonymousDonations = donations.filter { it.isAnonymous || it.donor == null }
        val anonymous = if (anonymousDonations.isEmpty()) {
            emptyList()
        } else {
            listOf(
                ProjectDonorDto(
                    donorId = null,
                    username = "Анонім",
                    imageId = null,
                    totalAmount = anonymousDonations.fold(java.math.BigDecimal.ZERO) { acc, d -> acc.add(d.amount) },
                    donationsCount = anonymousDonations.size,
                    lastDonatedAt = anonymousDonations.maxOf { it.createAt },
                    anonymous = true
                )
            )
        }

        return identified + anonymous
    }

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

    @Transactional
    fun closeFundraising(projectId: Long): ProjectDto {
        val project = projectRepository.findById(projectId)
            .orElseThrow { NoSuchElementException("Project not found") }
        if (project.creator.userId != currentUserId()) {
            throw IllegalAccessException("Not allowed")
        }
        if (project.status != "ACTIVE") {
            throw IllegalStateException("Закрити збір можна лише для активного проєкту")
        }
        if (project.fundraisingClosed) {
            throw IllegalStateException("Збір для цього проєкту уже закрито")
        }
        project.fundraisingClosed = true
        projectRepository.save(project)
        balanceService.unfreezeProjectFunds(projectId)
        balanceService.syncProjectFrozenEntries(project.creator.userId!!)
        return projectRepository.findById(projectId)
            .orElseThrow { NoSuchElementException("Project not found") }
            .toProjectDto()
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
            fundraisingClosed = fundraisingClosed,
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
            fundraisingClosed = fundraisingClosed,
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