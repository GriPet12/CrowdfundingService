package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.CreatorStatsDto
import com.gripet12.crowdfundingService.dto.DayActivityDto
import com.gripet12.crowdfundingService.dto.PieSliceDto
import com.gripet12.crowdfundingService.dto.TopPostDto
import com.gripet12.crowdfundingService.model.AnalyticsLog
import com.gripet12.crowdfundingService.model.User
import com.gripet12.crowdfundingService.repository.AnalyticsLogRepository
import com.gripet12.crowdfundingService.repository.CommentRepository
import com.gripet12.crowdfundingService.repository.DonateRepository
import com.gripet12.crowdfundingService.repository.PostLikeRepository
import com.gripet12.crowdfundingService.repository.PostRepository
import com.gripet12.crowdfundingService.repository.ProjectRepository
import com.gripet12.crowdfundingService.repository.SubscriptionRepository
import com.gripet12.crowdfundingService.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class AnalyticsLogService(
    private val analyticsLogRepository: AnalyticsLogRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val postRepository: PostRepository,
    private val postLikeRepository: PostLikeRepository,
    private val commentRepository: CommentRepository,
    private val donateRepository: DonateRepository
) {
    private val CLICK_DEDUP_MINUTES = 5L
    private val VIEW_DEDUP_MINUTES  = 10L

    private fun currentUserIdOrNull(): Long? {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated || auth.name == "anonymousUser") return null
        return userRepository.findByUsername(auth.name).orElse(null)?.userId
    }

    private fun resolveIp(): String? {
        val attrs = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes ?: return null
        val request: HttpServletRequest = attrs.request
        val forwarded = request.getHeader("X-Forwarded-For")
        return if (!forwarded.isNullOrBlank()) forwarded.split(",").first().trim()
        else request.remoteAddr
    }

    private fun saveProjectLogIfNotDuplicate(user: User?, actionType: String, projectId: Long, dedupMinutes: Long) {
        val since = LocalDateTime.now().minusMinutes(dedupMinutes)
        if (user != null) {
            if (analyticsLogRepository.countRecentDuplicateOnProject(user, actionType, projectId, since) > 0) return
        } else {
            val ip = resolveIp() ?: return
            if (analyticsLogRepository.countRecentDuplicateOnProjectByIp(ip, actionType, projectId, since) > 0) return
        }
        val project = projectRepository.findById(projectId).orElse(null) ?: return
        analyticsLogRepository.save(
            AnalyticsLog(
                user       = user,
                project    = project,
                targetUser = project.creator,
                actionType = actionType,
                actionTime = LocalDateTime.now(),
                ipAddress  = resolveIp()
            )
        )
    }

    @Transactional
    fun clickOnProject(projectId: Long) {
        val user = currentUserIdOrNull()?.let { userRepository.getReferenceById(it) }
        saveProjectLogIfNotDuplicate(user, "CLICK", projectId, CLICK_DEDUP_MINUTES)
    }

    @Transactional
    fun viewOnProject(projectId: Long) {
        val user = currentUserIdOrNull()?.let { userRepository.getReferenceById(it) }
        saveProjectLogIfNotDuplicate(user, "VIEW", projectId, VIEW_DEDUP_MINUTES)
    }

    @Transactional
    fun donateOnProject(projectId: Long) {
        val userId = currentUserIdOrNull() ?: return
        val project = projectRepository.findById(projectId).orElse(null) ?: return
        analyticsLogRepository.save(
            AnalyticsLog(
                user       = userRepository.getReferenceById(userId),
                project    = project,
                targetUser = project.creator,
                actionType = "DONATE",
                actionTime = LocalDateTime.now(),
                ipAddress  = resolveIp()
            )
        )
    }

    private fun saveCreatorLogIfNotDuplicate(user: User?, actionType: String, creatorId: Long, dedupMinutes: Long) {
        val since = LocalDateTime.now().minusMinutes(dedupMinutes)
        if (user != null) {
            if (analyticsLogRepository.countRecentDuplicateOnUser(user, actionType, creatorId, since) > 0) return
        } else {
            val ip = resolveIp() ?: return
            if (analyticsLogRepository.countRecentDuplicateOnUserByIp(ip, actionType, creatorId, since) > 0) return
        }
        analyticsLogRepository.save(
            AnalyticsLog(
                user       = user,
                project    = null,
                targetUser = userRepository.getReferenceById(creatorId),
                actionType = actionType,
                actionTime = LocalDateTime.now(),
                ipAddress  = resolveIp()
            )
        )
    }

    @Transactional
    fun clickOnCreator(creatorId: Long) {
        val user = currentUserIdOrNull()?.let { userRepository.getReferenceById(it) }
        saveCreatorLogIfNotDuplicate(user, "CLICK", creatorId, CLICK_DEDUP_MINUTES)
    }

    @Transactional
    fun viewOnCreator(creatorId: Long) {
        val user = currentUserIdOrNull()?.let { userRepository.getReferenceById(it) }
        saveCreatorLogIfNotDuplicate(user, "VIEW", creatorId, VIEW_DEDUP_MINUTES)
    }

    @Transactional
    fun subscribeOnUser(creatorId: Long) {
        val userId = currentUserIdOrNull() ?: return
        analyticsLogRepository.save(
            AnalyticsLog(
                user       = userRepository.getReferenceById(userId),
                project    = null,
                targetUser = userRepository.getReferenceById(creatorId),
                actionType = "SUBSCRIBE",
                actionTime = LocalDateTime.now(),
                ipAddress  = resolveIp()
            )
        )
    }

    @Transactional
    fun donateOnCreator(creatorId: Long) {
        val userId = currentUserIdOrNull() ?: return
        analyticsLogRepository.save(
            AnalyticsLog(
                user       = userRepository.getReferenceById(userId),
                project    = null,
                targetUser = userRepository.getReferenceById(creatorId),
                actionType = "DONATE",
                actionTime = LocalDateTime.now(),
                ipAddress  = resolveIp()
            )
        )
    }

    @Transactional
    fun followOnProject(projectId: Long) {
        val userId = currentUserIdOrNull() ?: return
        saveProjectLogIfNotDuplicate(userRepository.getReferenceById(userId), "FOLLOW", projectId, CLICK_DEDUP_MINUTES)
    }

    @Transactional
    fun followOnCreator(creatorId: Long) {
        val userId = currentUserIdOrNull() ?: return
        saveCreatorLogIfNotDuplicate(userRepository.getReferenceById(userId), "FOLLOW", creatorId, CLICK_DEDUP_MINUTES)
    }


    @Transactional(readOnly = true)
    fun getCreatorDashboard(creatorId: Long): CreatorStatsDto {
        val since30 = LocalDateTime.now().minusDays(30)
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val totalViews       = analyticsLogRepository.countViewsOnCreator(creatorId, LocalDateTime.now().minusDays(365))
        val totalFollowers   = analyticsLogRepository.countFollowsOnCreator(creatorId)
        val totalSubscribers = subscriptionRepository.countByCreatorUserIdAndActiveTrue(creatorId)
        val posts            = postRepository.findByMasterIdOrderByPostIdDesc(creatorId)
        val totalPosts       = posts.size.toLong()
        val totalLikes       = postLikeRepository.countLikesByCreator(creatorId)
        val totalComments    = posts.sumOf { commentRepository.countByPostPostId(it.postId) }
        val projectDonSum    = donateRepository.sumProjectDonationsToCreator(creatorId).toDouble()
        val donatCount       = donateRepository.countProjectDonationsToCreator(creatorId)
        val totalDonationsAmount = projectDonSum
        val totalDonationsCount  = donatCount

        val rawActivity = analyticsLogRepository.activityByDay(creatorId, since30)

        val dayMap = mutableMapOf<String, MutableMap<String, Long>>()
        for (row in rawActivity) {
            val date       = row[0].toString().substring(0, 10)
            val actionType = row[1] as String
            val count      = (row[2] as Number).toLong()
            dayMap.getOrPut(date) { mutableMapOf() }[actionType] = count
        }

        val activityByDay = (0L..29L).map { daysAgo ->
            val d = LocalDate.now().minusDays(29 - daysAgo).format(fmt)
            val m = dayMap[d] ?: emptyMap()
            DayActivityDto(
                date          = d,
                views         = m["VIEW"] ?: 0,
                follows       = m["FOLLOW"] ?: 0,
                donates       = m["DONATE"] ?: 0,
                subscriptions = m["SUBSCRIBE"] ?: 0
            )
        }

        val donationsByType = listOf(
            PieSliceDto("Через проекти", projectDonSum)
        ).filter { it.value > 0 }
            .ifEmpty { listOf(PieSliceDto("Донати", 0.0)) }

        val topPosts = posts.map { p ->
            TopPostDto(
                postId   = p.postId,
                title    = p.title.ifBlank { "Без назви" },
                likes    = postLikeRepository.countByPostPostId(p.postId),
                comments = commentRepository.countByPostPostId(p.postId)
            )
        }.sortedByDescending { it.likes }.take(5)

        return CreatorStatsDto(
            totalViews           = totalViews,
            totalFollowers       = totalFollowers,
            totalSubscribers     = totalSubscribers,
            totalPosts           = totalPosts,
            totalLikes           = totalLikes,
            totalComments        = totalComments,
            totalDonationsAmount = totalDonationsAmount,
            totalDonationsCount  = totalDonationsCount,
            activityByDay        = activityByDay,
            donationsByType      = donationsByType,
            topPosts             = topPosts
        )
    }
}
