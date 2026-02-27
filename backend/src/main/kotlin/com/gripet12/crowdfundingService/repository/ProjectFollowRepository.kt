package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.ProjectFollow
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProjectFollowRepository : JpaRepository<ProjectFollow, Long> {

    fun findByUserUserIdAndProjectProjectId(userId: Long, projectId: Long): ProjectFollow?

    fun existsByUserUserIdAndProjectProjectId(userId: Long, projectId: Long): Boolean

    @Query("SELECT pf FROM ProjectFollow pf JOIN FETCH pf.project p JOIN FETCH p.creator WHERE pf.user.userId = :userId")
    fun findAllByUserUserId(userId: Long): List<ProjectFollow>

    fun deleteByUserUserIdAndProjectProjectId(userId: Long, projectId: Long)

    @Query("SELECT pf.project.projectId FROM ProjectFollow pf WHERE pf.user.userId = :userId AND pf.project.projectId IN :projectIds")
    fun findFollowedProjectIds(userId: Long, projectIds: List<Long>): List<Long>
}

