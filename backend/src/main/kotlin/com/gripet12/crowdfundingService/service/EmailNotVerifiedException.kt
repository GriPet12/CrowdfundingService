package com.gripet12.crowdfundingService.service

data class EmailNotVerifiedException(val email: String) : RuntimeException("EMAIL_NOT_VERIFIED")
