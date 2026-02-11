package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.PostDto
import com.gripet12.crowdfundingService.model.Content
import com.gripet12.crowdfundingService.model.Post
import com.gripet12.crowdfundingService.repository.PostRepository
import com.gripet12.crowdfundingService.repository.SubscriptionTierRepository

class PostService (
    private val postRepository: PostRepository,
    private val subscriptionTierRepository: SubscriptionTierRepository
) {
    fun getPost(postId: Long) = postRepository.findById(postId)

    fun createPost(postDto: PostDto) {
        val requiredTier = postDto.requiredTier?.let { subscriptionTierRepository.findByTierId(it) }

        val contents: Set<Content> = postDto.content
            .map { contentDto ->
                Content(
                    contentId = 0,
                    contentType = contentDto.contentType,
                    mediaId = contentDto.mediaId
                )
            }
            .toSet()

        val post = Post(
            postId = 0,
            masterId = postDto.masterId,
            masterType = postDto.masterType,
            visibility = postDto.visibility,
            title = postDto.title,
            description = postDto.description,
            requiredTier = requiredTier,
            likeCount = postDto.likeCount,
            content = contents
        )

        postRepository.save(post)
    }

    fun deletePost(postId: Long) {
        postRepository.deleteById(postId)
    }

    fun editPost(postDto: PostDto) {
        val existingPost = postRepository.findById(postDto.postId)
            .orElseThrow { NoSuchElementException("Post not found with id ${postDto.postId}") }

        val requiredTier = postDto.requiredTier?.let { subscriptionTierRepository.findByTierId(it) }

        val updatedContents = postDto.content.map { contentDto ->
            Content(
                contentId = contentDto.contentId ?: 0,
                contentType = contentDto.contentType,
                mediaId = contentDto.mediaId
            )
        }.toSet()

        val updatedPost = existingPost.copy(
            masterId = postDto.masterId,
            masterType = postDto.masterType,
            visibility = postDto.visibility,
            title = postDto.title,
            description = postDto.description,
            requiredTier = requiredTier,
            likeCount = postDto.likeCount,
            content = updatedContents
        )

        postRepository.save(updatedPost)
    }
}