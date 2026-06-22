package com.gripet12.crowdfundingService.dto

import com.gripet12.crowdfundingService.model.Category

data class HomePageDto(
    val projects: PageResponseDto<PreviewProjectDto>,
    val categories: List<Category>,
    val followedProjectIds: List<Long> = emptyList()
)
