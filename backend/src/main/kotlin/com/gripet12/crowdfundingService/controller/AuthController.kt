package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.AuthRequest
import com.gripet12.crowdfundingService.dto.AuthResponse
import com.gripet12.crowdfundingService.dto.UserDto
import com.gripet12.crowdfundingService.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody authRequest: AuthRequest): ResponseEntity<AuthResponse> {
        val authResponse = authService.login(authRequest)
        return ResponseEntity.ok(authResponse)
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody userDto: UserDto): ResponseEntity<AuthResponse> {
        val authResponse = authService.register(userDto)
        return ResponseEntity.ok(authResponse)
    }
}