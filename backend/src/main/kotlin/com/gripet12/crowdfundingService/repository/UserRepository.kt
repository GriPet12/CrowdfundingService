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
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findByUserId(userId: Long): User
    override fun findAll(pageable: Pageable): Page<User>

    @Query(
        value = """
            SELECT u FROM User u
            WHERE (CAST(:search AS string) IS NULL
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(u.email)    LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
            WHERE (CAST(:search AS string) IS NULL
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(u.email)    LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
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
        @Param("search") search: String?,
        @Param("filterBanned") filterBanned: Int,
        @Param("filterRole") filterRole: Int,
        pageable: Pageable
    ): Page<User>

    @Query(
        value = """
            SELECT u FROM User u
            WHERE u.banned = false
              AND u.isPrivate = false
              AND (CAST(:search AS string) IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        """,
        countQuery = """
            SELECT COUNT(u) FROM User u
            WHERE u.banned = false
              AND u.isPrivate = false
              AND (CAST(:search AS string) IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        """
    )
    fun findActiveCreators(
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<User>
}