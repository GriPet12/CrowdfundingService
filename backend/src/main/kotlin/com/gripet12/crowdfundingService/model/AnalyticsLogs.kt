package com.gripet12.crowdfundingService.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import lombok.AllArgsConstructor
import lombok.NoArgsConstructor
import java.security.Timestamp

@Entity
@Table(name = "analytics_logs")
@AllArgsConstructor
@NoArgsConstructor
data class AnalyticsLogs(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val logId: Long,

    @ManyToOne
    val user: User,

    @ManyToOne
    val project: Project,

    val actionType: String,

    val actionTime: Timestamp,

    val ipAddress: String
)
