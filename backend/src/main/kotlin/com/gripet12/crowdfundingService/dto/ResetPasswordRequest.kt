package com.gripet12.crowdfundingService.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ResetPasswordRequest(
    @field:NotBlank
    val token: String,

    @field:NotBlank
    @field:Size(min = 10, message = "Пароль повинен містити мінімум 10 символів")
    @field:Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{10,}\$",
        message = "Пароль повинен містити хоча б одну велику літеру, цифру та спеціальний символ"
    )
    val newPassword: String
)
