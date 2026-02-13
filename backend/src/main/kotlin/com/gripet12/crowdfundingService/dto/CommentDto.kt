package com.gripet12.crowdfundingService.dto

data class CommentDto(
    var commentId: Long,

    var author: Long,

    var project: Long,

    var post: Long,

    val commentText: String
)
