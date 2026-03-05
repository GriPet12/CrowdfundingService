package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Project
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.Timestamp

@Repository
interface ProjectRepository : JpaRepository<Project, Long> {
    override fun findAll(pageable: Pageable): Page<Project>
    fun findByProjectId(projectId: Long): Project

    @Query("SELECT p FROM Project p WHERE p.creator.userId = :creatorId")
    fun findByCreatorUserId(@Param("creatorId") creatorId: Long): List<Project>

    @Query("SELECT p FROM Project p WHERE p.creator.userId = :creatorId")
    fun findByCreatorUserIdPageable(@Param("creatorId") creatorId: Long, pageable: Pageable): Page<Project>

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.categories WHERE p.projectId IN :ids")
    fun findAllWithCategoriesByIds(ids: List<Long>): List<Project>

    @Query("SELECT p.projectId FROM Project p")
    fun findAllIds(pageable: Pageable): Page<Long>

    @Query(
        value = """
            SELECT p FROM Project p
            WHERE (CAST(:search AS string) IS NULL
               OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(p.creator.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
              AND (CAST(:categoryId AS long) IS NULL OR EXISTS (SELECT 1 FROM p.categories c WHERE c.categoryId = CAST(:categoryId AS long)))
              AND (
                (:filterBanned = 0 AND p.banned = false)
                OR (:filterBanned = 1 AND p.banned = true AND p.bannedWithUser = false)
                OR (:filterBanned = 2)
              )
        """,
        countQuery = """
            SELECT COUNT(p) FROM Project p
            WHERE (CAST(:search AS string) IS NULL
               OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(p.creator.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
              AND (CAST(:categoryId AS long) IS NULL OR EXISTS (SELECT 1 FROM p.categories c WHERE c.categoryId = CAST(:categoryId AS long)))
              AND (
                (:filterBanned = 0 AND p.banned = false)
                OR (:filterBanned = 1 AND p.banned = true AND p.bannedWithUser = false)
                OR (:filterBanned = 2)
              )
        """
    )
    fun findByFilters(
        @Param("search") search: String?,
        @Param("categoryId") categoryId: Long?,
        @Param("filterBanned") filterBanned: Int,
        pageable: Pageable
    ): Page<Project>

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Project p SET p.collectedAmount = p.collectedAmount + :amount WHERE p.projectId = :projectId")
    fun increaseCollectedAmount(projectId: Long, amount: BigDecimal)

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