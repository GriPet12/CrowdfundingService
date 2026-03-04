package com.gripet12.crowdfundingService.controller

import com.gripet12.crowdfundingService.model.Category
import com.gripet12.crowdfundingService.repository.CategoryRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/categories")
class CategoryController(
    private val categoryRepository: CategoryRepository
) {
    @GetMapping
    fun getAll(): ResponseEntity<List<Category>> =
        ResponseEntity.ok(categoryRepository.findAll())
}
