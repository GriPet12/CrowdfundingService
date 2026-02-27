package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.PaymentRequest
import com.gripet12.crowdfundingService.dto.WayForPayCallbackDto
import com.gripet12.crowdfundingService.model.Donate
import com.gripet12.crowdfundingService.model.Payment
import com.gripet12.crowdfundingService.model.Subscription
import com.gripet12.crowdfundingService.repository.DonateRepository
import com.gripet12.crowdfundingService.repository.PaymentRepository
import com.gripet12.crowdfundingService.repository.ProjectRepository
import com.gripet12.crowdfundingService.repository.SubscriptionRepository
import com.gripet12.crowdfundingService.repository.SubscriptionTierRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets

@Service
class PaymentService(
    private val donateRepository: DonateRepository,
    private val paymentRepository: PaymentRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val subscriptionTierRepository: SubscriptionTierRepository,
    private val subscriptionRepository: SubscriptionRepository
) {
    private val merchantAccount = "test_merch_n1"
    private val merchantSecret = "flk3409refn54t54t*FNJRET"
    private val merchantDomainName = "www.market.ua"

    private val returnUrl = "http://localhost:5173"
    private val serviceUrl = "http://localhost:8081/api/payment/callback"

    @Transactional
    fun generatePaymentData(request: PaymentRequest, type: String): Map<String, String> {
        val currency = "UAH"
        val orderDate = System.currentTimeMillis() / 1000
        val productName = "CrowdfundingDonation"
        val productCount = "1"

        val formattedAmount = "%.2f".format(request.amount).replace(",", ".")

        val orderId = createPayment(request, type)

        val stringToSign = listOf(
            merchantAccount,
            merchantDomainName,
            orderId,
            orderDate.toString(),
            formattedAmount,
            currency,
            productName,
            productCount,
            formattedAmount
        ).joinToString(";")

        val signature = hmacMd5(stringToSign, merchantSecret)

        return mapOf(
            "merchantAccount"   to merchantAccount,
            "merchantDomainName" to merchantDomainName,
            "merchantSignature" to signature,
            "orderReference"    to orderId,
            "orderDate"         to orderDate.toString(),
            "amount"            to formattedAmount,
            "currency"          to currency,
            "productName[]"     to productName,
            "productCount[]"    to productCount,
            "productPrice[]"    to formattedAmount,
            "returnUrl"         to returnUrl,
            "serviceUrl"        to serviceUrl
        )
    }

    private fun hmacMd5(data: String, key: String): String {
        val algorithm = "HmacMD5"
        val secretKeySpec = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(secretKeySpec)
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

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
            val project = projectRepository.getReferenceById(request.project)

            val donate = Donate(
                donateId = null,
                donor = donorUser,
                project = project,
                amount = request.amount.toBigDecimal(),
                reward = request.reward,
                payment = savedPayment,
                isAnonymous = request.isAnonymous
            )
            donateRepository.save(donate)

        } else if (type == "SUBSCRIPTION") {
            val subscription = Subscription(
                subscriptionId = null,
                subscrber = userRepository.getReferenceById(request.donor),
                creator = userRepository.getReferenceById(request.creator),
                subscriptionTier = subscriptionTierRepository.findByTierId(request.reward.toLong()),
                payment = savedPayment,
                tierPrice = request.amount.toBigDecimal()
            )
            subscriptionRepository.save(subscription)
        }

        return orderReference
    }

    @Transactional
    fun processPaymentCallback(response: WayForPayCallbackDto) {
        val payment = paymentRepository.findByOrderReference(response.orderReference)
        payment.status = if (response.transactionStatus == "Approved") "APPROVED" else "DECLINED"
        paymentRepository.save(payment)

        if (response.transactionStatus == "Approved") {
            val donate = donateRepository.findByPayment(payment)
            if (donate != null) {
                projectRepository.increaseCollectedAmount(
                    donate.project.projectId!!,
                    donate.amount
                )
            }
        }
    }
}