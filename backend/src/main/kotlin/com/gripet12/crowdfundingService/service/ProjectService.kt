package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.MediaDto
import com.gripet12.crowdfundingService.dto.PreviewProjectDto
import com.gripet12.crowdfundingService.dto.ProjectDto
import com.gripet12.crowdfundingService.model.Project
import com.gripet12.crowdfundingService.repository.CategoryRepository
import com.gripet12.crowdfundingService.repository.FileRepository
import com.gripet12.crowdfundingService.repository.ProjectRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository,
    private val categoryRepository: CategoryRepository
) {

    @Transactional(readOnly = true)
    fun getAllProjectsForPreview(pageable: Pageable): Page<PreviewProjectDto> {
        val projectsPage = projectRepository.findAll(pageable)
        if (projectsPage.isEmpty) return projectsPage.map { it.toPreviewProjectDto() }

        val ids = projectsPage.content.mapNotNull { it.projectId }
        val withCategories = projectRepository.findAllWithCategoriesByIds(ids)
            .associateBy { it.projectId }

        return projectsPage.map { p ->
            val enriched = withCategories[p.projectId] ?: p
            enriched.toPreviewProjectDto()
        }
    }

    @Transactional(readOnly = true)
    fun getProject(id: Long): ProjectDto? =
        projectRepository.findById(id).orElse(null)?.toProjectDto()

    @Transactional
    fun saveProject(projectDto: ProjectDto) {
        projectRepository.save(projectDto.toProject())
    }

    @Transactional
    fun deleteProject(id: Long) {
        val project = projectRepository.findById(id).orElseThrow { RuntimeException("Project not found") }

        if (project.collectedAmount > 0.toBigDecimal()) {
            project.status = "CANCELLED"
            projectRepository.save(project)
        } else {
            projectRepository.deleteById(id)
        }
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
            media = media.filterNotNull().map { MediaDto(
                id = it.id,
                originalFileName = it.originalFileName,
                mimeType = it.mimeType,
                category = it.category.name,
                size = it.size,
                uploadedAt = it.uploadedAt
            ) }.toSet(),
            categories = categories.map { it?.categoryName }.toSet()
        )

    private fun ProjectDto.toProject(): Project =
        Project(
            projectId = projectId,
            creator = userRepository.findById(creator!!).orElseThrow(),
            title = title,
            goalAmount = goalAmount,
            collectedAmount = collectedAmount,
            status = status,
            description = description,
            hotnessScore = hotnessScore,
            mainImage = mainImage?.let { fileRepository.findById(it).orElse(null) },
            media = media.mapNotNull { it.id }.map { fileRepository.findById(it).orElseThrow() }.toSet(),
            categories = categories.filterNotNull().map {
                categoryRepository.findByCategoryName(it).orElseThrow()
            }.toSet()
        )

}