package com.gripet12.crowdfundingService.dto

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

