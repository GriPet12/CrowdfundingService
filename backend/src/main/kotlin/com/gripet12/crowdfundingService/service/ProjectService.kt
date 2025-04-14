package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.ProjectDto
import com.gripet12.crowdfundingService.repository.ProjectRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ProjectService(private val projectRepository: ProjectRepository) {

    fun getAllProjects(pageable: Pageable): Page<ProjectDto> {
        return projectRepository.findAll(pageable).map { project ->
            project.id?.let {
                project.author.id?.let { it1 ->
                    ProjectDto(
                        id = it,
                        title = project.title,
                        author = it1,
                    )
                }
            }
        }
    }

}