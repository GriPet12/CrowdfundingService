package com.gripet12.crowdfundingService.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "projects")
data class Project(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val projectId: Long? = null,

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    val creator: User,

    val title: String,

    val goalAmount: BigDecimal,

    var collectedAmount: BigDecimal,

    var status: String? = null,

    @Column(length = 2048)
    val description: String? = null,

    var hotnessScore: Double = 0.0,

    @ManyToOne
    @JoinColumn(name = "image_id", nullable = false)
    val mainImage: UploadedFile?,

    @ManyToMany(cascade = [CascadeType.ALL])
    val media: Set<UploadedFile?> = HashSet(),

    @ManyToMany(cascade = [CascadeType.ALL])
    val categories: Set<Category?> = HashSet(),

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    var banned: Boolean = false,

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    var bannedWithUser: Boolean = false,

    @Column(name = "created_at", nullable = false,
        columnDefinition = "timestamp not null default now()")
    val createdAt: LocalDateTime = LocalDateTime.now()
)