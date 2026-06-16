package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.AuthRequest
import com.gripet12.crowdfundingService.model.User
import com.gripet12.crowdfundingService.model.enums.Role
import com.gripet12.crowdfundingService.repository.UserRepository
import com.gripet12.crowdfundingService.security.JwtTokenProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.Mockito.*
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

class AuthServiceTest {

    private lateinit var authenticationManager: AuthenticationManager
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var emailService: EmailService
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authenticationManager = mock(AuthenticationManager::class.java)
        jwtTokenProvider = mock(JwtTokenProvider::class.java)
        userRepository = mock(UserRepository::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)
        emailService = mock(EmailService::class.java)
        authService = AuthService(
            authenticationManager, jwtTokenProvider, userRepository, passwordEncoder, emailService
        )
    }

    @Test
    fun `login returns auth response when successful`() {
        val request = AuthRequest("user", "pass")
        val authMock = mock(Authentication::class.java)
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken::class.java)))
            .willReturn(authMock)

        val user = User(
            userId = 1L,
            username = "user",
            password = "encoded",
            email = "user@example.com",
            isVerified = true,
            isPrivate = false,
            banned = false,
            roles = mutableSetOf(Role.ROLE_USER)
        )
        given(userRepository.findByUsername("user")).willReturn(Optional.of(user))
        given(jwtTokenProvider.createToken("user", listOf(Role.ROLE_USER))).willReturn("jwt-token")

        val response = authService.login(request)

        assertEquals("jwt-token", response.token)
        assertEquals(1L, response.id)
        assertEquals("user", response.username)
    }

    @Test
    fun `login throws EmailNotVerifiedException when user not verified`() {
        val request = AuthRequest("user", "pass")
        val authMock = mock(Authentication::class.java)
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken::class.java)))
            .willReturn(authMock)

        val user = User(
            userId = 1L,
            username = "user",
            password = "encoded",
            email = "user@example.com",
            isVerified = false,
            isPrivate = false,
            banned = false,
            roles = mutableSetOf(Role.ROLE_USER)
        )
        given(userRepository.findByUsername("user")).willReturn(Optional.of(user))

        assertThrows<EmailNotVerifiedException> {
            authService.login(request)
        }
    }
}

