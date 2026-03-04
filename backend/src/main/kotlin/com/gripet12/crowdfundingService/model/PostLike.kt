package com.gripet12.crowdfundingService.model

import jakarta.persistence.*

@Entity
@Table(
    name = "post_likes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["post_id", "user_id"])]
)
data class PostLike(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    val post: Post,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User
)
