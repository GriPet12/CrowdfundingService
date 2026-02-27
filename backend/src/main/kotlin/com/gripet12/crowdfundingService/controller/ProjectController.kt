package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.ProjectDto
import com.gripet12.crowdfundingService.service.ProjectService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/projects")
class ProjectController (
    private val projectService: ProjectService
) {
    fun addProject(project: ProjectDto) {
        projectService.saveProject(project)
    }

    @GetMapping("/{id}")
    fun getProject(@PathVariable id: Long) = projectService.getProject(id)
}