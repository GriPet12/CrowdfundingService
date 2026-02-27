package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.UserDto
import com.gripet12.crowdfundingService.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(private val userRepository: UserRepository) {

    @Transactional(readOnly = true)
    fun getCurrentUser(): UserDto {
        val authentication = SecurityContextHolder.getContext().authentication
        val username = authentication.name

        val user = userRepository.findByUsername(username).orElseThrow {
            IllegalStateException("User not found")
        }

        return UserDto(
            id = user.userId,
            username = user.username,
            password = "",
            email = user.email,
            isVerified = user.isVerified,
            imageId = user.image?.id,
            roles = user.roles.map { it }.toSet()
        )
    }

    @Transactional(readOnly = true)
    fun getAllCreators(pageable: Pageable): Page<UserDto> {
        return userRepository.findAll(pageable).map { user ->
            UserDto(
                id = user.userId,
                username = user.username,
                password = "",
                email = user.email,
                isVerified = user.isVerified,
                imageId = user.image?.id,
                roles = user.roles.map { it }.toSet()
            )
        }
    }

    @Transactional(readOnly = true)
    fun getUserById(id: Long): UserDto {
        val user = userRepository.findById(id).orElseThrow {
            IllegalStateException("User not found")
        }

        return UserDto(
            id = user.userId,
            username = user.username,
            password = "",
            email = user.email,
            isVerified = user.isVerified,
            imageId = user.image?.id,
            roles = user.roles.map { it }.toSet()
        )
    }
}