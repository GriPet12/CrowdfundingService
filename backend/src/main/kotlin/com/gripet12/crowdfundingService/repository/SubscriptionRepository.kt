package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Subscription
import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionRepository : JpaRepository<Subscription, Long> {
    fun findBySubscriptionId(subscriptionId: Long): Subscription?
}