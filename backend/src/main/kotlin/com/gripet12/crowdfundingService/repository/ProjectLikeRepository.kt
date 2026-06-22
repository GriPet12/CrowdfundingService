package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.ProjectLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProjectLikeRepository : JpaRepository<ProjectLike, Long> {
    fun existsByProjectProjectIdAndUserUserId(projectId: Long, userId: Long): Boolean
    fun findByProjectProjectIdAndUserUserId(projectId: Long, userId: Long): ProjectLike?
    fun countByProjectProjectId(projectId: Long): Long

    @Modifying
    @Query("DELETE FROM ProjectLike l WHERE l.project.projectId = :projectId")
    fun deleteByProjectProjectId(@Param("projectId") projectId: Long)
}
