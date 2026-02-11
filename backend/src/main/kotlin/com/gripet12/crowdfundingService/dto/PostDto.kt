package com.gripet12.crowdfundingService.dto

import org.springframework.web.multipart.MultipartFile

data class PostDto(

    var postId: Long?,

    var masterId: Long,

    var masterType: String,

    var visibility: String,

    var title: String,

    var description: String,

    var requiredTier: Long? = null,

    var likeCount: Int = 0,

    // IDs of previously uploaded files
    val content: Set<MultipartFile> = HashSet()
)
