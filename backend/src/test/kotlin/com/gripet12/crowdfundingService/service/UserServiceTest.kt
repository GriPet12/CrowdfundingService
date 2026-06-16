package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.UserDto
import com.gripet12.crowdfundingService.model.User
import com.gripet12.crowdfundingService.model.enums.Role
import com.gripet12.crowdfundingService.repository.FileRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var fileRepository: FileRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        fileRepository = mock(FileRepository::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)
        userService = UserService(userRepository, fileRepository, passwordEncoder)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `getCurrentUser returns UserDto`() {
        val auth = mock(Authentication::class.java)
        given(auth.name).willReturn("current_user")
        val ctx = mock(SecurityContext::class.java)
        given(ctx.authentication).willReturn(auth)
        SecurityContextHolder.setContext(ctx)

        val user = User(
            userId = 1L,
            username = "current_user",
            password = "pwd",
            email = "email@test.com",
            isVerified = true,
            isPrivate = false,
            banned = false,
            roles = mutableSetOf(Role.ROLE_USER)
        )
        given(userRepository.findByUsername("current_user")).willReturn(Optional.of(user))

        val dto = userService.getCurrentUser()

        assertEquals("current_user", dto.username)
        assertEquals("email@test.com", dto.email)
    }

    @Test
    fun `getUserById returns public Dto for public user`() {
        val user = User(
            userId = 2L,
            username = "other_user",
            password = "pwd",
            email = "email2@test.com",
            isVerified = true,
            isPrivate = false,
            banned = false,
            roles = mutableSetOf(Role.ROLE_USER)
        )
        given(userRepository.findById(2L)).willReturn(Optional.of(user))

        val dto = userService.getUserById(2L)

        assertEquals("other_user", dto.username)
        assertEquals("email2@test.com", dto.email)
        assertFalse(dto.isPrivate)
    }

    @Test
    fun `getUserById returns private Dto for private user when not owner`() {
        // No security context so no caller name, should hide details
        val user = User(
            userId = 3L,
            username = "private_user",
            password = "pwd",
            email = "hidden@test.com",
            isVerified = true,
            isPrivate = true,
            banned = false,
            roles = mutableSetOf(Role.ROLE_USER)
        )
        given(userRepository.findById(3L)).willReturn(Optional.of(user))

        val dto = userService.getUserById(3L)

        assertEquals("private_user", dto.username)
        assertEquals("", dto.email) // email must be blank
        assertTrue(dto.isPrivate)
    }
}

