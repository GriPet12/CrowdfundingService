package com.gripet12.crowdfundingService.dto

/**
 * Raw Stripe webhook payload – the full JSON body is forwarded as-is
 * so that the Stripe SDK can verify the signature and parse the Event.
 * We only need the raw body (String) and the Stripe-Signature header.
 * This DTO is therefore just a marker; the controller handles the raw bytes.
 */
data class StripeWebhookDto(
    val payload: String,
    val sigHeader: String
)

