package com.gripet12.crowdfundingService.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import lombok.AllArgsConstructor
import lombok.NoArgsConstructor

@Entity
@Table(name = "comments")
@AllArgsConstructor
@NoArgsConstructor
data class Comment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val commentId: Long,

    @ManyToOne
    val author: User,

    @ManyToOne
    val project: Project,

    @ManyToOne
    val content: Content,

    val commentText: String

)
