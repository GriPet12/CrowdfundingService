package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.config.PostgresSequenceSync
import com.gripet12.crowdfundingService.dto.CommentResponseDto
import com.gripet12.crowdfundingService.model.Comment
import com.gripet12.crowdfundingService.repository.CommentRepository
import com.gripet12.crowdfundingService.repository.PostRepository
import com.gripet12.crowdfundingService.repository.ProjectRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val postgresSequenceSync: PostgresSequenceSync
) {
    private fun currentUserId(): Long {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("Not authenticated")
        return userRepository.findByUsername(auth.name).orElseThrow()?.userId!!
    }

    @Transactional(readOnly = true)
    fun getComments(postId: Long): List<CommentResponseDto> =
        commentRepository.findByPostId(postId).map { it.toDto() }

    @Transactional(readOnly = true)
    fun getProjectComments(projectId: Long): List<CommentResponseDto> =
        commentRepository.findByProjectId(projectId).map { it.toDto() }

    @Transactional(noRollbackFor = [DataIntegrityViolationException::class])
    fun addComment(postId: Long, text: String): CommentResponseDto {
        if (text.isBlank()) throw IllegalArgumentException("Comment cannot be empty")
        val userId = currentUserId()
        val author = userRepository.findByUserId(userId)
        val post = postRepository.findByPostId(postId)
            ?: throw NoSuchElementException("Post not found")
        val comment = Comment(author = author, post = post, commentText = text.trim())
        return saveComment(comment)
    }

    @Transactional(noRollbackFor = [DataIntegrityViolationException::class])
    fun addProjectComment(projectId: Long, text: String): CommentResponseDto {
        if (text.isBlank()) throw IllegalArgumentException("Comment cannot be empty")
        val userId = currentUserId()
        val author = userRepository.findByUserId(userId)
        val project = projectRepository.findById(projectId)
            .orElseThrow { NoSuchElementException("Project not found") }
        if (project.status != "ACTIVE" || project.banned) {
            throw IllegalStateException("Коментарі недоступні для цього проєкту")
        }
        val comment = Comment(author = author, project = project, commentText = text.trim())
        return saveComment(comment)
    }

    private fun saveComment(comment: Comment): CommentResponseDto {
        return try {
            commentRepository.save(comment).toDto()
        } catch (e: DataIntegrityViolationException) {
            val message = e.mostSpecificCause?.message ?: e.message ?: ""
            if (comment.project != null && message.contains("post_id", ignoreCase = true)) {
                postgresSequenceSync.ensureCommentsSchema()
                return commentRepository.save(comment.copy(commentId = 0)).toDto()
            }
            postgresSequenceSync.syncTable("comments", "comment_id")
            commentRepository.save(comment.copy(commentId = 0)).toDto()
        }
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