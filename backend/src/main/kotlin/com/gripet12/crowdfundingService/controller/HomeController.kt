package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.PageResponseDto
import com.gripet12.crowdfundingService.dto.PreviewProjectDto
import com.gripet12.crowdfundingService.service.RecommendationService
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/")
class HomeController(
    private val recommendationService: RecommendationService,
) {

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