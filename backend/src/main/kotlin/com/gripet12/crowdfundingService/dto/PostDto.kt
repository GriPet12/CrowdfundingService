package com.gripet12.crowdfundingService.dto

data class PostDto(

    var postId: Long?,

    var masterId: Long,

    var masterType: String,

    var visibility: String,

    var title: String,

    var description: String,

    var requiredTier: Long? = null,

    var likeCount: Int = 0,

    val content: Set<ContentDto> = HashSet()
)
