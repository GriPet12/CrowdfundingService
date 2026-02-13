package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Comment
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository : JpaRepository<Comment, Long> {
}