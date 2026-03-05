package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.PaymentRequest
import com.gripet12.crowdfundingService.service.PaymentService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/payment")
class PaymentController(private val paymentService: PaymentService) {

    /**
     * Creates a Stripe PaymentIntent and returns the clientSecret to the frontend.
     * The frontend uses the clientSecret with Stripe.js to confirm the payment.
     */
    @PostMapping("/{type}/generate")
    fun getPaymentData(
        @RequestBody request: PaymentRequest,
        @PathVariable type: String
    ): ResponseEntity<Map<String, String>> {
        val data = paymentService.generatePaymentData(request, type)
        return ResponseEntity.ok(data)
    }

    /**
     * Stripe webhook endpoint.
     * Must read the raw body bytes to correctly validate the Stripe-Signature header.
     */
    @PostMapping("/callback")
    fun handleStripeWebhook(
        request: HttpServletRequest,
        @RequestHeader("Stripe-Signature") sigHeader: String
    ): ResponseEntity<String> {
        val payload = request.inputStream.bufferedReader(Charsets.UTF_8).readText()
        paymentService.processStripeWebhook(payload, sigHeader)
        return ResponseEntity.ok("received")
    }
}