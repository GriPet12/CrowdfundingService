package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Project
import com.gripet12.crowdfundingService.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): Optional<User>
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findByUserId(userId: Long): User
    override fun findAll(pageable: Pageable): Page<User>
}