package com.gripet12.crowdfundingService.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "ANALYTICS_LOGS")
data class AnalyticsLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val logId: Long,

    @ManyToOne
    val user: User,

    val action: String,

    val activityModelType: String,

    val activityModelId: Long,

    @ManyToMany(cascade = [CascadeType.ALL])
    val categories: Set<Category?> = HashSet(),

    @Column(columnDefinition = "timestamp without time zone")
    val actionTime: LocalDateTime = LocalDateTime.now()
)
