package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Donate
import com.gripet12.crowdfundingService.model.Payment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate

interface DonateRepository : JpaRepository<Donate, Long> {

    @Query(value = """
        SELECT
            d.donate_id,
            COALESCE(u.username, 'Анонім') AS from_user,
            p.title                         AS project_title,
            c.username                      AS creator_name,
            d.amount,
            pay.status,
            d.create_at
        FROM donate d
        LEFT JOIN payments  pay ON pay.payment_id  = d.payment_payment_id
        LEFT JOIN projects  p   ON p.project_id    = d.project_project_id
        LEFT JOIN users     u   ON u.user_id        = d.donor_user_id
        LEFT JOIN users     c   ON c.user_id        = d.creator_user_id
        WHERE (:search IS NULL
               OR LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(p.title,    '')) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:from IS NULL OR CAST(d.create_at AS date) >= CAST(:from AS date))
          AND (:to   IS NULL OR CAST(d.create_at AS date) <= CAST(:to AS date))
        ORDER BY d.donate_id DESC
    """,
    countQuery = """
        SELECT COUNT(d.donate_id)
        FROM donate d
        LEFT JOIN projects p ON p.project_id = d.project_project_id
        LEFT JOIN users    u ON u.user_id     = d.donor_user_id
        WHERE (:search IS NULL
               OR LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(p.title,    '')) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:from IS NULL OR CAST(d.create_at AS date) >= CAST(:from AS date))
          AND (:to   IS NULL OR CAST(d.create_at AS date) <= CAST(:to AS date))
    """,
    nativeQuery = true)
    fun findByFilters(
        @Param("search") search: String?,
        @Param("from") from: LocalDate?,
        @Param("to") to: LocalDate?,
        pageable: Pageable
    ): Page<Array<Any?>>

    @Query(value = """
        SELECT
            d.donate_id,
            COALESCE(u.username, 'Анонім') AS from_user,
            p.title                         AS project_title,
            c.username                      AS creator_name,
            d.amount,
            pay.status,
            d.create_at
        FROM donate d
        LEFT JOIN payments  pay ON pay.payment_id  = d.payment_payment_id
        LEFT JOIN projects  p   ON p.project_id    = d.project_project_id
        LEFT JOIN users     u   ON u.user_id        = d.donor_user_id
        LEFT JOIN users     c   ON c.user_id        = p.creator_id
        WHERE d.project_project_id = :projectId
        ORDER BY d.donate_id DESC
    """,
    countQuery = """
        SELECT COUNT(d.donate_id)
        FROM donate d
        WHERE d.project_project_id = :projectId
    """,
    nativeQuery = true)
    fun findByProjectIdForAdmin(
        @Param("projectId") projectId: Long,
        pageable: Pageable
    ): Page<Array<Any?>>
    fun save(donate: Donate): Donate
    fun findByPayment(payment: Payment): Donate?

    @Query(
        value = """
        SELECT COALESCE(SUM(d.amount), 0)
        FROM donate d
        INNER JOIN payments p ON p.payment_id = d.payment_payment_id
        LEFT JOIN projects pr ON pr.project_id = d.project_project_id
        WHERE d.donor_user_id = :donorId
          AND p.status = 'APPROVED'
          AND d.create_at >= :since
          AND (d.creator_user_id = :creatorId OR pr.creator_id = :creatorId)
        """,
        nativeQuery = true
    )
    fun sumApprovedDonationsByDonorToCreatorSince(
        @Param("donorId") donorId: Long,
        @Param("creatorId") creatorId: Long,
        @Param("since") since: Timestamp
    ): BigDecimal


    @Query("""
        SELECT COALESCE(SUM(d.amount), 0)
        FROM Donate d JOIN d.payment p
        WHERE d.project IS NOT NULL
          AND d.project.creator.userId = :creatorId
          AND p.status = 'APPROVED'
    """)
    fun sumProjectDonationsToCreator(creatorId: Long): BigDecimal

