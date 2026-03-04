package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.PostLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PostLikeRepository : JpaRepository<PostLike, Long> {
    fun existsByPostPostIdAndUserUserId(postId: Long, userId: Long): Boolean
    fun findByPostPostIdAndUserUserId(postId: Long, userId: Long): PostLike?
    fun countByPostPostId(postId: Long): Long

    @Query("SELECT COUNT(l) FROM PostLike l WHERE l.post.masterId = :creatorId")
    fun countLikesByCreator(creatorId: Long): Long
}
