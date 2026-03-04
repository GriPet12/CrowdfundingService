package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Comment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CommentRepository : JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c JOIN FETCH c.author LEFT JOIN FETCH c.author.image WHERE c.post.postId = :postId ORDER BY c.createdAt ASC")
    fun findByPostId(postId: Long): List<Comment>

    fun countByPostPostId(postId: Long): Long
}