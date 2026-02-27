package com.gripet12.crowdfundingService.dto

import com.gripet12.crowdfundingService.model.enums.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserDto(

    val id: Long? = null,

    @field:NotBlank
    @field:Size(min = 4, max = 50)
    val username: String,

    @field:NotBlank
    @field:Size(min = 6)
    val password: String,

    @field:NotBlank
    @field:Email
    val email: String,

    val isVerified: Boolean = false,

    val imageId: Long? = null,

    val roles: Set<Role> = setOf(Role.ROLE_USER)
)