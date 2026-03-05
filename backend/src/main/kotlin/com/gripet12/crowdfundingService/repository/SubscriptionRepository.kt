package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Payment
import com.gripet12.crowdfundingService.model.Subscription
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal
import java.time.LocalDate

interface SubscriptionRepository : JpaRepository<Subscription, Long> {

    @Query("SELECT s FROM Subscription s JOIN FETCH s.creator c LEFT JOIN FETCH c.image JOIN FETCH s.subscriptionTier WHERE s.subscrber.userId = :userId AND s.active = true")
    fun findApprovedBySubscrberUserId(userId: Long): List<Subscription>

    @Query("SELECT s FROM Subscription s JOIN FETCH s.creator c LEFT JOIN FETCH c.image JOIN FETCH s.subscriptionTier WHERE s.subscrber.userId = :userId ORDER BY s.subscriptionId DESC")
    fun findAllBySubscriberUserId(userId: Long): List<Subscription>

    fun findByPayment(payment: Payment): Subscription?

    fun findBySubscrberUserIdAndCreatorUserIdAndActiveTrue(subscriberId: Long, creatorId: Long): List<Subscription>

    @Query("SELECT s FROM Subscription s WHERE s.active = true AND s.expiresAt IS NOT NULL AND s.expiresAt <= :today")
    fun findExpiredActiveSubscriptions(today: LocalDate): List<Subscription>

    @Query("SELECT s FROM Subscription s WHERE s.subscrber.userId = :subscriberId AND s.creator.userId = :creatorId AND s.active = true AND s.grantType = 'AUTO' AND s.subscriptionTier.level = :level")
    fun findActiveAutoSubscriptionByLevel(subscriberId: Long, creatorId: Long, level: Int): List<Subscription>

    @Query("SELECT s FROM Subscription s JOIN FETCH s.subscriptionTier WHERE s.subscrber.userId = :subscriberId AND s.creator.userId = :creatorId AND s.active = true")
    fun findActiveSubscriptionsBySubscriberAndCreator(subscriberId: Long, creatorId: Long): List<Subscription>

    fun countByCreatorUserIdAndActiveTrue(creatorId: Long): Long

    fun findBySubscrberUserIdAndActiveTrue(subscriberId: Long): List<Subscription>

    @Query("SELECT COALESCE(SUM(s.tierPrice), 0) FROM Subscription s JOIN s.payment p WHERE s.creator.userId = :creatorId AND p.status = 'APPROVED'")
    fun sumSubscriptionRevenueForCreator(creatorId: Long): BigDecimal

    @Query("SELECT COUNT(s) FROM Subscription s JOIN s.payment p WHERE s.creator.userId = :creatorId AND p.status = 'APPROVED'")
    fun countApprovedSubscriptionsForCreator(creatorId: Long): Long

    @Query(value = """
        SELECT
            s.subscription_id,
            us.username  AS from_user,
            uc.username  AS to_user,
            s.tier_price,
            pay.status
        FROM subscriptions s
        LEFT JOIN payments pay ON pay.payment_id = s.payment_payment_id
        JOIN users us ON us.user_id = s.subscrber_user_id
        JOIN users uc ON uc.user_id = s.creator_user_id
        ORDER BY s.subscription_id DESC
    """,
    countQuery = "SELECT COUNT(*) FROM subscriptions",
    nativeQuery = true)
    fun findAllScalar(pageable: Pageable): Page<Array<Any?>>


    @Query("SELECT COALESCE(SUM(s.tierPrice), 0) FROM Subscription s JOIN s.payment p WHERE p.status = 'APPROVED'")
    fun sumAllApprovedSubscriptions(): BigDecimal

    @Query("SELECT COUNT(s) FROM Subscription s JOIN s.payment p WHERE p.status = 'APPROVED'")
    fun countAllApprovedSubscriptions(): Long
}
