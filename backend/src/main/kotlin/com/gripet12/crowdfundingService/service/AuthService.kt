package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.AuthRequest
import com.gripet12.crowdfundingService.dto.AuthResponse
import com.gripet12.crowdfundingService.dto.RegisterRequest
import com.gripet12.crowdfundingService.model.User
import com.gripet12.crowdfundingService.model.enums.Role
import com.gripet12.crowdfundingService.repository.UserRepository
import com.gripet12.crowdfundingService.security.JwtTokenProvider
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService
) {

    @Transactional
    fun login(authRequest: AuthRequest): AuthResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(authRequest.username, authRequest.password)
        )

        SecurityContextHolder.getContext().authentication = authentication

        val user = userRepository.findByUsername(authRequest.username).orElseThrow()
        val roles = user.roles.map { it }
        val token = jwtTokenProvider.createToken(authRequest.username, roles)

        return AuthResponse(
            token = token,
            id = user.userId,
            username = user.username,
            imageId = user.image?.id,
            roles = roles
        )
    }

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("USERNAME_TAKEN")
        }
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("EMAIL_TAKEN")
        }

        val roles = mutableSetOf(Role.ROLE_USER)
        val user = User(
            username = request.username,
            password = passwordEncoder.encode(request.password),
            email = request.email,
            isVerified = false,
            roles = roles
        )

        val savedUser = userRepository.save(user)

        try {
            emailService.sendVerificationEmail(savedUser)
        } catch (e: Exception) {  }

        val token = jwtTokenProvider.createToken(savedUser.username, roles.map { it })
        return AuthResponse(
            token = token,
            id = savedUser.userId,
            username = savedUser.username,
            imageId = savedUser.image?.id,
            roles = roles.map { it }
        )
    }
}
