package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.CreateProjectDto
import com.gripet12.crowdfundingService.dto.PageResponseDto
import com.gripet12.crowdfundingService.dto.PreviewProjectDto
import com.gripet12.crowdfundingService.dto.ProjectDto
import com.gripet12.crowdfundingService.service.ProjectService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/projects")
class ProjectController(
    private val projectService: ProjectService
) {

    @GetMapping
    fun getProjects(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "6") size: Int,
        @RequestParam(required = false) creatorId: Long?,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(defaultValue = "hotnessScore") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): PageResponseDto<PreviewProjectDto> =
        projectService.getProjectsPage(page, size, creatorId, search, categoryId, sortBy, sortDir)

    @PostMapping
    fun addProject(@RequestBody dto: CreateProjectDto): ResponseEntity<ProjectDto> =
        ResponseEntity.ok(projectService.createProject(dto))

    @GetMapping("/{id}")
    fun getProject(@PathVariable id: Long) = projectService.getProject(id)

    @PutMapping("/{id}")
    fun updateProject(
        @PathVariable id: Long,
        @RequestBody dto: CreateProjectDto
    ): ResponseEntity<ProjectDto> =
        ResponseEntity.ok(projectService.updateProject(id, dto))

    @GetMapping("/{id}/can-delete")
    fun canDelete(@PathVariable id: Long): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(projectService.canDelete(id))

    @DeleteMapping("/{id}")
    fun deleteProject(@PathVariable id: Long): ResponseEntity<Void> {
        projectService.deleteProject(id)
        return ResponseEntity.noContent().build()
    }
}
