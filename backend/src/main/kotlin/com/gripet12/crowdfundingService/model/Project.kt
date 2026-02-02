package com.gripet12.crowdfundingService.model

import jakarta.persistence.*
import java.math.BigDecimal
import lombok.AllArgsConstructor
import lombok.NoArgsConstructor

@Entity
@Table(name = "projects")
@AllArgsConstructor
@NoArgsConstructor
data class Project(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val projectId: Long? = null,

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    val creator: User,

    val title: String,

    val goalAmount: BigDecimal,

    val collectedAmount: BigDecimal,

    val status: String,

    @ManyToOne
    @JoinColumn(name = "image_id", nullable = false)
    val mainImage: Image,

    @ManyToMany(cascade = [CascadeType.ALL])
    val images: MutableSet<Image> = HashSet(),

    @ManyToMany(cascade = [CascadeType.ALL])
    val videos: MutableSet<Video> = HashSet(),

    @ManyToMany(cascade = [CascadeType.ALL])
    val categories: MutableSet<Category> = HashSet()
)