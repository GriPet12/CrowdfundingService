package com.gripet12.crowdfundingService.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.sql.Time
import java.sql.Timestamp

@Entity
@Table(name = "comments")
data class Comment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val commentId: Long,

    @ManyToOne
    val author: User,

    @ManyToOne
    val project: Project,

    @ManyToOne
    val post: Post?,

    val commentText: String,

    val createAt: Timestamp = Timestamp(System.currentTimeMillis())

)
