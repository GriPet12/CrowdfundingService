package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.PageResponseDto
import com.gripet12.crowdfundingService.dto.UserDto
import com.gripet12.crowdfundingService.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/creators")
class CreatorController(private val userService: UserService) {

    @GetMapping
    fun getCreators(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "12") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): PageResponseDto<UserDto> =
        userService.getCreatorsPage(page, size, search, sortBy, sortDir)
}
