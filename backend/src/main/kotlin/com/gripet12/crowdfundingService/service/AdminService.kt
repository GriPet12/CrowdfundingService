package com.gripet12.crowdfundingService.service

import com.gripet12.crowdfundingService.dto.*
import com.gripet12.crowdfundingService.model.Category
import com.gripet12.crowdfundingService.model.enums.Role
import com.gripet12.crowdfundingService.repository.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val postRepository: PostRepository,
    private val categoryRepository: CategoryRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val donateRepository: DonateRepository
) {

    @Transactional(readOnly = true)
    fun getUsers(
        search: String?, role: String?, status: String?,
        page: Int, size: Int, sortBy: String, sortDir: String
    ): Page<AdminUserDto> {
        val direction = if (sortDir.equals("desc", ignoreCase = true)) Sort.Direction.DESC else Sort.Direction.ASC
        val sortField = when (sortBy) {
            "id"        -> "userId"
            "username"  -> "username"
            "email"     -> "email"
            "createdAt" -> "createdAt"
            else        -> "userId"
        }
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortField))
        val filterBanned: Int = when (status?.lowercase()) {
            "banned" -> 2
            "active" -> 1
            else     -> 0
        }

        val filterRole: Int = when (role?.uppercase()) {
            "ADMIN" -> 2
            "USER"  -> 1
            else    -> 0
        }
        return userRepository.findByFilters(
            search = search?.takeIf { it.isNotBlank() },
            filterBanned = filterBanned,
            filterRole = filterRole,
            pageable = pageable
        ).map { u ->
            AdminUserDto(
                id = u.userId,
                username = u.username,
                email = u.email,
                role = if (u.roles.contains(Role.ROLE_ADMIN)) "ADMIN" else "USER",
                banned = u.banned,
                imageId = u.image?.id,
                createdAt = u.createdAt
            )
        }
    }

    @Transactional
    fun banUser(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow { NoSuchElementException("User not found") }
        user.banned = true
        userRepository.save(user)

        projectRepository.findByCreatorUserId(userId).forEach { p ->
            if (!p.banned) {
                p.banned = true
                p.bannedWithUser = true
                projectRepository.save(p)
            }
        }

        postRepository.findAllByMasterId(userId).forEach { p ->
            if (!p.banned) {
                p.banned = true
                p.bannedWithUser = true
                postRepository.save(p)
            }
        }
    }

    @Transactional
    fun unbanUser(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow { NoSuchElementException("User not found") }
        user.banned = false
        userRepository.save(user)

        projectRepository.findByCreatorUserId(userId).forEach { p ->
            if (p.bannedWithUser) {
                p.banned = false
                p.bannedWithUser = false
                projectRepository.save(p)
            }
        }
        postRepository.findAllByMasterId(userId).forEach { p ->
            if (p.bannedWithUser) {
                p.banned = false
                p.bannedWithUser = false
                postRepository.save(p)
            }
        }
    }

    @Transactional
    fun cancelUserSubscriptions(userId: Long) {
        val subs = subscriptionRepository.findBySubscrberUserIdAndActiveTrue(userId)
        subs.forEach { it.active = false }
        subscriptionRepository.saveAll(subs)
    }

    @Transactional
    fun makeAdmin(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow { NoSuchElementException("User not found") }
        user.roles.add(Role.ROLE_ADMIN)
        userRepository.save(user)
    }

    @Transactional
    fun removeAdmin(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow { NoSuchElementException("User not found") }
        user.roles.remove(Role.ROLE_ADMIN)
        userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun getProjects(
        search: String?, categoryId: Long?, showBanned: Boolean,
        page: Int, size: Int, sortBy: String, sortDir: String
    ): Page<AdminProjectDto> {
        val direction = if (sortDir.equals("desc", ignoreCase = true)) Sort.Direction.DESC else Sort.Direction.ASC
        val sortField = when (sortBy) {
            "title"        -> "title"
            "goalAmount"   -> "goalAmount"
            "raisedAmount" -> "collectedAmount"
            else           -> "projectId"
        }
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortField))
        return projectRepository.findByFilters(
            search = search?.takeIf { it.isNotBlank() },
            categoryId = categoryId,
            filterBanned = if (showBanned) 2 else 0,
            pageable = pageable
        ).map { p ->
            AdminProjectDto(
                projectId = p.projectId,
                title = p.title,
                creatorName = p.creator.username,
                category = p.categories.firstOrNull()?.categoryName,
                goalAmount = p.goalAmount,
                raisedAmount = p.collectedAmount,
                status = p.status,
                banned = p.banned
            )
        }
    }

    @Transactional
    fun banProject(projectId: Long) {
        val project = projectRepository.findById(projectId).orElseThrow { NoSuchElementException("Project not found") }
        project.banned = true
        project.bannedWithUser = false
        projectRepository.save(project)
    }

    @Transactional
    fun unbanProject(projectId: Long) {
        val project = projectRepository.findById(projectId).orElseThrow { NoSuchElementException("Project not found") }
        project.banned = false
        project.bannedWithUser = false
        projectRepository.save(project)
    }

    @Transactional
    fun deleteProject(projectId: Long) {
        val project = projectRepository.findById(projectId)
            .orElseThrow { NoSuchElementException("Project not found") }
        if (project.collectedAmount > BigDecimal.ZERO) {
            project.status = "CANCELLED"
            projectRepository.save(project)
        } else {
            projectRepository.deleteById(projectId)
        }
    }

    @Transactional(readOnly = true)
    fun getPosts(search: String?, showBanned: Boolean, page: Int, size: Int): Page<AdminPostDto> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return postRepository.findByFilters(
            search = search?.takeIf { it.isNotBlank() },
            filterBanned = if (showBanned) 2 else 0,
            pageable = pageable
        ).map { p ->
            AdminPostDto(
                postId = p.postId,
                title = p.title,
                authorId = p.masterId,
                authorName = userRepository.findById(p.masterId).map { it.username }.orElse("—"),
                requiredTierId = p.requiredTier?.tierId,
                banned = p.banned,
                createdAt = p.createdAt
            )
        }
    }

    @Transactional
    fun banPost(postId: Long) {
        val post = postRepository.findById(postId).orElseThrow { NoSuchElementException("Post not found") }
        post.banned = true
        post.bannedWithUser = false
        postRepository.save(post)
    }

    @Transactional
    fun unbanPost(postId: Long) {
        val post = postRepository.findById(postId).orElseThrow { NoSuchElementException("Post not found") }
        post.banned = false
        post.bannedWithUser = false
        postRepository.save(post)
    }

    @Transactional
    fun deletePost(postId: Long) {
        postRepository.findById(postId).orElseThrow { NoSuchElementException("Post not found") }
        postRepository.deleteById(postId)
    }

    @Transactional(readOnly = true)
    fun getCategories(): List<CategoryResponseDto> =
        categoryRepository.findAll().map { it.toDto() }

    @Transactional
    fun createCategory(req: CategoryRequestDto): CategoryResponseDto {
        val category = Category(categoryId = 0, categoryName = req.name, description = req.description ?: "")
        return categoryRepository.save(category).toDto()
    }

    @Transactional
    fun updateCategory(id: Long, req: CategoryRequestDto): CategoryResponseDto {
        val category = categoryRepository.findById(id).orElseThrow { NoSuchElementException("Category not found") }
        return categoryRepository.save(category.copy(categoryName = req.name, description = req.description ?: category.description)).toDto()
    }

    @Transactional
    fun deleteCategory(id: Long) {
        categoryRepository.findById(id).orElseThrow { NoSuchElementException("Category not found") }
        categoryRepository.deleteById(id)
    }

    private fun Category.toDto() = CategoryResponseDto(id = categoryId, name = categoryName, description = description)

    @Transactional(readOnly = true)
    fun getTransactions(
        type: String?,
        search: String?,
        from: LocalDate?,
        to: LocalDate?,
        page: Int,
        size: Int
    ): Page<TransactionDto> {
        val normalizedType = type?.uppercase()

        if (normalizedType == "SUBSCRIPTION") {
            val subPageable = PageRequest.of(page, size)
            return subscriptionRepository.findAllScalar(subPageable).map { row ->
                TransactionDto(
                    id        = row[0] as? Long,
                    type      = "SUBSCRIPTION",
                    fromUser  = row[1] as? String,
                    toUser    = row[2] as? String,
                    amount    = row[3] as BigDecimal,
                    status    = (row[4] as? String) ?: "AUTO",
                    createdAt = null
                )
            }
        }

        val pageable = PageRequest.of(page, size)
        return donateRepository.findByFilters(
            search = search?.takeIf { it.isNotBlank() },
            from = from,
            to = to,
            pageable = pageable
        ).map { row ->
            TransactionDto(
                id        = row[0] as? Long,
                type      = "DONATION",
                fromUser  = row[1] as? String ?: "Анонім",
                toUser    = (row[2] as? String) ?: (row[3] as? String),
                amount    = row[4] as BigDecimal,
                status    = (row[5] as? String) ?: "PENDING",
                createdAt = (row[6] as? java.sql.Timestamp)?.toLocalDateTime()
            )
        }
    }

    @Transactional(readOnly = true)
    fun getTransactionSummary(): TransactionSummaryDto {
        val totalDonations      = donateRepository.sumAllApprovedDonations()
        val donationsCount      = donateRepository.countAllApprovedDonations()
        val totalSubscriptions  = subscriptionRepository.sumAllApprovedSubscriptions()
        val subscriptionsCount  = subscriptionRepository.countAllApprovedSubscriptions()

        return TransactionSummaryDto(
            totalDonations     = totalDonations,
            donationsCount     = donationsCount,
            totalWithdrawals   = BigDecimal.ZERO,
            withdrawalsCount   = 0L,
            totalSubscriptions = totalSubscriptions,
            subscriptionsCount = subscriptionsCount,
            totalVolume        = totalDonations + totalSubscriptions,
            totalCount         = donationsCount + subscriptionsCount
        )
    }
}
