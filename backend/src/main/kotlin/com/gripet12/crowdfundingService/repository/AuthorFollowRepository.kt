package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.AuthorFollow
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AuthorFollowRepository : JpaRepository<AuthorFollow, Long> {

    @Query("SELECT af FROM AuthorFollow af WHERE af.follower.userId = :followerId AND af.creator.userId = :creatorId")
    fun findByFollowerUserIdAndCreatorUserId(followerId: Long, creatorId: Long): AuthorFollow?

    @Query("SELECT COUNT(af) FROM AuthorFollow af WHERE af.follower.userId = :followerId AND af.creator.userId = :creatorId")
    fun countByFollowerUserIdAndCreatorUserId(followerId: Long, creatorId: Long): Long

    @Query("SELECT af FROM AuthorFollow af JOIN FETCH af.creator c LEFT JOIN FETCH c.image WHERE af.follower.userId = :userId")
    fun findAllByFollowerUserId(userId: Long): List<AuthorFollow>

    @Query("""
        SELECT af FROM AuthorFollow af
        JOIN FETCH af.follower f LEFT JOIN FETCH f.image
        WHERE af.creator.userId = :creatorId
        ORDER BY af.id DESC
    """)
    fun findAllByCreatorUserId(@Param("creatorId") creatorId: Long): List<AuthorFollow>

    @Query("SELECT af.creator.userId FROM AuthorFollow af WHERE af.follower.userId = :userId AND af.creator.userId IN :creatorIds")
    fun findFollowedAuthorIds(userId: Long, creatorIds: List<Long>): List<Long>
}
