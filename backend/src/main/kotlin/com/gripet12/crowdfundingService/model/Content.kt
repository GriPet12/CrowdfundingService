package com.gripet12.crowdfundingService.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import lombok.AllArgsConstructor
import lombok.NoArgsConstructor

@Entity
@Table(name = "contents")
@AllArgsConstructor
@NoArgsConstructor
data class Content(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val contentId: Long,

    @ManyToOne
    val creator: User,

    @ManyToOne
    val project: Project,

    val contentType: String,

    val visibility: String,

    val requiredTier: String,

    val likeCount: Int = 0
)
