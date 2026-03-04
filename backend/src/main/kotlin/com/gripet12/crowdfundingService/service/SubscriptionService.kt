package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.SubscriptionDto
import com.gripet12.crowdfundingService.model.Subscription
import com.gripet12.crowdfundingService.repository.DonateRepository
import com.gripet12.crowdfundingService.repository.SubscriptionRepository
import com.gripet12.crowdfundingService.repository.SubscriptionTierRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionTierRepository: SubscriptionTierRepository,
    private val donateRepository: DonateRepository,
    private val userRepository: UserRepository
) {
    private val log = LoggerFactory.getLogger(SubscriptionService::class.java)

    private fun currentUserId(): Long {
        val username = SecurityContextHolder.getContext().authentication.name
        return userRepository.findByUsername(username)
            .orElseThrow { IllegalStateException("User not found") }
            .userId!!
    }


    @Transactional(readOnly = true)
    fun getSubscriptionStatusForCreator(creatorId: Long): List<SubscriptionDto> {
        val userId = currentUserId()
        return subscriptionRepository.findActiveSubscriptionsBySubscriberAndCreator(userId, creatorId)
            .map { it.toDto() }
    }

    @Transactional
    fun checkAndGrantAutoSubscription(donorId: Long, creatorId: Long) {
        val since = Timestamp.valueOf(LocalDateTime.now().minusDays(30))
        val totalDonated: BigDecimal = donateRepository.sumApprovedDonationsByDonorToCreatorSince(
            donorId, creatorId, since
        )

        log.info("Auto-sub check: donor=$donorId creator=$creatorId totalDonated=$totalDonated")

        val eligibleTier = subscriptionTierRepository.findByCreatorId(creatorId)
            .filter { it.amount <= totalDonated.toLong() }
            .maxByOrNull { it.level }
            ?: return 

        val subscriber = userRepository.getReferenceById(donorId)
        val creator = userRepository.getReferenceById(creatorId)

        val existing = subscriptionRepository.findActiveAutoSubscriptionByLevel(
            donorId, creatorId, eligibleTier.level
        )

        val expiryDate = LocalDate.now().plusMonths(1)

        if (existing.isNotEmpty()) {

            val sub = existing.first()
            sub.expiresAt = expiryDate
            subscriptionRepository.save(sub)
            log.info("Auto-sub extended: subscriptionId=${sub.subscriptionId} expiresAt=$expiryDate")
        } else {

            subscriptionRepository.findBySubscrberUserIdAndCreatorUserIdAndActiveTrue(donorId, creatorId)
                .filter { it.grantType == "AUTO" && (it.subscriptionTier?.level ?: 0) < eligibleTier.level }
                .forEach { old ->
                    old.active = false
                    subscriptionRepository.save(old)
                }

            val newSub = Subscription(
                subscriptionId = null,
                subscrber = subscriber,
                creator = creator,
                subscriptionTier = eligibleTier,
                payment = null,
                tierPrice = eligibleTier.amount.toBigDecimal(),
                expiresAt = expiryDate,
                active = true,
                grantType = "AUTO"
            )
            subscriptionRepository.save(newSub)
            log.info("Auto-sub granted: donor=$donorId creator=$creatorId tier=${eligibleTier.tierId} level=${eligibleTier.level} expiresAt=$expiryDate")
        }
    }

    @Scheduled(cron = "0 0 12 * * *")
    @Transactional
    fun expireSubscriptions() {
        val today = LocalDate.now()
        val expired = subscriptionRepository.findExpiredActiveSubscriptions(today)
        log.info("Subscription expiry job: checking ${expired.size} subscriptions (today=$today)")
        expired.forEach { sub ->
            sub.active = false
            subscriptionRepository.save(sub)
            log.info("Subscription expired: id=${sub.subscriptionId} userId=${sub.subscrber.userId} creatorId=${sub.creator.userId}")
        }
    }

    private fun Subscription.toDto(): SubscriptionDto {
        val tier = subscriptionTier
        return SubscriptionDto(
            subscriptionId = subscriptionId,
            creatorId = creator.userId!!,
            creatorName = creator.username,
            creatorImageId = creator.image?.id,
            tierName = tier?.name ?: "—",
            tierLevel = tier?.level ?: 0,
            tierPrice = tierPrice,
            paymentStatus = payment?.status ?: "AUTO",
            expiresAt = expiresAt,
            isActive = active,
            grantType = grantType
        )
    }
}
