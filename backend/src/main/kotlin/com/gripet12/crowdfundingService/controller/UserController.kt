package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.UserDto
import com.gripet12.crowdfundingService.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(private val userService: UserService) {

    @GetMapping("/me")
    fun getCurrentUser(): ResponseEntity<UserDto> {
        val userDto = userService.getCurrentUser()
        return ResponseEntity.ok(userDto)
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    fun adminEndpoint(): ResponseEntity<String> {
        return ResponseEntity.ok("This is an admin protected endpoint")
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('ROLE_USER')")
    fun userEndpoint(): ResponseEntity<String> {
        return ResponseEntity.ok("This is a user protected endpoint")
    }
}