package com.gripet12.crowdfundingService.model

import jakarta.persistence.*

@Entity
@Table(
    name = "author_follows",
    uniqueConstraints = [UniqueConstraint(columnNames = ["follower_id", "creator_id"])]
)
data class AuthorFollow(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    val follower: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    val creator: User
)

