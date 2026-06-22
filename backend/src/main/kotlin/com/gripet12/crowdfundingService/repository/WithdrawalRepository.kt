package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Withdrawal
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDate

interface WithdrawalRepository : JpaRepository<Withdrawal, Long> {
    fun findByUserUserIdOrderByWithdrawalIdDesc(userId: Long): List<Withdrawal>

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM Withdrawal w WHERE w.status = 'COMPLETED'")
    fun sumAllCompleted(): BigDecimal

    @Query("SELECT COUNT(w) FROM Withdrawal w WHERE w.status = 'COMPLETED'")
    fun countAllCompleted(): Long

    fun countByStatus(status: String): Long

    @Query(
        value = """
        SELECT
            w.withdrawal_id,
            u.username AS from_user,
            u.email AS user_email,
            w.amount,
            w.status,
            w.created_at,
            w.payout_method,
            w.payout_destination,
            w.recipient_name
        FROM withdrawals w
        JOIN users u ON u.user_id = w.user_user_id
        WHERE (:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:status IS NULL OR w.status = :status)
          AND (:from IS NULL OR CAST(w.created_at AS date) >= CAST(:from AS date))
          AND (:to IS NULL OR CAST(w.created_at AS date) <= CAST(:to AS date))
        ORDER BY w.withdrawal_id DESC
        """,
        countQuery = """
        SELECT COUNT(w.withdrawal_id)
        FROM withdrawals w
        JOIN users u ON u.user_id = w.user_user_id
        WHERE (:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:status IS NULL OR w.status = :status)
          AND (:from IS NULL OR CAST(w.created_at AS date) >= CAST(:from AS date))
          AND (:to IS NULL OR CAST(w.created_at AS date) <= CAST(:to AS date))
        """,
        nativeQuery = true
    )
    fun findByFilters(
        @Param("search") search: String?,
        @Param("status") status: String?,
        @Param("from") from: LocalDate?,
        @Param("to") to: LocalDate?,
        pageable: Pageable
    ): Page<Array<Any?>>
}
