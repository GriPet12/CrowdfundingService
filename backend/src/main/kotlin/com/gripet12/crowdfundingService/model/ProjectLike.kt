package com.gripet12.crowdfundingService.model

import jakarta.persistence.*

@Entity
@Table(
    name = "project_likes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "user_id"])]
)
data class ProjectLike(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    val project: Project,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User
)
