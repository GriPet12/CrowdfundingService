package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.BalanceSummaryDto
import com.gripet12.crowdfundingService.dto.ConnectOnboardingDto
import com.gripet12.crowdfundingService.dto.ConnectStatusDto
import com.gripet12.crowdfundingService.dto.WithdrawalDto
import com.gripet12.crowdfundingService.model.BalanceEntry
import com.gripet12.crowdfundingService.model.Payment
import com.gripet12.crowdfundingService.model.User
import com.gripet12.crowdfundingService.model.Withdrawal
import com.gripet12.crowdfundingService.repository.BalanceEntryRepository
import com.gripet12.crowdfundingService.repository.DonateRepository
import com.gripet12.crowdfundingService.repository.SubscriptionRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import com.gripet12.crowdfundingService.repository.WithdrawalRepository
import com.stripe.Stripe
import com.stripe.model.Account
import com.stripe.model.Event
import com.stripe.param.AccountCreateParams
import com.stripe.param.AccountLinkCreateParams
import com.stripe.param.TransferCreateParams
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.time.LocalDateTime

@Service
class BalanceService(
    private val balanceEntryRepository: BalanceEntryRepository,
    private val withdrawalRepository: WithdrawalRepository,
    private val userRepository: UserRepository,
    private val donateRepository: DonateRepository,
    private val subscriptionRepository: SubscriptionRepository,
    @Value("\${stripe.secret-key}") private val stripeSecretKey: String,
    @Value("\${stripe.currency:uah}") private val stripeCurrency: String,
    @Value("\${stripe.return-url:http://localhost:5173}") private val returnUrl: String,
    @Value("\${platform.fee-percent:5}") private val platformFeePercent: Int,
    @Value("\${platform.min-withdrawal:100}") private val minWithdrawalAmount: BigDecimal,
    @Value("\${platform.connect-country:UA}") private val connectCountry: String
) {
    private val log = LoggerFactory.getLogger(BalanceService::class.java)
    private val currency = stripeCurrency.lowercase()

    @PostConstruct
    fun init() {
        Stripe.apiKey = stripeSecretKey
    }

    private fun currentUserId(): Long {
        val username = SecurityContextHolder.getContext().authentication.name
        return userRepository.findByUsername(username)
            .orElseThrow { IllegalStateException("User not found") }
            .userId!!
    }

    private fun currentUser(): User =
        userRepository.findByUserId(currentUserId())

    private fun netAmount(gross: BigDecimal): BigDecimal {
        if (platformFeePercent <= 0) return gross
        val fee = gross
            .multiply(BigDecimal(platformFeePercent))
            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        return gross.subtract(fee).max(BigDecimal.ZERO)
    }

    private fun toMinorUnits(amount: BigDecimal): Long =
        amount.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()

    @Transactional
    fun creditFromApprovedPayment(payment: Payment) {
        val key = "pay-${payment.orderReference}"
        if (balanceEntryRepository.existsByIdempotencyKey(key)) return

        val donate = donateRepository.findByPayment(payment)
        if (donate != null) {
            val creatorId = donate.creator?.userId ?: donate.project?.creator?.userId ?: return
            creditUser(
                creatorId = creatorId,
                grossAmount = payment.amount,
                entryType = "DONATION",
                idempotencyKey = key,
                description = "Донат ${payment.orderReference.take(8)}"
            )
            return
        }

        val subscription = subscriptionRepository.findByPayment(payment) ?: return
        val creatorId = subscription.creator.userId ?: return
        creditUser(
            creatorId = creatorId,
            grossAmount = payment.amount,
            entryType = "SUBSCRIPTION",
            idempotencyKey = key,
            description = "Підписка ${payment.orderReference.take(8)}"
        )
    }

    private fun creditUser(
        creatorId: Long,
        grossAmount: BigDecimal,
        entryType: String,
        idempotencyKey: String,
        description: String
    ) {
        if (balanceEntryRepository.existsByIdempotencyKey(idempotencyKey)) return

        val net = netAmount(grossAmount)
        if (net <= BigDecimal.ZERO) return

        val creator = userRepository.getReferenceById(creatorId)
        balanceEntryRepository.save(
            BalanceEntry(
                user = creator,
                amount = net,
                entryType = entryType,
                idempotencyKey = idempotencyKey,
                description = description
            )
        )
        log.info("Balance credited: userId=$creatorId amount=$net type=$entryType key=$idempotencyKey")
    }

    @Transactional
    fun backfillBalanceIfNeeded(userId: Long) {
        if (balanceEntryRepository.existsByUserUserId(userId)) return

        donateRepository.findAllApprovedForCreator(userId).forEach { donate ->
            val payment = donate.payment ?: return@forEach
            creditFromApprovedPayment(payment)
        }
        subscriptionRepository.findAllApprovedPaidForCreator(userId).forEach { sub ->
            val payment = sub.payment ?: return@forEach
            creditFromApprovedPayment(payment)
        }
        log.info("Balance backfill completed for userId=$userId")
    }

    @Transactional
    fun getBalanceSummary(): BalanceSummaryDto {
        val userId = currentUserId()
        backfillBalanceIfNeeded(userId)
        val user = currentUser()
        val available = balanceEntryRepository.sumByUserId(userId)
        val earned = balanceEntryRepository.sumCreditsByUserId(userId)
        val withdrawn = balanceEntryRepository.sumDebitsByUserId(userId)

        return BalanceSummaryDto(
            availableBalance = available,
            totalEarned = earned,
            totalWithdrawn = withdrawn,
            platformFeePercent = platformFeePercent,
            minWithdrawal = minWithdrawalAmount,
            stripeConnected = !user.stripeConnectAccountId.isNullOrBlank(),
            stripePayoutsEnabled = user.stripePayoutsEnabled
        )
    }

    @Transactional(readOnly = true)
    fun getWithdrawals(): List<WithdrawalDto> {
        val userId = currentUserId()
        return withdrawalRepository.findByUserUserIdOrderByWithdrawalIdDesc(userId).map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getConnectStatus(): ConnectStatusDto {
        val user = currentUser()
        return ConnectStatusDto(
            connected = !user.stripeConnectAccountId.isNullOrBlank(),
            payoutsEnabled = user.stripePayoutsEnabled,
            accountId = user.stripeConnectAccountId
        )
    }

    @Transactional
    fun createConnectOnboarding(): ConnectOnboardingDto {
        val user = currentUser()
        if (user.banned) throw IllegalStateException("Заблокований акаунт не може підключати виплати")

        val accountId = user.stripeConnectAccountId ?: run {
            val account = Account.create(
                AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setCountry(connectCountry)
                    .setEmail(user.email)
                    .setCapabilities(
                        AccountCreateParams.Capabilities.builder()
                            .setTransfers(
                                AccountCreateParams.Capabilities.Transfers.builder()
                                    .setRequested(true)
                                    .build()
                            )
                            .build()
                    )
                    .putMetadata("userId", user.userId.toString())
                    .build()
            )
            user.stripeConnectAccountId = account.id
            userRepository.save(user)
            account.id
        }

        refreshConnectStatus(user)

        val link = com.stripe.model.AccountLink.create(
            AccountLinkCreateParams.builder()
                .setAccount(accountId)
                .setRefreshUrl("$returnUrl/me?tab=balance&connect=refresh")
                .setReturnUrl("$returnUrl/me?tab=balance&connect=done")
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build()
        )
        return ConnectOnboardingDto(url = link.url)
    }

    @Transactional
    fun requestWithdrawal(amount: BigDecimal): WithdrawalDto {
        val user = currentUser()
        if (user.banned) throw IllegalStateException("Заблокований акаунт не може виводити кошти")
        if (amount < minWithdrawalAmount) {
            throw IllegalArgumentException("Мінімальна сума виведення: ₴$minWithdrawalAmount")
        }

        backfillBalanceIfNeeded(user.userId!!)
        val available = balanceEntryRepository.sumByUserId(user.userId!!)
        if (amount > available) {
            throw IllegalArgumentException("Недостатньо коштів на балансі. Доступно: ₴$available")
        }

        val withdrawal = withdrawalRepository.save(
            Withdrawal(user = user, amount = amount, status = "PROCESSING")
        )

        balanceEntryRepository.save(
            BalanceEntry(
                user = user,
                amount = amount.negate(),
                entryType = "WITHDRAWAL",
                idempotencyKey = "withdrawal-${withdrawal.withdrawalId}",
                description = "Виведення #${withdrawal.withdrawalId}"
            )
        )

        val accountId = user.stripeConnectAccountId
        if (accountId.isNullOrBlank() || !user.stripePayoutsEnabled) {
            withdrawal.status = "PENDING"
            withdrawal.failureReason = "Підключіть Stripe для автоматичного виведення"
            withdrawalRepository.save(withdrawal)
            return withdrawal.toDto()
        }

        return processStripeTransfer(withdrawal, user, accountId)
    }

    private fun processStripeTransfer(withdrawal: Withdrawal, user: User, accountId: String): WithdrawalDto {
        return try {
            val transfer = com.stripe.model.Transfer.create(
                TransferCreateParams.builder()
                    .setAmount(toMinorUnits(withdrawal.amount))
                    .setCurrency(currency)
                    .setDestination(accountId)
                    .putMetadata("withdrawalId", withdrawal.withdrawalId.toString())
                    .putMetadata("userId", user.userId.toString())
                    .build()
            )
            withdrawal.status = "COMPLETED"
            withdrawal.stripeTransferId = transfer.id
            withdrawal.processedAt = Timestamp(System.currentTimeMillis())
            withdrawalRepository.save(withdrawal)
            log.info("Withdrawal completed: id=${withdrawal.withdrawalId} transfer=${transfer.id} amount=${withdrawal.amount}")
            withdrawal.toDto()
        } catch (ex: Exception) {
            log.error("Stripe transfer failed for withdrawal ${withdrawal.withdrawalId}: ${ex.message}", ex)
            reverseWithdrawal(withdrawal, ex.message ?: "Stripe transfer failed")
            throw IllegalStateException("Не вдалося виконати виведення через Stripe: ${ex.message}")
        }
    }

    private fun reverseWithdrawal(withdrawal: Withdrawal, reason: String) {
        withdrawal.status = "FAILED"
        withdrawal.failureReason = reason
        withdrawal.processedAt = Timestamp(System.currentTimeMillis())
        withdrawalRepository.save(withdrawal)

        balanceEntryRepository.save(
            BalanceEntry(
                user = withdrawal.user,
                amount = withdrawal.amount,
                entryType = "ADJUSTMENT",
                idempotencyKey = "withdrawal-reversal-${withdrawal.withdrawalId}",
                description = "Повернення коштів після невдалого виведення #${withdrawal.withdrawalId}"
            )
        )
    }

    @Transactional
    fun handleAccountUpdated(event: Event) {
        val account = runCatching {
            event.dataObjectDeserializer.deserializeUnsafe() as Account
        }.getOrNull() ?: return
        val userId = account.metadata?.get("userId")?.toLongOrNull()
        val user = when {
            userId != null -> runCatching { userRepository.findByUserId(userId) }.getOrNull()
            else -> userRepository.findByStripeConnectAccountId(account.id)
        } ?: return

        user.stripeConnectAccountId = account.id
        user.stripePayoutsEnabled = account.payoutsEnabled == true && account.chargesEnabled == true
        userRepository.save(user)
        log.info("Stripe Connect updated: userId=${user.userId} payoutsEnabled=${user.stripePayoutsEnabled}")
    }

    private fun refreshConnectStatus(user: User) {
        val accountId = user.stripeConnectAccountId ?: return
        runCatching {
            val account = Account.retrieve(accountId)
            user.stripePayoutsEnabled = account.payoutsEnabled == true && account.chargesEnabled == true
            userRepository.save(user)
        }.onFailure { log.warn("Could not refresh Connect status for user ${user.userId}: ${it.message}") }
    }

    private fun Withdrawal.toDto() = WithdrawalDto(
        withdrawalId = withdrawalId!!,
        amount = amount,
        status = status,
        stripeTransferId = stripeTransferId,
        failureReason = failureReason,
        createdAt = createdAt.toLocalDateTime(),
        processedAt = processedAt?.toLocalDateTime()
    )
}
