package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.PreviewProjectDto
import com.gripet12.crowdfundingService.dto.ProjectDto
import com.gripet12.crowdfundingService.model.Project
import com.gripet12.crowdfundingService.repository.CategoryRepository
import com.gripet12.crowdfundingService.repository.ImageRepository
import com.gripet12.crowdfundingService.repository.ProjectRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import com.gripet12.crowdfundingService.repository.VideoRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val imageRepository: ImageRepository,
    private val videoRepository: VideoRepository,
    private val categoryRepository: CategoryRepository
) {

    fun getAllProjectsForPreview(pageable: Pageable): Page<PreviewProjectDto> {
        val projectsPage = projectRepository.findAll(pageable)
        return projectsPage.map { it.toPreviewProjectDto() }
    }

    fun getProject(id: Long): ProjectDto? =
        projectRepository.findById(id).orElse(null).toProjectDto()

    fun saveProject(projectDto: ProjectDto) {
        projectRepository.save(projectDto.toProject())
    }

    fun deleteProject(id: Long) {
        val project = projectRepository.findById(id).orElseThrow { RuntimeException("Project not found") }

        if (project.collectedAmount > 0.toBigDecimal()) {
            project.status = "CANCELLED"
            projectRepository.save(project)
            // todo start process of refund
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
            mainImage = mainImage?.id
        )

    private fun Project.toProjectDto(): ProjectDto =
        ProjectDto(
            projectId = projectId,
            creator = creator.userId,
            title = title,
            goalAmount = goalAmount,
            collectedAmount = collectedAmount,
            status = status,
            mainImage = mainImage?.id,
            images = images.map() { it?.id }.toSet(),
            videos = videos.map() { it?.id }.toSet(),
            categories = categories.map { it?.categoryId }.toSet()
        )

    private fun ProjectDto.toProject(): Project =
        Project(
            projectId = projectId,
            creator = userRepository.findById(creator!!).orElseThrow(),
            title = title,
            goalAmount = goalAmount,
            collectedAmount = collectedAmount,
            status = status,
            mainImage = mainImage?.let { imageRepository.findById(it).orElse(null) },
            images = images.filterNotNull().map { imageRepository.findById(it).orElseThrow() }.toSet(),
            videos = videos.filterNotNull().map { videoRepository.findById(it).orElseThrow() }.toSet(),
            categories = categories.filterNotNull().map {
                categoryRepository.findByCategoryId(it).orElseThrow()
            }.toSet()
        )

}