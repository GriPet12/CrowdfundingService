package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Post
import org.springframework.data.jpa.repository.JpaRepository

interface PostRepository : JpaRepository<Post, Long> {
}