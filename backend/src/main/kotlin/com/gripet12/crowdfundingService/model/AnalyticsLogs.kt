package com.gripet12.crowdfundingService.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "analytics_logs")
data class AnalyticsLogs(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val logId: Long,

    @ManyToOne
    val user: User,

    @ManyToOne
    val project: Project,

    val actionType: String,

    @Column(columnDefinition = "timestamp without time zone")
    val actionTime: LocalDateTime,

    val ipAddress: String
)
