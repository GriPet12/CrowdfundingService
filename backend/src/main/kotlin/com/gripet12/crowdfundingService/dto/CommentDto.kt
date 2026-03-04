package com.gripet12.crowdfundingService.dto

import java.time.Instant

data class CommentResponseDto(
    val commentId: Long,
    val authorId: Long,
    val authorName: String,
    val authorImageId: Long?,
    val commentText: String,
    val createdAt: Instant
)
