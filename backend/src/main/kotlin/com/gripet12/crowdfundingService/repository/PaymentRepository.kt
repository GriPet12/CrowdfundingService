package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun save(payment: Payment): Payment
    fun findByPaymentId(paymentId: Long): Payment
    fun findByOrderReference(orderReference: String): Payment
}