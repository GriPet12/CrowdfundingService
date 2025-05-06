package com.gripet12.crowdfundingService.repository

import com.gripet12.crowdfundingService.model.Image
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ImageRepository : JpaRepository<Image, Long> {
    fun save(image: Image): Image
    override fun findById(id: Long): Optional<Image?>
    override fun deleteById(id: Long)
}