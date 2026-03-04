package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Donate
import com.gripet12.crowdfundingService.model.Payment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate

interface DonateRepository : JpaRepository<Donate, Long> {

    @Query("""
        SELECT d FROM Donate d
        LEFT JOIN FETCH d.payment
        LEFT JOIN FETCH d.project proj
        LEFT JOIN FETCH proj.creator
        LEFT JOIN FETCH d.donor
        LEFT JOIN FETCH d.creator
        WHERE (CAST(:search AS string) IS NULL
               OR LOWER(COALESCE(d.donor.username, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(COALESCE(proj.title, ''))        LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (CAST(:from AS date) IS NULL    OR CAST(d.createAt AS date) >= CAST(:from AS date))
          AND (CAST(:to   AS date) IS NULL    OR CAST(d.createAt AS date) <= CAST(:to   AS date))
    """,
    countQuery = """
        SELECT COUNT(d) FROM Donate d
        LEFT JOIN d.project proj
        LEFT JOIN d.donor
        WHERE (CAST(:search AS string) IS NULL
               OR LOWER(COALESCE(d.donor.username, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
               OR LOWER(COALESCE(proj.title, ''))        LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
          AND (CAST(:from AS date) IS NULL    OR CAST(d.createAt AS date) >= CAST(:from AS date))
          AND (CAST(:to   AS date) IS NULL    OR CAST(d.createAt AS date) <= CAST(:to   AS date))
    """)
    fun findByFilters(
        @Param("search") search: String?,
        @Param("from") from: LocalDate?,
        @Param("to") to: LocalDate?,
        pageable: Pageable
    ): Page<Donate>
    fun save(donate: Donate): Donate
    fun findByPayment(payment: Payment): Donate?

    @Query("""
        SELECT COALESCE(SUM(d.amount), 0)
        FROM Donate d
        JOIN d.payment p
        WHERE d.donor.userId = :donorId
          AND d.project IS NOT NULL
          AND d.project.creator.userId = :creatorId
          AND p.status = 'APPROVED'
          AND d.createAt >= :since
    """)
    fun sumApprovedDonationsByDonorToCreatorSince(
        donorId: Long,
        creatorId: Long,
        since: Timestamp
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
        SELECT d FROM Donate d
        LEFT JOIN FETCH d.payment
        LEFT JOIN FETCH d.project p
        LEFT JOIN FETCH p.creator
        LEFT JOIN FETCH d.creator
        WHERE d.donor.userId = :donorId
        ORDER BY d.createAt DESC
    """)
    fun findAllByDonorUserId(donorId: Long): List<Donate>
}
