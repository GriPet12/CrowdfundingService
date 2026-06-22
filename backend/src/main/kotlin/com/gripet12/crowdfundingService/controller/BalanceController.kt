package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.BalanceSummaryDto
import com.gripet12.crowdfundingService.dto.ConnectOnboardingDto
import com.gripet12.crowdfundingService.dto.ConnectStatusDto
import com.gripet12.crowdfundingService.dto.WithdrawalDto
import com.gripet12.crowdfundingService.dto.WithdrawalRequestDto
import com.gripet12.crowdfundingService.service.BalanceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/balance")
class BalanceController(private val balanceService: BalanceService) {

    @GetMapping
    fun getBalance(): ResponseEntity<BalanceSummaryDto> =
        ResponseEntity.ok(balanceService.getBalanceSummary())

    @GetMapping("/withdrawals")
    fun getWithdrawals(): ResponseEntity<List<WithdrawalDto>> =
        ResponseEntity.ok(balanceService.getWithdrawals())

    @GetMapping("/connect/status")
    fun getConnectStatus(): ResponseEntity<ConnectStatusDto> =
        ResponseEntity.ok(balanceService.getConnectStatus())

    @PostMapping("/connect/onboarding")
    fun startConnectOnboarding(): ResponseEntity<ConnectOnboardingDto> =
        ResponseEntity.ok(balanceService.createConnectOnboarding())

    @PostMapping("/withdraw")
    fun withdraw(@RequestBody request: WithdrawalRequestDto): ResponseEntity<WithdrawalDto> =
        ResponseEntity.ok(balanceService.requestWithdrawal(request))
}
