package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.*
import com.gripet12.crowdfundingService.service.AdminService
import com.gripet12.crowdfundingService.service.BalanceService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

private fun <T> Page<T>.toDto() = PageResponseDto(
    content = content,
    totalElements = totalElements,
    totalPages = totalPages,
    currentPage = number,
    size = size
)

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
class AdminController(
    private val adminService: AdminService,
    private val balanceService: BalanceService
) {

    @GetMapping("/users")
    fun getUsers(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) role: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "id") sort: String
    ): ResponseEntity<PageResponseDto<AdminUserDto>> {
        val parts = sort.split(",")
        return ResponseEntity.ok(
            adminService.getUsers(
                search, role, status, page, size,
                parts.getOrElse(0) { "id" },
                parts.getOrElse(1) { "asc" }
            ).toDto()
        )
    }

    @PostMapping("/users/{id}/ban")
    fun banUser(@PathVariable id: Long): ResponseEntity<Void> {
        adminService.banUser(id)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/users/{id}/unban")
    fun unbanUser(@PathVariable id: Long): ResponseEntity<Void> {
        adminService.unbanUser(id)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/users/{id}/cancel-subscriptions")
    fun cancelSubscriptions(@PathVariable id: Long): ResponseEntity<Void> {
        adminService.cancelUserSubscriptions(id)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/users/{id}/make-admin")
    fun makeAdmin(@PathVariable id: Long): ResponseEntity<Void> {
        adminService.makeAdmin(id)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/users/{id}/remove-admin")
    fun removeAdmin(@PathVariable id: Long): ResponseEntity<Void> {
        adminService.removeAdmin(id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/projects/pending")
    fun getPendingProjects(
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponseDto<AdminProjectDto>> =
        ResponseEntity.ok(adminService.getPendingProjects(search, page, size).toDto())

    @PostMapping("/projects/{id}/approve")
    fun approveProject(@PathVariable id: Long): ResponseEntity<Void> {
        adminService.approveProject(id)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/projects/{id}/reject")
    fun rejectProject(@PathVariable id: Long): ResponseEntity<Void> {
        adminService.rejectProject(id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/projects")
    fun getProjects(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) category: Long?,
        @RequestParam(defaultValue = "false") showBanned: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt,desc") sort: String
    ): ResponseEntity<PageResponseDto<AdminProjectDto>> {
        val parts = sort.split(",")
        return ResponseEntity.ok(
            adminService.getProjects(
                search, category, showBanned, page, size,
                parts.getOrElse(0) { "createdAt" },
                parts.getOrElse(1) { "desc" }
            ).toDto()
        )
    }

    @GetMapping("/projects/{id}/transactions")
    fun getProjectTransactions(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponseDto<TransactionDto>> =
        ResponseEntity.ok(adminService.getProjectTransactions(id, page, size).toDto())

    @PostMapping("/projects/{id}/ban")
    fun banProject(@PathVariable id: Long): ResponseEntity<Void> {
        adminService.banProject(id)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/projects/{id}/unban")
    fun unbanProject(@PathVariable id: Long): ResponseEntity<Void> {
        adminService.unbanProject(id)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/projects/{id}")
    fun deleteProject(@PathVariable id: Long): ResponseEntity<Void> {
        adminService.deleteProject(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/posts")
    fun getPosts(
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "false") showBanned: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponseDto<AdminPostDto>> =
        ResponseEntity.ok(adminService.getPosts(search, showBanned, page, size).toDto())

    @PostMapping("/posts/{id}/ban")
    fun banPost(@PathVariable id: Long): ResponseEntity<Void> {
        adminService.banPost(id)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/posts/{id}/unban")
    fun unbanPost(@PathVariable id: Long): ResponseEntity<Void> {
        adminService.unbanPost(id)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/posts/{id}")
    fun deletePost(@PathVariable id: Long): ResponseEntity<Void> {
        adminService.deletePost(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/categories")
    fun getCategories(): ResponseEntity<List<CategoryResponseDto>> = ResponseEntity.ok(adminService.getCategories())

    @PostMapping("/categories")
    fun createCategory(@RequestBody req: CategoryRequestDto): ResponseEntity<CategoryResponseDto> = ResponseEntity.ok(adminService.createCategory(req))

    @PutMapping("/categories/{id}")
    fun updateCategory(@PathVariable id: Long, @RequestBody req: CategoryRequestDto): ResponseEntity<CategoryResponseDto> = ResponseEntity.ok(adminService.updateCategory(id, req))

    @DeleteMapping("/categories/{id}")
    fun deleteCategory(@PathVariable id: Long): ResponseEntity<Void> { adminService.deleteCategory(id); return ResponseEntity.noContent().build() }

    @GetMapping("/transactions")
    fun getTransactions(
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponseDto<TransactionDto>> =
        ResponseEntity.ok(adminService.getTransactions(type, search, from, to, page, size).toDto())

    @GetMapping("/transactions/summary")
    fun getTransactionSummary(): ResponseEntity<TransactionSummaryDto> = ResponseEntity.ok(adminService.getTransactionSummary())

    @GetMapping("/withdrawals")
    fun getWithdrawals(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponseDto<TransactionDto>> =
        ResponseEntity.ok(adminService.getWithdrawals(search, status, from, to, page, size).toDto())

    @PostMapping("/withdrawals/{id}/complete")
    fun completeWithdrawal(@PathVariable id: Long): ResponseEntity<Void> {
        balanceService.completeWithdrawalAdmin(id)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/withdrawals/{id}/reject")
    fun rejectWithdrawal(
        @PathVariable id: Long,
        @RequestBody(required = false) body: Map<String, String>?
    ): ResponseEntity<Void> {
        balanceService.rejectWithdrawalAdmin(id, body?.get("reason"))
        return ResponseEntity.ok().build()
    }
}
