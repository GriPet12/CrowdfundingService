package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.HomePageDto
import com.gripet12.crowdfundingService.dto.PageResponseDto
import com.gripet12.crowdfundingService.dto.PreviewProjectDto
import com.gripet12.crowdfundingService.service.HomeService
import com.gripet12.crowdfundingService.service.RecommendationService
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class HomeController(
    private val recommendationService: RecommendationService,
    private val homeService: HomeService,
) {

    @GetMapping("/home")
    fun getHome(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "6") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(defaultValue = "hotnessScore") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): HomePageDto =
        homeService.getHomePage(page, size, search, categoryId, sortBy, sortDir)

    @GetMapping("/recommendations")
    fun getRecommendations(
        @RequestParam(defaultValue = "12") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): PageResponseDto<PreviewProjectDto> {
        val pageable = Pageable.ofSize(size).withPage(page)
        val recommendationsPage = recommendationService.getRecommendationsForCurrentUser(pageable)

        return PageResponseDto(
            content = recommendationsPage.content,
            totalElements = recommendationsPage.totalElements,
            totalPages = recommendationsPage.totalPages,
            currentPage = recommendationsPage.number,
            size = recommendationsPage.size
        )
    }
}
