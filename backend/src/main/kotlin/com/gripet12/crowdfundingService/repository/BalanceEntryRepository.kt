package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.BalanceEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal

interface BalanceEntryRepository : JpaRepository<BalanceEntry, Long> {
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean

    fun findByIdempotencyKey(idempotencyKey: String): BalanceEntry?

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM BalanceEntry b WHERE b.user.userId = :userId AND b.frozen = false")
    fun sumAvailableByUserId(@Param("userId") userId: Long): BigDecimal

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM BalanceEntry b WHERE b.user.userId = :userId AND b.frozen = true")
    fun sumFrozenByUserId(@Param("userId") userId: Long): BigDecimal

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM BalanceEntry b WHERE b.user.userId = :userId")
    fun sumByUserId(@Param("userId") userId: Long): BigDecimal

    fun existsByUserUserId(userId: Long): Boolean

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM BalanceEntry b WHERE b.user.userId = :userId AND b.amount > 0")
    fun sumCreditsByUserId(@Param("userId") userId: Long): BigDecimal

    @Query("SELECT COALESCE(SUM(ABS(b.amount)), 0) FROM BalanceEntry b WHERE b.user.userId = :userId AND b.amount < 0")
    fun sumDebitsByUserId(@Param("userId") userId: Long): BigDecimal

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE BalanceEntry b SET b.frozen = false WHERE b.projectId = :projectId AND b.frozen = true")
    fun unfreezeByProjectId(@Param("projectId") projectId: Long): Int
}
