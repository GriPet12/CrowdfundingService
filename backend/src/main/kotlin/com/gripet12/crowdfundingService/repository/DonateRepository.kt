package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Donate
import com.gripet12.crowdfundingService.model.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface DonateRepository : JpaRepository<Donate, Long> {
    fun save(donate: Donate): Donate
    fun findByPayment(payment: Payment): Donate?
}