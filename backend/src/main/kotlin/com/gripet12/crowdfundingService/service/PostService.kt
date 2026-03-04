package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.CreatePostDto
import com.gripet12.crowdfundingService.dto.PostFileDto
import com.gripet12.crowdfundingService.dto.PostResponseDto
import com.gripet12.crowdfundingService.model.Post
import com.gripet12.crowdfundingService.model.PostLike
import com.gripet12.crowdfundingService.repository.CommentRepository
import com.gripet12.crowdfundingService.repository.FileRepository
import com.gripet12.crowdfundingService.repository.PostLikeRepository
import com.gripet12.crowdfundingService.repository.PostRepository
import com.gripet12.crowdfundingService.repository.SubscriptionRepository
import com.gripet12.crowdfundingService.repository.SubscriptionTierRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService(
    private val postRepository: PostRepository,
    private val subscriptionTierRepository: SubscriptionTierRepository,
    private val fileRepository: FileRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val userRepository: UserRepository,
    private val postLikeRepository: PostLikeRepository,
    private val commentRepository: CommentRepository
) {
    private fun currentUserIdOrNull(): Long? {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated || auth.name == "anonymousUser") return null
        return userRepository.findByUsername(auth.name).orElse(null)?.userId
    }

    private fun currentUserId(): Long =
        currentUserIdOrNull() ?: throw IllegalStateException("Not authenticated")

    private fun hasAccess(viewerId: Long?, authorId: Long, requiredTierLevel: Int?): Boolean {
        if (requiredTierLevel == null) return true           
        if (viewerId == null) return false                   
        if (viewerId == authorId) return true                
        return subscriptionRepository.findActiveSubscriptionsBySubscriberAndCreator(viewerId, authorId)
            .any { (it.subscriptionTier?.level ?: 0) >= requiredTierLevel }
    }

    @Transactional(readOnly = true)
    fun getPostsByAuthor(authorId: Long): List<PostResponseDto> {
        val viewerId = currentUserIdOrNull()
        val isOwner = viewerId != null && viewerId == authorId
        val posts = if (isOwner)
            postRepository.findByMasterIdIncludingBanned(authorId)
        else
            postRepository.findByMasterIdOrderByPostIdDesc(authorId)
        return posts.map { it.toResponse(authorId, viewerId) }
    }

    @Transactional
    fun createPost(dto: CreatePostDto): PostResponseDto {
        val authorId = currentUserId()
        val tier = dto.requiredTierId?.let { subscriptionTierRepository.findByTierId(it) }
        val files = fileRepository.findAllById(dto.mediaIds).toHashSet()

        val post = Post(
            postId = 0,
            masterId = authorId,
            masterType = "USER",
            visibility = if (tier == null) "PUBLIC" else "SUBSCRIBERS",
            title = dto.title,
            description = dto.content,
            requiredTier = tier,
            content = files
        )
        val saved = postRepository.save(post)
        return saved.toResponse(authorId, authorId)  
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
        val access = hasAccess(viewerId, authorId, level)

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
            hasAccess = access,
            banned = banned,
            files = fileList,
            visibility = visibility,
            likeCount = likeCount,
            likedByMe = likedByMe,
            commentCount = commentCount
        )
    }
}