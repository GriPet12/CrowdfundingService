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
import java.nio.charset.StandardCharsets

@Service
class PaymentService (
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

    fun generatePaymentData(request: PaymentRequest, type: String): Map<String, String> {
        val currency = "UAH"
        val orderDate = System.currentTimeMillis() / 1000
        val productName = "CrowdfundingDonation"
        val productCount = "1"

        val formattedAmount = String.format("%.2f", request.amount).replace(",", ".")

        val orderId = createPayment(request, type).toString()

        val stringToSign = "$merchantAccount;$merchantDomainName;$orderId;$orderDate;$formattedAmount;$currency;$productName;$productCount;$formattedAmount"

        val signature = hmacMd5(stringToSign, merchantSecret)

        return mapOf(
            "merchantAccount" to merchantAccount,
            "merchantDomainName" to merchantDomainName,
            "merchantSignature" to signature,
            "orderReference" to orderId,
            "orderDate" to orderDate.toString(),
            "amount" to formattedAmount,
            "currency" to currency,
            "productName[]" to productName,
            "productCount[]" to productCount,
            "productPrice[]" to formattedAmount
        )
    }

    private fun hmacMd5(data: String, key: String): String {
        val algorithm = "HmacMD5"
        val secretKeySpec = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(secretKeySpec)

        val bytes = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))

        return bytes.joinToString("") { "%02x".format(it) }
    }


    fun createPayment(request: PaymentRequest, type: String): Long? {
        val payment = Payment(
            paymentId = null,
            amount = request.amount.toBigDecimal(),
            status = "PENDING",
        )

        val paymentId = paymentRepository.save(payment).paymentId

        if (type == "DONATION") {
            val donate = Donate(
                donateId = null,
                donor = userRepository.findByUserId(request.donateId),
                project = projectRepository.findByProjectId(request.project),
                amount = request.amount.toBigDecimal(),
                reward = request.reward,
                payment = payment
            )

            donateRepository.save(donate)
        } else if (type == "SUBSCRIPTION") {
            val subscription = Subscription(
                subscriptionId = null,
                subscrber = userRepository.findByUserId(request.donor),
                creator = userRepository.findByUserId(request.creator),
                subscriptionTier = subscriptionTierRepository.findByTierId(request.reward.toLong()),
                payment = payment,
                tierPrice = request.amount.toBigDecimal()
            )

            subscriptionRepository.save(subscription)
        }

        return paymentId
    }

    fun processPaymentCallback(response: WayForPayCallbackDto) {

        val paymentId = response.orderReference.toLong()
        val payment = paymentRepository.findByPaymentId(paymentId)

        if (response.transactionStatus == "Approved") {
            payment.status = "APPROVED"

        } else {
            payment.status = "DECLINED"
        }
        paymentRepository.save(payment)
    }
}