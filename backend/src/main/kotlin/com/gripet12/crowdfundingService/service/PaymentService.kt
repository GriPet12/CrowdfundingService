package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.PaymentRequest
import com.gripet12.crowdfundingService.model.Donate
import com.gripet12.crowdfundingService.model.Payment
import com.gripet12.crowdfundingService.model.Subscription
import com.gripet12.crowdfundingService.repository.DonateRepository
import com.gripet12.crowdfundingService.repository.PaymentRepository
import com.gripet12.crowdfundingService.repository.ProjectRepository
import com.gripet12.crowdfundingService.repository.SubscriptionRepository
import com.gripet12.crowdfundingService.repository.SubscriptionTierRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import com.stripe.Stripe
import com.stripe.model.Charge
import com.stripe.model.Event
import com.stripe.model.PaymentIntent
import com.stripe.net.Webhook
import com.stripe.param.PaymentIntentCreateParams
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Collections

@Service
class PaymentService(
    private val donateRepository: DonateRepository,
    private val paymentRepository: PaymentRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val subscriptionTierRepository: SubscriptionTierRepository,
    private val subscriptionRepository: SubscriptionRepository,
    @Lazy private val subscriptionService: SubscriptionService,
    @Lazy private val self: PaymentService,
    @Value("\${stripe.secret-key}") private val stripeSecretKey: String,
    @Value("\${stripe.webhook-secret}") private val webhookSecret: String,
    @Value("\${stripe.return-url:http://localhost:5173}") private val returnUrl: String
) {
    private val log = LoggerFactory.getLogger(PaymentService::class.java)

    @PostConstruct
    fun init() {
        Stripe.apiKey = stripeSecretKey
    }

    /**
     * Creates a Stripe PaymentIntent and returns its client_secret together
     * with the internal orderReference so the frontend can confirm the payment
     * and the backend can correlate the webhook.
     */
    @Transactional
    fun generatePaymentData(request: PaymentRequest, type: String): Map<String, String> {
        val orderReference = createPayment(request, type)

        // Stripe amounts are in the smallest currency unit (cents for USD, kopecks for UAH…)
        val amountInCents = (request.amount * 100).toLong()

        val params = PaymentIntentCreateParams.builder()
            .setAmount(amountInCents)
            .setCurrency("usd")
            .putMetadata("orderReference", orderReference)
            .putMetadata("type", type)
            .build()

        val intent = PaymentIntent.create(params)
        log.info("Created PaymentIntent ${intent.id} for orderReference=$orderReference amount=${request.amount}")

        return mapOf(
            "clientSecret"     to intent.clientSecret,
            "paymentIntentId"  to intent.id,
            "orderReference"   to orderReference,
            "amount"           to request.amount.toString(),
            "currency"         to "usd",
            "returnUrl"        to returnUrl
        )
    }

    @Transactional
    fun createPayment(request: PaymentRequest, type: String): String {
        val payment = Payment(
            paymentId = null,
            amount = request.amount.toBigDecimal(),
            status = "PENDING"
        )
        val savedPayment = paymentRepository.save(payment)
        val orderReference = savedPayment.orderReference

        if (type == "DONATION") {
            val donorUser = if (request.isAnonymous || request.donateId == 0L) null
                            else userRepository.getReferenceById(request.donateId)

            val project = if (request.project > 0L) projectRepository.getReferenceById(request.project) else null

            val creatorUser = if (project == null && request.creator > 0L)
                userRepository.getReferenceById(request.creator) else null

            donateRepository.save(Donate(
                donateId  = null,
                donor     = donorUser,
                project   = project,
                creator   = creatorUser,
                amount    = request.amount.toBigDecimal(),
                reward    = request.reward,
                payment   = savedPayment,
                isAnonymous = request.isAnonymous
            ))
        } else if (type == "SUBSCRIPTION") {
            subscriptionRepository.save(Subscription(
                subscriptionId   = null,
                subscrber        = userRepository.getReferenceById(request.donor),
                creator          = userRepository.getReferenceById(request.creator),
                subscriptionTier = subscriptionTierRepository.findByTierId(request.reward.toLong()),
                payment          = savedPayment,
                tierPrice        = request.amount.toBigDecimal()
            ))
        }

        return orderReference
    }

    // Deduplication: track already-processed Stripe event IDs (in-memory, sufficient for single-instance)
    private val processedEventIds: MutableSet<String> =
        Collections.synchronizedSet(LinkedHashSet<String>())

    /** Extracts the "id" field from a raw Stripe JSON object string. */
    private fun extractIdFromRawJson(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val match = Regex(""""id"\s*:\s*"([^"]+)"""").find(raw)
        return match?.groupValues?.get(1)
    }

    /**
     * Handles raw Stripe webhook events.
     * Throws SignatureVerificationException for invalid signatures (→ 400).
     * Uses deserializeUnsafe() to bypass API-version mismatch deserialization failures.
     * Falls back to retrieve() by ID extracted from raw JSON if deserialization still fails.
     */
    fun processStripeWebhook(payload: String, sigHeader: String) {
        val event: Event = Webhook.constructEvent(payload, sigHeader, webhookSecret)
        log.info("Received Stripe event: ${event.type} id=${event.id}")

        // Deduplicate: Stripe may deliver the same event more than once
        if (!processedEventIds.add(event.id)) {
            log.info("Duplicate Stripe event ${event.id} — skipping")
            return
        }
        // Keep the set bounded
        if (processedEventIds.size > 1000) {
            val oldest = processedEventIds.iterator()
            oldest.next()
            oldest.remove()
        }

        when (event.type) {
            "payment_intent.succeeded" -> {
                val intent = resolvePaymentIntent(event) ?: return
                val orderReference = intent.metadata["orderReference"]
                if (orderReference.isNullOrBlank()) {
                    log.warn("No orderReference in metadata for PaymentIntent ${intent.id}")
                    return
                }
                log.info("Payment succeeded: intentId=${intent.id} orderReference=$orderReference")
                self.approvePayment(orderReference)
            }

            "charge.succeeded", "charge.updated" -> {
                val charge = resolveCharge(event) ?: return
                if (charge.status != "succeeded") {
                    log.debug("${event.type} ignored: status=${charge.status} chargeId=${charge.id}")
                    return
                }
                val paymentIntentId = charge.paymentIntent
                if (paymentIntentId.isNullOrBlank()) {
                    log.warn("No paymentIntentId on charge ${charge.id}")
                    return
                }
                val intent = PaymentIntent.retrieve(paymentIntentId)
                val orderReference = intent.metadata["orderReference"]
                if (orderReference.isNullOrBlank()) {
                    log.warn("No orderReference in metadata for PaymentIntent ${intent.id} (charge ${charge.id})")
                    return
                }
                log.info("Charge succeeded: chargeId=${charge.id} intentId=${intent.id} orderReference=$orderReference")
                self.approvePayment(orderReference)
            }

            "payment_intent.payment_failed" -> {
                val intent = resolvePaymentIntent(event) ?: return
                val orderReference = intent.metadata["orderReference"] ?: return
                log.warn("Payment failed: intentId=${intent.id} orderReference=$orderReference")
                self.declinePayment(orderReference)
            }

            else -> log.debug("Ignored Stripe event type: ${event.type}")
        }
    }

    private fun resolvePaymentIntent(event: Event): PaymentIntent? {
        val deserialized = runCatching { event.dataObjectDeserializer.deserializeUnsafe() as PaymentIntent }
        if (deserialized.isSuccess) return deserialized.getOrNull()
        log.warn("deserializeUnsafe failed for ${event.id}: ${deserialized.exceptionOrNull()?.message}")
        val id = extractIdFromRawJson(event.dataObjectDeserializer.rawJson)
        if (id.isNullOrBlank()) { log.warn("Cannot extract id from raw JSON for event ${event.id}"); return null }
        return runCatching { PaymentIntent.retrieve(id) }.getOrElse {
            log.error("Failed to retrieve PaymentIntent $id: ${it.message}"); null
        }
    }

    private fun resolveCharge(event: Event): Charge? {
        val deserialized = runCatching { event.dataObjectDeserializer.deserializeUnsafe() as Charge }
        if (deserialized.isSuccess) return deserialized.getOrNull()
        log.warn("deserializeUnsafe failed for ${event.id}: ${deserialized.exceptionOrNull()?.message}")
        val id = extractIdFromRawJson(event.dataObjectDeserializer.rawJson)
        if (id.isNullOrBlank()) { log.warn("Cannot extract id from raw JSON for event ${event.id}"); return null }
        return runCatching { Charge.retrieve(id) }.getOrElse {
            log.error("Failed to retrieve Charge $id: ${it.message}"); null
        }
    }

    @Transactional
    fun approvePayment(orderReference: String) {
        val payment = paymentRepository.findByOrderReference(orderReference)
        if (payment == null) {
            log.warn("approvePayment: Payment not found for orderReference=$orderReference")
            return
        }
        if (payment.status == "APPROVED") {
            log.warn("Payment $orderReference already APPROVED, skipping")
            return
        }

        // Direct UPDATE — bypasses Hibernate dirty-check issues with data classes
        val updated = paymentRepository.updateStatusByOrderReference(orderReference, "APPROVED")
        log.info("Payment $orderReference → APPROVED (rows updated: $updated)")

        // Re-fetch after the direct UPDATE so the entity is fresh (not stale from L1 cache)
        val freshPayment = paymentRepository.findByOrderReference(orderReference)!!

        val donate = donateRepository.findByPayment(freshPayment)
        if (donate != null) {
            if (donate.project != null) {
                projectRepository.increaseCollectedAmount(donate.project.projectId!!, donate.amount)
                log.info("Increased collectedAmount for project ${donate.project.projectId} by ${donate.amount}")
            }
            val donorId   = donate.donor?.userId
            val creatorId = donate.project?.creator?.userId
            if (donorId != null && creatorId != null) {
                subscriptionService.checkAndGrantAutoSubscription(donorId, creatorId)
            }
        }

        val subscription = subscriptionRepository.findByPayment(freshPayment)
        if (subscription != null) {
            subscription.active    = true
            subscription.expiresAt = LocalDate.now().plusMonths(1)
            subscriptionRepository.save(subscription)
            log.info("Activated subscription ${subscription.subscriptionId}")
        }
    }

    @Transactional
    fun declinePayment(orderReference: String) {
        val updated = paymentRepository.updateStatusByOrderReference(orderReference, "DECLINED")
        log.info("Payment $orderReference → DECLINED (rows updated: $updated)")
    }
}