package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.PageResponseDto
import com.gripet12.crowdfundingService.dto.PreviewProjectDto
import com.gripet12.crowdfundingService.dto.UserDto
import com.gripet12.crowdfundingService.service.ProjectService
import com.gripet12.crowdfundingService.service.RecommendationService
import com.gripet12.crowdfundingService.service.UserService
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class HomeController(
    private val projectService: ProjectService,
    private val userService: UserService,
    private val recommendationService: RecommendationService
) {

    @GetMapping("/projects")
    fun getProjects(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): PageResponseDto<PreviewProjectDto> {
        val pageable = Pageable.ofSize(size).withPage(page)
        val projectsPage = projectService.getAllProjectsForPreview(pageable)

        return PageResponseDto(
            content = projectsPage.content,
            totalElements = projectsPage.totalElements,
            totalPages = projectsPage.totalPages,
            currentPage = projectsPage.number,
            size = projectsPage.size
        )
    }
    @GetMapping("/creators")
    fun getCreators(size: Int = 0, page: Int = 10): PageResponseDto<UserDto> {
        val pageable = Pageable.ofSize(size).withPage(page)
        val creatorsPage = userService.getAllCreators(pageable)

        return PageResponseDto(
            content = creatorsPage.content,
            totalElements = creatorsPage.totalElements,
            totalPages = creatorsPage.totalPages,
            currentPage = creatorsPage.number,
            size = creatorsPage.size
        )
    }

    @GetMapping("/recommendations/{userId}")
    fun getRecommendations(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): PageResponseDto<PreviewProjectDto> {
        val pageable = Pageable.ofSize(size).withPage(page)
        val recommendationsPage = recommendationService.getProjectRecommendations(userId, pageable)

        return PageResponseDto(
            content = recommendationsPage.content,
            totalElements = recommendationsPage.totalElements,
            totalPages = recommendationsPage.totalPages,
            currentPage = recommendationsPage.number,
            size = recommendationsPage.size
        )
    }
}