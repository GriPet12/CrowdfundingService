package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.AuthRequest
import com.gripet12.crowdfundingService.dto.AuthResponse
import com.gripet12.crowdfundingService.dto.RegisterRequest
import com.gripet12.crowdfundingService.service.AuthService
import com.gripet12.crowdfundingService.service.EmailService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val emailService: EmailService
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody authRequest: AuthRequest): ResponseEntity<AuthResponse> {
        val authResponse = authService.login(authRequest)
        return ResponseEntity.ok(authResponse)
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val authResponse = authService.register(request)
        return ResponseEntity.ok(authResponse)
    }

    @GetMapping("/verify-email")
    fun verifyEmail(@RequestParam token: String): ResponseEntity<Map<String, String>> {
        return try {
            emailService.verifyEmail(token)
            ResponseEntity.ok(mapOf("message" to "EMAIL_VERIFIED"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "INVALID_TOKEN")))
        }
    }

    @PostMapping("/resend-verification")
    fun resendVerification(@RequestBody body: Map<String, String>): ResponseEntity<Map<String, String>> {
        val email = body["email"] ?: return ResponseEntity.badRequest().body(mapOf("error" to "EMAIL_REQUIRED"))
        return try {
            emailService.resendVerificationEmail(email)
            ResponseEntity.ok(mapOf("message" to "VERIFICATION_SENT"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "ERROR")))
        }
    }
}
