package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.SubscriptionTier
import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionTierRepository : JpaRepository<SubscriptionTier, Long> {
    fun findByTierId(tierId: Long?): SubscriptionTier?
}