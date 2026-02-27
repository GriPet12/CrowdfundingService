package com.gripet12.crowdfundingService.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class WayForPayCallbackDto(
    @JsonProperty("merchantAccount") val merchantAccount: String = "",
    @JsonProperty("orderReference") val orderReference: String,
    @JsonProperty("merchantSignature") val merchantSignature: String = "",
    @JsonProperty("amount") val amount: Double = 0.0,
    @JsonProperty("currency") val currency: String = "",
    @JsonProperty("authCode") val authCode: String? = null,
    @JsonProperty("createdDate") val createdDate: Long = 0,
    @JsonProperty("processingDate") val processingDate: Long = 0,
    @JsonProperty("cardPan") val cardPan: String? = null,
    @JsonProperty("transactionStatus") val transactionStatus: String,
    @JsonProperty("reasonCode") val reasonCode: Int = 0,
    @JsonProperty("reason") val reason: String? = null
)
