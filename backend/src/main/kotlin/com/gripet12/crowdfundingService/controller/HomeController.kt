package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.PageResponseDto
import com.gripet12.crowdfundingService.dto.ProjectDto
import com.gripet12.crowdfundingService.dto.UserDto
import com.gripet12.crowdfundingService.service.ProjectService
import com.gripet12.crowdfundingService.service.UserService
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
@CrossOrigin(origins = ["\${frontend.url}"])
class HomeController(private val projectService: ProjectService, private val userService: UserService) {

    @GetMapping("/projects")
    fun getProjects(size: Int, page: Int): PageResponseDto<ProjectDto> {
        val pageable = Pageable.ofSize(size).withPage(page)
        val projectsPage = projectService.getAllProjects(pageable)

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
}