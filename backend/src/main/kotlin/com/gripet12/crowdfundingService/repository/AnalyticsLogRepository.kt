package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.AnalyticsLog
import com.gripet12.crowdfundingService.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface AnalyticsLogRepository : JpaRepository<AnalyticsLog, Long> {
    fun getAnalyticsLogsByUser(user: User): MutableList<AnalyticsLog>

    @Query("""
        SELECT COUNT(a) FROM AnalyticsLog a
        WHERE a.user = :user
          AND a.actionType = :actionType
          AND a.project IS NOT NULL
          AND a.project.projectId = :projectId
          AND a.actionTime > :since
    """)
    fun countRecentDuplicateOnProject(user: User, actionType: String, projectId: Long, since: LocalDateTime): Long

    @Query("""
        SELECT COUNT(a) FROM AnalyticsLog a
        WHERE a.user IS NULL
          AND a.ipAddress = :ip
          AND a.actionType = :actionType
          AND a.project IS NOT NULL
          AND a.project.projectId = :projectId
          AND a.actionTime > :since
    """)
    fun countRecentDuplicateOnProjectByIp(ip: String, actionType: String, projectId: Long, since: LocalDateTime): Long

    @Query("""
        SELECT COUNT(a) FROM AnalyticsLog a
        WHERE a.user = :user
          AND a.actionType = :actionType
          AND a.targetUser IS NOT NULL
          AND a.targetUser.userId = :targetUserId
          AND a.actionTime > :since
    """)
    fun countRecentDuplicateOnUser(user: User, actionType: String, targetUserId: Long, since: LocalDateTime): Long

    @Query("""
        SELECT COUNT(a) FROM AnalyticsLog a
        WHERE a.user IS NULL
          AND a.ipAddress = :ip
          AND a.actionType = :actionType
          AND a.targetUser IS NOT NULL
          AND a.targetUser.userId = :targetUserId
          AND a.actionTime > :since
    """)
    fun countRecentDuplicateOnUserByIp(ip: String, actionType: String, targetUserId: Long, since: LocalDateTime): Long

    @Query("""
        SELECT COUNT(a) FROM AnalyticsLog a
        WHERE a.actionType = 'VIEW'
          AND a.actionTime >= :since
          AND (a.targetUser.userId = :creatorId
               OR (a.project IS NOT NULL AND a.project.creator.userId = :creatorId))
    """)
    fun countViewsOnCreator(creatorId: Long, since: LocalDateTime): Long

    @Query("""
        SELECT COUNT(a) FROM AnalyticsLog a
        WHERE a.actionType = 'FOLLOW'
          AND (a.targetUser.userId = :creatorId
               OR (a.project IS NOT NULL AND a.project.creator.userId = :creatorId))
    """)
    fun countFollowsOnCreator(creatorId: Long): Long


    @Query("""
        SELECT CAST(a.actionTime AS date), a.actionType, COUNT(a)
        FROM AnalyticsLog a
        WHERE a.actionTime >= :since
          AND (a.targetUser.userId = :creatorId
               OR (a.project IS NOT NULL AND a.project.creator.userId = :creatorId))
        GROUP BY CAST(a.actionTime AS date), a.actionType
        ORDER BY CAST(a.actionTime AS date)
    """)
    fun activityByDay(creatorId: Long, since: LocalDateTime): List<Array<Any>>
}