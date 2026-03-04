package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<Post, Long> {
    fun findByPostId(postId: Long): Post?

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.requiredTier LEFT JOIN FETCH p.content WHERE p.masterId = :masterId AND p.masterType = 'USER' AND p.banned = false ORDER BY p.postId DESC")
    fun findByMasterIdOrderByPostIdDesc(masterId: Long): List<Post>

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.requiredTier WHERE p.masterId = :masterId AND p.masterType = 'USER' ORDER BY p.postId DESC")
    fun findByMasterIdIncludingBanned(masterId: Long): List<Post>

    @Query("SELECT p FROM Post p WHERE p.masterId = :masterId AND p.masterType = 'USER'")
    fun findAllByMasterId(@Param("masterId") masterId: Long): List<Post>

    @Query(
        value = """
            SELECT p FROM Post p
            WHERE p.masterType = 'USER'
              AND (CAST(:search AS string) IS NULL
                   OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
              AND (
                (:filterBanned = 0 AND p.banned = false)
                OR (:filterBanned = 1 AND p.banned = true AND p.bannedWithUser = false)
                OR (:filterBanned = 2)
              )
        """,
        countQuery = """
            SELECT COUNT(p) FROM Post p
            WHERE p.masterType = 'USER'
              AND (CAST(:search AS string) IS NULL
                   OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
              AND (
                (:filterBanned = 0 AND p.banned = false)
                OR (:filterBanned = 1 AND p.banned = true AND p.bannedWithUser = false)
                OR (:filterBanned = 2)
              )
        """
    )
    fun findByFilters(
        @Param("search") search: String?,
        @Param("filterBanned") filterBanned: Int,
        pageable: Pageable
    ): Page<Post>
}
