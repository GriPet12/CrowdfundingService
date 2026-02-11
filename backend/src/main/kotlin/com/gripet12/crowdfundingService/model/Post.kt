package com.gripet12.crowdfundingService.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

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

    val likeCount: Int = 0,

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val content: Set<UploadedFile?> = HashSet()
)
