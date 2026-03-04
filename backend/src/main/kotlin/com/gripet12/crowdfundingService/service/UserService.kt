package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.ChangePasswordRequest
import com.gripet12.crowdfundingService.dto.PageResponseDto
import com.gripet12.crowdfundingService.dto.UpdateUserRequest
import com.gripet12.crowdfundingService.dto.UserDto
import com.gripet12.crowdfundingService.model.User
import com.gripet12.crowdfundingService.repository.FileRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository,
    private val passwordEncoder: PasswordEncoder
) {

    private fun User.toDto() = UserDto(
        id = userId,
        username = username,
        password = "",
        email = email,
        isVerified = isVerified,
        description = description,
        isPrivate = isPrivate,
        imageId = image?.id,
        roles = roles.toSet(),
        banned = banned,
        createdAt = createdAt
    )

    private fun User.toPrivateDto() = UserDto(
        id = userId,
        username = username,
        password = "",
        email = "",
        isVerified = false,
        description = null,
        isPrivate = true,
        imageId = null,
        roles = setOf(),
        banned = false,
        createdAt = null
    )

    @Transactional(readOnly = true)
    fun getCurrentUser(): UserDto {
        val authentication = SecurityContextHolder.getContext().authentication
        val username = authentication.name

        val user = userRepository.findByUsername(username).orElseThrow {
            IllegalStateException("User not found")
        }

        return user.toDto()
    }

    @Transactional
    fun updateCurrentUser(request: UpdateUserRequest): UserDto {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = userRepository.findByUsername(authentication.name).orElseThrow {
            IllegalStateException("User not found")
        }
        val newImage = request.imageId?.let {
            fileRepository.findById(it).orElse(user.image)
        } ?: user.image

        val updated = user.copy(
            username    = request.username.trim(),
            description = request.description?.trim(),
            isPrivate   = request.isPrivate,
            image       = newImage
        )
        return userRepository.save(updated).toDto()
    }

    @Transactional
    fun changePassword(request: ChangePasswordRequest) {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = userRepository.findByUsername(authentication.name).orElseThrow {
            IllegalStateException("User not found")
        }

        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            throw BadCredentialsException("Current password is incorrect")
        }

        val updated = user.copy(password = passwordEncoder.encode(request.newPassword))
        userRepository.save(updated)
    }


    @Transactional(readOnly = true)
    fun getCreatorsPage(
        page: Int,
        size: Int,
        search: String? = null,
        sortBy: String = "createdAt",
        sortDir: String = "desc"
    ): PageResponseDto<UserDto> {

        val sortField = when (sortBy) {
            "username"  -> "username"
            "createdAt" -> "createdAt"
            else        -> "createdAt"
        }
        val direction = if (sortDir.lowercase() == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(direction, sortField))

        val usersPage = userRepository.findActiveCreators(
            search = if (search.isNullOrBlank()) null else search,
            pageable = pageable
        )

        return PageResponseDto(
            content      = usersPage.content.map { it.toDto() },
            totalElements = usersPage.totalElements,
            totalPages   = usersPage.totalPages,
            currentPage  = usersPage.number,
            size         = usersPage.size
        )
    }

    @Transactional(readOnly = true)
    fun getUserById(id: Long): UserDto {
        val user = userRepository.findById(id).orElseThrow {
            IllegalStateException("User not found")
        }

        if (user.isPrivate) {
            val callerName = try {
                SecurityContextHolder.getContext().authentication?.name
            } catch (_: Exception) { null }
            val isOwner = callerName != null && callerName == user.username
            if (!isOwner) return user.toPrivateDto()
        }

        return user.toDto()
    }
}