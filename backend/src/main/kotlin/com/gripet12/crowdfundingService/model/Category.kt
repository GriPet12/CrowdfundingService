package com.gripet12.crowdfundingService.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import lombok.AllArgsConstructor
import lombok.NoArgsConstructor

@Entity
@Table(name = "categories")
@AllArgsConstructor
@NoArgsConstructor
data class Category(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var categoryId: Long,

    var categoryName: String,

    var description: String
)