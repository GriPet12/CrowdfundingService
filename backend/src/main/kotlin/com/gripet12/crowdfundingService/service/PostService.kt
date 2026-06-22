package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.CreatePostDto
import com.gripet12.crowdfundingService.dto.PostFileDto
import com.gripet12.crowdfundingService.dto.PostResponseDto
import com.gripet12.crowdfundingService.dto.UpdatePostDto
import com.gripet12.crowdfundingService.model.Post
import com.gripet12.crowdfundingService.model.PostLike
import com.gripet12.crowdfundingService.repository.CommentRepository
import com.gripet12.crowdfundingService.repository.DonateRepository
import com.gripet12.crowdfundingService.repository.FileRepository
import com.gripet12.crowdfundingService.repository.PostLikeRepository
import com.gripet12.crowdfundingService.repository.PostRepository
import com.gripet12.crowdfundingService.repository.SubscriptionRepository
import com.gripet12.crowdfundingService.repository.SubscriptionTierRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Lazy
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.Timestamp

@Service
class PostService(
    private val jdbcTemplate: JdbcTemplate,
    private val entityManager: EntityManager,
    private val postRepository: PostRepository,
    private val subscriptionTierRepository: SubscriptionTierRepository,
    private val fileRepository: FileRepository,
    private val donateRepository: DonateRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val userRepository: UserRepository,
    private val postLikeRepository: PostLikeRepository,
    private val commentRepository: CommentRepository,
    @Lazy private val subscriptionService: SubscriptionService
) {
    private fun currentUserIdOrNull(): Long? {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated || auth.name == "anonymousUser") return null
        return userRepository.findByUsername(auth.name).orElse(null)?.userId
    }

    private fun currentUserId(): Long =
        currentUserIdOrNull() ?: throw IllegalStateException("Not authenticated")

    private fun hasAccess(viewerId: Long?, authorId: Long, post: Post): Boolean {
        if (viewerId != null && viewerId == authorId) return true

        return when (post.visibility) {
            "PRIVATE" -> false
            "DONATION" -> {
                if (viewerId == null) return false
                val min = post.minDonationAmount ?: return false
                val total = donateRepository.sumApprovedDonationsByDonorToCreatorSince(
                    viewerId, authorId, ALL_TIME_SINCE
                )
                total >= min
            }
            "SUBSCRIBERS" -> {
                val level = post.requiredTier?.level ?: return false
                if (viewerId == null) return false
                subscriptionRepository.findActiveSubscriptionsBySubscriberAndCreator(viewerId, authorId)
                    .any { (it.subscriptionTier?.level ?: 0) >= level }
            }
            else -> true
        }
    }

    private data class ResolvedPostAccess(
        val visibility: String,
        val tier: com.gripet12.crowdfundingService.model.SubscriptionTier?,
        val minDonationAmount: BigDecimal?
    )

    private fun resolvePostAccess(
        visibility: String,
        requiredTierId: Long?,
        minDonationAmount: BigDecimal?,
        authorId: Long
    ): ResolvedPostAccess {
        return when (visibility.uppercase()) {
            "PRIVATE" -> ResolvedPostAccess("PRIVATE", null, null)
            "DONATION" -> {
                val min = minDonationAmount
                    ?: throw IllegalArgumentException("Вкажіть мінімальну суму донату")
                if (min <= BigDecimal.ZERO) {
                    throw IllegalArgumentException("Мінімальна сума донату має бути більше 0")
                }
                ResolvedPostAccess("DONATION", null, min)
            }
            "SUBSCRIBERS", "TIER" -> {
                val tierId = requiredTierId
                    ?: throw IllegalArgumentException("Оберіть рівень підписки")
                val tier = subscriptionTierRepository.findByTierIdAndCreatorId(tierId, authorId)
                    ?: throw IllegalArgumentException("Невірний рівень підписки")
                ResolvedPostAccess("SUBSCRIBERS", tier, null)
            }
            else -> ResolvedPostAccess("PUBLIC", null, null)
        }
    }

    private fun syncPostAccess(postId: Long, access: ResolvedPostAccess) {
        jdbcTemplate.update(
            "UPDATE posts SET visibility = ?, min_donation_amount = ? WHERE post_id = ?",
            access.visibility,
            access.minDonationAmount,
            postId
        )
        val tierId = access.tier?.tierId
        if (tierId == null) {
            clearRequiredTierColumn(postId)
        } else {
            setRequiredTierColumn(postId, tierId)
        }
    }

    private fun clearRequiredTierColumn(postId: Long) {
        for (column in REQUIRED_TIER_COLUMNS) {
            try {
                jdbcTemplate.update("UPDATE posts SET $column = NULL WHERE post_id = ?", postId)
                return
            } catch (_: Exception) {
                // try next legacy column name
            }
        }
    }

    private fun setRequiredTierColumn(postId: Long, tierId: Long) {
        for (column in REQUIRED_TIER_COLUMNS) {
            try {
                jdbcTemplate.update("UPDATE posts SET $column = ? WHERE post_id = ?", tierId, postId)
                return
            } catch (_: Exception) {
                // try next legacy column name
            }
        }
    }

    @Transactional
    fun getPostsByAuthor(authorId: Long): List<PostResponseDto> {
        val viewerId = currentUserIdOrNull()
        if (viewerId != null && viewerId != authorId) {
            subscriptionService.checkAndGrantAutoSubscription(viewerId, authorId)
        }
        val isOwner = viewerId != null && viewerId == authorId
        val posts = if (isOwner)
            postRepository.findByMasterIdIncludingBanned(authorId)
        else
            postRepository.findByMasterIdOrderByPostIdDesc(authorId)
                .filter { it.visibility != "PRIVATE" }
        return posts.map { it.toResponse(authorId, viewerId) }
    }

    @Transactional
    fun createPost(dto: CreatePostDto): PostResponseDto {
        val authorId = currentUserId()
        val access = resolvePostAccess(dto.visibility, dto.requiredTierId, dto.minDonationAmount, authorId)
        val mediaIds = dto.mediaIds.distinct()
        val files = loadMediaFiles(mediaIds)

        val post = Post(
            postId = 0,
            masterId = authorId,
            masterType = "USER",
            visibility = access.visibility,
            title = dto.title,
            description = dto.content,
            requiredTier = access.tier,
            minDonationAmount = access.minDonationAmount,
            content = files
        )
        val saved = postRepository.save(post)
        syncPostFiles(saved.postId, mediaIds)
        syncPostAccess(saved.postId, access)
        return reloadPost(saved.postId, authorId)
    }

    @Transactional
    fun updatePost(postId: Long, dto: UpdatePostDto): PostResponseDto {
        val userId = currentUserId()
        val post = postRepository.findByPostIdWithContent(postId)
            ?: throw NoSuchElementException("Post not found")
        if (post.masterId != userId) throw IllegalAccessException("Access denied")

        val access = resolvePostAccess(dto.visibility, dto.requiredTierId, dto.minDonationAmount, userId)
        val mediaIds = dto.mediaIds.distinct()
        val files = loadMediaFiles(mediaIds)

        val updated = post.copy(
            title = dto.title,
            description = dto.content,
            visibility = access.visibility,
            requiredTier = access.tier,
            minDonationAmount = access.minDonationAmount,
            content = files
        )
        postRepository.save(updated)
        syncPostFiles(postId, mediaIds)
        syncPostAccess(postId, access)
        return reloadPost(postId, userId)
    }

    private fun loadMediaFiles(mediaIds: List<Long>): Set<com.gripet12.crowdfundingService.model.UploadedFile> {
        if (mediaIds.isEmpty()) return emptySet()
        val loaded = fileRepository.findAllById(mediaIds).toHashSet()
        if (loaded.size != mediaIds.size) {
            throw IllegalArgumentException("Не вдалося знайти прикріплені файли. Спробуйте завантажити їх ще раз.")
        }
        return loaded
    }

    private fun reloadPost(postId: Long, authorId: Long): PostResponseDto {
        postRepository.flush()
        entityManager.clear()
        val reloaded = postRepository.findByPostIdWithContent(postId)
            ?: throw NoSuchElementException("Post not found")
        return reloaded.toResponse(authorId, authorId)
    }

    private fun syncPostFiles(postId: Long, mediaIds: List<Long>) {
        if (mediaIds.isEmpty()) {
            jdbcTemplate.update("UPDATE files SET post_id = NULL WHERE post_id = ?", postId)
            return
        }
        val placeholders = mediaIds.joinToString(",") { "?" }
        val unlinkArgs = mutableListOf<Any>(postId)
        unlinkArgs.addAll(mediaIds)
        jdbcTemplate.update(
            "UPDATE files SET post_id = NULL WHERE post_id = ? AND id NOT IN ($placeholders)",
            *unlinkArgs.toTypedArray()
        )
        linkFilesToPost(postId, mediaIds)
    }

    private fun linkFilesToPost(postId: Long, mediaIds: List<Long>) {
        val sql = "UPDATE files SET post_id = ? WHERE id IN (${mediaIds.joinToString(",") { "?" }})"
        val args = mutableListOf<Any>(postId)
        args.addAll(mediaIds)
        jdbcTemplate.update(sql, *args.toTypedArray())
    }

    @Transactional
    fun deletePost(postId: Long) {
        val userId = currentUserId()
        val post = postRepository.findByPostId(postId)
            ?: throw NoSuchElementException("Post not found")
        if (post.masterId != userId) throw IllegalAccessException("Access denied")
        postRepository.delete(post)
    }

    @Transactional
    fun toggleLike(postId: Long): Map<String, Any> {
        val userId = currentUserId()
        val post = postRepository.findByPostId(postId)
            ?: throw NoSuchElementException("Post not found")
        val user = userRepository.findByUserId(userId)

        val existing = postLikeRepository.findByPostPostIdAndUserUserId(postId, userId)
        val likedByMe: Boolean
        if (existing != null) {
            postLikeRepository.delete(existing)
            likedByMe = false
        } else {
            postLikeRepository.save(PostLike(post = post, user = user))
            likedByMe = true
        }
        val likeCount = postLikeRepository.countByPostPostId(postId)
        return mapOf("likeCount" to likeCount, "likedByMe" to likedByMe)
    }

    private fun Post.toResponse(authorId: Long, viewerId: Long?): PostResponseDto {
        val level = requiredTier?.level
        val access = hasAccess(viewerId, authorId, this)

        val showContent = access && !banned
        val fileList = if (showContent)
            content.mapNotNull { f ->
                f?.id?.let { id -> PostFileDto(id, f.originalFileName, f.mimeType, f.category.name) }
            }
        else emptyList()
        val likeCount = postLikeRepository.countByPostPostId(postId)
        val likedByMe = viewerId?.let { postLikeRepository.existsByPostPostIdAndUserUserId(postId, it) } ?: false
        val commentCount = commentRepository.countByPostPostId(postId)
        return PostResponseDto(
            postId = postId,
            masterId = masterId,
            title = title,
            description = if (showContent) description else "",
            requiredTierLevel = level,
            requiredTierName = requiredTier?.name,
            requiredTierId = requiredTier?.tierId,
            minDonationAmount = minDonationAmount,
            hasAccess = access,
            banned = banned,
            files = fileList,
            visibility = visibility,
            likeCount = likeCount,
            likedByMe = likedByMe,
            commentCount = commentCount
        )
    }

    companion object {
        private val REQUIRED_TIER_COLUMNS = listOf("required_tier_id", "required_tier_tier_id")
        private val ALL_TIME_SINCE = Timestamp(0)
    }
}