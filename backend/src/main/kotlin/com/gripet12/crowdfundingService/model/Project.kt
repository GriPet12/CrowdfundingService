package com.gripet12.crowdfundingService.model

import jakarta.persistence.*
import java.math.BigDecimal

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

    @ManyToOne
    @JoinColumn(name = "image_id", nullable = false)
    val mainImage: Image?,

    @ManyToMany(cascade = [CascadeType.ALL])
    val images: Set<Image?> = HashSet(),

    @ManyToMany(cascade = [CascadeType.ALL])
    val videos: Set<Video?> = HashSet(),

    @ManyToMany(cascade = [CascadeType.ALL])
    val categories: Set<Category?> = HashSet()
)