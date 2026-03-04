package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.CommentResponseDto
import com.gripet12.crowdfundingService.model.Comment
import com.gripet12.crowdfundingService.repository.CommentRepository
import com.gripet12.crowdfundingService.repository.PostRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) {
    private fun currentUserId(): Long {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("Not authenticated")
        return userRepository.findByUsername(auth.name).orElseThrow()?.userId!!
    }

    @Transactional(readOnly = true)
    fun getComments(postId: Long): List<CommentResponseDto> =
        commentRepository.findByPostId(postId).map { it.toDto() }

    @Transactional
    fun addComment(postId: Long, text: String): CommentResponseDto {
        if (text.isBlank()) throw IllegalArgumentException("Comment cannot be empty")
        val userId = currentUserId()
        val author = userRepository.findByUserId(userId)
        val post = postRepository.findByPostId(postId)
            ?: throw NoSuchElementException("Post not found")
        val comment = commentRepository.save(
            Comment(author = author, post = post, commentText = text.trim())
        )
        return comment.toDto()
    }

    @Transactional
    fun deleteComment(commentId: Long) {
        val userId = currentUserId()
        val comment = commentRepository.findById(commentId).orElseThrow { NoSuchElementException("Comment not found") }
        if (comment.author.userId != userId) throw IllegalAccessException("Access denied")
        commentRepository.delete(comment)
    }

    private fun Comment.toDto() = CommentResponseDto(
        commentId = commentId,
        authorId = author.userId!!,
        authorName = author.username,
        authorImageId = author.image?.id,
        commentText = commentText,
        createdAt = createdAt
    )
}