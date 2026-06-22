package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.SubscriptionDto
import com.gripet12.crowdfundingService.service.SubscriptionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/subscriptions")
class SubscriptionController(
    private val subscriptionService: SubscriptionService
) {

    @GetMapping("/status/{creatorId}")
    fun getSubscriptionStatus(@PathVariable creatorId: Long): ResponseEntity<List<SubscriptionDto>> =
        ResponseEntity.ok(subscriptionService.getSubscriptionStatusForCreator(creatorId))

    @GetMapping("/my")
    fun getMySubscriptions(): ResponseEntity<List<SubscriptionDto>> =
        ResponseEntity.ok(subscriptionService.getMySubscriptions())
}
