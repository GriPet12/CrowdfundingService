package com.gripet12.crowdfundingService.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "analytics_logs")
data class AnalyticsLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val logId: Long = 0,

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    val user: User? = null,

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = true)
    val project: Project? = null,

    @ManyToOne
    @JoinColumn(name = "target_user_id", nullable = true)
    val targetUser: User? = null,

    val actionType: String,

    @Column(columnDefinition = "timestamp without time zone")
    val actionTime: LocalDateTime = LocalDateTime.now(),

    val ipAddress: String? = null
)
