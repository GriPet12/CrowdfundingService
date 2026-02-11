package com.gripet12.crowdfundingService.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "contents")
data class Content(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val contentId: Long,

    var contentType: String,

    var mediaId: Long
)
