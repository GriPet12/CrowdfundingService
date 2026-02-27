package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {
    fun save(category: Category): Category
    fun findByCategoryId(id: Long?): Optional<Category?>
    fun deleteByCategoryId(id: Long)
    fun findByCategoryName(categoryName: String): Optional<Category>
}