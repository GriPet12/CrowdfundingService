package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.PreviewProjectDto
import com.gripet12.crowdfundingService.dto.SubscriptionDto
import com.gripet12.crowdfundingService.dto.UserDto
import com.gripet12.crowdfundingService.model.AuthorFollow
import com.gripet12.crowdfundingService.model.ProjectFollow
import com.gripet12.crowdfundingService.repository.AuthorFollowRepository
import com.gripet12.crowdfundingService.repository.ProjectFollowRepository
import com.gripet12.crowdfundingService.repository.ProjectRepository
import com.gripet12.crowdfundingService.repository.SubscriptionRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FollowService(
    private val followRepository: ProjectFollowRepository,
    private val authorFollowRepository: AuthorFollowRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val subscriptionRepository: SubscriptionRepository
) {

    private fun currentUserId(): Long {
        val username = SecurityContextHolder.getContext().authentication.name
        return userRepository.findByUsername(username)
            .orElseThrow { IllegalStateException("User not found") }
            .userId!!
    }

    @Transactional
    fun toggleFollow(projectId: Long): Boolean {
        val userId = currentUserId()
        val project = projectRepository.findById(projectId).orElseThrow { IllegalArgumentException("Project not found") }
        if (project.creator.userId == userId) throw IllegalArgumentException("Cannot follow your own project")
        val existing = followRepository.findByUserUserIdAndProjectProjectId(userId, projectId)
        return if (existing != null) {
            followRepository.delete(existing)
            false
        } else {
            val user = userRepository.getReferenceById(userId)
            followRepository.save(ProjectFollow(user = user, project = project))
            true
        }
    }

    @Transactional(readOnly = true)
    fun isFollowing(projectId: Long): Boolean {
        val userId = currentUserId()
        return followRepository.existsByUserUserIdAndProjectProjectId(userId, projectId)
    }

    @Transactional(readOnly = true)
    fun getFollowedProjects(): List<PreviewProjectDto> {
        val userId = currentUserId()
        return followRepository.findAllByUserUserId(userId).map { pf ->
            val p = pf.project
            PreviewProjectDto(
                projectId = p.projectId,
                creatorId = p.creator.userId,
                title = p.title,
                goalAmount = p.goalAmount,
                collectedAmount = p.collectedAmount,
                status = p.status,
                hotnessScore = p.hotnessScore,
                mainImage = p.mainImage?.id,
                categories = p.categories.map { it?.categoryName }.toSet()
            )
        }
    }

    @Transactional(readOnly = true)
    fun getMySubscriptions(): List<SubscriptionDto> {
        val userId = currentUserId()
        return subscriptionRepository.findApprovedBySubscrberUserId(userId).map { sub ->
            SubscriptionDto(
                subscriptionId = sub.subscriptionId,
                creatorId = sub.creator.userId!!,
                creatorName = sub.creator.username,
                creatorImageId = sub.creator.image?.id,
                tierName = sub.subscriptionTier?.name ?: "—",
                tierLevel = sub.subscriptionTier?.level ?: 0,
                tierPrice = sub.tierPrice,
                paymentStatus = sub.payment?.status ?: "UNKNOWN"
            )
        }
    }

    @Transactional(readOnly = true)
    fun getFollowedProjectIds(projectIds: List<Long>): Set<Long> {
        if (projectIds.isEmpty()) return emptySet()
        val userId = currentUserId()
        return followRepository.findFollowedProjectIds(userId, projectIds).toSet()
    }

    @Transactional(readOnly = true)
    fun getFollowedAuthorIds(creatorIds: List<Long>): Set<Long> {
        if (creatorIds.isEmpty()) return emptySet()
        val userId = currentUserId()
        return authorFollowRepository.findFollowedAuthorIds(userId, creatorIds).toSet()
    }

    @Transactional
    fun toggleAuthorFollow(creatorId: Long): Boolean {
        val followerId = currentUserId()
        if (followerId == creatorId) throw IllegalArgumentException("Cannot follow yourself")
        val existing = authorFollowRepository.findByFollowerUserIdAndCreatorUserId(followerId, creatorId)
        return if (existing != null) {
            authorFollowRepository.delete(existing)
            false
        } else {
            val follower = userRepository.getReferenceById(followerId)
            val creator  = userRepository.getReferenceById(creatorId)
            authorFollowRepository.save(AuthorFollow(follower = follower, creator = creator))
            true
        }
    }

    @Transactional(readOnly = true)
    fun isFollowingAuthor(creatorId: Long): Boolean {
        val followerId = currentUserId()
        return authorFollowRepository.existsByFollowerUserIdAndCreatorUserId(followerId, creatorId)
    }

    @Transactional(readOnly = true)
    fun getFollowedAuthors(): List<UserDto> {
        val userId = currentUserId()
        return authorFollowRepository.findAllByFollowerUserId(userId).map { af ->
            val c = af.creator
            UserDto(
                id        = c.userId,
                username  = c.username,
                password  = "",
                email     = c.email,
                isVerified = c.isVerified,
                imageId   = c.image?.id,
                roles     = c.roles
            )
        }
    }
}

