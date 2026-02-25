package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Project
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.Timestamp

@Repository
interface ProjectRepository : JpaRepository<Project, Long> {
    override fun findAll(pageable: Pageable): Page<Project>
    fun findByProjectId(projectId: Long): Project

    @Modifying
    @Query(
        value = """
        UPDATE projects p SET hotness_score = (
            (SELECT COALESCE(SUM(d.amount), 0) FROM donate d WHERE d.project_id = p.project_id AND d.create_at > :startDate) * 0.4 / :maxAmount +
            (SELECT COUNT(DISTINCT d.donor_id) FROM donate d WHERE d.project_id = p.project_id AND d.create_at > :startDate) * 0.4 / :maxDonors +
            (SELECT COUNT(c.comment_id) FROM comments c WHERE c.project_id = p.project_id AND c.create_at > :startDate) * 0.2 / :maxComments
        )
    """, nativeQuery = true
    )
    fun updateHotnessScore(startDate: Timestamp, maxAmount: BigDecimal, maxDonors: Long, maxComments: Long)

    @Query(value = """
        SELECT MAX(total_amount) FROM (
            SELECT COALESCE(SUM(d.amount), 0) as total_amount
            FROM projects p
            LEFT JOIN donate d ON d.project_id = p.project_id AND d.create_at > :startDate
            GROUP BY p.project_id
        ) as sub
    """, nativeQuery = true)
    fun getMaxDonationAmount(startDate: Timestamp): BigDecimal?

    @Query(value = """
        SELECT MAX(cnt) FROM (
            SELECT COUNT(DISTINCT d.donor_id) as cnt
            FROM projects p
            LEFT JOIN donate d ON d.project_id = p.project_id AND d.create_at > :startDate
            GROUP BY p.project_id
        ) as sub
    """, nativeQuery = true)
    fun getMaxDonors(startDate: Timestamp): Long?

    @Query(value = """
        SELECT MAX(cnt) FROM (
            SELECT COUNT(c.comment_id) as cnt
            FROM projects p
            LEFT JOIN comments c ON c.project_id = p.project_id AND c.create_at > :startDate
            GROUP BY p.project_id
        ) as sub
    """, nativeQuery = true)
    fun getMaxComments(startDate: Timestamp): Long?

}