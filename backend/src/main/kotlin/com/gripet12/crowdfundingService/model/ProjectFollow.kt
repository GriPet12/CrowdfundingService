package com.gripet12.crowdfundingService.model

import jakarta.persistence.*

@Entity
@Table(
    name = "project_follows",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "project_id"])]
)
data class ProjectFollow(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    val project: Project
)

