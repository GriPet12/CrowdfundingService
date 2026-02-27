package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.AuthorFollow
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AuthorFollowRepository : JpaRepository<AuthorFollow, Long> {

    fun findByFollowerUserIdAndCreatorUserId(followerId: Long, creatorId: Long): AuthorFollow?

    fun existsByFollowerUserIdAndCreatorUserId(followerId: Long, creatorId: Long): Boolean

    @Query("SELECT af FROM AuthorFollow af JOIN FETCH af.creator c LEFT JOIN FETCH c.image WHERE af.follower.userId = :userId")
    fun findAllByFollowerUserId(userId: Long): List<AuthorFollow>

    @Query("SELECT af.creator.userId FROM AuthorFollow af WHERE af.follower.userId = :userId AND af.creator.userId IN :creatorIds")
    fun findFollowedAuthorIds(userId: Long, creatorIds: List<Long>): List<Long>
}

