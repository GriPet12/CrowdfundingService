package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.AuthRequest
import com.gripet12.crowdfundingService.dto.AuthResponse
import com.gripet12.crowdfundingService.dto.UserDto
import com.gripet12.crowdfundingService.model.User
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
    private val passwordEncoder: PasswordEncoder
) {

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
    fun register(userDto: UserDto): AuthResponse {
        if (userRepository.existsByUsername(userDto.username)) {
            throw IllegalArgumentException("Username is already taken")
        }

        if (userRepository.existsByEmail(userDto.email)) {
            throw IllegalArgumentException("Email is already in use")
        }

        val roles = userDto.roles.map { it }.toSet()

        val user = User(
            username = userDto.username,
            password = passwordEncoder.encode(userDto.password),
            email = userDto.email,
            roles = roles
        )

        userRepository.save(user)

        val token = jwtTokenProvider.createToken(user.username, roles.map { it})

        return AuthResponse(
            token = token,
            id = user.userId,
            username = user.username,
            imageId = user.image?.id,
            roles = roles.map { it }
        )
    }
}