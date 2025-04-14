package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.ProjectDto
import com.gripet12.crowdfundingService.dto.UserDto
import com.gripet12.crowdfundingService.service.ProjectService
import com.gripet12.crowdfundingService.service.UserService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class HomeController(private val projectService: ProjectService, private val userService: UserService) {

    @GetMapping("/projects")
    fun getProjects(pageable: Pageable): Page<ProjectDto> {
        return projectService.getAllProjects(pageable)
    }

    @GetMapping("/creators")
    fun getCreators(pageable: Pageable): Page<UserDto> {
        return userService.getAllCreators(pageable)
    }
}