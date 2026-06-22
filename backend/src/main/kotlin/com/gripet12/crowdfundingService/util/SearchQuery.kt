package com.gripet12.crowdfundingService.util

fun searchPattern(raw: String?): String? =
    raw?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()?.let { "%$it%" }
