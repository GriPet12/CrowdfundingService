package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.CommentResponseDto
import com.gripet12.crowdfundingService.dto.CreatePostDto
import com.gripet12.crowdfundingService.dto.PostResponseDto
import com.gripet12.crowdfundingService.service.CommentService
import com.gripet12.crowdfundingService.service.PostService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/posts")
class PostController(
    private val postService: PostService,
    private val commentService: CommentService
) {

    @GetMapping("/author/{authorId}")
    fun getPostsByAuthor(@PathVariable authorId: Long): ResponseEntity<List<PostResponseDto>> =
        ResponseEntity.ok(postService.getPostsByAuthor(authorId))

    @PostMapping
    fun createPost(@RequestBody dto: CreatePostDto): ResponseEntity<PostResponseDto> =
        ResponseEntity.ok(postService.createPost(dto))

    @DeleteMapping("/{postId}")
    fun deletePost(@PathVariable postId: Long): ResponseEntity<Void> {
        postService.deletePost(postId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{postId}/like")
    fun toggleLike(@PathVariable postId: Long): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(postService.toggleLike(postId))

    @GetMapping("/{postId}/comments")
    fun getComments(@PathVariable postId: Long): ResponseEntity<List<CommentResponseDto>> =
        ResponseEntity.ok(commentService.getComments(postId))

    @PostMapping("/{postId}/comments")
    fun addComment(
        @PathVariable postId: Long,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<CommentResponseDto> {
        val text = body["text"] ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(commentService.addComment(postId, text))
    }

    @DeleteMapping("/comments/{commentId}")
    fun deleteComment(@PathVariable commentId: Long): ResponseEntity<Void> {
        commentService.deleteComment(commentId)
        return ResponseEntity.noContent().build()
    }
}
