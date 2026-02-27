package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Subscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SubscriptionRepository : JpaRepository<Subscription, Long> {
    fun findBySubscriptionId(subscriptionId: Long): Subscription?

    @Query("SELECT s FROM Subscription s JOIN FETCH s.creator c LEFT JOIN FETCH c.image JOIN FETCH s.subscriptionTier JOIN FETCH s.payment WHERE s.subscrber.userId = :userId AND s.payment.status = 'APPROVED'")
    fun findApprovedBySubscrberUserId(userId: Long): List<Subscription>
}