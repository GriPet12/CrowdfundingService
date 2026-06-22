package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.BalanceSummaryDto
import com.gripet12.crowdfundingService.dto.ConnectOnboardingDto
import com.gripet12.crowdfundingService.dto.ConnectStatusDto
import com.gripet12.crowdfundingService.dto.WithdrawalRequestDto
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
import com.stripe.exception.StripeException
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
    @Value("\${platform.connect-country:UA}") private val connectCountry: String,
    @Value("\${platform.connect-enabled:false}") private val connectEnabled: Boolean
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
            val project = donate.project
            val frozen = project != null && !project.fundraisingClosed
            creditUser(
                creatorId = creatorId,
                grossAmount = payment.amount,
                entryType = "DONATION",
                idempotencyKey = key,
                description = if (project != null) "Донат на проект «${project.title}»" else "Донат ${payment.orderReference.take(8)}",
                frozen = frozen,
                projectId = project?.projectId
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
        description: String,
        frozen: Boolean = false,
        projectId: Long? = null
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
                description = description,
                frozen = frozen,
                projectId = projectId
            )
        )
        log.info("Balance credited: userId=$creatorId amount=$net frozen=$frozen projectId=$projectId type=$entryType key=$idempotencyKey")
    }

    @Transactional
    fun unfreezeProjectFunds(projectId: Long) {
        val updated = balanceEntryRepository.unfreezeByProjectId(projectId)
        log.info("Unfroze project funds: projectId=$projectId entries=$updated")
    }

    @Transactional
    fun syncProjectFrozenEntries(userId: Long) {
        donateRepository.findAllApprovedForCreator(userId)
            .filter { it.project != null && it.payment != null }
            .forEach { donate ->
                val payment = donate.payment ?: return@forEach
                val key = "pay-${payment.orderReference}"
                val entry = balanceEntryRepository.findByIdempotencyKey(key) ?: return@forEach
                val project = donate.project ?: return@forEach
                val shouldFreeze = !project.fundraisingClosed
                if (entry.projectId != project.projectId || entry.frozen != shouldFreeze) {
                    entry.projectId = project.projectId
                    entry.frozen = shouldFreeze
                    balanceEntryRepository.save(entry)
                }
            }
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
        syncProjectFrozenEntries(userId)
        val user = currentUser()
        val available = balanceEntryRepository.sumAvailableByUserId(userId)
        val frozen = balanceEntryRepository.sumFrozenByUserId(userId)
        val earned = balanceEntryRepository.sumCreditsByUserId(userId)
        val withdrawn = balanceEntryRepository.sumDebitsByUserId(userId)

        return BalanceSummaryDto(
            availableBalance = available,
            frozenBalance = frozen,
            totalEarned = earned,
            totalWithdrawn = withdrawn,
            platformFeePercent = platformFeePercent,
            minWithdrawal = minWithdrawalAmount,
            stripeConnected = !user.stripeConnectAccountId.isNullOrBlank(),
            stripePayoutsEnabled = user.stripePayoutsEnabled,
            connectAvailable = connectEnabled
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
            accountId = user.stripeConnectAccountId,
            connectAvailable = connectEnabled
        )
    }

    @Transactional
    fun createConnectOnboarding(): ConnectOnboardingDto {
        if (!connectEnabled) {
            throw IllegalArgumentException(
                "Автоматичні виплати через Stripe Connect ще не налаштовані. " +
                    "Створіть заявку на виведення нижче — адміністратор обробить її вручну."
            )
        }

        val user = currentUser()
        if (user.banned) throw IllegalStateException("Заблокований акаунт не може підключати виплати")

        return try {
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
            ConnectOnboardingDto(url = link.url)
        } catch (ex: StripeException) {
            log.error("Stripe Connect onboarding failed: ${ex.message}", ex)
            throw IllegalArgumentException(connectErrorMessage(ex))
        }
    }

    private fun connectErrorMessage(ex: StripeException): String {
        val msg = ex.message ?: ""
        if (msg.contains("signed up for Connect", ignoreCase = true)) {
            return "Stripe Connect не активовано в акаунті платформи. " +
                "Адміністратор має увімкнути Connect у Stripe Dashboard. " +
                "Поки що подайте заявку на виведення — її оброблять вручну."
        }
        return ex.userMessage ?: "Не вдалося підключити Stripe: $msg"
    }

    @Transactional
    fun requestWithdrawal(request: WithdrawalRequestDto): WithdrawalDto {
        val amount = request.amount
        val user = currentUser()
        if (user.banned) throw IllegalStateException("Заблокований акаунт не може виводити кошти")
        if (amount < minWithdrawalAmount) {
            throw IllegalArgumentException("Мінімальна сума виведення: ₴$minWithdrawalAmount")
        }

        backfillBalanceIfNeeded(user.userId!!)
        syncProjectFrozenEntries(user.userId!!)
        val available = balanceEntryRepository.sumAvailableByUserId(user.userId!!)
        if (amount > available) {
            throw IllegalArgumentException("Недостатньо доступних коштів. Доступно: ₴$available (частина зібрана на проекти заморожена до закриття збору)")
        }

        val usesManualPayout = !connectEnabled || user.stripeConnectAccountId.isNullOrBlank() || !user.stripePayoutsEnabled
        val payoutMethod = request.payoutMethod?.trim()?.uppercase()
        val payoutDestination = request.payoutDestination?.trim()
        val recipientName = request.recipientName?.trim()

        if (usesManualPayout) {
            validateManualPayoutDetails(payoutMethod, payoutDestination, recipientName)
        }

        val withdrawal = withdrawalRepository.save(
            Withdrawal(
                user = user,
                amount = amount,
                status = "PROCESSING",
                payoutMethod = payoutMethod,
                payoutDestination = payoutDestination,
                recipientName = recipientName
            )
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
        if (!connectEnabled || accountId.isNullOrBlank() || !user.stripePayoutsEnabled) {
            withdrawal.status = "PENDING"
            withdrawalRepository.save(withdrawal)
            log.info("Manual withdrawal queued: id=${withdrawal.withdrawalId} userId=${user.userId} amount=$amount")
            return withdrawal.toDto()
        }

        return processStripeTransfer(withdrawal, user, accountId)
    }

    private fun validateManualPayoutDetails(
        payoutMethod: String?,
        payoutDestination: String?,
        recipientName: String?
    ) {
        if (payoutMethod.isNullOrBlank() || payoutMethod !in setOf("CARD", "IBAN")) {
            throw IllegalArgumentException("Оберіть спосіб виплати: карта або IBAN")
        }
        if (payoutDestination.isNullOrBlank() || payoutDestination.length < 8) {
            throw IllegalArgumentException("Вкажіть номер картки або IBAN (мінімум 8 символів)")
        }
        if (recipientName.isNullOrBlank() || recipientName.length < 3) {
            throw IllegalArgumentException("Вкажіть ПІБ отримувача")
        }
        if (payoutMethod == "CARD") {
            val digits = payoutDestination.replace("\\s".toRegex(), "")
            if (!digits.matches(Regex("\\d{13,19}"))) {
                throw IllegalArgumentException("Номер картки має містити 13–19 цифр")
            }
        }
        if (payoutMethod == "IBAN") {
            val normalized = payoutDestination.replace("\\s".toRegex(), "").uppercase()
            if (!normalized.matches(Regex("UA\\d{27}"))) {
                throw IllegalArgumentException("IBAN має бути у форматі UA + 27 цифр")
            }
        }
    }

    @Transactional
    fun completeWithdrawalAdmin(withdrawalId: Long) {
        val withdrawal = withdrawalRepository.findById(withdrawalId)
            .orElseThrow { IllegalArgumentException("Заявку не знайдено") }
        if (withdrawal.status != "PENDING") {
            throw IllegalStateException("Заявку в статусі ${withdrawal.status} не можна підтвердити")
        }
        withdrawal.status = "COMPLETED"
        withdrawal.processedAt = Timestamp(System.currentTimeMillis())
        withdrawalRepository.save(withdrawal)
        log.info("Withdrawal completed by admin: id=$withdrawalId")
    }

    @Transactional
    fun rejectWithdrawalAdmin(withdrawalId: Long, reason: String?) {
        val withdrawal = withdrawalRepository.findById(withdrawalId)
            .orElseThrow { IllegalArgumentException("Заявку не знайдено") }
        if (withdrawal.status != "PENDING") {
            throw IllegalStateException("Заявку в статусі ${withdrawal.status} не можна відхилити")
        }
        withdrawal.status = "REJECTED"
        withdrawal.failureReason = reason?.takeIf { it.isNotBlank() } ?: "Відхилено адміністратором"
        withdrawal.processedAt = Timestamp(System.currentTimeMillis())
        withdrawalRepository.save(withdrawal)

        balanceEntryRepository.save(
            BalanceEntry(
                user = withdrawal.user,
                amount = withdrawal.amount,
                entryType = "ADJUSTMENT",
                idempotencyKey = "withdrawal-reject-${withdrawal.withdrawalId}",
                description = "Повернення коштів після відхилення виведення #${withdrawal.withdrawalId}"
            )
        )
        log.info("Withdrawal rejected by admin: id=$withdrawalId")
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
        payoutMethod = payoutMethod,
        payoutDestination = payoutDestination,
        recipientName = recipientName,
        createdAt = createdAt.toLocalDateTime(),
        processedAt = processedAt?.toLocalDateTime()
    )
}
