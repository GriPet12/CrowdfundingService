package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByOrderReference(orderReference: String): Payment?

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Payment p SET p.status = :status WHERE p.orderReference = :orderReference")
    fun updateStatusByOrderReference(
        @Param("orderReference") orderReference: String,
        @Param("status") status: String
    ): Int
}