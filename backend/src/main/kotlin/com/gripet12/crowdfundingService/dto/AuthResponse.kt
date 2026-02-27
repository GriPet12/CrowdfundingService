package com.gripet12.crowdfundingService.dto

import com.gripet12.crowdfundingService.model.enums.Role

data class AuthResponse(
    val token: String,
    val id: Long?,
    val username: String,
    val imageId: Long?,
    val roles: List<Role>
)