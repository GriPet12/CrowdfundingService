package com.gripet12.crowdfundingService.dto

data class UpdateUserRequest(
    val username: String,
    val description: String? = null,
    val isPrivate: Boolean = false,
    val imageId: Long? = null
)
