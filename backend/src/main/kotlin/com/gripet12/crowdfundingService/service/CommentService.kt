package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.CommentDto
import com.gripet12.crowdfundingService.model.Comment
import com.gripet12.crowdfundingService.model.Post
import com.gripet12.crowdfundingService.model.Project
import com.gripet12.crowdfundingService.model.User
import com.gripet12.crowdfundingService.repository.CommentRepository
import com.gripet12.crowdfundingService.repository.PostRepository
import com.gripet12.crowdfundingService.repository.ProjectRepository
import com.gripet12.crowdfundingService.repository.UserRepository

class CommentService (
    private val commentRepository: CommentRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) {
    fun addComment(commentDto: CommentDto) {
        val author : User = userRepository.findByUserId(commentDto.author)
        val project : Project = projectRepository.findByProjectId(commentDto.project)
        val post : Post? = postRepository.findByPostId(commentDto.post)

        commentRepository.save(Comment(
            commentId = 0,
            author = author,
            project = project,
            post = post,
            commentText = commentDto.commentText
        ))
    }
}