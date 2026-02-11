package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.PostDto
import com.gripet12.crowdfundingService.model.Post
import com.gripet12.crowdfundingService.model.UploadedFile
import com.gripet12.crowdfundingService.repository.FileRepository
import com.gripet12.crowdfundingService.repository.PostRepository
import com.gripet12.crowdfundingService.repository.SubscriptionTierRepository

class PostService (
    private val postRepository: PostRepository,
    private val subscriptionTierRepository: SubscriptionTierRepository,
    private val fileRepository: FileRepository,
    private val fileStorageService: FileStorageService
) {
    fun getPost(postId: Long) = postRepository.findById(postId)

    fun createPost(postDto: PostDto) {
        val requiredTier = postDto.requiredTier?.let { subscriptionTierRepository.findByTierId(it) }

        val contents: Set<UploadedFile?> = postDto.content
            .map { fileId ->
                fileStorageService.uploadFile(fileId)
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
        val postId = postDto.postId
            ?: throw IllegalArgumentException("postId is required for editPost")

        val existingPost = postRepository.findById(postId)
            .orElseThrow { NoSuchElementException("Post not found with id $postId") }

        val requiredTier = postDto.requiredTier?.let { subscriptionTierRepository.findByTierId(it) }

        val updatedPost = existingPost.copy(
            masterId = postDto.masterId,
            masterType = postDto.masterType,
            visibility = postDto.visibility,
            title = postDto.title,
            description = postDto.description,
            requiredTier = requiredTier,
            likeCount = postDto.likeCount
        )

        postRepository.save(updatedPost)
    }
}