    @Query("""
        SELECT COUNT(d)
        FROM Donate d JOIN d.payment p
        WHERE d.project IS NOT NULL
          AND d.project.creator.userId = :creatorId
          AND p.status = 'APPROVED'
    """)
    fun countProjectDonationsToCreator(creatorId: Long): Long

    @Query("""
        SELECT COALESCE(SUM(d.amount), 0)
        FROM Donate d JOIN d.payment p
        WHERE d.project IS NULL
          AND d.creator.userId = :creatorId
          AND p.status = 'APPROVED'
    """)
    fun sumDirectDonationsToCreator(creatorId: Long): BigDecimal

    @Query("""
        SELECT COUNT(d)
        FROM Donate d JOIN d.payment p
        WHERE d.project IS NULL
          AND d.creator.userId = :creatorId
          AND p.status = 'APPROVED'
    """)
    fun countDirectDonationsToCreator(creatorId: Long): Long

    @Query(value = """
        SELECT
            d.donate_id,
            COALESCE(p.title, '')                            AS project_title,
            COALESCE(uc.username, ud.username, '')           AS creator_name,
            d.reward,
            d.amount,
            pay.status,
            d.create_at
        FROM donate d
        LEFT JOIN payments pay ON pay.payment_id = d.payment_payment_id
        LEFT JOIN projects p   ON p.project_id   = d.project_project_id
        LEFT JOIN users    uc  ON uc.user_id      = p.creator_id
        LEFT JOIN users    ud  ON ud.user_id      = d.creator_user_id
        WHERE d.donor_user_id = :donorId
        ORDER BY d.create_at DESC
    """, nativeQuery = true)
    fun findAllByDonorUserId(@Param("donorId") donorId: Long): List<Array<Any?>>

    @Query(value = """
        SELECT
            d.donate_id,
            COALESCE(p.title, '')                            AS project_title,
            COALESCE(u.username, 'Анонім')                   AS donor_name,
            d.reward,
            d.amount,
            pay.status,
            d.create_at,
            d.is_anonymous
        FROM donate d
        LEFT JOIN payments pay ON pay.payment_id = d.payment_payment_id
        LEFT JOIN projects p   ON p.project_id   = d.project_project_id
        LEFT JOIN users    u   ON u.user_id       = d.donor_user_id
        WHERE (d.creator_user_id = :creatorId OR p.creator_id = :creatorId)
        ORDER BY d.create_at DESC
    """, nativeQuery = true)
    fun findAllByCreatorUserId(@Param("creatorId") creatorId: Long): List<Array<Any?>>

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Donate d JOIN d.payment p WHERE p.status = 'APPROVED'")
    fun sumAllApprovedDonations(): BigDecimal

    @Query("SELECT COUNT(d) FROM Donate d JOIN d.payment p WHERE p.status = 'APPROVED'")
    fun countAllApprovedDonations(): Long

    @Query("""
        SELECT d FROM Donate d JOIN FETCH d.payment p
        LEFT JOIN FETCH d.project pr LEFT JOIN FETCH pr.creator
        LEFT JOIN FETCH d.creator
        WHERE p.status = 'APPROVED'
          AND ((d.creator.userId = :creatorId) OR (d.project.creator.userId = :creatorId))
    """)
    fun findAllApprovedForCreator(@Param("creatorId") creatorId: Long): List<Donate>

    @Query("""
        SELECT d FROM Donate d JOIN FETCH d.payment p
        LEFT JOIN FETCH d.donor dr LEFT JOIN FETCH dr.image
        WHERE d.project.projectId = :projectId
          AND p.status = 'APPROVED'
        ORDER BY d.createAt DESC
    """)
    fun findApprovedByProjectId(@Param("projectId") projectId: Long): List<Donate>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Donate d WHERE d.project.projectId = :projectId")
    fun deleteByProjectProjectId(@Param("projectId") projectId: Long)
}
