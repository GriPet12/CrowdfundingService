package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): Optional<User>
    fun findByEmail(email: String): Optional<User>

    @Query("SELECT u.userId FROM User u WHERE u.username = :username")
    fun findUserIdByUsername(@Param("username") username: String): Long?
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findByUserId(userId: Long): User
    fun findByStripeConnectAccountId(stripeConnectAccountId: String): User?
    override fun findAll(pageable: Pageable): Page<User>

    @Query(
        value = """
            SELECT u FROM User u
            WHERE (:searchPattern IS NULL
                   OR LOWER(u.username) LIKE :searchPattern
                   OR LOWER(u.email)    LIKE :searchPattern)
              AND (
                   :filterBanned = 0
                   OR (:filterBanned = 1 AND u.banned = false)
                   OR (:filterBanned = 2 AND u.banned = true)
              )
              AND (
                   :filterRole = 0
                   OR (:filterRole = 1 AND NOT EXISTS (SELECT 1 FROM u.roles r WHERE r = 'ROLE_ADMIN'))
                   OR (:filterRole = 2 AND EXISTS     (SELECT 1 FROM u.roles r WHERE r = 'ROLE_ADMIN'))
              )
        """,
        countQuery = """
            SELECT COUNT(u) FROM User u
            WHERE (:searchPattern IS NULL
                   OR LOWER(u.username) LIKE :searchPattern
                   OR LOWER(u.email)    LIKE :searchPattern)
              AND (
                   :filterBanned = 0
                   OR (:filterBanned = 1 AND u.banned = false)
                   OR (:filterBanned = 2 AND u.banned = true)
              )
              AND (
                   :filterRole = 0
                   OR (:filterRole = 1 AND NOT EXISTS (SELECT 1 FROM u.roles r WHERE r = 'ROLE_ADMIN'))
                   OR (:filterRole = 2 AND EXISTS     (SELECT 1 FROM u.roles r WHERE r = 'ROLE_ADMIN'))
              )
        """
    )
    fun findByFilters(
        @Param("searchPattern") searchPattern: String?,
        @Param("filterBanned") filterBanned: Int,
        @Param("filterRole") filterRole: Int,
        pageable: Pageable
    ): Page<User>

    @Query(
        value = """
            SELECT u FROM User u
            WHERE u.banned = false
              AND u.isPrivate = false
              AND (:searchPattern IS NULL OR LOWER(u.username) LIKE :searchPattern)
        """,
        countQuery = """
            SELECT COUNT(u) FROM User u
            WHERE u.banned = false
              AND u.isPrivate = false
              AND (:searchPattern IS NULL OR LOWER(u.username) LIKE :searchPattern)
        """
    )
    fun findActiveCreators(
        @Param("searchPattern") searchPattern: String?,
        pageable: Pageable
    ): Page<User>
}