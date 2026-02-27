package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.SubscriptionTierDto
import com.gripet12.crowdfundingService.service.SubscriptionTierService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/subscription-tiers")
class SubscriptionTierController(
    private val subscriptionTierService: SubscriptionTierService
) {

    @GetMapping("/creator/{creatorId}")
    fun getTiersByCreator(@PathVariable creatorId: Long): ResponseEntity<List<SubscriptionTierDto>> =
        ResponseEntity.ok(subscriptionTierService.getTiersByCreator(creatorId))

    @PostMapping
    fun createTier(@RequestBody dto: SubscriptionTierDto): ResponseEntity<SubscriptionTierDto> =
        ResponseEntity.ok(subscriptionTierService.createTier(dto))

    @DeleteMapping("/{tierId}")
    fun deleteTier(@PathVariable tierId: Long): ResponseEntity<Void> {
        subscriptionTierService.deleteTier(tierId)
        return ResponseEntity.noContent().build()
    }
}
