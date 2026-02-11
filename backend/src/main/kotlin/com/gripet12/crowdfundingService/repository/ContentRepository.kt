package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Content
import org.springframework.data.jpa.repository.JpaRepository

interface ContentRepository : JpaRepository<Content, Long> {
    fun findByContentId(contentId: Long): Content?
}