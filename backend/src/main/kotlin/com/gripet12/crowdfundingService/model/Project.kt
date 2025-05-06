package com.gripet12.crowdfundingService.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "projects")
data class Project(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    val author: User,

    @NotBlank
    val title: String
) {
    constructor() : this(null, User(), "")
}