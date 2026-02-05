package com.gripet12.crowdfundingService.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class WayForPayCallbackDto(
    @JsonProperty("merchantAccount") val merchantAccount: String,
    @JsonProperty("orderReference") val orderReference: String,
    @JsonProperty("merchantSignature") val merchantSignature: String,
    @JsonProperty("amount") val amount: Double,
    @JsonProperty("currency") val currency: String,
    @JsonProperty("authCode") val authCode: String,
    @JsonProperty("createdDate") val createdDate: Long,
    @JsonProperty("processingDate") val processingDate: Long,
    @JsonProperty("cardPan") val cardPan: String,
    @JsonProperty("transactionStatus") val transactionStatus: String,
    @JsonProperty("reasonCode") val reasonCode: Int,
    @JsonProperty("reason") val reason: String
)
