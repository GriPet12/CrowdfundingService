package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.dto.AnalyticsLogDto
import com.gripet12.crowdfundingService.model.Project
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : JpaRepository<Project, Long> {
    override fun findAll(pageable: Pageable): Page<Project>
    fun findByProjectId(projectId: Long): Project


}