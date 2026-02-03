package com.gripet12.crowdfundingService.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "categories")
data class Category(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var categoryId: Long,

    var categoryName: String,

    var description: String
)