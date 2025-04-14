package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.UserDto
import com.gripet12.crowdfundingService.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class UserService(private val userRepository: UserRepository) {

    fun getCurrentUser(): UserDto {
        val authentication = SecurityContextHolder.getContext().authentication
        val username = authentication.name

        val user = userRepository.findByUsername(username).orElseThrow {
            IllegalStateException("User not found")
        }

        return UserDto(
            username = user.username,
            password = "",
            email = user.email,
            roles = user.roles.map { it }.toSet()
        )
    }

    fun getAllCreators(pageable: Pageable): Page<UserDto> {
        return userRepository.findAll(pageable).map { user ->
            UserDto(
                username = user.username,
                password = "",
                email = user.email,
                roles = user.roles.map { it }.toSet()
            )
        }
    }
}