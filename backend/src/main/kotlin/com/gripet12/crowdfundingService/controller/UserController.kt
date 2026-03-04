package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.dto.UpdateUserRequest
import com.gripet12.crowdfundingService.dto.UserDto
import com.gripet12.crowdfundingService.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(private val userService: UserService) {

    @GetMapping("/me")
    fun getCurrentUser(): ResponseEntity<UserDto> =
        ResponseEntity.ok(userService.getCurrentUser())

    @PutMapping("/me")
    fun updateCurrentUser(@RequestBody request: UpdateUserRequest): ResponseEntity<UserDto> =
        ResponseEntity.ok(userService.updateCurrentUser(request))


    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Long) = userService.getUserById(id)
}