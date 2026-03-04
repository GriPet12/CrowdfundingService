package com.gripet12.crowdfundingService.dto

import com.gripet12.crowdfundingService.model.enums.Role
import java.time.LocalDateTime

data class UserDto(

    val id: Long? = null,

    val username: String,

    val password: String,

    val email: String,

    val isVerified: Boolean = false,

    val description: String? = null,

    val isPrivate: Boolean = false,

    val imageId: Long? = null,

    val roles: Set<Role> = setOf(Role.ROLE_USER),

    val role: String = if (roles.contains(Role.ROLE_ADMIN)) "ADMIN" else "USER",

    val banned: Boolean = false,

    val createdAt: LocalDateTime? = null
)