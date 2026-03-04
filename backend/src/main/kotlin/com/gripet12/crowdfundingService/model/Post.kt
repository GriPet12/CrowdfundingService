package com.gripet12.crowdfundingService.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "posts")
data class Post(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val postId: Long,

    val masterId: Long,

    val masterType: String,

    val visibility: String,

    val title: String,

    val description: String,

    @ManyToOne
    val requiredTier: SubscriptionTier? = null,

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val content: Set<UploadedFile?> = HashSet(),

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    var banned: Boolean = false,

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    var bannedWithUser: Boolean = false,

    @Column(name = "created_at", nullable = false,
        columnDefinition = "timestamp not null default now()")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
