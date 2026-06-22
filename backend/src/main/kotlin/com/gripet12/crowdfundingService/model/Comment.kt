package com.gripet12.crowdfundingService.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "comments")
data class Comment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val commentId: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    val author: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: Post? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    val project: Project? = null,

    @Column(nullable = false, length = 2000)
    val commentText: String,

    val createdAt: Instant = Instant.now()
)
