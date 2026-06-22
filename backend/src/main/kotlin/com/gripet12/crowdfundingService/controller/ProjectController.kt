package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.CreateProjectDto
import com.gripet12.crowdfundingService.dto.PageResponseDto
import com.gripet12.crowdfundingService.dto.PreviewProjectDto
import com.gripet12.crowdfundingService.dto.ProjectDonorDto
import com.gripet12.crowdfundingService.dto.ProjectDto
import com.gripet12.crowdfundingService.service.ProjectService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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

    @GetMapping("/my/pending")
    @PreAuthorize("isAuthenticated()")
    fun getMyPendingProjects(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): PageResponseDto<PreviewProjectDto> =
        projectService.getMyPendingProjects(page, size)

    @GetMapping("/my/rejected")
    @PreAuthorize("isAuthenticated()")
    fun getMyRejectedProjects(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): PageResponseDto<PreviewProjectDto> =
        projectService.getMyRejectedProjects(page, size)

    @PostMapping
    fun addProject(@RequestBody dto: CreateProjectDto): ResponseEntity<ProjectDto> =
        ResponseEntity.ok(projectService.createProject(dto))

    @GetMapping("/{id}")
    fun getProject(@PathVariable id: Long): ResponseEntity<ProjectDto> {
        val project = projectService.getProject(id)
        return if (project != null) ResponseEntity.ok(project) else ResponseEntity.notFound().build()
    }

    @PutMapping("/{id}")
    fun updateProject(
        @PathVariable id: Long,
        @RequestBody dto: CreateProjectDto
    ): ResponseEntity<ProjectDto> =
        ResponseEntity.ok(projectService.updateProject(id, dto))

    @GetMapping("/{id}/donors")
    fun getProjectDonors(@PathVariable id: Long): ResponseEntity<List<ProjectDonorDto>> =
        ResponseEntity.ok(projectService.getProjectDonors(id))

    @PostMapping("/{id}/close-fundraising")
    @PreAuthorize("isAuthenticated()")
    fun closeFundraising(@PathVariable id: Long): ResponseEntity<ProjectDto> =
        ResponseEntity.ok(projectService.closeFundraising(id))

    @GetMapping("/{id}/can-delete")
    fun canDelete(@PathVariable id: Long): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(projectService.canDelete(id))

    @DeleteMapping("/{id}")
    fun deleteProject(@PathVariable id: Long): ResponseEntity<Void> {
        projectService.deleteProject(id)
        return ResponseEntity.noContent().build()
    }
}
