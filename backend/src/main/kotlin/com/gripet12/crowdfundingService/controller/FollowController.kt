package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.PreviewProjectDto
import com.gripet12.crowdfundingService.dto.SubscriptionDto
import com.gripet12.crowdfundingService.dto.UserDto
import com.gripet12.crowdfundingService.service.FollowService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/follows")
class FollowController(private val followService: FollowService) {


    @PostMapping("/projects/{projectId}")
    fun toggleFollow(@PathVariable projectId: Long): ResponseEntity<Map<String, Any>> =
        try {
            ResponseEntity.ok(mapOf("following" to followService.toggleFollow(projectId)))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Bad request")))
        }

    @GetMapping("/projects/{projectId}/status")
    fun isFollowing(@PathVariable projectId: Long): ResponseEntity<Map<String, Boolean>> =
        ResponseEntity.ok(mapOf("following" to followService.isFollowing(projectId)))

    @GetMapping("/projects")
    fun getFollowedProjects(): ResponseEntity<List<PreviewProjectDto>> =
        ResponseEntity.ok(followService.getFollowedProjects())

    @PostMapping("/projects/batch-status")
    fun batchProjectStatus(@RequestBody ids: List<Long>): ResponseEntity<Set<Long>> =
        ResponseEntity.ok(followService.getFollowedProjectIds(ids))


    @PostMapping("/authors/{creatorId}")
    fun toggleAuthorFollow(@PathVariable creatorId: Long): ResponseEntity<Map<String, Any>> =
        try {
            ResponseEntity.ok(mapOf("following" to followService.toggleAuthorFollow(creatorId)))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Bad request")))
        }

    @GetMapping("/authors/{creatorId}/status")
    fun isFollowingAuthor(@PathVariable creatorId: Long): ResponseEntity<Map<String, Boolean>> =
        ResponseEntity.ok(mapOf("following" to followService.isFollowingAuthor(creatorId)))

    @GetMapping("/authors")
    fun getFollowedAuthors(): ResponseEntity<List<UserDto>> =
        ResponseEntity.ok(followService.getFollowedAuthors())

    @PostMapping("/authors/batch-status")
    fun batchAuthorStatus(@RequestBody ids: List<Long>): ResponseEntity<Set<Long>> =
        ResponseEntity.ok(followService.getFollowedAuthorIds(ids))


    @GetMapping("/subscriptions")
    fun getMySubscriptions(): ResponseEntity<List<SubscriptionDto>> =
        ResponseEntity.ok(followService.getMySubscriptions())
}

