package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.PaymentRequest
import com.gripet12.crowdfundingService.dto.WayForPayCallbackDto
import com.gripet12.crowdfundingService.service.PaymentService
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("/payment")
class PaymentController(private val paymentService: PaymentService) {

    @PostMapping("/{type}/generate")
    fun getPaymentData(@RequestBody request: PaymentRequest, @PathVariable type: String): ResponseEntity<Map<String, String>> {
        val data = paymentService.generatePaymentData(request, type)

        return ResponseEntity.ok(data)
    }

    @PostMapping("/callback")
    fun handleCallback(@RequestBody response: WayForPayCallbackDto): ResponseEntity<String> {
        paymentService.processPaymentCallback(response)

        return ResponseEntity.ok("accept")
    }
}