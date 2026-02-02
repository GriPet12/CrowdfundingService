package com.gripet12.crowdfundingService.dto

import com.gripet12.crowdfundingService.model.enums.Role

data class AuthResponse(
    val token: String,
    val username: String,
    val roles: List<Role>
)